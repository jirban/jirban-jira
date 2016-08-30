//a simple service
import {Injectable} from "@angular/core";
import {Headers, Http, Response} from "@angular/http";
import {Observable, Subject} from "rxjs/Rx";
import "rxjs/add/operator/map";
import {BoardData} from "../data/board/boardData";
import {RestUrlUtil} from "../common/RestUrlUtil";
import {IssueData} from "../data/board/issueData";
import {BoardProject} from "../data/board/project";
import {ProgressErrorService} from "./progressErrorService";
import Timer = NodeJS.Timer;

@Injectable()
export class IssuesService {
    private _bigTimeout:number = 60000;
    static _smallTimeout:number = 15000;
    private _http : Http;
    private _progressError:ProgressErrorService;
    private _boardData:BoardData;


    private _pollFailureCount:number = 0;
    private _maxPollFailureCount = 3;

    private _currentTimeout:Timer;

    private _destroyed:boolean = false;

    private _defaultPollInterval:number = 30000;

    private _visible:boolean = true;

    constructor(http:Http, progressError:ProgressErrorService, boardData:BoardData) {
        this._http = http;
        this._progressError = progressError;
        this._boardData = boardData;
        console.log("Create issues service");
    }

    set visible(value:boolean) {
        this._visible = value;
        if (!this._visible) {
            let restartPolling:boolean = this._pollFailureCount >= this._maxPollFailureCount;
            restartPolling = restartPolling && !this._progressError.error;
            this._pollFailureCount = 0;
            console.log("Blur - restarting polling: " + restartPolling);
            if (restartPolling) {
                this.pollIssues();
            }
        }
    }

    destroy():void {
        this.clearPollTimeout();
        this._destroyed = true;
    }

    toggleBacklog() {
        this._progressError.startProgress(true);
        this.getIssuesData(this._boardData.code, this._boardData.showBacklog).subscribe(
            data => {
                this._boardData.processChanges(data);
            },
            err => {
                this._progressError.setError(err);
            },
            () => {
                this._progressError.finishProgress();
            }
        );
    }

    pollBoard(boardData:BoardData) : Observable<Response> {
        let url:string = 'rest/jirban/1.0/issues/' + boardData.code + "/updates/" + boardData.view;
        if (boardData.showBacklog) {
            url += "?backlog=" + true;
        }
        let path:string = RestUrlUtil.caclulateRestUrl(url);
        console.log("Poll " + path);
        return this._http.get(path)
            .timeout(this._bigTimeout, "The server did not respond in a timely manner for GET " + path)
            .map(res => (<Response>res).json());
    }

    moveIssue(boardData:BoardData, issue:IssueData, toBoardState:string):Observable<any>{
        let mi:MoveIssueAction = new MoveIssueAction(this, this._http, boardData, issue, toBoardState);
        return mi.execute();
    }

    commentOnIssue(boardData:BoardData, issue:IssueData, comment:string):Observable<any>{
        let url:string = boardData.jiraUrl + '/rest/api/2/issue/' + issue.key + "/comment";
        let headers:Headers = new Headers();
        headers.append("Content-Type", "application/json");
        headers.append("Accept", "application/json");

        let payload:any = {body: comment};

        return this._http.post(url, JSON.stringify(payload), {headers : headers})
            .timeout(IssuesService._smallTimeout, "The server did not respond in a timely manner for POST " + url);
    }

    loadHelpTexts(board:string, boardData:BoardData) : void {
        let url = 'rest/jirban/1.0/issues/' + board + "/help";
        let path:string = RestUrlUtil.caclulateRestUrl(url);
        this._http.get(path)
            .timeout(this._bigTimeout, "The server did not respond in a timely manner for GET " + path)
            .map(res => (<Response>res).json()).subscribe(
                data => {
                    console.log("Got help texts " + JSON.stringify(data));
                    let content:any = data;
                    boardData.helpTexts = content;
                },
                err => {
                    console.log("Error loading help texts " + err);
                }
            );
    }

    populateIssues(board:string, issueDataLoaded:()=>void) {
        this._progressError.startProgress(true);
        this.getIssuesData(board, this._boardData.showBacklog).subscribe(
            data => {
                this._boardData.deserialize(board, data);
                issueDataLoaded();
            },
            err => {
                this._progressError.setError(err);
            },
            () => {
                this.pollIssues();
                this._progressError.finishProgress();
            }
        );
    }

    performRerank(issue:IssueData, beforeKey:string, afterKey:string):Observable<any> {
        let observableWrapper:ObservableWrapper = new ObservableWrapper();
        let path: string = this._boardData.jiraUrl + '/rest/greenhopper/1.0/rank';
        let payload: any = {
            customFieldId: this._boardData.rankCustomFieldId,
            issueKeys: [issue.key],
        };
        if (beforeKey) {
            payload.rankBeforeKey = beforeKey;
        }
        if (afterKey) {
            payload.rankAfterKey = afterKey;
        }

        let headers: Headers = new Headers();
        headers.append("Content-Type", "application/json");
        headers.append("Accept", "application/json");

        console.log("PUT " + JSON.stringify(payload) + " to " + path);
        this._http.put(path, JSON.stringify(payload), {headers: headers})
            .timeout(IssuesService._smallTimeout, "The server did not respond in a timely manner for POST " + path)
            .map(res => (<Response>res).json())
            .subscribe(
                data => {
                    this.refreshBoardFollowingIssueChange(observableWrapper);
                },
                error => observableWrapper.setError(error)
            );
        return observableWrapper.observable;
    }

    private getIssuesData(board:string, backlog:boolean) : Observable<Response> {
        let url = 'rest/jirban/1.0/issues/' + board;
        if (backlog) {
            url += "?backlog=" + true;
        }
        let path:string = RestUrlUtil.caclulateRestUrl(url);
        console.log("Populate " + path);
        return this._http.get(path)
            .timeout(this._bigTimeout, "The server did not respond in a timely manner for GET " + path)
            .map(res => (<Response>res).json());
    }

    private clearPollTimeout() {
        let timeout:Timer = this._currentTimeout;
        if (timeout) {
            clearTimeout(timeout);
        }
    }

    private pollIssues() {
        this._currentTimeout = setTimeout(()=>{this.doPoll()}, this._defaultPollInterval);
    }

    private doPoll() {
        if (this._destroyed) {
            return;
        }
        if (this._progressError.progress) {
            //Another request is already in progress (probably a move)
            //Just do another poll and return so we don't clash the error codes etc.
            this.pollIssues();
            return;
        }

        let wasInvisible = false;
        if (!this._visible) {
            //We are not the active window so just skip this update
            this.pollIssues();
            return;
        }

        //Don't use the progress monitor for this background task.
        //Simply set the error in it if one happened
        this.pollBoard(this._boardData)
            .subscribe(
                data => {
                    console.log("----> Received changes: " + JSON.stringify(data));
                    this._boardData.processChanges(data);
                    this.pollIssues();
                },
                err => {
                    console.log("FC" + this._pollFailureCount);
                    this._pollFailureCount++;
                    console.log(err);
                    if (this._pollFailureCount < this._maxPollFailureCount) {
                        this._progressError.finishProgress();
                        this.pollIssues();
                    } else {
                        if (err.status === 401) {
                            this._progressError.setError(err);
                        } else {
                            this._progressError.setError("Connection to the board lost.");
                        }
                    }
                },
                () => {
                    this._progressError.finishProgress();
                    this._pollFailureCount = 0;
                }
            );
    }

    //Only for use from within this file
    refreshBoardFollowingIssueChange(observableWrapper:ObservableWrapper) {
        this.pollBoard(this._boardData)
            .subscribe(
                data => {
                    this._boardData.processChanges(data);
                    observableWrapper.done();
                },
                error => observableWrapper.setError(error));
    }
}

class MoveIssueAction {
    private _toOwnState:string;

    private _observableWrapper:ObservableWrapper = new ObservableWrapper();

    constructor(private _issuesService:IssuesService, private _http:Http, private _boardData:BoardData, private _issue:IssueData,
                     private _toBoardState:string) {
        let project:BoardProject = _boardData.boardProjects.forKey(_issue.projectCode);
        this._toOwnState = project.mapBoardStateToOwnState(_toBoardState);
    }

    execute():Observable<any> {
        this.getTransitionsAndPerform();
        return this._observableWrapper.observable;
    }

    getTransitionsAndPerform() {
        let path = this._boardData.jiraUrl + '/rest/api/2/issue/' + this._issue.key + '/transitions';
        console.log("URL " + path);

        let headers:Headers = new Headers();
        headers.append("Accept", "application/json");

        this._http.get(path, {headers : headers})
            .timeout(IssuesService._smallTimeout, "The server did not respond in a timely manner for GET " + path)
            .map(res => (<Response>res).json())
            .subscribe(
                data => this.performStateTransition(data),
                error => this._observableWrapper.setError(error)
            );
    }

    performStateTransition(transitionsValue:any) {
        let transitionId:number = -1;
        let transitions:any = transitionsValue.transitions;
        for (let transition of transitions) {
            if (transition.to) {
                if (transition.to.name === this._toOwnState) {
                    transitionId = transition.id;
                    break;
                }
            }
        }

        if (transitionId == -1) {
            let state:string = this._toBoardState;
            if (this._toOwnState != this._toBoardState) {
                state = state + "(" + this._toOwnState + ")";
            }
            this._observableWrapper.setError({message: "Could not find a valid transition to " + state});
        } else {
            let path = this._boardData.jiraUrl + '/rest/api/2/issue/' + this._issue.key + '/transitions';
            //path = path + "?issueIdOrKey=" + this._issue.key;
            let payload:any = {transition: {id: transitionId}};

            let headers:Headers = new Headers();
            headers.append("Content-Type", "application/json");
            headers.append("Accept", "application/json");

            console.log("post to URL " + path);

            this._http.post(path, JSON.stringify(payload), {headers : headers})
                .timeout(IssuesService._smallTimeout, "The server did not respond in a timely manner for POST " + path)
                //.map(res => (<Response>res).json())
                .subscribe(
                    data => {
                        this._issuesService.refreshBoardFollowingIssueChange(this._observableWrapper);
                    },
                    error => this._observableWrapper.setError(error)
                );
        }
    }
}

class ObservableWrapper {
    private _observable:Subject<any> = new Subject<any>();

    get observable(): Subject<any> {
        return this._observable;
    }

    done() {
        this._observable.next({});
        this._observable.complete();
    }

    setError(error:any) {
        this._observable.error(error);
    }

}



//a simple service
import {Injectable} from 'angular2/core';
import {Headers, Http, Response} from 'angular2/http';
import {Router} from 'angular2/router';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import {BoardData} from "../data/board/boardData";
import {RestUrlUtil} from "../common/RestUrlUtil";
import {IssueData} from "../data/board/issueData";
import {BoardProject} from "../data/board/project";
import {ProgressErrorService} from "./progressErrorService";
import {Observer} from "rxjs/Observer";

@Injectable()
export class IssuesService {
    private bigTimeout:number = 30000;
    static smallTimeout:number = 15000;
    private http : Http;

    constructor(http:Http) {
        this.http = http;
    }

    getIssuesData(board:number) : Observable<Response> {
        let path:string = RestUrlUtil.caclulateRestUrl('rest/jirban/1.0/issues/' + board);
        return this.http.get(path)
            .timeout(this.bigTimeout, "The server did not respond in a timely manner for GET " + path)
            .map(res => (<Response>res).json());
    }

    pollBoard(board:number, view:number) : Observable<Response> {
        let path = RestUrlUtil.caclulateRestUrl('rest/jirban/1.0/issues/' + board + "/updates/" + view);
        return this.http.get(path)
            .timeout(this.bigTimeout, "The server did not respond in a timely manner for GET " + path)
            .map(res => (<Response>res).json());
    }

    moveIssue(boardData:BoardData, issue:IssueData, toBoardState:string, beforeKey:string, afterKey:string):Observable<any>{
        let mi:MoveIssueAction = new MoveIssueAction(this.http, boardData, issue, toBoardState, beforeKey, afterKey);
        return mi.execute();
    }
}

class MoveIssueAction {
    private _toOwnState:string;

    private _changeState;
    private _changeRank;

    private _observable:Observable<any>;
    private _observer:Observer<any>;

    constructor(private _http:Http, private _boardData:BoardData, private _issue:IssueData,
                     private _toBoardState, private _beforeKey:string, private _afterKey:string) {
        let project:BoardProject = _boardData.boardProjects.forKey(_issue.projectCode);
        this._toOwnState = project.mapBoardStateToOwnState(_toBoardState);
        this._changeState = this._toOwnState != this._issue.ownStatus;
        this._changeRank = this._beforeKey || this._afterKey;

        console.log("--- Creating observer");
        this._observable = Observable.create((observer:Observer<any>) => {
            this._observer = observer;
            console.log("---- Created observer");
        })

        this._observable.subscribe(
            data => {console.log("---- data")},
            error => {console.log("---- error " + error)},
            () => {console.log("---- done")}
        );
    }

    execute():Observable<any> {
        if (this._changeState) {
            this.getTransitionsAndPerform();
        } else if (this._changeRank) {
            this.performRerank();
        } else {
            this.done();
        }
        return this._observable;
    }

    getTransitionsAndPerform() {
        let path = this._boardData.jiraUrl + '/rest/api/2/issue/' + this._issue.key + '/transitions';
        console.log("URL " + path);

        let headers:Headers = new Headers();
        headers.append("Accept", "application/json");

        this._http.get(path, {headers : headers})
            .timeout(IssuesService.smallTimeout, "The server did not respond in a timely manner for GET " + path)
            .map(res => (<Response>res).json())
            .subscribe(
                data => this.performStateTransition(data),
                error => this.setError(error)
            );
    }

    performStateTransition(transitionsValue:any) {
        let transitionId:number = -1;
        let transitions:any = transitionsValue.transitions;
        for (let transition of transitions) {
            if (transition.name === this._toOwnState) {
                transitionId = transition.id;
                break;
            }
        }

        if (transitionId == -1) {
            let state:string = this._toBoardState;
            if (this._toOwnState != this._toBoardState) {
                state = state + "(" + this._toOwnState + ")";
            }
            this.setError({msg: "Could not find a valid transition to " + state});
        } else {
            let path = this._boardData.jiraUrl + '/rest/api/2/issue/' + this._issue.key + '/transitions';
            //path = path + "?issueIdOrKey=" + this._issue.key;
            let payload:any = {transition: {id: transitionId}};

            let headers:Headers = new Headers();
            headers.append("Content-Type", "application/json");
            headers.append("Accept", "application/json");

            console.log("post to URL " + path);

            this._http.post(path, JSON.stringify(payload), {headers : headers})
                .timeout(IssuesService.smallTimeout, "The server did not respond in a timely manner for POST " + path)
                //.map(res => (<Response>res).json())
                .subscribe(
                    data => {
                        if (this._changeRank) {
                            this.performRerank();
                        } else {
                            this.done();
                        }
                    },
                    error => this.setError(error)
                );
        }
    }

    performRerank() {
        let path:string = this._boardData.jiraUrl + '/rest/greenhopper/1.0/rank';
        let payload:any =  {
            customFieldId: this._boardData.rankCustomFieldId,
            issueKeys: [this._issue.key],
        };
        if (this._beforeKey) {
            payload.rankBeforeKey = this._beforeKey;
        }
        if (this._afterKey) {
            payload.rankAfterKey = this._afterKey;
        }

        let headers:Headers = new Headers();
        headers.append("Content-Type", "application/json");
        headers.append("Accept", "application/json");

        console.log("PUT " + JSON.stringify(payload) + " to " + path);
        this._http.put(path, JSON.stringify(payload), {headers : headers})
            .timeout(IssuesService.smallTimeout, "The server did not respond in a timely manner for POST " + path)
            .map(res => (<Response>res).json())
            .subscribe(
                data => {
                    this.done();
                },
                error => this.setError(error)
            );
    }

    done() {
        this._observer.next({});
        this._observer.complete();
    }

    private setError(error:any) {
        this._observer.error(error);
    }

}




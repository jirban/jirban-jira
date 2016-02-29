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

@Injectable()
export class IssuesService {
    private router : Router;
    private http : Http;

    //private ws : WebSocket;

    constructor(http:Http, router:Router) {
        this.http = http;
    }

    getIssuesData(board:number) : Observable<Response> {
        let path:string = RestUrlUtil.caclulateRestUrl('rest/issues/' + board);
        return this.http.get(path).map(res => (<Response>res).json());
    }

    pollBoard(board:number, view:number) : Observable<Response> {
        let path = RestUrlUtil.caclulateRestUrl('rest/issues/' + board + "/updates/" + view);
        return this.http.get(path).map(res => (<Response>res).json());
    }

    moveIssue(boardData:BoardData, issue:IssueData, toBoardState:string, beforeKey:string, afterKey:string) {
        let mi:MoveIssueAction = new MoveIssueAction(this.http, boardData, issue, toBoardState, beforeKey, afterKey);
        mi.execute();
    }
}

class MoveIssueAction {
    private _toOwnState:string;

    private _changeState;
    private _changeRank;
    constructor(private _http:Http, private _boardData:BoardData, private _issue:IssueData,
                     private _toBoardState, private _beforeKey:string, private _afterKey:string) {
        let project:BoardProject = _boardData.boardProjects.forKey(_issue.projectCode);
        this._toOwnState = project.mapBoardStateToOwnState(_toBoardState);
        this._changeState = this._toOwnState != this._issue.ownStatus;
        this._changeRank = this._beforeKey || this._afterKey;
    }

    execute() {
        if (this._changeState) {
            this.getTransitionsAndPerform();
        } else if (this._changeRank) {
            this.performRerank();
        }
    }

    getTransitionsAndPerform() {
        let path = this._boardData.jiraUrl + '/rest/api/2/issue/' + this._issue.key + '/transitions';
        console.log("URL " + path);

        let headers:Headers = new Headers();
        headers.append("Accept", "application/json");

        this._http.get(path, {headers : headers})
            .map(res => (<Response>res).json())
            .subscribe(
                data => this.performStateTransition(data),
                error => this.error(error)
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
            this.error({msg: "Could not find a valid transition to " + state});
        } else {
            let path = this._boardData.jiraUrl + '/rest/api/2/issue/' + this._issue.key + '/transitions';
            //path = path + "?issueIdOrKey=" + this._issue.key;
            let payload:any = {transition: {id: transitionId}};

            let headers:Headers = new Headers();
            headers.append("Content-Type", "application/json");
            headers.append("Accept", "application/json");

            console.log("post to URL " + path);

            this._http.post(path, JSON.stringify(payload), {headers : headers})
                .map(res => (<Response>res).json())
                .subscribe(
                    data => {
                        if (this._changeRank) {
                            this.performRerank();
                        } else {
                            this.done();
                        }
                    },
                    error => this.error(error)
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

        this._http.post(path, JSON.stringify(payload), {headers : headers})
            .map(res => (<Response>res).json())
            .subscribe(
                data => {
                    this.done();
                },
                error => this.error(error)
            );
    }

    done() {
    }

    error(error:any) {
        //TOOD
        console.log(error);
    }

}




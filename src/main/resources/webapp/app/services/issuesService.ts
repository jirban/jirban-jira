//a simple service
import {Injectable} from 'angular2/core';
import {Headers, Http, Response} from 'angular2/http';
import {Router} from 'angular2/router';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import {BoardData} from "../data/board/boardData";
import {RestUrlUtil} from "../common/RestUrlUtil";

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

    moveIssue(boardName:string, issueKey:string, toState:string, insertBeforeIssueKey:string, insertAfterIssueKey:string) : Observable<void> {
        let payload:any = {
            boardName: boardName,
            issueKey: issueKey,
            toState: toState,
            afterIssue: insertAfterIssueKey,
            beforeIssue: insertBeforeIssueKey
        }

        console.log("IssuesService - Initiating move " + new Date());
        return this.http.post(
            'rest/move-issue', JSON.stringify(payload))
            .map(res => (<Response>res).json());
    }

    private getWebSocketUrl(board:string) : string {
        let location : Location = window.location;
        let wsUrl = location.protocol === "https:" ? "wss://" : "ws://";
        wsUrl += location.hostname;
        if (location.port) {
            wsUrl += ":" + location.port;
        }
        wsUrl += location.pathname;
        wsUrl += "websocket/issuerefresh/";
        wsUrl += board;
        console.log("--> web socket url " + wsUrl);
        return wsUrl;
    }

}
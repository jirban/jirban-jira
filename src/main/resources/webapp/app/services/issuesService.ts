//a simple service
import {Injectable} from 'angular2/core';
import {Headers, Http, Response} from 'angular2/http';
import {Router} from 'angular2/router';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import {getToken, hasToken} from '../services/authenticationHelper';
import {BoardData} from "../data/board/boardData";
import {RestUrlUtil} from "../common/RestUrlUtil";

@Injectable()
export class IssuesService {
    private router : Router;
    private http : Http;

    //private ws : WebSocket;

    constructor(http:Http, router:Router) {
        if (!hasToken()) {
            router.navigateByUrl('/login');
        }
        this.http = http;
    }

    getIssuesData(board:number) : Observable<Response> {
        let token = getToken();
        let headers = new Headers();
        headers.append("Authorization", token);
        let path:string = RestUrlUtil.caclulateRestUrl('rest/issues/' + board);
        return this.http.get(path, {
            headers: headers
        }).map(res => (<Response>res).json());
    }


    //registerWebSocket(board:string,
    //                  messageCallback : (data : any) => void) {
    //    let wsUrl : string = this.getWebSocketUrl(board);
    //    this.ws = new WebSocket(wsUrl);
    //    this.ws.onmessage = (evt:MessageEvent) => {
    //        console.log("got data " + evt.data);
    //        messageCallback(JSON.parse(evt.data));
    //    };
    //    this.ws.onerror = (evt:Event) => {
    //        console.log("Error: " + JSON.stringify(evt));
    //    }
    //    this.ws.onclose = (evt:Event) => {
    //        console.log("Close ws " + JSON.stringify(evt));
    //    }
    //}
    //
    //closeWebSocket() {
    //    if (this.ws) {
    //        this.ws.close();
    //    }
    //}

    moveIssue(boardName:string, issueKey:string, toState:string, insertBeforeIssueKey:string, insertAfterIssueKey:string) : Observable<void> {
        let token = getToken();
        let headers = new Headers();
        headers.append("Authorization", token);
        let payload:any = {
            boardName: boardName,
            issueKey: issueKey,
            toState: toState,
            afterIssue: insertAfterIssueKey,
            beforeIssue: insertBeforeIssueKey
        }

        console.log("IssuesService - Initiating move " + new Date());
        return this.http.post(
            'rest/move-issue', JSON.stringify(payload), {
                headers: headers
            })
            .map(res => (<Response>res).json())
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
System.register(['angular2/core', 'angular2/http', 'angular2/router', 'rxjs/add/operator/map', '../services/authenticationHelper', "../common/RestUrlUtil"], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, http_1, router_1, authenticationHelper_1, RestUrlUtil_1;
    var IssuesService;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (http_1_1) {
                http_1 = http_1_1;
            },
            function (router_1_1) {
                router_1 = router_1_1;
            },
            function (_1) {},
            function (authenticationHelper_1_1) {
                authenticationHelper_1 = authenticationHelper_1_1;
            },
            function (RestUrlUtil_1_1) {
                RestUrlUtil_1 = RestUrlUtil_1_1;
            }],
        execute: function() {
            IssuesService = (function () {
                //private ws : WebSocket;
                function IssuesService(http, router) {
                    if (!authenticationHelper_1.hasToken()) {
                        router.navigateByUrl('/login');
                    }
                    this.http = http;
                }
                IssuesService.prototype.getIssuesData = function (board) {
                    var token = authenticationHelper_1.getToken();
                    var headers = new http_1.Headers();
                    headers.append("Authorization", token);
                    var path = RestUrlUtil_1.RestUrlUtil.caclulateUrl('rest/issues.json?board=' + board);
                    return this.http.get(path, {
                        headers: headers
                    }).map(function (res) { return res.json(); });
                };
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
                IssuesService.prototype.moveIssue = function (boardName, issueKey, toState, insertBeforeIssueKey, insertAfterIssueKey) {
                    var token = authenticationHelper_1.getToken();
                    var headers = new http_1.Headers();
                    headers.append("Authorization", token);
                    var payload = {
                        boardName: boardName,
                        issueKey: issueKey,
                        toState: toState,
                        afterIssue: insertAfterIssueKey,
                        beforeIssue: insertBeforeIssueKey
                    };
                    console.log("IssuesService - Initiating move " + new Date());
                    return this.http.post('rest/move-issue', JSON.stringify(payload), {
                        headers: headers
                    })
                        .map(function (res) { return res.json(); });
                };
                IssuesService.prototype.getWebSocketUrl = function (board) {
                    var location = window.location;
                    var wsUrl = location.protocol === "https:" ? "wss://" : "ws://";
                    wsUrl += location.hostname;
                    if (location.port) {
                        wsUrl += ":" + location.port;
                    }
                    wsUrl += location.pathname;
                    wsUrl += "websocket/issuerefresh/";
                    wsUrl += board;
                    console.log("--> web socket url " + wsUrl);
                    return wsUrl;
                };
                IssuesService = __decorate([
                    core_1.Injectable(), 
                    __metadata('design:paramtypes', [http_1.Http, router_1.Router])
                ], IssuesService);
                return IssuesService;
            })();
            exports_1("IssuesService", IssuesService);
        }
    }
});
//# sourceMappingURL=issuesService.js.map
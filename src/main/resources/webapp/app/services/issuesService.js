System.register(['angular2/core', 'angular2/http', 'angular2/router', 'rxjs/add/operator/map', "../common/RestUrlUtil"], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, http_1, router_1, RestUrlUtil_1;
    var IssuesService, MoveIssueAction;
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
            function (RestUrlUtil_1_1) {
                RestUrlUtil_1 = RestUrlUtil_1_1;
            }],
        execute: function() {
            IssuesService = (function () {
                //private ws : WebSocket;
                function IssuesService(http, router) {
                    this.http = http;
                }
                IssuesService.prototype.getIssuesData = function (board) {
                    var path = RestUrlUtil_1.RestUrlUtil.caclulateRestUrl('rest/issues/' + board);
                    return this.http.get(path).map(function (res) { return res.json(); });
                };
                IssuesService.prototype.pollBoard = function (board, view) {
                    var path = RestUrlUtil_1.RestUrlUtil.caclulateRestUrl('rest/issues/' + board + "/updates/" + view);
                    return this.http.get(path).map(function (res) { return res.json(); });
                };
                //moveIssue(board:number, issueKey:string, toState:string, insertBeforeIssueKey:string, insertAfterIssueKey:string) : Observable<void> {
                //    let payload:any = {
                //        board: board,
                //        issueKey: issueKey,
                //        toState: toState,
                //        afterIssue: insertAfterIssueKey,
                //        beforeIssue: insertBeforeIssueKey
                //    }
                //
                //    console.log("IssuesService - Initiating move " + new Date());
                //    return this.http.post(
                //        'rest/move-issue', JSON.stringify(payload))
                //        .map(res => (<Response>res).json());
                //}
                IssuesService.prototype.moveIssue = function (boardData, issue, toBoardState, beforeKey, afterKey) {
                    var mi = new MoveIssueAction(this.http, boardData, issue, toBoardState, beforeKey, afterKey);
                    mi.execute();
                };
                IssuesService = __decorate([
                    core_1.Injectable(), 
                    __metadata('design:paramtypes', [http_1.Http, router_1.Router])
                ], IssuesService);
                return IssuesService;
            })();
            exports_1("IssuesService", IssuesService);
            MoveIssueAction = (function () {
                function MoveIssueAction(_http, _boardData, _issue, _toBoardState, _beforeKey, _afterKey) {
                    this._http = _http;
                    this._boardData = _boardData;
                    this._issue = _issue;
                    this._toBoardState = _toBoardState;
                    this._beforeKey = _beforeKey;
                    this._afterKey = _afterKey;
                    var project = _boardData.boardProjects.forKey(_issue.projectCode);
                    this._toOwnState = project.mapBoardStateToOwnState(_toBoardState);
                    this._changeState = this._toOwnState != this._issue.ownStatus;
                    this._changeRank = this._beforeKey || this._afterKey;
                }
                MoveIssueAction.prototype.execute = function () {
                    if (this._changeState) {
                        this.getTransitionsAndPerform();
                    }
                    else if (this._changeRank) {
                        this.performRerank();
                    }
                };
                MoveIssueAction.prototype.getTransitionsAndPerform = function () {
                    var _this = this;
                    var path = this._boardData.jiraUrl + '/rest/api/2/issue/' + this._issue.key + '/transitions';
                    console.log("URL " + path);
                    var headers = new http_1.Headers();
                    headers.append("Accept", "application/json");
                    this._http.get(path, { headers: headers })
                        .map(function (res) { return res.json(); })
                        .subscribe(function (data) { return _this.performStateTransition(data); }, function (error) { return _this.error(error); });
                };
                MoveIssueAction.prototype.performStateTransition = function (transitionsValue) {
                    var _this = this;
                    var transitionId = -1;
                    var transitions = transitionsValue.transitions;
                    for (var _i = 0; _i < transitions.length; _i++) {
                        var transition = transitions[_i];
                        if (transition.name === this._toOwnState) {
                            transitionId = transition.id;
                            break;
                        }
                    }
                    if (transitionId == -1) {
                        var state = this._toBoardState;
                        if (this._toOwnState != this._toBoardState) {
                            state = state + "(" + this._toOwnState + ")";
                        }
                        this.error({ msg: "Could not find a valid transition to " + state });
                    }
                    else {
                        var path = this._boardData.jiraUrl + '/rest/api/2/issue/' + this._issue.key + '/transitions';
                        //path = path + "?issueIdOrKey=" + this._issue.key;
                        var payload = { transition: { id: transitionId } };
                        var headers = new http_1.Headers();
                        headers.append("Content-Type", "application/json");
                        headers.append("Accept", "application/json");
                        console.log("post to URL " + path);
                        this._http.post(path, JSON.stringify(payload), { headers: headers })
                            .map(function (res) { return res.json(); })
                            .subscribe(function (data) {
                            if (_this._changeRank) {
                                _this.performRerank();
                            }
                            else {
                                _this.done();
                            }
                        }, function (error) { return _this.error(error); });
                    }
                };
                MoveIssueAction.prototype.performRerank = function () {
                    var _this = this;
                    var path = this._boardData.jiraUrl + '/rest/greenhopper/1.0/rank';
                    var payload = {
                        customFieldId: this._boardData.rankCustomFieldId,
                        issueKeys: [this._issue.key],
                    };
                    if (this._beforeKey) {
                        payload.rankBeforeKey = this._beforeKey;
                    }
                    if (this._afterKey) {
                        payload.rankAfterKey = this._afterKey;
                    }
                    var headers = new http_1.Headers();
                    headers.append("Content-Type", "application/json");
                    headers.append("Accept", "application/json");
                    this._http.post(path, JSON.stringify(payload), { headers: headers })
                        .map(function (res) { return res.json(); })
                        .subscribe(function (data) {
                        _this.done();
                    }, function (error) { return _this.error(error); });
                };
                MoveIssueAction.prototype.done = function () {
                };
                MoveIssueAction.prototype.error = function (error) {
                    //TOOD
                    console.log(error);
                };
                return MoveIssueAction;
            })();
        }
    }
});
//# sourceMappingURL=issuesService.js.map
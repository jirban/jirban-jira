System.register(['angular2/core', 'angular2/router', '../../services/issuesService', '../../data/board/boardData', './issue/issue', './swimlaneEntry/swimlaneEntry', "./panelMenu/panelMenu", "./issueContextMenu/issueContextMenu"], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, router_1, issuesService_1, boardData_1, issue_1, swimlaneEntry_1, panelMenu_1, issueContextMenu_1;
    var BoardComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (router_1_1) {
                router_1 = router_1_1;
            },
            function (issuesService_1_1) {
                issuesService_1 = issuesService_1_1;
            },
            function (boardData_1_1) {
                boardData_1 = boardData_1_1;
            },
            function (issue_1_1) {
                issue_1 = issue_1_1;
            },
            function (swimlaneEntry_1_1) {
                swimlaneEntry_1 = swimlaneEntry_1_1;
            },
            function (panelMenu_1_1) {
                panelMenu_1 = panelMenu_1_1;
            },
            function (issueContextMenu_1_1) {
                issueContextMenu_1 = issueContextMenu_1_1;
            }],
        execute: function() {
            BoardComponent = (function () {
                function BoardComponent(issuesService, router, routeParams, boardData) {
                    var _this = this;
                    this.issuesService = issuesService;
                    this.router = router;
                    this.routeParams = routeParams;
                    this.boardData = boardData;
                    var boardId = routeParams.get('board');
                    if (boardId) {
                        this.boardId = Number(boardId);
                    }
                    issuesService.getIssuesData(this.boardId).subscribe(function (data) {
                        _this.setIssueData(data);
                    }, function (err) {
                        console.log(err);
                        //TODO logout locally if 401, and redirect to login
                        //err seems to contain a complaint about the json marshalling of the empty body having gone wrong,
                        //rather than about the auth problems
                    }, function () { return console.log('Board: data loaded'); });
                    this.setWindowSize();
                }
                BoardComponent.prototype.ngOnDestroy = function () {
                    //this.issuesService.closeWebSocket();
                    return null;
                };
                BoardComponent.prototype.setIssueData = function (issueData) {
                    this.boardData.deserialize(this.boardId, issueData);
                    //this.issuesService.registerWebSocket(this.boardName, (data : any) => {
                    //    let command:string = data.command;
                    //    if (command === "full-refresh") {
                    //        let payload:any = data["payload"];
                    //        this.boardData.messageFullRefresh(payload);
                    //        console.log("Got new data!")
                    //    } else if (command === "issue-move") {
                    //        let payload:any = data["payload"];
                    //        this.boardData.messageIssueMove(payload);
                    //        console.log("Got new data!")
                    //    }
                    //});
                };
                Object.defineProperty(BoardComponent.prototype, "visibleColumns", {
                    get: function () {
                        return this.boardData.visibleColumns;
                    },
                    enumerable: true,
                    configurable: true
                });
                BoardComponent.prototype.toggleColumn = function (stateIndex) {
                    this.boardData.toggleColumnVisibility(stateIndex);
                };
                BoardComponent.prototype.toCharArray = function (state) {
                    var arr = [];
                    for (var i = 0; i < state.length; i++) {
                        var s = state.charAt(i);
                        if (s == " ") {
                        }
                        arr.push(s);
                    }
                    return arr;
                };
                Object.defineProperty(BoardComponent.prototype, "boardStates", {
                    get: function () {
                        return this.boardData.boardStates;
                    },
                    enumerable: true,
                    configurable: true
                });
                BoardComponent.prototype.onResize = function (event) {
                    this.setWindowSize();
                };
                BoardComponent.prototype.setWindowSize = function () {
                    //Whole height - toolbars - borders
                    this.boardHeight = window.innerHeight - 30 - 4;
                    //board height - header - borders
                    this.boardBodyHeight = this.boardHeight - 30 - 3;
                    this.width = window.innerWidth - 2; //subtract width of border
                };
                BoardComponent.prototype.showIssueContextMenu = function (event) {
                    this.issueContextMenuData = new issueContextMenu_1.IssueContextMenuData(event.issueId, event.x, event.y);
                };
                BoardComponent.prototype.hideMenus = function () {
                    this.boardData.hideHideables();
                    this.issueContextMenuData = null;
                };
                BoardComponent.prototype.onCloseIssueContextMenu = function (event) {
                    this.issueContextMenuData = null;
                };
                BoardComponent = __decorate([
                    core_1.Component({
                        selector: 'board',
                        providers: [issuesService_1.IssuesService, boardData_1.BoardData]
                    }),
                    core_1.View({
                        templateUrl: 'app/components/board/board.html',
                        styleUrls: ['app/components/board/board.css'],
                        directives: [issue_1.IssueComponent, issueContextMenu_1.IssueContextMenuComponent, panelMenu_1.PanelMenuComponent, swimlaneEntry_1.SwimlaneEntryComponent]
                    }), 
                    __metadata('design:paramtypes', [issuesService_1.IssuesService, router_1.Router, router_1.RouteParams, boardData_1.BoardData])
                ], BoardComponent);
                return BoardComponent;
            })();
            exports_1("BoardComponent", BoardComponent);
        }
    }
});
//# sourceMappingURL=board.js.map
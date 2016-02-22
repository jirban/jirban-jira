System.register(['angular2/core', '../../../data/board/boardData', '../../../services/issuesService', '../issue/issue'], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, boardData_1, issuesService_1, issue_1;
    var IssueContextMenuComponent, IssueContextMenuData;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (boardData_1_1) {
                boardData_1 = boardData_1_1;
            },
            function (issuesService_1_1) {
                issuesService_1 = issuesService_1_1;
            },
            function (issue_1_1) {
                issue_1 = issue_1_1;
            }],
        execute: function() {
            IssueContextMenuComponent = (function () {
                function IssueContextMenuComponent(boardData, issuesService) {
                    this.boardData = boardData;
                    this.issuesService = issuesService;
                    this.showContext = false;
                    this.closeContextMenu = new core_1.EventEmitter();
                    this.setWindowSize();
                }
                Object.defineProperty(IssueContextMenuComponent.prototype, "data", {
                    get: function () {
                        return this._data;
                    },
                    set: function (data) {
                        this.showContext = !!data;
                        this.move = false;
                        this.toState = null;
                        this.issue = null;
                        this.endIssue = false;
                        this.issuesForState = null;
                        this.insertBeforeIssueKey = null;
                        this._data = data;
                        this.issue = null;
                        if (data) {
                            this.issue = this.boardData.getIssue(data.issueKey);
                            this.toState = this.issue.boardStatus;
                            this.issuesForState = this.boardData.getValidMoveBeforeIssues(this.issue.key, this.toState);
                        }
                    },
                    enumerable: true,
                    configurable: true
                });
                IssueContextMenuComponent.prototype.clearMoveMenu = function () {
                    this.move = false;
                };
                Object.defineProperty(IssueContextMenuComponent.prototype, "displayContextMenu", {
                    get: function () {
                        return !!this._data && !!this.issue && this.showContext;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueContextMenuComponent.prototype, "moveStates", {
                    get: function () {
                        return this.boardData.boardStates;
                    },
                    enumerable: true,
                    configurable: true
                });
                IssueContextMenuComponent.prototype.isValidState = function (state) {
                    return this.boardData.isValidStateForProject(this.issue.projectCode, state);
                };
                IssueContextMenuComponent.prototype.onShowMovePanel = function (event) {
                    event.preventDefault();
                    this.showContext = false;
                    this.move = true;
                };
                IssueContextMenuComponent.prototype.onSelectMoveState = function (event, toState) {
                    event.preventDefault();
                    this.issuesForState = this.boardData.getValidMoveBeforeIssues(this.issue.key, toState);
                    this.toState = toState;
                };
                IssueContextMenuComponent.prototype.onSelectMoveIssue = function (event, beforeIssueKey) {
                    console.log("onSelectMoveIssue - " + beforeIssueKey);
                    event.preventDefault();
                    this.insertBeforeIssueKey = beforeIssueKey;
                    if (this.issue.key == beforeIssueKey) {
                        //If we are moving to ourself just abort
                        console.log("onSelectMoveIssue - key is self, returning");
                        this.clearMoveMenu();
                        return;
                    }
                    var beforeKey = beforeIssueKey === "" ? null : beforeIssueKey;
                    var afterKey;
                    if (!beforeKey && this.issuesForState.length > 0) {
                        afterKey = this.issuesForState[this.issuesForState.length - 1].key;
                    }
                    console.log("onSelectMoveIssue key - afterKey " + afterKey);
                    //Tell the server to move the issue. The actual move will come in via the board's web socket since the actions
                    //are queued on the server and once done the changes are broadcast to everyone connected.
                    //this.boardData.initiateMoveIssue(this.issuesService, this.issue, this.toState, beforeKey, afterKey);
                    //this.issuesService.moveIssue(this.boardData.boardName, this.issue.key, this.toState, beforeKey, afterKey)
                    //    .subscribe(
                    //        data => {
                    //            console.log("Executed move!");
                    //        }
                    //        ,
                    //        err => {
                    //            console.error(err);
                    //        },
                    //        () => {
                    //            console.log('request completed')
                    //            this.clearMoveMenu();
                    //        }
                    //    );
                    //
                    //console.log("insertBeforeIssueKey " + this.insertBeforeIssueKey);
                    this.issuesService.moveIssue(this.boardData, this.issue, this.toState, beforeKey, afterKey);
                };
                IssueContextMenuComponent.prototype.onResize = function (event) {
                    this.setWindowSize();
                };
                IssueContextMenuComponent.prototype.setWindowSize = function () {
                    var movePanelTop, movePanelLeft = 0;
                    //css hardcodes the height as 350px
                    if (window.innerHeight > 350) {
                        movePanelTop = window.innerHeight / 2 - 350 / 2;
                    }
                    //css hardcodes the width as 720px;
                    if (window.innerWidth > 720) {
                        movePanelLeft = window.innerWidth / 2 - 720 / 2;
                    }
                    this.movePanelTop = movePanelTop;
                    this.movePanelLeft = movePanelLeft;
                };
                IssueContextMenuComponent.prototype.isIssueSelected = function (issue) {
                    if (this.insertBeforeIssueKey) {
                        return issue.key === this.insertBeforeIssueKey;
                    }
                    return this.issue.key == issue.key;
                };
                IssueContextMenuComponent.prototype.onClickClose = function (event) {
                    this.closeContextMenu.emit({});
                    event.preventDefault();
                };
                IssueContextMenuComponent = __decorate([
                    core_1.Component({
                        inputs: ['data'],
                        outputs: ['closeContextMenu'],
                        selector: 'issue-context-menu'
                    }),
                    core_1.View({
                        templateUrl: 'app/components/board/issueContextMenu/issueContextMenu.html',
                        styleUrls: ['app/components/board/issueContextMenu/issueContextMenu.css'],
                        directives: [issue_1.IssueComponent]
                    }), 
                    __metadata('design:paramtypes', [boardData_1.BoardData, issuesService_1.IssuesService])
                ], IssueContextMenuComponent);
                return IssueContextMenuComponent;
            })();
            exports_1("IssueContextMenuComponent", IssueContextMenuComponent);
            IssueContextMenuData = (function () {
                function IssueContextMenuData(_issueKey, _x, _y) {
                    this._issueKey = _issueKey;
                    this._x = _x;
                    this._y = _y;
                }
                Object.defineProperty(IssueContextMenuData.prototype, "issueKey", {
                    get: function () {
                        return this._issueKey;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueContextMenuData.prototype, "x", {
                    get: function () {
                        return this._x;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueContextMenuData.prototype, "y", {
                    get: function () {
                        return this._y;
                    },
                    enumerable: true,
                    configurable: true
                });
                return IssueContextMenuData;
            })();
            exports_1("IssueContextMenuData", IssueContextMenuData);
        }
    }
});
//# sourceMappingURL=issueContextMenu.js.map
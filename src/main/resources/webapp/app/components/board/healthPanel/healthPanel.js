System.register(['angular2/core', '../../../data/board/boardData'], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, boardData_1;
    var HealthPanelComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (boardData_1_1) {
                boardData_1 = boardData_1_1;
            }],
        execute: function() {
            HealthPanelComponent = (function () {
                function HealthPanelComponent(boardData) {
                    this.boardData = boardData;
                    this.closeHealthPanel = new core_1.EventEmitter();
                }
                Object.defineProperty(HealthPanelComponent.prototype, "states", {
                    get: function () {
                        if (!this._states) {
                            this._states = this.getNames("states");
                        }
                        return this._states;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(HealthPanelComponent.prototype, "issueTypes", {
                    get: function () {
                        if (!this._issueTypes) {
                            this._issueTypes = this.getNames("issue-types");
                        }
                        return this._issueTypes;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(HealthPanelComponent.prototype, "priorities", {
                    get: function () {
                        if (!this._priorities) {
                            this._priorities = this.getNames("priorities");
                        }
                        return this._priorities;
                    },
                    enumerable: true,
                    configurable: true
                });
                HealthPanelComponent.prototype.getIssuesForState = function (name) {
                    return this.getIssues("states", name);
                };
                HealthPanelComponent.prototype.getIssuesForIssueType = function (name) {
                    return this.getIssues("issue-types", name);
                };
                HealthPanelComponent.prototype.getIssuesForPriority = function (name) {
                    return this.getIssues("priorities", name);
                };
                HealthPanelComponent.prototype.getNames = function (type) {
                    var arr = [];
                    if (this.boardData.missing) {
                        for (var name_1 in this.boardData.missing[type]) {
                            arr.push(name_1);
                        }
                    }
                    return arr;
                };
                HealthPanelComponent.prototype.getIssues = function (type, name) {
                    return this.boardData.missing[type][name].issues;
                };
                HealthPanelComponent.prototype.formatUrl = function (issue) {
                    return this.boardData.jiraUrl + "/browse/" + issue;
                };
                HealthPanelComponent.prototype.onClickClose = function (event) {
                    this.closeHealthPanel.emit({});
                    event.preventDefault();
                };
                HealthPanelComponent = __decorate([
                    core_1.Component({
                        selector: 'health-panel',
                        outputs: ['closeHealthPanel']
                    }),
                    core_1.View({
                        templateUrl: 'app/components/board/healthPanel/healthPanel.html',
                        styleUrls: ['app/components/board/healthPanel/healthPanel.css'],
                    }), 
                    __metadata('design:paramtypes', [boardData_1.BoardData])
                ], HealthPanelComponent);
                return HealthPanelComponent;
            })();
            exports_1("HealthPanelComponent", HealthPanelComponent);
        }
    }
});
//# sourceMappingURL=healthPanel.js.map
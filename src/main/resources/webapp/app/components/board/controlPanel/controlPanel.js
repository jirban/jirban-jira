System.register(['angular2/core', 'angular2/common', '../../../data/board/boardData', '../../../data/board/assignee'], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, common_1, boardData_1, assignee_1;
    var ControlPanelComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (common_1_1) {
                common_1 = common_1_1;
            },
            function (boardData_1_1) {
                boardData_1 = boardData_1_1;
            },
            function (assignee_1_1) {
                assignee_1 = assignee_1_1;
            }],
        execute: function() {
            ControlPanelComponent = (function () {
                function ControlPanelComponent(boardData, formBuilder) {
                    this.boardData = boardData;
                    this.formBuilder = formBuilder;
                    this.closeControlPanel = new core_1.EventEmitter();
                    this.noAssignee = assignee_1.NO_ASSIGNEE;
                }
                Object.defineProperty(ControlPanelComponent.prototype, "swimlaneForm", {
                    get: function () {
                        var _this = this;
                        if (this._swimlaneForm) {
                            return this._swimlaneForm;
                        }
                        var form = this.formBuilder.group({});
                        form.addControl("swimlane", new common_1.Control(null));
                        form.valueChanges
                            .subscribe(function (value) {
                            _this.updateSwimlane(value);
                        });
                        this._swimlaneForm = form;
                        return this._swimlaneForm;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ControlPanelComponent.prototype, "issueDetailForm", {
                    get: function () {
                        var _this = this;
                        if (this._issueDetailForm) {
                            return this._issueDetailForm;
                        }
                        var form = this.formBuilder.group({});
                        form.addControl("assignee", new common_1.Control(true));
                        form.addControl("description", new common_1.Control(true));
                        form.addControl("info", new common_1.Control(true));
                        form.addControl("linked", new common_1.Control(true));
                        form.valueChanges
                            .subscribe(function (value) {
                            _this.updateIssueDetail(value);
                        });
                        this._issueDetailForm = form;
                        return this._issueDetailForm;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ControlPanelComponent.prototype, "projectFilterForm", {
                    get: function () {
                        var _this = this;
                        if (this._projectFilterForm) {
                            return this._projectFilterForm;
                        }
                        var form = this.formBuilder.group({});
                        //Add the projects to the form
                        for (var _i = 0, _a = this.boardData.boardProjectCodes; _i < _a.length; _i++) {
                            var projectName = _a[_i];
                            form.addControl(projectName, new common_1.Control(false));
                        }
                        form.valueChanges
                            .subscribe(function (value) {
                            _this.updateProjectFilter(value);
                        });
                        this._projectFilterForm = form;
                        return this._projectFilterForm;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ControlPanelComponent.prototype, "priorityFilterForm", {
                    get: function () {
                        var _this = this;
                        if (this._priorityFilterForm) {
                            return this._priorityFilterForm;
                        }
                        var form = this.formBuilder.group({});
                        for (var _i = 0, _a = this.priorities; _i < _a.length; _i++) {
                            var priority = _a[_i];
                            form.addControl(priority.name, new common_1.Control(false));
                        }
                        form.valueChanges
                            .subscribe(function (value) {
                            _this.updatePriorityFilter(value);
                        });
                        this._priorityFilterForm = form;
                        return this._priorityFilterForm;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ControlPanelComponent.prototype, "issueTypeFilterForm", {
                    get: function () {
                        var _this = this;
                        if (this._issueTypeFilterForm) {
                            return this._issueTypeFilterForm;
                        }
                        var form = this.formBuilder.group({});
                        for (var _i = 0, _a = this.issueTypes; _i < _a.length; _i++) {
                            var issueType = _a[_i];
                            form.addControl(issueType.name, new common_1.Control(false));
                        }
                        form.valueChanges
                            .subscribe(function (value) {
                            _this.updateIssueTypeFilter(value);
                        });
                        this._issueTypeFilterForm = form;
                        return this._issueTypeFilterForm;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ControlPanelComponent.prototype, "assigneeFilterForm", {
                    get: function () {
                        var _this = this;
                        if (!this._assigneeFilterForm) {
                            console.log("----> assignee form");
                            var form = this.formBuilder.group({});
                            //The unassigned assignee and the ones configured in the project
                            form.addControl(assignee_1.NO_ASSIGNEE, new common_1.Control(false));
                            for (var _i = 0, _a = this.assignees; _i < _a.length; _i++) {
                                var assignee = _a[_i];
                                form.addControl(assignee.key, new common_1.Control(false));
                            }
                            form.valueChanges
                                .subscribe(function (value) {
                                _this.updateAssigneeFilter(value);
                            });
                            this._assigneeFilterForm = form;
                        }
                        else if (this.boardData.getAndClearHasNewAssignees()) {
                            //TODO look into an Observable instead
                            console.log("----> checking assignee form");
                            var form = this._assigneeFilterForm;
                            for (var _b = 0, _c = this.assignees; _b < _c.length; _b++) {
                                var assignee = _c[_b];
                                var control = form.controls[assignee.key];
                                if (!control) {
                                    form.addControl(assignee.key, new common_1.Control(false));
                                }
                            }
                        }
                        return this._assigneeFilterForm;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ControlPanelComponent.prototype, "assignees", {
                    get: function () {
                        return this.boardData.assignees.array;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ControlPanelComponent.prototype, "priorities", {
                    get: function () {
                        return this.boardData.priorities.array;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ControlPanelComponent.prototype, "issueTypes", {
                    get: function () {
                        return this.boardData.issueTypes.array;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ControlPanelComponent.prototype, "boardProjectCodes", {
                    get: function () {
                        return this.boardData.boardProjectCodes;
                    },
                    enumerable: true,
                    configurable: true
                });
                ControlPanelComponent.prototype.updateSwimlane = function (value) {
                    console.log("Swimlane: " + JSON.stringify(value));
                    this.boardData.swimlane = value.swimlane;
                };
                ControlPanelComponent.prototype.updateIssueDetail = function (value) {
                    console.log("Detail: " + JSON.stringify(value));
                    this.boardData.updateIssueDetail(value.assignee, value.description, value.info, value.linked);
                };
                ControlPanelComponent.prototype.updateProjectFilter = function (value) {
                    console.log(JSON.stringify(value));
                    this.boardData.updateProjectFilter(value);
                };
                ControlPanelComponent.prototype.updatePriorityFilter = function (value) {
                    console.log(JSON.stringify(value));
                    this.boardData.updatePriorityFilter(value);
                };
                ControlPanelComponent.prototype.updateIssueTypeFilter = function (value) {
                    console.log(JSON.stringify(value));
                    this.boardData.updateIssueTypeFilter(value);
                };
                ControlPanelComponent.prototype.updateAssigneeFilter = function (value) {
                    console.log(JSON.stringify(value));
                    this.boardData.updateAssigneeFilter(value);
                };
                ControlPanelComponent.prototype.onClickClose = function (event) {
                    this.closeControlPanel.emit({});
                    event.preventDefault();
                };
                ControlPanelComponent = __decorate([
                    core_1.Component({
                        selector: 'control-panel',
                        outputs: ['closeControlPanel']
                    }),
                    core_1.View({
                        templateUrl: 'app/components/board/controlPanel/controlPanel.html',
                        styleUrls: ['app/components/board/controlPanel/controlPanel.css'],
                        directives: [common_1.FORM_DIRECTIVES]
                    }), 
                    __metadata('design:paramtypes', [boardData_1.BoardData, common_1.FormBuilder])
                ], ControlPanelComponent);
                return ControlPanelComponent;
            })();
            exports_1("ControlPanelComponent", ControlPanelComponent);
        }
    }
});
//# sourceMappingURL=controlPanel.js.map
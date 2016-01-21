System.register(["./assignee"], function(exports_1) {
    var assignee_1;
    var BoardFilters, IssueDisplayDetails;
    return {
        setters:[
            function (assignee_1_1) {
                assignee_1 = assignee_1_1;
            }],
        execute: function() {
            BoardFilters = (function () {
                function BoardFilters() {
                    this.projects = false;
                    this.assignees = false;
                    this.priorities = false;
                    this.issueTypes = false;
                }
                BoardFilters.prototype.setProjectFilter = function (filter, boardProjectCodes) {
                    this._projectFilter = filter;
                    this.projects = false;
                    if (boardProjectCodes) {
                        for (var _i = 0; _i < boardProjectCodes.length; _i++) {
                            var key = boardProjectCodes[_i];
                            if (filter[key]) {
                                this.projects = true;
                                break;
                            }
                        }
                    }
                };
                BoardFilters.prototype.setPriorityFilter = function (filter, priorities) {
                    this._priorityFilter = filter;
                    this.priorities = false;
                    if (priorities) {
                        for (var _i = 0, _a = priorities.array; _i < _a.length; _i++) {
                            var priority = _a[_i];
                            if (filter[priority.name]) {
                                this.priorities = true;
                                break;
                            }
                        }
                    }
                };
                BoardFilters.prototype.setIssueTypeFilter = function (filter, issueTypes) {
                    this._issueTypeFilter = filter;
                    this.issueTypes = false;
                    if (issueTypes) {
                        for (var _i = 0, _a = issueTypes.array; _i < _a.length; _i++) {
                            var issueType = _a[_i];
                            if (filter[issueType.name]) {
                                this.issueTypes = true;
                                break;
                            }
                        }
                    }
                };
                BoardFilters.prototype.setAssigneeFilter = function (filter, assignees) {
                    this._assigneeFilter = filter;
                    this.assignees = false;
                    if (filter[assignee_1.NO_ASSIGNEE]) {
                        this.assignees = true;
                    }
                    else if (assignees) {
                        for (var _i = 0, _a = assignees.array; _i < _a.length; _i++) {
                            var assignee = _a[_i];
                            if (filter[assignee.key]) {
                                this.assignees = true;
                                break;
                            }
                        }
                    }
                };
                BoardFilters.prototype.filterIssue = function (issue) {
                    if (this.filterProject(issue.projectCode)) {
                        return true;
                    }
                    if (this.filterAssignee(issue.assignee ? issue.assignee.key : null)) {
                        return true;
                    }
                    if (this.filterPriority(issue.priority.name)) {
                        return true;
                    }
                    if (this.filterIssueType(issue.type.name)) {
                        return true;
                    }
                    return false;
                };
                BoardFilters.prototype.filterProject = function (projectCode) {
                    if (this.projects) {
                        return !this._projectFilter[projectCode];
                    }
                    return false;
                };
                BoardFilters.prototype.filterAssignee = function (assigneeKey) {
                    if (this.assignees) {
                        return !this._assigneeFilter[assigneeKey ? assigneeKey : assignee_1.NO_ASSIGNEE];
                    }
                    return false;
                };
                BoardFilters.prototype.filterPriority = function (priorityName) {
                    if (this.priorities) {
                        return !this._priorityFilter[priorityName];
                    }
                    return false;
                };
                BoardFilters.prototype.filterIssueType = function (issueTypeName) {
                    if (this.issueTypes) {
                        return !this._issueTypeFilter[issueTypeName];
                    }
                    return false;
                };
                return BoardFilters;
            })();
            exports_1("BoardFilters", BoardFilters);
            /**
             * The details to show for the issues
             */
            IssueDisplayDetails = (function () {
                function IssueDisplayDetails(assignee, summary, info, linkedIssues) {
                    if (assignee === void 0) { assignee = true; }
                    if (summary === void 0) { summary = true; }
                    if (info === void 0) { info = true; }
                    if (linkedIssues === void 0) { linkedIssues = true; }
                    this._assignee = true;
                    this._summary = true;
                    this._info = true;
                    this._linkedIssues = true;
                    this._assignee = assignee;
                    this._summary = summary;
                    this._info = info;
                    this._linkedIssues = linkedIssues;
                }
                Object.defineProperty(IssueDisplayDetails.prototype, "assignee", {
                    get: function () {
                        return this._assignee;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueDisplayDetails.prototype, "summary", {
                    get: function () {
                        return this._summary;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueDisplayDetails.prototype, "info", {
                    get: function () {
                        return this._info;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueDisplayDetails.prototype, "linkedIssues", {
                    get: function () {
                        return this._linkedIssues;
                    },
                    enumerable: true,
                    configurable: true
                });
                return IssueDisplayDetails;
            })();
            exports_1("IssueDisplayDetails", IssueDisplayDetails);
        }
    }
});
//# sourceMappingURL=boardFilters.js.map
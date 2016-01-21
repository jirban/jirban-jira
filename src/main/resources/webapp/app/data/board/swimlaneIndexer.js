System.register(["./issueTable"], function(exports_1) {
    var __extends = (this && this.__extends) || function (d, b) {
        for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
    var issueTable_1;
    var SwimlaneMatcher, SwimlaneIndexerFactory, BaseIndexer, ProjectSwimlaneIndexer, PrioritySwimlaneIndexer, IssueTypeSwimlaneIndexer, AssigneeSwimlaneIndexer;
    function createTable(boardData, swimlaneNames) {
        var swimlaneTable = [];
        var slIndex = 0;
        for (var _i = 0; _i < swimlaneNames.length; _i++) {
            var swimlaneName = swimlaneNames[_i];
            swimlaneTable.push(new issueTable_1.SwimlaneData(boardData, swimlaneName, slIndex++));
        }
        return swimlaneTable;
    }
    return {
        setters:[
            function (issueTable_1_1) {
                issueTable_1 = issueTable_1_1;
            }],
        execute: function() {
            SwimlaneMatcher = (function () {
                function SwimlaneMatcher(_targetIssue, _indexer) {
                    this._targetIssue = _targetIssue;
                    this._indexer = _indexer;
                }
                SwimlaneMatcher.prototype.matchesSwimlane = function (issue) {
                    return this._indexer.matchIssues(this._targetIssue, issue);
                };
                return SwimlaneMatcher;
            })();
            exports_1("SwimlaneMatcher", SwimlaneMatcher);
            SwimlaneIndexerFactory = (function () {
                function SwimlaneIndexerFactory() {
                }
                SwimlaneIndexerFactory.prototype.createSwimlaneIndexer = function (swimlane, filters, boardData) {
                    return this._createIndexer(swimlane, filters, boardData, true);
                };
                SwimlaneIndexerFactory.prototype.createSwimlaneMatcher = function (swimlane, targetIssue) {
                    var indexer = this._createIndexer(swimlane, null, null, false);
                    if (indexer == null) {
                        return null;
                    }
                    return new SwimlaneMatcher(targetIssue, indexer);
                };
                SwimlaneIndexerFactory.prototype._createIndexer = function (swimlane, filters, boardData, initTable) {
                    if (swimlane === "project") {
                        return new ProjectSwimlaneIndexer(filters, boardData, initTable);
                    }
                    else if (swimlane === "priority") {
                        return new PrioritySwimlaneIndexer(filters, boardData, initTable);
                    }
                    else if (swimlane === "issue-type") {
                        return new IssueTypeSwimlaneIndexer(filters, boardData, initTable);
                    }
                    else if (swimlane === "assignee") {
                        return new AssigneeSwimlaneIndexer(filters, boardData, initTable);
                    }
                    return null;
                };
                return SwimlaneIndexerFactory;
            })();
            exports_1("SwimlaneIndexerFactory", SwimlaneIndexerFactory);
            BaseIndexer = (function () {
                function BaseIndexer(_filters, _boardData) {
                    this._filters = _filters;
                    this._boardData = _boardData;
                }
                Object.defineProperty(BaseIndexer.prototype, "swimlaneTable", {
                    get: function () {
                        return this._swimlaneTable;
                    },
                    enumerable: true,
                    configurable: true
                });
                return BaseIndexer;
            })();
            ProjectSwimlaneIndexer = (function (_super) {
                __extends(ProjectSwimlaneIndexer, _super);
                function ProjectSwimlaneIndexer(filters, boardData, initTable) {
                    _super.call(this, filters, boardData);
                    this._indices = {};
                    if (initTable) {
                        var i = 0;
                        for (var _i = 0, _a = boardData.boardProjectCodes; _i < _a.length; _i++) {
                            var name_1 = _a[_i];
                            this._indices[name_1] = i;
                            i++;
                        }
                        this._swimlaneTable = createTable(boardData, boardData.boardProjectCodes);
                    }
                }
                ProjectSwimlaneIndexer.prototype.swimlaneIndex = function (issue) {
                    return this._indices[issue.projectCode];
                };
                ProjectSwimlaneIndexer.prototype.filter = function (swimlaneData) {
                    return this._filters.filterProject(swimlaneData.name);
                };
                ProjectSwimlaneIndexer.prototype.matchIssues = function (targetIssue, issue) {
                    return targetIssue.projectCode === issue.projectCode;
                };
                return ProjectSwimlaneIndexer;
            })(BaseIndexer);
            PrioritySwimlaneIndexer = (function (_super) {
                __extends(PrioritySwimlaneIndexer, _super);
                function PrioritySwimlaneIndexer(filters, boardData, initTable) {
                    _super.call(this, filters, boardData);
                    this._swimlaneNames = [];
                    if (initTable) {
                        var i = 0;
                        for (var _i = 0, _a = boardData.priorities.array; _i < _a.length; _i++) {
                            var priority = _a[_i];
                            this._swimlaneNames.push(priority.name);
                            i++;
                        }
                        this._swimlaneTable = createTable(boardData, this._swimlaneNames);
                    }
                }
                PrioritySwimlaneIndexer.prototype.swimlaneIndex = function (issue) {
                    return this._boardData.priorities.indices[issue.priorityName];
                };
                PrioritySwimlaneIndexer.prototype.filter = function (swimlaneData) {
                    return this._filters.filterPriority(swimlaneData.name);
                };
                PrioritySwimlaneIndexer.prototype.matchIssues = function (targetIssue, issue) {
                    return targetIssue.priority.name === issue.priority.name;
                };
                return PrioritySwimlaneIndexer;
            })(BaseIndexer);
            IssueTypeSwimlaneIndexer = (function (_super) {
                __extends(IssueTypeSwimlaneIndexer, _super);
                function IssueTypeSwimlaneIndexer(filters, boardData, initTable) {
                    _super.call(this, filters, boardData);
                    this._swimlaneNames = [];
                    if (initTable) {
                        var i = 0;
                        for (var _i = 0, _a = boardData.issueTypes.array; _i < _a.length; _i++) {
                            var issueType = _a[_i];
                            this._swimlaneNames.push(issueType.name);
                            i++;
                        }
                        this._swimlaneTable = createTable(boardData, this._swimlaneNames);
                    }
                }
                Object.defineProperty(IssueTypeSwimlaneIndexer.prototype, "swimlaneTable", {
                    get: function () {
                        return this._swimlaneTable;
                    },
                    enumerable: true,
                    configurable: true
                });
                IssueTypeSwimlaneIndexer.prototype.swimlaneIndex = function (issue) {
                    return this._boardData.issueTypes.indices[issue.typeName];
                };
                IssueTypeSwimlaneIndexer.prototype.filter = function (swimlaneData) {
                    return this._filters.filterIssueType(swimlaneData.name);
                };
                IssueTypeSwimlaneIndexer.prototype.matchIssues = function (targetIssue, issue) {
                    return targetIssue.type.name === issue.type.name;
                };
                return IssueTypeSwimlaneIndexer;
            })(BaseIndexer);
            AssigneeSwimlaneIndexer = (function (_super) {
                __extends(AssigneeSwimlaneIndexer, _super);
                function AssigneeSwimlaneIndexer(filters, boardData, initTable) {
                    _super.call(this, filters, boardData);
                    this._swimlaneNames = [];
                    if (initTable) {
                        var i = 0;
                        for (var _i = 0, _a = this._boardData.assignees.array; _i < _a.length; _i++) {
                            var assignee = _a[_i];
                            this._swimlaneNames.push(assignee.name);
                        }
                        //Add an additional entry for the no assignee case
                        this._swimlaneNames.push("None");
                        this._swimlaneTable = createTable(boardData, this._swimlaneNames);
                    }
                }
                Object.defineProperty(AssigneeSwimlaneIndexer.prototype, "swimlaneTable", {
                    get: function () {
                        return this._swimlaneTable;
                    },
                    enumerable: true,
                    configurable: true
                });
                AssigneeSwimlaneIndexer.prototype.swimlaneIndex = function (issue) {
                    if (!issue.assignee) {
                        return this._swimlaneNames.length - 1;
                    }
                    return this._boardData.assignees.indices[issue.assignee.key];
                };
                AssigneeSwimlaneIndexer.prototype.filter = function (swimlaneData) {
                    var assigneeKey = null;
                    if (swimlaneData.index < this._swimlaneNames.length - 1) {
                        assigneeKey = this._boardData.assignees.forIndex(swimlaneData.index).key;
                    }
                    return this._filters.filterAssignee(assigneeKey);
                };
                AssigneeSwimlaneIndexer.prototype.matchIssues = function (targetIssue, issue) {
                    if (!targetIssue.assignee && !issue.assignee) {
                        return true;
                    }
                    else if (targetIssue.assignee && issue.assignee) {
                        return targetIssue.assignee.key === issue.assignee.key;
                    }
                    return false;
                };
                return AssigneeSwimlaneIndexer;
            })(BaseIndexer);
        }
    }
});
//# sourceMappingURL=swimlaneIndexer.js.map
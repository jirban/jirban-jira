System.register(['./assignee', './priority', './issueType', './boardFilters', "./project", "./issueTable", "../../common/RestUrlUtil"], function(exports_1) {
    var assignee_1, priority_1, issueType_1, boardFilters_1, project_1, issueTable_1, RestUrlUtil_1;
    var BoardData;
    return {
        setters:[
            function (assignee_1_1) {
                assignee_1 = assignee_1_1;
            },
            function (priority_1_1) {
                priority_1 = priority_1_1;
            },
            function (issueType_1_1) {
                issueType_1 = issueType_1_1;
            },
            function (boardFilters_1_1) {
                boardFilters_1 = boardFilters_1_1;
            },
            function (project_1_1) {
                project_1 = project_1_1;
            },
            function (issueTable_1_1) {
                issueTable_1 = issueTable_1_1;
            },
            function (RestUrlUtil_1_1) {
                RestUrlUtil_1 = RestUrlUtil_1_1;
            }],
        execute: function() {
            BoardData = (function () {
                function BoardData() {
                    this._visibleColumns = [];
                    this.initialized = false;
                    //Issue details
                    this._issueDisplayDetails = new boardFilters_1.IssueDisplayDetails();
                    this._boardFilters = new boardFilters_1.BoardFilters();
                    this.hideables = [];
                }
                /**
                 * Called on loading the board the first time
                 * @param input the json containing the issue tables
                 */
                BoardData.prototype.deserialize = function (boardId, input) {
                    this.boardName = input.name;
                    this.internalDeserialize(input, true);
                    var arr = [];
                    for (var i = 0; i < this.boardStates.length; i++) {
                        arr.push(true);
                    }
                    this._visibleColumns = arr;
                    this.initialized = true;
                    return this;
                };
                /**
                 * Called when changed data is pushed from the server
                 * @param input the json containing the issue tables
                 */
                BoardData.prototype.messageFullRefresh = function (input) {
                    this.internalDeserialize(input);
                };
                /**
                 * Called when an issue is moved on the server's board
                 * @param input the json containing the details of the issue move
                 */
                BoardData.prototype.messageIssueMove = function (input) {
                    console.log(input);
                    this._issueTable.moveIssue(input.issueKey, input.toState, input.beforeIssue);
                };
                /**
                 * Called when changes are made to the issue detail to display in the control panel
                 * @param issueDisplayDetails
                 */
                BoardData.prototype.updateIssueDisplayDetails = function (issueDisplayDetails) {
                    this.issueDisplayDetails = issueDisplayDetails;
                };
                BoardData.prototype.internalDeserialize = function (input, first) {
                    if (first === void 0) { first = false; }
                    this.jiraUrl = RestUrlUtil_1.RestUrlUtil.calculateJiraUrl();
                    this.missing = input.missing;
                    this._projects = new project_1.ProjectDeserializer().deserialize(input);
                    this._assignees = new assignee_1.AssigneeDeserializer().deserialize(input);
                    this._priorities = new priority_1.PriorityDeserializer().deserialize(input);
                    this._issueTypes = new issueType_1.IssueTypeDeserializer().deserialize(input);
                    if (first) {
                        this._issueTable = new issueTable_1.IssueTable(this, this._projects, this._boardFilters, this._swimlane, input);
                    }
                    else {
                        this._issueTable.fullRefresh(this._projects, input);
                    }
                    //this.updateIssueTables();
                };
                BoardData.prototype.toggleColumnVisibility = function (stateIndex) {
                    this._visibleColumns[stateIndex] = !this._visibleColumns[stateIndex];
                };
                BoardData.prototype.toggleSwimlaneVisibility = function (swimlaneIndex) {
                    this._issueTable.toggleSwimlaneVisibility(swimlaneIndex);
                };
                Object.defineProperty(BoardData.prototype, "visibleColumns", {
                    get: function () {
                        return this._visibleColumns;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "issueTable", {
                    get: function () {
                        return this._issueTable.issueTable;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "swimlaneTable", {
                    get: function () {
                        return this._issueTable.swimlaneTable;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "totalIssuesByState", {
                    get: function () {
                        return this._issueTable.totalIssuesByState;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "assignees", {
                    get: function () {
                        return this._assignees;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "priorities", {
                    get: function () {
                        return this._priorities;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "issueTypes", {
                    get: function () {
                        return this._issueTypes;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "boardStates", {
                    get: function () {
                        return this._projects.boardStates.array;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "owner", {
                    get: function () {
                        return this._projects.owner;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "linkedProjects", {
                    get: function () {
                        return this._projects.linkedProjects;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "boardProjects", {
                    get: function () {
                        return this._projects.boardProjects;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "boardProjectCodes", {
                    get: function () {
                        return this._projects.boardProjectCodes;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "swimlane", {
                    get: function () {
                        return this._swimlane;
                    },
                    set: function (swimlane) {
                        this._swimlane = swimlane;
                        this._issueTable.swimlane = swimlane;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardData.prototype, "issueDisplayDetails", {
                    get: function () {
                        return this._issueDisplayDetails;
                    },
                    enumerable: true,
                    configurable: true
                });
                BoardData.prototype.getIssue = function (issueKey) {
                    return this._issueTable.getIssue(issueKey);
                };
                BoardData.prototype.updateIssueDetail = function (assignee, description, info, linked) {
                    this._issueDisplayDetails = new boardFilters_1.IssueDisplayDetails(assignee, description, info, linked);
                };
                BoardData.prototype.updateProjectFilter = function (filter) {
                    this._boardFilters.setProjectFilter(filter, this._projects.boardProjectCodes);
                    this._issueTable.filters = this._boardFilters;
                };
                BoardData.prototype.updatePriorityFilter = function (filter) {
                    this._boardFilters.setPriorityFilter(filter, this._priorities);
                    this._issueTable.filters = this._boardFilters;
                };
                BoardData.prototype.updateIssueTypeFilter = function (filter) {
                    this._boardFilters.setIssueTypeFilter(filter, this._issueTypes);
                    this._issueTable.filters = this._boardFilters;
                };
                BoardData.prototype.updateAssigneeFilter = function (filter) {
                    this._boardFilters.setAssigneeFilter(filter, this._assignees);
                    this._issueTable.filters = this._boardFilters;
                };
                BoardData.prototype.hideHideables = function () {
                    for (var _i = 0, _a = this.hideables; _i < _a.length; _i++) {
                        var hideable = _a[_i];
                        hideable.hide();
                    }
                };
                BoardData.prototype.registerHideable = function (hideable) {
                    this.hideables.push(hideable);
                };
                /**
                 * Checks whether a board state is valid for an issue
                 * @param projectCode the project code
                 * @param state the state to check
                 */
                BoardData.prototype.isValidStateForProject = function (projectCode, state) {
                    return this.boardProjects.forKey(projectCode).isValidState(state);
                };
                /**
                 * Gets a list of the valid issues for a state, that an issue can be moved before/after. For example we don't allow
                 * mixing of priority between issues from different projects. When swimlanes are used, we stay within the same swimlane,
                 * or we would have to change the swimlane selector (e.g. assignee, project, priority, component etc.) in the
                 * upstream jira issue.
                 *
                 * @param issueKey the key of the issue
                 * @param toState the board state we are moving to
                 * @returns {IssueData[]} the list of valid issues we can use for positioning
                 */
                BoardData.prototype.getValidMoveBeforeIssues = function (issueKey, toState) {
                    var moveIssue = this._issueTable.getIssue(issueKey);
                    return this._projects.getValidMoveBeforeIssues(this._issueTable, this._swimlane, moveIssue, toState);
                };
                return BoardData;
            })();
            exports_1("BoardData", BoardData);
        }
    }
});
//# sourceMappingURL=boardData.js.map
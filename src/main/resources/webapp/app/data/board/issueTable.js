System.register(["./issueData", './swimlaneIndexer', "../../common/indexed"], function(exports_1) {
    var issueData_1, swimlaneIndexer_1, indexed_1;
    var IssueTable, SwimlaneData, StateIssueCounter;
    return {
        setters:[
            function (issueData_1_1) {
                issueData_1 = issueData_1_1;
            },
            function (swimlaneIndexer_1_1) {
                swimlaneIndexer_1 = swimlaneIndexer_1_1;
            },
            function (indexed_1_1) {
                indexed_1 = indexed_1_1;
            }],
        execute: function() {
            IssueTable = (function () {
                /**
                 * Called when first loading a board
                 * @param _boardData
                 * @param _projects
                 * @param _filters
                 * @param _swimlane
                 * @param input
                 */
                function IssueTable(_boardData, _projects, _filters, _swimlane, input) {
                    this._boardData = _boardData;
                    this._projects = _projects;
                    this._filters = _filters;
                    this._swimlane = _swimlane;
                    this.internalFullRefresh(input, true);
                }
                /**
                 * Called when we receive the full table over the web socket
                 * @param input
                 */
                IssueTable.prototype.fullRefresh = function (projects, input) {
                    this._projects = projects;
                    this.internalFullRefresh(input, false);
                };
                Object.defineProperty(IssueTable.prototype, "issueTable", {
                    get: function () {
                        return this._issueTable;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueTable.prototype, "swimlaneTable", {
                    get: function () {
                        return this._swimlaneTable;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueTable.prototype, "totalIssuesByState", {
                    get: function () {
                        return this._totalIssuesByState;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueTable.prototype, "filters", {
                    set: function (filters) {
                        this._filters = filters;
                        for (var _i = 0, _a = this._allIssues.array; _i < _a.length; _i++) {
                            var issue = _a[_i];
                            issue.filtered = this._filters.filterIssue(issue);
                        }
                        if (this._swimlane) {
                            var indexer = this.createSwimlaneIndexer();
                            for (var _b = 0, _c = this._swimlaneTable; _b < _c.length; _b++) {
                                var swimlaneData = _c[_b];
                                swimlaneData.filtered = indexer.filter(swimlaneData);
                            }
                        }
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueTable.prototype, "swimlane", {
                    set: function (swimlane) {
                        this._swimlane = swimlane;
                        this.createTable();
                    },
                    enumerable: true,
                    configurable: true
                });
                IssueTable.prototype.toggleSwimlaneVisibility = function (swimlaneIndex) {
                    if (this._swimlaneTable) {
                        this._swimlaneTable[swimlaneIndex].toggleVisibility();
                    }
                };
                IssueTable.prototype.getIssue = function (issueKey) {
                    return this._allIssues.forKey(issueKey);
                };
                IssueTable.prototype.processTableChanges = function (boardData, changeSet) {
                    var storedSwimlaneVisibilities = this.storeSwimlaneVisibilities(false);
                    //Delete from the "all issues table"
                    var deletedIssues = this._allIssues.deleteKeys(changeSet.deletedIssueKeys);
                    if (changeSet.issueUpdates) {
                        for (var _i = 0, _a = changeSet.issueUpdates; _i < _a.length; _i++) {
                            var update = _a[_i];
                            var issue = this._allIssues.forKey(update.key);
                            if (!issue) {
                                console.log("Could not find issue to update " + update.key);
                                continue;
                            }
                            if (update.state) {
                                //The issue has moved its state, so we need to delete it from the old state column
                                deletedIssues.push(issue);
                            }
                        }
                    }
                    //Delete all the deleted issues from the project issue tables
                    //This also includes all the issues that have been moved
                    this._projects.deleteIssues(deletedIssues);
                    //Now do the actual application of the updates
                    if (changeSet.issueUpdates) {
                        for (var _b = 0, _c = changeSet.issueUpdates; _b < _c.length; _b++) {
                            var update = _c[_b];
                            var issue = this._allIssues.forKey(update.key);
                            if (!issue) {
                                console.log("Could not find issue to update " + update.key);
                                continue;
                            }
                            issue.applyUpdate(update);
                        }
                    }
                    //Add all the created issues
                    if (changeSet.issueAdds) {
                        for (var _d = 0, _e = changeSet.issueAdds; _d < _e.length; _d++) {
                            var add = _e[_d];
                            var issue = issueData_1.IssueData.createFromChangeSet(boardData, add);
                            this._allIssues.add(issue.key, issue);
                        }
                    }
                    //Now update the changed states
                    if (changeSet.stateChanges) {
                        var ownerProject = this._projects.ownerProject;
                        for (var projectCode in changeSet.stateChanges.indices) {
                            var projectStates = changeSet.stateChanges.forKey(projectCode);
                            var project = this._projects.boardProjects.forKey(projectCode);
                            for (var stateName in projectStates.indices) {
                                var boardState = project.mapStateStringToBoard(stateName);
                                var boardIndex = ownerProject.getOwnStateIndex(boardState);
                                project.updateStateIssues(boardIndex, projectStates.forKey(stateName));
                            }
                        }
                    }
                    this.createTable();
                    this.restoreSwimlaneVisibilities(storedSwimlaneVisibilities);
                };
                IssueTable.prototype.internalFullRefresh = function (input, initial) {
                    var _this = this;
                    var storedSwimlaneVisibilities = this.storeSwimlaneVisibilities(initial);
                    this._allIssues = new indexed_1.Indexed();
                    this._allIssues.indexMap(input.issues, function (key, data) {
                        return issueData_1.IssueData.createFullRefresh(_this._boardData, data);
                    });
                    this.createTable();
                    this.restoreSwimlaneVisibilities(storedSwimlaneVisibilities);
                };
                IssueTable.prototype.storeSwimlaneVisibilities = function (initial) {
                    var swimlaneVisibilities;
                    if (!initial && this._swimlane && this._swimlaneTable) {
                        //Store the visibilities from the users collapsing swimlanes
                        swimlaneVisibilities = {};
                        for (var _i = 0, _a = this._swimlaneTable; _i < _a.length; _i++) {
                            var swimlane = _a[_i];
                            swimlaneVisibilities[swimlane.name] = swimlane.visible;
                        }
                    }
                    return swimlaneVisibilities;
                };
                IssueTable.prototype.restoreSwimlaneVisibilities = function (storedSwimlaneVisibilities) {
                    if (storedSwimlaneVisibilities) {
                        //Restore the user defined visibilities
                        for (var _i = 0, _a = this._swimlaneTable; _i < _a.length; _i++) {
                            var swimlane = _a[_i];
                            swimlane.restoreVisibility(storedSwimlaneVisibilities);
                        }
                    }
                };
                IssueTable.prototype.createTable = function () {
                    if (this._swimlane) {
                        this._swimlaneTable = this.createSwimlaneTable();
                        this._issueTable = null;
                    }
                    else {
                        this._issueTable = this.createIssueTable();
                        this._swimlaneTable = null;
                    }
                };
                IssueTable.prototype.createIssueTable = function () {
                    var numStates = this._boardData.boardStates.length;
                    this._totalIssuesByState = [numStates];
                    var issueTable = new Array(numStates);
                    //Now copy across the issues for each project for each state
                    for (var stateIndex = 0; stateIndex < issueTable.length; stateIndex++) {
                        var counter = new StateIssueCounter();
                        var stateColumn = this.createIssueTableStateColumn(stateIndex, counter);
                        this._totalIssuesByState[stateIndex] = counter.count;
                        issueTable[stateIndex] = stateColumn;
                    }
                    return issueTable;
                };
                IssueTable.prototype.createIssueTableStateColumn = function (stateIndex, counter) {
                    var stateColumn = [];
                    for (var _i = 0, _a = this._boardData.boardProjects.array; _i < _a.length; _i++) {
                        var project = _a[_i];
                        var projectIssues = project.issueKeys;
                        var issueKeysForState = projectIssues[stateIndex];
                        for (var index = 0; index < issueKeysForState.length; index++) {
                            var issue = this._allIssues.forKey(issueKeysForState[index]);
                            stateColumn.push(issue);
                            counter.increment();
                            issue.filtered = this._filters.filterIssue(issue);
                        }
                    }
                    return stateColumn;
                };
                IssueTable.prototype.createSwimlaneTable = function () {
                    var numStates = this._boardData.boardStates.length;
                    this._totalIssuesByState = [numStates];
                    var indexer = this.createSwimlaneIndexer();
                    var swimlaneTable = indexer.swimlaneTable;
                    //Now copy across the issues for each project for each state
                    for (var stateIndex = 0; stateIndex < this._boardData.boardStates.length; stateIndex++) {
                        var counter = new StateIssueCounter();
                        this.createSwimlaneTableStateColumn(indexer, swimlaneTable, stateIndex, counter);
                        this._totalIssuesByState[stateIndex] = counter.count;
                    }
                    //Apply the filters to the swimlanes
                    for (var _i = 0; _i < swimlaneTable.length; _i++) {
                        var swimlaneData = swimlaneTable[_i];
                        swimlaneData.filtered = indexer.filter(swimlaneData);
                    }
                    return swimlaneTable;
                };
                IssueTable.prototype.createSwimlaneTableStateColumn = function (indexer, swimlaneTable, stateIndex, counter) {
                    for (var _i = 0, _a = this._boardData.boardProjects.array; _i < _a.length; _i++) {
                        var project = _a[_i];
                        var projectIssues = project.issueKeys;
                        var issueKeysForState = projectIssues[stateIndex];
                        for (var index = 0; index < issueKeysForState.length; index++) {
                            var issue = this._allIssues.forKey(issueKeysForState[index]);
                            var swimlaneIndex = indexer.swimlaneIndex(issue);
                            issue.filtered = this._filters.filterIssue(issue);
                            var targetSwimlane = swimlaneTable[swimlaneIndex];
                            targetSwimlane.issueTable[stateIndex].push(issue);
                            counter.increment();
                        }
                    }
                };
                IssueTable.prototype.createSwimlaneIndexer = function () {
                    return new swimlaneIndexer_1.SwimlaneIndexerFactory().createSwimlaneIndexer(this._swimlane, this._filters, this._boardData);
                };
                return IssueTable;
            })();
            exports_1("IssueTable", IssueTable);
            SwimlaneData = (function () {
                function SwimlaneData(boardData, name, index) {
                    this.boardData = boardData;
                    this._visible = true;
                    this._name = name;
                    this._index = index;
                    var states = boardData.boardStates.length;
                    this.issueTable = new Array(states);
                    for (var i = 0; i < states; i++) {
                        this.issueTable[i] = [];
                    }
                }
                SwimlaneData.prototype.toggleVisibility = function () {
                    this._visible = !this._visible;
                };
                Object.defineProperty(SwimlaneData.prototype, "visible", {
                    get: function () {
                        return this._visible;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(SwimlaneData.prototype, "name", {
                    get: function () {
                        return this._name;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(SwimlaneData.prototype, "index", {
                    get: function () {
                        return this._index;
                    },
                    enumerable: true,
                    configurable: true
                });
                SwimlaneData.prototype.resetState = function (stateIndex) {
                    this.issueTable[stateIndex] = [];
                };
                SwimlaneData.prototype.restoreVisibility = function (savedVisibilities) {
                    //When restoring the visibility, take into account that new swimlanes would not have been saved,
                    //and so do not appear in the map
                    this._visible = !(savedVisibilities[this._name] == false);
                };
                return SwimlaneData;
            })();
            exports_1("SwimlaneData", SwimlaneData);
            StateIssueCounter = (function () {
                function StateIssueCounter() {
                    this._count = 0;
                }
                StateIssueCounter.prototype.increment = function () {
                    this._count++;
                };
                Object.defineProperty(StateIssueCounter.prototype, "count", {
                    get: function () {
                        return this._count;
                    },
                    enumerable: true,
                    configurable: true
                });
                return StateIssueCounter;
            })();
        }
    }
});
//# sourceMappingURL=issueTable.js.map
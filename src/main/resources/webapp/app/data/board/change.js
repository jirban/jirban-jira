System.register(["./assignee", "../../common/indexed", "./blacklist", "./component"], function(exports_1) {
    var assignee_1, indexed_1, blacklist_1, component_1;
    var ChangeSet, IssueChange;
    return {
        setters:[
            function (assignee_1_1) {
                assignee_1 = assignee_1_1;
            },
            function (indexed_1_1) {
                indexed_1 = indexed_1_1;
            },
            function (blacklist_1_1) {
                blacklist_1 = blacklist_1_1;
            },
            function (component_1_1) {
                component_1 = component_1_1;
            }],
        execute: function() {
            ChangeSet = (function () {
                function ChangeSet(input) {
                    var changes = input.changes;
                    this._view = changes.view;
                    var issues = changes.issues;
                    if (issues) {
                        var newIssues = issues["new"];
                        var updatedIssues = issues.update;
                        var deletedIssues = issues.delete;
                        if (newIssues) {
                            this._issueAdds = new Array(newIssues.length);
                            for (var i = 0; i < newIssues.length; i++) {
                                this._issueAdds[i] = IssueChange.deserializeAdd(newIssues[i]);
                            }
                        }
                        if (updatedIssues) {
                            this._issueUpdates = new Array(updatedIssues.length);
                            for (var i = 0; i < updatedIssues.length; i++) {
                                this._issueUpdates[i] = IssueChange.deserializeUpdate(updatedIssues[i]);
                            }
                        }
                        if (deletedIssues) {
                            this._issueDeletes = deletedIssues;
                        }
                    }
                    if (changes.assignees) {
                        this._addedAssignees = new assignee_1.AssigneeDeserializer().deserialize(changes).array;
                    }
                    if (changes.components) {
                        this._addedComponents = new component_1.ComponentDeserializer().deserialize(changes).array;
                    }
                    var blacklist = changes.blacklist;
                    if (blacklist) {
                        this._blacklistChange = blacklist_1.BlacklistData.fromInput(blacklist);
                        if (blacklist["removed-issues"]) {
                            this._blacklistClearedIssues = blacklist["removed-issues"];
                        }
                    }
                    var stateChanges = changes.states;
                    this._stateChanges = new indexed_1.Indexed();
                    this._stateChanges.indexMap(stateChanges, function (projectCode, projectEntry) {
                        var projStates = new indexed_1.Indexed();
                        projStates.indexMap(projectEntry, function (stateName, stateEntry) {
                            return stateEntry;
                        });
                        return projStates;
                    });
                }
                Object.defineProperty(ChangeSet.prototype, "view", {
                    get: function () {
                        return this._view;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "issueAdds", {
                    get: function () {
                        return this._issueAdds;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "issueUpdates", {
                    get: function () {
                        return this._issueUpdates;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "deletedIssueKeys", {
                    /**
                     * Gets the issues which should be totally removed from the board
                     */
                    get: function () {
                        var deleteKeys = [];
                        if (this._blacklistChange && this._blacklistChange) {
                            deleteKeys = deleteKeys.concat(this._blacklistChange.issues);
                        }
                        if (this._blacklistClearedIssues) {
                            deleteKeys = deleteKeys.concat(this._blacklistClearedIssues);
                        }
                        if (this._issueDeletes) {
                            deleteKeys = deleteKeys.concat(this._issueDeletes);
                        }
                        return deleteKeys;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "blacklistChanges", {
                    get: function () {
                        if (this._blacklistClearedIssues || this._blacklistChange) {
                            return true;
                        }
                        return false;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "addedAssignees", {
                    get: function () {
                        return this._addedAssignees;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "addedComponents", {
                    get: function () {
                        return this._addedComponents;
                    },
                    enumerable: true,
                    configurable: true
                });
                ChangeSet.prototype.addToBlacklist = function (blacklist) {
                    blacklist.addChanges(this._blacklistChange);
                };
                Object.defineProperty(ChangeSet.prototype, "stateChanges", {
                    get: function () {
                        return this._stateChanges;
                    },
                    enumerable: true,
                    configurable: true
                });
                return ChangeSet;
            })();
            exports_1("ChangeSet", ChangeSet);
            IssueChange = (function () {
                function IssueChange(key, type, priority, summary, state, assignee, components) {
                    this._unassigned = false;
                    this._clearedComponents = false;
                    this._key = key;
                    this._type = type;
                    this._priority = priority;
                    this._summary = summary;
                    this._state = state;
                    this._assignee = assignee;
                    this._components = components;
                }
                Object.defineProperty(IssueChange.prototype, "key", {
                    get: function () {
                        return this._key;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueChange.prototype, "type", {
                    get: function () {
                        return this._type;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueChange.prototype, "priority", {
                    get: function () {
                        return this._priority;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueChange.prototype, "summary", {
                    get: function () {
                        return this._summary;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueChange.prototype, "state", {
                    get: function () {
                        return this._state;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueChange.prototype, "assignee", {
                    get: function () {
                        return this._assignee;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueChange.prototype, "components", {
                    get: function () {
                        return this._components;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueChange.prototype, "unassigned", {
                    get: function () {
                        return this._unassigned;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueChange.prototype, "clearedComponents", {
                    get: function () {
                        return this._clearedComponents;
                    },
                    enumerable: true,
                    configurable: true
                });
                IssueChange.deserializeAdd = function (input) {
                    return new IssueChange(input.key, input.type, input.priority, input.summary, input.state, input.assignee, input.components);
                };
                IssueChange.deserializeUpdate = function (input) {
                    var change = new IssueChange(input.key, input.type, input.priority, input.summary, input.state, input.assignee, input.components);
                    if (input.unassigned) {
                        change._unassigned = true;
                    }
                    if (input["clear-components"]) {
                        change._clearedComponents = true;
                    }
                    return change;
                };
                return IssueChange;
            })();
            exports_1("IssueChange", IssueChange);
        }
    }
});
//# sourceMappingURL=change.js.map
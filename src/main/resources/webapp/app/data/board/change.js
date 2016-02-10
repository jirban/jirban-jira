System.register([], function(exports_1) {
    var __extends = (this && this.__extends) || function (d, b) {
        for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
    var ChangeSet, IssueChange, IssueDetailChange, IssueAdd, IssueUpdate, IssueDelete;
    return {
        setters:[],
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
                            this._issueAdds = new IssueAdd[newIssues.length];
                            for (var i = 0; i < newIssues.length; i++) {
                                this._issueAdds[i] = IssueAdd.deserialize(newIssues[i]);
                            }
                        }
                        if (updatedIssues) {
                            this._issueUpdates = new IssueUpdate[updatedIssues.length];
                            for (var i = 0; i < updatedIssues.length; i++) {
                                this._issueUpdates[i] = IssueUpdate.deserialize(newIssues[i]);
                            }
                        }
                        if (deletedIssues) {
                            this._issueDeletes = new IssueDelete[deletedIssues.length];
                            for (var i = 0; i < deletedIssues.length; i++) {
                                this._issueDeletes[i] = IssueDelete.deserialize(deletedIssues[i]);
                            }
                        }
                    }
                    var blacklist = changes.blacklist;
                    if (blacklist) {
                        if (blacklist.states) {
                            this._blacklistStates = blacklist.states;
                        }
                        if (blacklist["issue-types"]) {
                            this._blacklistTypes = blacklist["issue-types"];
                        }
                        if (blacklist.priorities) {
                            this._blacklistPriorities = blacklist.priorities;
                        }
                        if (blacklist.issues) {
                            this._blacklistIssues = blacklist.issues;
                        }
                        if (blacklist["removed-issues"]) {
                            this._blacklistClearedIssues = blacklist["removed-issues"];
                        }
                    }
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
                Object.defineProperty(ChangeSet.prototype, "issueDeletes", {
                    get: function () {
                        return this._issueDeletes;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "blacklistStates", {
                    get: function () {
                        return this._blacklistStates;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "blacklistTypes", {
                    get: function () {
                        return this._blacklistTypes;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "blacklistPriorities", {
                    get: function () {
                        return this._blacklistPriorities;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "blacklistIssues", {
                    get: function () {
                        return this._blacklistIssues;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "blacklistClearedIssues", {
                    get: function () {
                        return this._blacklistClearedIssues;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(ChangeSet.prototype, "blacklistChanges", {
                    get: function () {
                        if (this._blacklistClearedIssues || this._blacklistIssues || this._blacklistPriorities ||
                            this._blacklistStates || this._blacklistTypes) {
                            return true;
                        }
                        return false;
                    },
                    enumerable: true,
                    configurable: true
                });
                return ChangeSet;
            })();
            exports_1("ChangeSet", ChangeSet);
            IssueChange = (function () {
                function IssueChange(key) {
                    this._key = key;
                }
                Object.defineProperty(IssueChange.prototype, "key", {
                    get: function () {
                        return this._key;
                    },
                    enumerable: true,
                    configurable: true
                });
                return IssueChange;
            })();
            exports_1("IssueChange", IssueChange);
            IssueDetailChange = (function (_super) {
                __extends(IssueDetailChange, _super);
                function IssueDetailChange(key, type, priority, summary, state) {
                    _super.call(this, key);
                    this._type = type;
                    this._priority = priority;
                    this._summary = summary;
                    this._state = state;
                }
                Object.defineProperty(IssueDetailChange.prototype, "type", {
                    get: function () {
                        return this._type;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueDetailChange.prototype, "priority", {
                    get: function () {
                        return this._priority;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueDetailChange.prototype, "summary", {
                    get: function () {
                        return this._summary;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueDetailChange.prototype, "state", {
                    get: function () {
                        return this._state;
                    },
                    enumerable: true,
                    configurable: true
                });
                return IssueDetailChange;
            })(IssueChange);
            exports_1("IssueDetailChange", IssueDetailChange);
            IssueAdd = (function (_super) {
                __extends(IssueAdd, _super);
                function IssueAdd(key, type, priority, summary, state) {
                    _super.call(this, key, type, priority, summary, state);
                }
                IssueAdd.deserialize = function (input) {
                    //TODO state!!!
                    return new IssueAdd(input.key, input.type, input.priority, input.summary, input.state);
                };
                return IssueAdd;
            })(IssueDetailChange);
            exports_1("IssueAdd", IssueAdd);
            IssueUpdate = (function (_super) {
                __extends(IssueUpdate, _super);
                function IssueUpdate(key, type, priority, summary, state) {
                    _super.call(this, key, type, priority, summary, state);
                }
                IssueUpdate.deserialize = function (input) {
                    //TODO state!!!
                    return new IssueUpdate(input.key, input.type, input.priority, input.summary, input.state);
                };
                return IssueUpdate;
            })(IssueDetailChange);
            exports_1("IssueUpdate", IssueUpdate);
            IssueDelete = (function (_super) {
                __extends(IssueDelete, _super);
                function IssueDelete(key) {
                    _super.call(this, key);
                }
                IssueDelete.deserialize = function (input) {
                    return new IssueDelete(input.key);
                };
                return IssueDelete;
            })(IssueChange);
            exports_1("IssueDelete", IssueDelete);
        }
    }
});
//# sourceMappingURL=change.js.map
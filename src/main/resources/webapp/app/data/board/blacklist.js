System.register([], function(exports_1) {
    var BlacklistData;
    return {
        setters:[],
        execute: function() {
            BlacklistData = (function () {
                function BlacklistData() {
                    this._states = [];
                    this._issueTypes = [];
                    this._priorities = [];
                    this._issues = [];
                }
                BlacklistData.fromInput = function (input) {
                    if (input) {
                        return null;
                    }
                    var bd = new BlacklistData();
                    if (input.states) {
                        bd._states = input.states;
                    }
                    if (input.priorities) {
                        bd._priorities = input.priorities;
                    }
                    if (input["issue-types"]) {
                        bd._issueTypes = input["issue-types"];
                    }
                    if (input.issues) {
                        bd._issues = input.issues;
                    }
                    return bd;
                };
                BlacklistData.fromChangeSet = function (changeSet) {
                    var bd = new BlacklistData();
                    if (changeSet.blacklistStates) {
                        bd._states = changeSet.blacklistStates;
                    }
                    if (changeSet.blacklistPriorities) {
                        bd._priorities = changeSet.blacklistPriorities;
                    }
                    if (changeSet.blacklistTypes) {
                        bd._issueTypes = changeSet.blacklistTypes;
                    }
                    if (changeSet.blacklistIssues) {
                        bd._issues = changeSet.blacklistIssues;
                    }
                    return bd;
                };
                Object.defineProperty(BlacklistData.prototype, "states", {
                    get: function () {
                        return this._states;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BlacklistData.prototype, "issueTypes", {
                    get: function () {
                        return this._issueTypes;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BlacklistData.prototype, "priorities", {
                    get: function () {
                        return this._priorities;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BlacklistData.prototype, "issues", {
                    get: function () {
                        return this._issues;
                    },
                    enumerable: true,
                    configurable: true
                });
                return BlacklistData;
            })();
            exports_1("BlacklistData", BlacklistData);
        }
    }
});
//# sourceMappingURL=blacklist.js.map
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
                    if (!input) {
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
                BlacklistData.prototype.addChangeSet = function (changeSet) {
                    //Use slice to copy the arrays here to avoid side-effects
                    if (changeSet.blacklistStates) {
                        this._states = changeSet.blacklistStates.slice();
                    }
                    if (changeSet.blacklistPriorities) {
                        this._priorities = changeSet.blacklistPriorities.slice();
                    }
                    if (changeSet.blacklistTypes) {
                        this._issueTypes = changeSet.blacklistTypes.slice();
                    }
                    if (changeSet.blacklistIssues) {
                        this._issues = changeSet.blacklistIssues.slice();
                    }
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
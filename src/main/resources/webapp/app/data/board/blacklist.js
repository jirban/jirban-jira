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
                    changeSet.addToBlacklist(this);
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
                BlacklistData.prototype.addChanges = function (change) {
                    if (change.issueTypes.length > 0) {
                        this._issueTypes = this._issueTypes.concat(change.issueTypes.slice());
                    }
                    if (change.priorities) {
                        this._priorities = this._priorities.concat(change.priorities.slice());
                    }
                    if (change.states) {
                        this._states = this._states.concat(change.states.slice());
                    }
                    if (change.issues) {
                        this._issues = this._issues.concat(change.issues.slice());
                    }
                };
                return BlacklistData;
            })();
            exports_1("BlacklistData", BlacklistData);
        }
    }
});
//# sourceMappingURL=blacklist.js.map
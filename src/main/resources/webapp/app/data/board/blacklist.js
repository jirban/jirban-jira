System.register([], function(exports_1) {
    var BlacklistData;
    return {
        setters:[],
        execute: function() {
            BlacklistData = (function () {
                function BlacklistData(input) {
                    this._states = [];
                    this._issueTypes = [];
                    this._priorities = [];
                    this._issues = [];
                    if (!input) {
                        return;
                    }
                    if (input.states) {
                        this._states = input.states;
                    }
                    if (input.priorities) {
                        this._priorities = input.priorities;
                    }
                    if (input["issue-types"]) {
                        this._issueTypes = input["issue-types"];
                    }
                    if (input.issues) {
                        this._issues = input.issues;
                    }
                }
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
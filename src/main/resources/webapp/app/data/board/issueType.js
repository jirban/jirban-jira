System.register(['../../common/indexed'], function(exports_1) {
    var indexed_1;
    var IssueType, IssueTypeDeserializer;
    return {
        setters:[
            function (indexed_1_1) {
                indexed_1 = indexed_1_1;
            }],
        execute: function() {
            IssueType = (function () {
                function IssueType(name, icon) {
                    this._name = name;
                    this._icon = icon;
                }
                Object.defineProperty(IssueType.prototype, "name", {
                    get: function () {
                        return this._name;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueType.prototype, "icon", {
                    get: function () {
                        return this._icon;
                    },
                    enumerable: true,
                    configurable: true
                });
                return IssueType;
            })();
            exports_1("IssueType", IssueType);
            IssueTypeDeserializer = (function () {
                function IssueTypeDeserializer() {
                }
                IssueTypeDeserializer.prototype.deserialize = function (input) {
                    var issueTypes = new indexed_1.Indexed();
                    issueTypes.indexArray(input["issue-types"], function (entry) {
                        return new IssueType(entry.name, entry.icon);
                    }, function (priority) {
                        return priority.name;
                    });
                    return issueTypes;
                };
                return IssueTypeDeserializer;
            })();
            exports_1("IssueTypeDeserializer", IssueTypeDeserializer);
        }
    }
});
//# sourceMappingURL=issueType.js.map
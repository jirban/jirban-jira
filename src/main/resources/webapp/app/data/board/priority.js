System.register(['../../common/indexed'], function(exports_1) {
    var indexed_1;
    var Priority, PriorityDeserializer;
    return {
        setters:[
            function (indexed_1_1) {
                indexed_1 = indexed_1_1;
            }],
        execute: function() {
            Priority = (function () {
                function Priority(name, icon) {
                    this._name = name;
                    this._icon = icon;
                }
                Object.defineProperty(Priority.prototype, "name", {
                    get: function () {
                        return this._name;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Priority.prototype, "icon", {
                    get: function () {
                        return this._icon;
                    },
                    enumerable: true,
                    configurable: true
                });
                return Priority;
            })();
            exports_1("Priority", Priority);
            PriorityDeserializer = (function () {
                function PriorityDeserializer() {
                }
                PriorityDeserializer.prototype.deserialize = function (input) {
                    var priorities = new indexed_1.Indexed();
                    priorities.indexArray(input.priorities, function (entry) {
                        return new Priority(entry.name, entry.icon);
                    }, function (priority) {
                        return priority.name;
                    });
                    return priorities;
                };
                return PriorityDeserializer;
            })();
            exports_1("PriorityDeserializer", PriorityDeserializer);
        }
    }
});
//# sourceMappingURL=priority.js.map
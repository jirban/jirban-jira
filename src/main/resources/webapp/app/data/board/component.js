System.register(["../../common/indexed"], function(exports_1) {
    var indexed_1;
    var Component, ComponentDeserializer;
    return {
        setters:[
            function (indexed_1_1) {
                indexed_1 = indexed_1_1;
            }],
        execute: function() {
            Component = (function () {
                function Component(name) {
                    this._name = name;
                }
                Object.defineProperty(Component.prototype, "name", {
                    get: function () {
                        return this._name;
                    },
                    enumerable: true,
                    configurable: true
                });
                return Component;
            })();
            exports_1("Component", Component);
            ComponentDeserializer = (function () {
                function ComponentDeserializer() {
                }
                ComponentDeserializer.prototype.deserialize = function (input) {
                    var components = new indexed_1.Indexed();
                    components.indexArray(input.components, function (entry) {
                        return new Component(entry);
                    }, function (component) {
                        return component.name;
                    });
                    return components;
                };
                return ComponentDeserializer;
            })();
            exports_1("ComponentDeserializer", ComponentDeserializer);
        }
    }
});
//# sourceMappingURL=component.js.map
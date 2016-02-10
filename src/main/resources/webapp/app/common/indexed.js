System.register([], function(exports_1) {
    var Indexed, KeyAndIndex;
    return {
        setters:[],
        execute: function() {
            /**
             * Container for an array, and a lookup of the array index by key
             */
            Indexed = (function () {
                function Indexed() {
                    this._array = [];
                    this._indices = {};
                }
                /**
                 * Creates an index where the input is an array of entries
                 * @param input the array input
                 * @param factory function to create the entries of type T
                 * @param keyValue function to get the key to index by
                 */
                Indexed.prototype.indexArray = function (input, factory, keyValue) {
                    var i = 0;
                    for (var _i = 0; _i < input.length; _i++) {
                        var entry = input[_i];
                        var value = factory(entry);
                        var key = keyValue(value);
                        this._array.push(value);
                        this._indices[key] = i++;
                    }
                };
                /**
                 * Creates an index where the input is a map of entries
                 * @param input the array input
                 * @param factory function to create the entries of type T
                 * @param keyValue function to get the key to index by
                 */
                Indexed.prototype.indexMap = function (input, factory) {
                    var i = 0;
                    for (var key in input) {
                        var value = factory(key, input[key]);
                        this._array.push(value);
                        this._indices[key] = i++;
                    }
                };
                Indexed.prototype.forKey = function (key) {
                    var index = this._indices[key];
                    if (isNaN(index)) {
                        return null;
                    }
                    return this._array[index];
                };
                Indexed.prototype.forIndex = function (index) {
                    return this._array[index];
                };
                /**
                 * Deletes the entries with the selected keys
                 * @param keys the keys to remove
                 */
                Indexed.prototype.deleteKeys = function (keys) {
                    var deleted = [];
                    var indices = [];
                    for (var _i = 0; _i < keys.length; _i++) {
                        var key = keys[_i];
                        var index = this._indices[key];
                        if (index) {
                            delete this._indices[key];
                            indices.push(index);
                            keys.push(key);
                        }
                    }
                    if (deleted.length > 0) {
                        indices.sort();
                        for (var i = indices.length - 1; i >= 0; i--) {
                            this.array.splice(indices[i], 1);
                        }
                        return deleted;
                    }
                };
                Object.defineProperty(Indexed.prototype, "array", {
                    get: function () {
                        return this._array;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Indexed.prototype, "indices", {
                    get: function () {
                        return this._indices;
                    },
                    enumerable: true,
                    configurable: true
                });
                return Indexed;
            })();
            exports_1("Indexed", Indexed);
            KeyAndIndex = (function () {
                function KeyAndIndex(index, key) {
                    this.index = index;
                    this.key = key;
                }
                return KeyAndIndex;
            })();
        }
    }
});
//# sourceMappingURL=indexed.js.map
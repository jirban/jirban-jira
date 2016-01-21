System.register(['../../common/indexed'], function(exports_1) {
    var indexed_1;
    var NO_ASSIGNEE, Assignee, AssigneeDeserializer;
    return {
        setters:[
            function (indexed_1_1) {
                indexed_1 = indexed_1_1;
            }],
        execute: function() {
            exports_1("NO_ASSIGNEE", NO_ASSIGNEE = "$no$assignee");
            Assignee = (function () {
                function Assignee(key, email, avatar, name) {
                    this._key = key;
                    this._email = email;
                    this._avatar = avatar;
                    this._name = name;
                    this._initials = this.calculateInitials();
                }
                Object.defineProperty(Assignee.prototype, "key", {
                    get: function () {
                        return this._key;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Assignee.prototype, "email", {
                    get: function () {
                        return this._email;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Assignee.prototype, "avatar", {
                    get: function () {
                        return this._avatar;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Assignee.prototype, "name", {
                    get: function () {
                        return this._name;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Assignee.prototype, "initials", {
                    get: function () {
                        return this._initials;
                    },
                    enumerable: true,
                    configurable: true
                });
                Assignee.prototype.calculateInitials = function () {
                    var name = this._name;
                    var arr = name.split(" ");
                    if (arr.length == 1) {
                        var ret_1 = "";
                        for (var i = 0; i < 3 && i < name.length; i++) {
                            var char = ret_1[i];
                            if (i == 0) {
                                char = char.toUpperCase();
                            }
                            else {
                                char = char.toLowerCase();
                            }
                        }
                        return ret_1;
                    }
                    var ret = "";
                    for (var i = 0; i < 3 && i < arr.length; i++) {
                        ret = ret + arr[i][0];
                    }
                    return ret.toUpperCase();
                };
                return Assignee;
            })();
            exports_1("Assignee", Assignee);
            AssigneeDeserializer = (function () {
                function AssigneeDeserializer() {
                }
                AssigneeDeserializer.prototype.deserialize = function (input) {
                    var assignees = new indexed_1.Indexed();
                    assignees.indexArray(input.assignees, function (entry) {
                        return new Assignee(entry.key, entry.email, entry.avatar, entry.name);
                    }, function (assignee) {
                        return assignee.key;
                    });
                    return assignees;
                };
                return AssigneeDeserializer;
            })();
            exports_1("AssigneeDeserializer", AssigneeDeserializer);
        }
    }
});
//# sourceMappingURL=assignee.js.map
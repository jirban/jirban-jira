System.register(["angular2/src/facade/lang"], function(exports_1) {
    var lang_1;
    var IssueData;
    return {
        setters:[
            function (lang_1_1) {
                lang_1 = lang_1_1;
            }],
        execute: function() {
            IssueData = (function () {
                function IssueData(boardData, input) {
                    this._filtered = false;
                    this._boardData = boardData;
                    this._key = input.key;
                    var index = this._key.lastIndexOf("-");
                    this._projectCode = this._key.substring(0, index);
                    this._statusIndex = input.state;
                    this._summary = input.summary;
                    this._assignee = boardData.assignees.forIndex(input.assignee);
                    this._priority = boardData.priorities.forIndex(input.priority);
                    this._type = boardData.issueTypes.forIndex(input.type);
                    var project = boardData.boardProjects.forKey(this._projectCode);
                    if (project) {
                        this._colour = project.colour;
                    }
                    var linkedIssues = input["linked-issues"];
                    if (!!linkedIssues && linkedIssues.length > 0) {
                        this._linked = [];
                        for (var i = 0; i < linkedIssues.length; i++) {
                            this._linked.push(new IssueData(boardData, linkedIssues[i]));
                        }
                    }
                }
                Object.defineProperty(IssueData.prototype, "key", {
                    //Plain getters
                    get: function () {
                        return this._key;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "projectCode", {
                    get: function () {
                        return this._projectCode;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "colour", {
                    get: function () {
                        return this._colour;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "summary", {
                    get: function () {
                        return this._summary;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "assignee", {
                    get: function () {
                        return this._assignee;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "priority", {
                    get: function () {
                        return this._priority;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "type", {
                    get: function () {
                        return this._type;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "statusIndex", {
                    get: function () {
                        return this._statusIndex;
                    },
                    set: function (index) {
                        this._statusIndex = index;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "boardData", {
                    get: function () {
                        return this._boardData;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "linkedIssues", {
                    get: function () {
                        return this._linked;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "filtered", {
                    get: function () {
                        return this._filtered;
                    },
                    set: function (filtered) {
                        this._filtered = filtered;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "boardStatus", {
                    //'Advanced'/'Nested' getters
                    get: function () {
                        //The state we map to in the board (only for board projects)
                        var project = this._boardData.boardProjects.forKey(this._projectCode);
                        var myStatusName = project.states.forIndex(this._statusIndex);
                        var boardStatus = project.mapStateStringToBoard(myStatusName);
                        return boardStatus;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "ownStatus", {
                    get: function () {
                        //Used to report the status in mouse-over a card
                        //Note that the main projects which are not the 'owner', will report their own status rather than
                        // the mapped owner column they appear in
                        var project = this._boardData.linkedProjects[this._projectCode];
                        if (!project) {
                            project = this._boardData.boardProjects.forKey(this._projectCode);
                        }
                        return project.getStateText(this._statusIndex);
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "assigneeName", {
                    get: function () {
                        if (!this._assignee) {
                            return "Unassigned";
                        }
                        return this._assignee.name;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "assigneeAvatar", {
                    get: function () {
                        if (!this._assignee) {
                            return "images/person-4x.png";
                        }
                        return this._assignee.avatar;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "assigneeInitials", {
                    get: function () {
                        if (!this._assignee) {
                            return "None";
                        }
                        return this._assignee.initials;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "priorityName", {
                    get: function () {
                        return this._priority.name;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "priorityUrl", {
                    get: function () {
                        return this._boardData.jiraUrl + this._priority.icon;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "typeName", {
                    get: function () {
                        return this._type.name;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "typeUrl", {
                    get: function () {
                        return this._boardData.jiraUrl + this._type.icon;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueData.prototype, "linkedProject", {
                    get: function () {
                        return this._boardData.linkedProjects[this._projectCode];
                    },
                    enumerable: true,
                    configurable: true
                });
                IssueData.prototype.isDefined = function (index) {
                    if (index) {
                        return true;
                    }
                    if (lang_1.isNumber(index)) {
                        return true;
                    }
                    return false;
                };
                Object.defineProperty(IssueData.prototype, "boardProject", {
                    get: function () {
                        return this._boardData.boardProjects.forKey(this._projectCode);
                    },
                    enumerable: true,
                    configurable: true
                });
                //Update functions
                IssueData.prototype.applyUpdate = function (update) {
                    if (update.type) {
                        this._type = this._boardData.issueTypes.forKey(update.type);
                    }
                    if (update.priority) {
                        this._priority = this._boardData.priorities.forKey(update.priority);
                    }
                    if (update.summary) {
                        this._summary = update.summary;
                    }
                    if (update.state) {
                        var project = this.boardProject;
                        this._statusIndex = project.getOwnStateIndex(update.state);
                    }
                    if (update.assignee) {
                        this._assignee = this.boardData.assignees.forKey(update.assignee);
                    }
                };
                return IssueData;
            })();
            exports_1("IssueData", IssueData);
        }
    }
});
//# sourceMappingURL=issueData.js.map
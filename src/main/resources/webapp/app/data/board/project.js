System.register(['../../common/indexed', "./swimlaneIndexer"], function(exports_1) {
    var __extends = (this && this.__extends) || function (d, b) {
        for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
    var indexed_1, swimlaneIndexer_1;
    var ownKeys, Projects, Project, BoardProject, OwnerProject, OtherMainProject, LinkedProject, ProjectDeserializer;
    return {
        setters:[
            function (indexed_1_1) {
                indexed_1 = indexed_1_1;
            },
            function (swimlaneIndexer_1_1) {
                swimlaneIndexer_1 = swimlaneIndexer_1_1;
            }],
        execute: function() {
            /**
             * Registry of project related data got from the server
             */
            Projects = (function () {
                function Projects(owner, boardProjects, linkedProjects) {
                    this._boardProjects = new indexed_1.Indexed();
                    this._linkedProjects = {};
                    this._boardProjectCodes = [];
                    this._owner = owner;
                    this._boardProjects = boardProjects;
                    this._linkedProjects = linkedProjects;
                    for (var key in boardProjects.indices) {
                        this._boardProjectCodes.push(key);
                    }
                }
                Object.defineProperty(Projects.prototype, "owner", {
                    get: function () {
                        return this._owner;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Projects.prototype, "boardStates", {
                    get: function () {
                        return this._boardProjects.forKey(this.owner).states;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Projects.prototype, "linkedProjects", {
                    get: function () {
                        return this._linkedProjects;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Projects.prototype, "boardProjects", {
                    get: function () {
                        return this._boardProjects;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Projects.prototype, "boardProjectCodes", {
                    get: function () {
                        return this._boardProjectCodes;
                    },
                    enumerable: true,
                    configurable: true
                });
                Projects.prototype.getValidMoveBeforeIssues = function (issueTable, swimlane, moveIssue, toState) {
                    var project = this._boardProjects.forKey(moveIssue.projectCode);
                    var toStateIndex = this.boardStates.indices[toState];
                    return project.getValidMoveBeforeIssues(issueTable, swimlane, moveIssue, toStateIndex);
                };
                return Projects;
            })();
            exports_1("Projects", Projects);
            /**
             * Base class for projects
             */
            Project = (function () {
                function Project(code, states) {
                    this._code = code;
                    this._states = states;
                }
                Object.defineProperty(Project.prototype, "code", {
                    get: function () {
                        return this._code;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Project.prototype, "states", {
                    get: function () {
                        return this._states;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(Project.prototype, "statesLength", {
                    get: function () {
                        return this._states.array.length;
                    },
                    enumerable: true,
                    configurable: true
                });
                Project.prototype.getStateText = function (index) {
                    return this._states.forIndex(index);
                };
                return Project;
            })();
            exports_1("Project", Project);
            /**
             * These are the projects whose issues will appear as cards on the board.
             */
            BoardProject = (function (_super) {
                __extends(BoardProject, _super);
                function BoardProject(code, colour, states, issueKeys) {
                    _super.call(this, code, states);
                    this._colour = colour;
                    this._issueKeys = issueKeys;
                }
                Object.defineProperty(BoardProject.prototype, "colour", {
                    get: function () {
                        return this._colour;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(BoardProject.prototype, "issueKeys", {
                    get: function () {
                        return this._issueKeys;
                    },
                    enumerable: true,
                    configurable: true
                });
                BoardProject.prototype.getValidMoveBeforeIssues = function (issueTable, swimlane, moveIssue, toStateIndex) {
                    var issueKeys = this._issueKeys[toStateIndex];
                    var validIssues = [];
                    var swimlaneMatcher = new swimlaneIndexer_1.SwimlaneIndexerFactory().createSwimlaneMatcher(swimlane, moveIssue);
                    for (var _i = 0; _i < issueKeys.length; _i++) {
                        var issueKey = issueKeys[_i];
                        var issue = issueTable.getIssue(issueKey);
                        if (!swimlaneMatcher || swimlaneMatcher.matchesSwimlane(issue)) {
                            validIssues.push(issue);
                        }
                    }
                    return validIssues;
                };
                return BoardProject;
            })(Project);
            exports_1("BoardProject", BoardProject);
            /**
             * This is the 'owner' project.
             * Its states will map directly onto the board states.
             */
            OwnerProject = (function (_super) {
                __extends(OwnerProject, _super);
                function OwnerProject(code, colour, states, issueKeys) {
                    _super.call(this, code, colour, states, issueKeys);
                }
                OwnerProject.prototype.isValidState = function (state) {
                    return !!this._states.forKey(state);
                };
                OwnerProject.prototype.mapStateStringToBoard = function (state) {
                    return state;
                };
                return OwnerProject;
            })(BoardProject);
            /**
             * This is for other projects whose issues appear as cards on the board.
             * Its states will need mapping onto the board states.
             */
            OtherMainProject = (function (_super) {
                __extends(OtherMainProject, _super);
                function OtherMainProject(code, colour, states, issueKeys, boardStatesToProjectState, projectStatesToBoardState) {
                    _super.call(this, code, colour, states, issueKeys);
                    this._boardStatesToProjectState = {};
                    this._projectStatesToBoardState = {};
                    this._boardStatesToProjectState = boardStatesToProjectState;
                    this._projectStatesToBoardState = projectStatesToBoardState;
                }
                OtherMainProject.prototype.isValidState = function (state) {
                    return !!this._boardStatesToProjectState[state];
                };
                OtherMainProject.prototype.mapStateStringToBoard = function (state) {
                    return this._projectStatesToBoardState[state];
                };
                return OtherMainProject;
            })(BoardProject);
            /**
             * A linked projects whose issues are linked to from the main board projects.
             */
            LinkedProject = (function (_super) {
                __extends(LinkedProject, _super);
                function LinkedProject(code, states) {
                    _super.call(this, code, states);
                }
                return LinkedProject;
            })(Project);
            exports_1("LinkedProject", LinkedProject);
            ProjectDeserializer = (function () {
                function ProjectDeserializer() {
                }
                ProjectDeserializer.prototype.deserialize = function (input) {
                    var projectsInput = input.projects;
                    var owner = projectsInput.owner;
                    var mainProjectsInput = projectsInput.main;
                    var boardProjects = this.deserializeBoardProjects(owner, mainProjectsInput);
                    var linkedProjects = this.deserializeLinkedProjects(projectsInput);
                    return new Projects(owner, boardProjects, linkedProjects);
                };
                ProjectDeserializer.prototype.deserializeLinkedProjects = function (projectsInput) {
                    var linkedProjects = {};
                    var linkedInput = projectsInput.linked;
                    for (var key in linkedInput) {
                        var entry = linkedInput[key];
                        var states = this.deserializeStateArray(entry.states);
                        linkedProjects[key] = new LinkedProject(key, states);
                    }
                    return linkedProjects;
                };
                ProjectDeserializer.prototype.deserializeBoardProjects = function (owner, mainProjectsInput) {
                    var _this = this;
                    var boardProjects = new indexed_1.Indexed();
                    boardProjects.indexMap(mainProjectsInput, function (key, projectInput) {
                        var colour = projectInput.colour;
                        var states = _this.deserializeStateArray(projectInput.states);
                        var issues = projectInput.issues;
                        if (key === owner) {
                            return new OwnerProject(key, colour, states, issues);
                        }
                        else {
                            var boardStatesToProjectState = {};
                            var projectStatesToBoardState = {};
                            var stateLinksInput = projectInput["state-links"];
                            for (var boardState in stateLinksInput) {
                                var projectState = stateLinksInput[boardState];
                                if (projectState) {
                                    boardStatesToProjectState[boardState] = projectState;
                                    projectStatesToBoardState[projectState] = boardState;
                                }
                            }
                            return new OtherMainProject(key, colour, states, issues, boardStatesToProjectState, projectStatesToBoardState);
                        }
                    });
                    return boardProjects;
                };
                ProjectDeserializer.prototype.deserializeStateArray = function (statesInput) {
                    var states = new indexed_1.Indexed();
                    states.indexArray(statesInput, function (state) {
                        return state;
                    }, function (state) {
                        return state;
                    });
                    return states;
                };
                return ProjectDeserializer;
            })();
            exports_1("ProjectDeserializer", ProjectDeserializer);
        }
    }
});
//# sourceMappingURL=project.js.map
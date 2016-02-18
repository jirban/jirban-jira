System.register(['angular2/core', 'angular2/router', '../../services/boardsService', "angular2/common", "../../common/indexed"], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, router_1, boardsService_1, common_1, common_2, indexed_1;
    var ConfigComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (router_1_1) {
                router_1 = router_1_1;
            },
            function (boardsService_1_1) {
                boardsService_1 = boardsService_1_1;
            },
            function (common_1_1) {
                common_1 = common_1_1;
                common_2 = common_1_1;
            },
            function (indexed_1_1) {
                indexed_1 = indexed_1_1;
            }],
        execute: function() {
            ConfigComponent = (function () {
                function ConfigComponent(_boardsService, _router, _formBuilder) {
                    this._boardsService = _boardsService;
                    this._router = _router;
                    this._formBuilder = _formBuilder;
                    this.selected = -1;
                    this.edit = false;
                    this.deleting = false;
                    this.jsonErrorEdit = null;
                    this.jsonErrorCreate = null;
                    this.loadBoards();
                }
                ConfigComponent.prototype.loadBoards = function () {
                    var _this = this;
                    this._boardsService.loadBoardsList(false).subscribe(function (data) {
                        console.log('Boards: Got data' + JSON.stringify(data));
                        _this._boards = _this.indexBoard(data);
                    }, function (err) {
                        console.log(err);
                        //TODO logout locally if 401, and redirect to login
                        //err seems to contain a complaint about the json marshalling of the empty body having gone wrong,
                        //rather than about the auth problems
                    }, function () { return console.log('Board: done'); });
                    this.updateNewForm();
                };
                Object.defineProperty(ConfigComponent.prototype, "boards", {
                    get: function () {
                        if (!this._boards) {
                            return [];
                        }
                        return this._boards.array;
                    },
                    enumerable: true,
                    configurable: true
                });
                ConfigComponent.prototype.updateNewForm = function () {
                    this.newForm = this._formBuilder.group({
                        "newJson": ["", common_2.Validators.required]
                    });
                };
                ConfigComponent.prototype.hasBoards = function () {
                    return this._boards && this._boards.array.length > 0;
                };
                ConfigComponent.prototype.isSelected = function (id) {
                    return id == this.selected;
                };
                ConfigComponent.prototype.getConfigJson = function (board) {
                    var config = board.config;
                    var json = JSON.stringify(config, null, 2);
                    return json;
                };
                ConfigComponent.prototype.toggleBoard = function (event, id) {
                    this.clearJsonErrors();
                    this.edit = false;
                    if (this.selected == id) {
                        this.selected = -1;
                    }
                    else {
                        this.selected = id;
                    }
                    event.preventDefault();
                };
                ConfigComponent.prototype.toggleEdit = function (event, board) {
                    this.clearJsonErrors();
                    this.edit = !this.edit;
                    if (this.edit) {
                        this.editForm = this._formBuilder.group({
                            "editJson": [this.getConfigJson(board), common_2.Validators.required]
                        });
                    }
                    event.preventDefault();
                };
                ConfigComponent.prototype.toggleDelete = function (event, id) {
                    this.deleting = !this.deleting;
                    if (this.deleting) {
                        //I wasn't able to get 'this' working with the lambda below
                        var component = this;
                        this.deleteForm = this._formBuilder.group({
                            "boardName": ['', common_2.Validators.compose([common_2.Validators.required, function (control) {
                                        if (component.selected) {
                                            var board = component._boards.forKey(component.selected.toString());
                                            if (board.name != control.value) {
                                                return { "boardName": true };
                                            }
                                        }
                                        return null;
                                    }])]
                        });
                    }
                    event.preventDefault();
                };
                ConfigComponent.prototype.deleteBoard = function () {
                    var _this = this;
                    this._boardsService.deleteBoard(this.selected)
                        .subscribe(function (data) {
                        console.log("Deleted board");
                        _this._boards = _this.indexBoard(data);
                        _this.edit = false;
                        _this.deleting = false;
                    }, function (err) {
                        console.log(err);
                        //TODO error reporting
                    }, function () { });
                };
                ConfigComponent.prototype.editBoard = function () {
                    var _this = this;
                    var value = this.editForm.value.editJson;
                    if (!this.checkJson(value)) {
                        this.jsonErrorEdit = "The contents must be valid json";
                        return;
                    }
                    this._boardsService.saveBoard(this.selected, value)
                        .subscribe(function (data) {
                        console.log("Edited board");
                        _this._boards = _this.indexBoard(data);
                        _this.edit = false;
                    }, function (err) {
                        console.log(err);
                        //TODO error reporting
                    }, function () { });
                };
                ConfigComponent.prototype.newBoard = function () {
                    var _this = this;
                    var value = this.newForm.value.newJson;
                    if (!this.checkJson(value)) {
                        this.jsonErrorCreate = "The contents must be valid json";
                        return;
                    }
                    this._boardsService.createBoard(this.newForm.value.newJson)
                        .subscribe(function (data) {
                        console.log("Saved new board");
                        _this._boards = _this.indexBoard(data);
                        _this.updateNewForm();
                    }, function (err) {
                        console.log(err);
                        //TODO error reporting
                    }, function () { });
                };
                ConfigComponent.prototype.checkJson = function (value) {
                    try {
                        JSON.parse(value);
                        return true;
                    }
                    catch (e) {
                        return false;
                    }
                };
                ConfigComponent.prototype.clearJsonErrors = function () {
                    this.jsonErrorCreate = null;
                    this.jsonErrorEdit = null;
                };
                ConfigComponent.prototype.indexBoard = function (data) {
                    var boards = new indexed_1.Indexed();
                    boards.indexArray(data, function (entry) { return entry; }, function (board) { return board.id; });
                    return boards;
                };
                ConfigComponent = __decorate([
                    core_1.Component({
                        selector: 'boards',
                        inputs: ['boards'],
                        providers: [boardsService_1.BoardsService]
                    }),
                    core_1.View({
                        templateUrl: 'app/components/config/config.html',
                        directives: [router_1.ROUTER_DIRECTIVES]
                    }), 
                    __metadata('design:paramtypes', [boardsService_1.BoardsService, router_1.Router, common_1.FormBuilder])
                ], ConfigComponent);
                return ConfigComponent;
            })();
            exports_1("ConfigComponent", ConfigComponent);
        }
    }
});
//# sourceMappingURL=config.js.map
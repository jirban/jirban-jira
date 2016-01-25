System.register(['angular2/core', 'angular2/router', '../../services/boardsService', '../../services/authenticationHelper', "angular2/common"], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, router_1, boardsService_1, authenticationHelper_1, common_1, common_2;
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
            function (authenticationHelper_1_1) {
                authenticationHelper_1 = authenticationHelper_1_1;
            },
            function (common_1_1) {
                common_1 = common_1_1;
                common_2 = common_1_1;
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
                    this.loadBoards();
                }
                ConfigComponent.prototype.loadBoards = function () {
                    var _this = this;
                    this._boardsService.loadBoardsList(false).subscribe(function (data) {
                        console.log('Boards: Got data' + JSON.stringify(data));
                        _this.boards = data;
                    }, function (err) {
                        console.log(err);
                        //TODO logout locally if 401, and redirect to login
                        //err seems to contain a complaint about the json marshalling of the empty body having gone wrong,
                        //rather than about the auth problems
                        //To be safe, go back to the error page
                        authenticationHelper_1.clearToken();
                        _this._router.navigateByUrl('/login');
                    }, function () { return console.log('Board: done'); });
                    this.updateNewForm();
                };
                ConfigComponent.prototype.updateNewForm = function () {
                    this.newForm = this._formBuilder.group({
                        "newJson": ["", common_2.Validators.nullValidator(null)] //TODO validate that is is valid json at least
                    });
                };
                ConfigComponent.prototype.hasBoards = function () {
                    return this.boards && this.boards.length > 0;
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
                    this.edit = !this.edit;
                    if (this.edit) {
                        this.editForm = this._formBuilder.group({
                            "editJson": [this.getConfigJson(board), common_2.Validators.nullValidator(null)] //TODO validate that is is valid json at least
                        });
                    }
                    event.preventDefault();
                };
                ConfigComponent.prototype.toggleDelete = function (event, id) {
                    this.deleting = !this.deleting;
                    if (this.deleting) {
                        this.deleteForm = this._formBuilder.group({
                            "boardName": ['', common_2.Validators.nullValidator(null)] //TODO proper validation
                        });
                    }
                    event.preventDefault();
                };
                ConfigComponent.prototype.deleteBoard = function () {
                    var _this = this;
                    this._boardsService.deleteBoard(this.selected)
                        .subscribe(function (data) {
                        console.log("Deleted board");
                        _this.boards = data;
                        _this.edit = false;
                        _this.deleting = false;
                    }, function (err) {
                        console.log(err);
                        //TODO error reporting
                    }, function () { });
                };
                ConfigComponent.prototype.editBoard = function () {
                    var _this = this;
                    this._boardsService.saveBoard(this.selected, this.editForm.value.editJson)
                        .subscribe(function (data) {
                        console.log("Edited board");
                        _this.boards = data;
                        _this.edit = false;
                    }, function (err) {
                        console.log(err);
                        //TODO error reporting
                    }, function () { });
                };
                ConfigComponent.prototype.newBoard = function () {
                    var _this = this;
                    this._boardsService.createBoard(this.newForm.value.newJson)
                        .subscribe(function (data) {
                        console.log("Saved new board");
                        _this.boards = data;
                        _this.updateNewForm();
                    }, function (err) {
                        console.log(err);
                        //TODO error reporting
                    }, function () { });
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
System.register(['angular2/core', 'angular2/http', 'angular2/router', 'rxjs/add/operator/map', '../services/authenticationHelper', "../common/RestUrlUtil"], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, http_1, router_1, authenticationHelper_1, RestUrlUtil_1;
    var BoardsService;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (http_1_1) {
                http_1 = http_1_1;
            },
            function (router_1_1) {
                router_1 = router_1_1;
            },
            function (_1) {},
            function (authenticationHelper_1_1) {
                authenticationHelper_1 = authenticationHelper_1_1;
            },
            function (RestUrlUtil_1_1) {
                RestUrlUtil_1 = RestUrlUtil_1_1;
            }],
        execute: function() {
            BoardsService = (function () {
                function BoardsService(_http, _router) {
                    this._http = _http;
                    this._router = _router;
                }
                BoardsService.prototype.loadBoardsList = function (summaryOnly) {
                    if (!authenticationHelper_1.hasToken()) {
                        this._router.navigateByUrl("/login");
                    }
                    var token = authenticationHelper_1.getToken();
                    var headers = new http_1.Headers();
                    headers.append("Authorization", token);
                    var path = RestUrlUtil_1.RestUrlUtil.caclulateUrl(summaryOnly ? 'rest/boards.json' : 'rest/boards.json?full=1');
                    var ret = this._http.get(path, {
                        headers: headers
                    }).
                        map(function (res) { return res.json(); });
                    return ret;
                };
                BoardsService.prototype.saveBoard = function (id, json) {
                    var path = RestUrlUtil_1.RestUrlUtil.caclulateUrl('rest/save-board');
                    if (id >= 0) {
                        path += "?id=" + id;
                    }
                    var headers = new http_1.Headers();
                    console.log("Saving board " + path);
                    var ret = this._http.post(path, json, {
                        headers: headers
                    }).
                        map(function (res) { return res.json(); });
                    return ret;
                };
                BoardsService = __decorate([
                    core_1.Injectable(), 
                    __metadata('design:paramtypes', [http_1.Http, router_1.Router])
                ], BoardsService);
                return BoardsService;
            })();
            exports_1("BoardsService", BoardsService);
        }
    }
});
//# sourceMappingURL=boardsService.js.map
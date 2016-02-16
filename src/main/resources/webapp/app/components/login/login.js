System.register(['angular2/core', 'angular2/common', 'angular2/http', 'angular2/router', 'rxjs/add/operator/map', '../../services/authenticationHelper'], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, common_1, http_1, router_1, authenticationHelper_1;
    var LoginComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (common_1_1) {
                common_1 = common_1_1;
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
            }],
        execute: function() {
            LoginComponent = (function () {
                function LoginComponent(http, router, fb) {
                    this.http = http;
                    this.router = router;
                    this.form = fb.group({
                        "username": ['', common_1.Validators.required],
                        "password": ['', common_1.Validators.required]
                    });
                    authenticationHelper_1.clearToken();
                }
                LoginComponent.prototype.onSubmit = function () {
                    var _this = this;
                    var headers = new http_1.Headers();
                    headers.append('Content-Type', 'application/x-www-form-urlencoded');
                    this.http.post('rest/login', JSON.stringify(this.form.value), {
                        headers: headers
                    })
                        .map(function (res) { return res.json(); })
                        .subscribe(function (data) {
                        console.log('Login: Got data' + JSON.stringify(data));
                        var token = data["auth-token"];
                        if (!!token) {
                            authenticationHelper_1.setToken(token);
                            _this.router.navigateByUrl('/boards');
                        }
                    }, function (err) {
                        //TODO No idea how to get error code to differentiate between auth and other errors
                        _this.error = true;
                        console.error(err);
                    }, function () { return console.log('Login: auth finished'); });
                };
                LoginComponent.prototype.clearError = function () {
                    this.error = null;
                };
                LoginComponent = __decorate([
                    core_1.Component({
                        selector: 'login-form',
                        providers: [common_1.FormBuilder]
                    }),
                    core_1.View({
                        templateUrl: 'app/components/login/login.html',
                        directives: [common_1.FORM_DIRECTIVES]
                    }), 
                    __metadata('design:paramtypes', [http_1.Http, router_1.Router, common_1.FormBuilder])
                ], LoginComponent);
                return LoginComponent;
            })();
            exports_1("LoginComponent", LoginComponent);
        }
    }
});
//# sourceMappingURL=login.js.map
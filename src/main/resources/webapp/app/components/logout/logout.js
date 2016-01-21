System.register(['angular2/core', 'angular2/http', 'angular2/router', '../../services/authenticationHelper'], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, http_1, router_1, authenticationHelper_1;
    var LogoutComponent;
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
            function (authenticationHelper_1_1) {
                authenticationHelper_1 = authenticationHelper_1_1;
            }],
        execute: function() {
            LogoutComponent = (function () {
                function LogoutComponent(http, router) {
                    this.http = http;
                    this.router = router;
                    console.log("Logging out");
                    if (authenticationHelper_1.hasToken()) {
                        console.log("Rest call to log out");
                        var headers = new http_1.Headers();
                        headers.append("Authorization", authenticationHelper_1.getToken());
                        this.http.post('rest/logout', '', {
                            headers: headers
                        }).map(function (res) { return res; }).subscribe();
                    }
                    authenticationHelper_1.clearToken();
                    //Go back to the about page
                    this.router.navigateByUrl('');
                }
                LogoutComponent = __decorate([
                    core_1.Component({
                        selector: 'logout-form'
                    }),
                    core_1.View({
                        template: '<p>This will never be shown</p>'
                    }), 
                    __metadata('design:paramtypes', [http_1.Http, router_1.Router])
                ], LogoutComponent);
                return LogoutComponent;
            })();
            exports_1("LogoutComponent", LogoutComponent);
        }
    }
});
//# sourceMappingURL=logout.js.map
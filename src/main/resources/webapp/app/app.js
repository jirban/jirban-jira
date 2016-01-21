System.register(['angular2/core', 'angular2/router', './components/about/about', './components/board/board', './components/boards/boards', './components/login/login', './components/logout/logout', './services/loggedInRouterOutlet', './services/authenticationHelper'], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, router_1, about_1, board_1, boards_1, login_1, logout_1, loggedInRouterOutlet_1, authenticationHelper_1;
    var App;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (router_1_1) {
                router_1 = router_1_1;
            },
            function (about_1_1) {
                about_1 = about_1_1;
            },
            function (board_1_1) {
                board_1 = board_1_1;
            },
            function (boards_1_1) {
                boards_1 = boards_1_1;
            },
            function (login_1_1) {
                login_1 = login_1_1;
            },
            function (logout_1_1) {
                logout_1 = logout_1_1;
            },
            function (loggedInRouterOutlet_1_1) {
                loggedInRouterOutlet_1 = loggedInRouterOutlet_1_1;
            },
            function (authenticationHelper_1_1) {
                authenticationHelper_1 = authenticationHelper_1_1;
            }],
        execute: function() {
            App = (function () {
                function App(router, location) {
                    this.router = router;
                    this.location = location;
                }
                App.prototype.isLoggedIn = function () {
                    return authenticationHelper_1.hasToken();
                };
                App = __decorate([
                    core_1.Component({
                        selector: 'my-app'
                    }),
                    router_1.RouteConfig([
                        new router_1.Route({ path: '/', component: about_1.AboutComponent, name: 'About' }),
                        new router_1.Route({ path: '/board', component: board_1.BoardComponent, name: 'Board' }),
                        new router_1.Route({ path: '/boards', component: boards_1.BoardsComponent, name: 'Boards' }),
                        new router_1.Route({ path: '/login', component: login_1.LoginComponent, name: 'Login' }),
                        new router_1.Route({ path: '/logout', component: logout_1.LogoutComponent, name: 'Logout' })
                    ]),
                    core_1.View({
                        template: "\n<div class=\"toolbar\">\n    <div class=\"toolbar-left\">\n        <a [routerLink]=\"['/About']\" class=\"toolbar-link\"><b>JirBan</b></a>\n        <span *ngIf=\"isLoggedIn()\"> <a [routerLink]=\"['/Boards']\" class=\"toolbar-link\">Boards</a></span>\n    </div>\n    <div class=\"toolbar-right\">\n        <span *ngIf=\"!isLoggedIn()\"><a [routerLink]=\"['/Login']\"  class=\"toolbar-link\">Log in</a></span>\n        <span *ngIf=\"isLoggedIn()\"><a [routerLink]=\"['/Logout']\" class=\"toolbar-link\">Log Out</a></span>\n    </div>\n</div>\n<router-outlet></router-outlet>\n    ",
                        directives: [router_1.ROUTER_DIRECTIVES, loggedInRouterOutlet_1.LoggedInRouterOutlet, about_1.AboutComponent, board_1.BoardComponent, login_1.LoginComponent]
                    }), 
                    __metadata('design:paramtypes', [router_1.Router, router_1.Location])
                ], App);
                return App;
            })();
            exports_1("App", App);
        }
    }
});
//# sourceMappingURL=app.js.map
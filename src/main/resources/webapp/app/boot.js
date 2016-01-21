System.register(['angular2/core', 'angular2/platform/browser', 'angular2/http', 'angular2/router', './app'], function(exports_1) {
    var core_1, browser_1, http_1, router_1, core_2, app_1;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
                core_2 = core_1_1;
            },
            function (browser_1_1) {
                browser_1 = browser_1_1;
            },
            function (http_1_1) {
                http_1 = http_1_1;
            },
            function (router_1_1) {
                router_1 = router_1_1;
            },
            function (app_1_1) {
                app_1 = app_1_1;
            }],
        execute: function() {
            core_2.enableProdMode();
            browser_1.bootstrap(app_1.App, [
                http_1.HTTP_PROVIDERS,
                router_1.ROUTER_PROVIDERS,
                core_1.provide(router_1.LocationStrategy, { useClass: router_1.HashLocationStrategy }),
                core_1.provide(router_1.APP_BASE_HREF, { useValue: '/app/' })
            ])
                .catch(function (err) { return console.error(err); });
        }
    }
});
//# sourceMappingURL=boot.js.map

//our root app component
import {provide, enableProdMode} from "angular2/core";
import {bootstrap} from "angular2/platform/browser";
import {HTTP_PROVIDERS} from "angular2/http";
import {APP_BASE_HREF, LocationStrategy, HashLocationStrategy} from "angular2/platform/common";
import {ROUTER_PROVIDERS} from "angular2/router";
import {App} from "./app";
import {ProgressErrorService} from "./services/progressErrorService";


enableProdMode();
bootstrap(App, [
    HTTP_PROVIDERS,
    ROUTER_PROVIDERS,
    ProgressErrorService,
    provide(LocationStrategy, {useClass: HashLocationStrategy}),
    provide(APP_BASE_HREF, {useValue: '../../app/'})
])
    .catch(err => console.error(err));
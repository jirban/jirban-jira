//our root app component
import {provide, enableProdMode} from "@angular/core";
import {bootstrap} from "@angular/platform-browser-dynamic";
import {Title} from "@angular/platform-browser";
import {HTTP_PROVIDERS} from "@angular/http";
import {App} from "./app";
import {ProgressErrorService} from "./services/progressErrorService";
import {AppHeaderService} from "./services/appHeaderService";
import {LocationStrategy, HashLocationStrategy, APP_BASE_HREF} from "@angular/common";
import {APP_ROUTER_PROVIDERS} from "./routes";


enableProdMode();
bootstrap(App, [
    HTTP_PROVIDERS,
    ProgressErrorService,
    AppHeaderService,
    APP_ROUTER_PROVIDERS,
    provide(LocationStrategy, {useClass: HashLocationStrategy}),
    provide(APP_BASE_HREF, {useValue: '../../app/'}),
    Title
])
    .catch(err => console.error(err));
//our root app component
import {provide, enableProdMode} from "@angular/core";
import {bootstrap} from "@angular/platform-browser-dynamic";
import {Title} from "@angular/platform-browser";
import {HTTP_PROVIDERS} from "@angular/http";
import {ROUTER_PROVIDERS} from "@angular/router-deprecated";
import {App} from "./app";
import {ProgressErrorService} from "./services/progressErrorService";
import {TitleFormatService} from "./services/TitleFormatService";
import {LocationStrategy, HashLocationStrategy, APP_BASE_HREF} from "@angular/common";


enableProdMode();
bootstrap(App, [
    HTTP_PROVIDERS,
    ROUTER_PROVIDERS,
    ProgressErrorService,
    TitleFormatService,
    provide(LocationStrategy, {useClass: HashLocationStrategy}),
    provide(APP_BASE_HREF, {useValue: '../../app/'}),
    Title
])
    .catch(err => console.error(err));
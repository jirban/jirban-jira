//our root app component
import {provide} from 'angular2/core'
import {bootstrap} from 'angular2/platform/browser'
import {HTTP_PROVIDERS} from 'angular2/http';
import {APP_BASE_HREF, ROUTER_PROVIDERS, HashLocationStrategy, Location, LocationStrategy} from 'angular2/router';

import {enableProdMode} from 'angular2/core';
import {App} from './app';


enableProdMode();
bootstrap(App, [
    HTTP_PROVIDERS,
    ROUTER_PROVIDERS,
    provide(LocationStrategy, {useClass: HashLocationStrategy}),
    provide(APP_BASE_HREF, {useValue: '/app/'})
])
    .catch(err => console.error(err));
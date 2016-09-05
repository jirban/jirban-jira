//our root app component
import {enableProdMode} from "@angular/core";
import {platformBrowserDynamic} from "@angular/platform-browser-dynamic";
import {AppModule} from "./app.module";


enableProdMode();
platformBrowserDynamic().bootstrapModule(AppModule)
    .catch(err => console.error(err));

/*
 bootstrap(App, [
 HTTP_PROVIDERS,
 ProgressErrorService,
 AppHeaderService,
 APP_ROUTER_PROVIDERS,
 disableDeprecatedForms(),
 provideForms(),
 provide(LocationStrategy, {useClass: HashLocationStrategy}),
 provide(APP_BASE_HREF, {useValue: '../../app/'}),
 Title
 ])

 */
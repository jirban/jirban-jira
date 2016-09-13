var __webpack_public_path__ = calculatePublicPath();

console.log("Calculated webpack public path: " + __webpack_public_path__);

import {platformBrowserDynamic} from "@angular/platform-browser-dynamic";
import {enableProdMode} from "@angular/core";
import {AppModule} from "./app.module";
if (process.env.ENV === 'production') {
    enableProdMode();
}
platformBrowserDynamic().bootstrapModule(AppModule);

function calculatePublicPath():string {
    let searchElement:string = "/download/resources/org.jirban.jirban-jira/webapp/";
    let location:Location = window.location;
    let href:string = location.href;
    let index:number = href.indexOf(searchElement);
    if (index < 0) {
        let url:string = location.protocol + "//";
        url += location.hostname;
        if (location.port) {
            url += ":" + location.port;
        }
        url += "/";
        return url;
    } else {
        //Do the http(s):// slashes
        //First slash, the second one will be the next index
        let slashIndex:number = href.indexOf("/", 0);
        //Find the third slash which should be the root url
        slashIndex = href.indexOf("/", slashIndex + 2);
        let url:string = searchElement;
        if (index > slashIndex) {
            url = href.substr(slashIndex, index) + url;
        }
        return url;
    }
}

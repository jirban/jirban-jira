import {Component, View} from 'angular2/core';
import {ROUTER_DIRECTIVES, RouteConfig, Route, Router} from 'angular2/router';


@Component({
    selector: 'about',
    templateUrl: 'app/components/about/about.html',
    styles: [`
    .content {
        padding: 10px;
        width:800px;
    }
    `]

})
export class AboutComponent {
    constructor() {
    }
}
//our root app component
import {Component, View, provide} from 'angular2/core'
import {HTTP_PROVIDERS} from 'angular2/http';
import {APP_BASE_HREF, ROUTER_DIRECTIVES, ROUTER_PROVIDERS, HashLocationStrategy, Location, LocationStrategy, Route, RouteConfig, Router, RouterLink, RouterOutlet} from 'angular2/router';
import {AboutComponent} from './components/about/about';
import {BoardComponent} from './components/board/board';
import {BoardsComponent} from './components/boards/boards';
import {LoginComponent} from './components/login/login';
import {LogoutComponent} from './components/logout/logout';
import {LoggedInRouterOutlet} from './services/loggedInRouterOutlet';
import {hasToken} from './services/authenticationHelper';

@Component({
    selector: 'my-app'
})
@RouteConfig([
    new Route({path: '/', component: AboutComponent, name: 'About'}),
    new Route({path: '/board', component: BoardComponent, name: 'Board'}),
    new Route({path: '/boards', component: BoardsComponent, name: 'Boards'})/*,
    new Route({path: '/login', component: LoginComponent, name: 'Login'}),
    new Route({path: '/logout', component: LogoutComponent, name: 'Logout'})*/
])
@View({
    template: `
<!--<div class="toolbar">
    <div class="toolbar-left">
        <a [routerLink]="['/About']" class="toolbar-link"><b>JirBan</b></a>
        <span *ngIf="isLoggedIn()"> <a [routerLink]="['/Boards']" class="toolbar-link">Boards</a></span>
    </div>
    <div class="toolbar-right">
        <span *ngIf="!isLoggedIn()"><a [routerLink]="['/Login']"  class="toolbar-link">Log in</a></span>
        <span *ngIf="isLoggedIn()"><a [routerLink]="['/Logout']" class="toolbar-link">Log Out</a></span>
    </div>
</div>-->
<div class="toolbar">
    <div class="toolbar-left">
        <a [routerLink]="['/About']" class="toolbar-link"><b>JirBan</b></a>
        <span> <a [routerLink]="['/Boards']" class="toolbar-link">Boards</a></span>
    </div>
</div>

<router-outlet></router-outlet>
    `,
    directives: [ROUTER_DIRECTIVES, LoggedInRouterOutlet, AboutComponent, BoardComponent, LoginComponent]
})
export class App {
    router:Router;
    location:Location;

    constructor(router:Router, location:Location) {
        this.router = router;
        this.location = location;
    }

    private isLoggedIn() {
        return hasToken();
    }
}

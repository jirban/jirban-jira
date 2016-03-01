//our root app component
import {Component, View, provide} from 'angular2/core'
import {HTTP_PROVIDERS} from 'angular2/http';
import {APP_BASE_HREF, ROUTER_DIRECTIVES, ROUTER_PROVIDERS, HashLocationStrategy, Location, LocationStrategy, Route, RouteConfig, Router, RouterLink, RouterOutlet} from 'angular2/router';
import {AboutComponent} from './components/about/about';
import {BoardComponent} from './components/board/board';
import {BoardsComponent} from './components/boards/boards';
import {ConfigComponent} from "./components/config/config";
import {version} from "angular2/src/upgrade/angular_js";
import {ProgressErrorService} from "./services/progressErrorService";

@Component({
    selector: 'my-app'
})
@RouteConfig([
    new Route({path: '/', component: AboutComponent, name: 'About'}),
    new Route({path: '/board', component: BoardComponent, name: 'Board'}),
    new Route({path: '/boards', component: BoardsComponent, name: 'Boards'}),
    new Route({path: '/config', component: ConfigComponent, name: 'Config'})
])
@View({
    template: `

<div class="toolbar">
    <div class="toolbar-left">
        <span><a [routerLink]="['/About']" class="toolbar-link"><b>JirBan</b></a></span>
        <span> <a [routerLink]="['/Boards']" class="toolbar-link">Boards</a></span>
        <!-- TODO Only display this if it is an admin -->
        <span> <a [routerLink]="['/Config']" class="toolbar-link">Config</a></span>
    </div>

</div>
<router-outlet></router-outlet>

<div class="wait-screen" [hidden]="hideProgress"> </div>
<div id="error-panel" [hidden]="!error" (click)="onClickErrorClose($event)">
    <div class="header">
            <div class="header-text">Board Settings</div>
            <div class="header-close-button">
                <a href="close" class="close" (click)="onClickErrorClose($event)">X</a>
            </div>
    </div>
    {{error}}
</div>

    `,
    directives: [ROUTER_DIRECTIVES, AboutComponent, BoardComponent]
})
export class App {
    _router:Router;
    _progressError:ProgressErrorService;

    constructor(router:Router, progressError:ProgressErrorService) {
        this._router = router;
        this._progressError = progressError;

        router.subscribe((route:string) => {
            //Hack to hide the body scroll bars on the board page.
            //This is only really necessary on FireFox on linux, where the board's table
            //seems have extra width added to allow for the scrollbars on the board's divs
            let showBodyScrollbars:boolean = true;
            if (route.startsWith("board?board")) {
                showBodyScrollbars = false;
            }
            document.getElementsByTagName("body")[0].className = showBodyScrollbars ? "" : "no-scrollbars";
        });
    }

    get hideProgress():boolean {
        return !this._progressError.displayProgressIcon();
    }

    get error():string {
        return this._progressError.getError();
    }

    onClickErrorClose(event:MouseEvent):void {
        event.preventDefault();
        this._progressError.clearError();
    }
}

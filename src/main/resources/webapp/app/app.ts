//our root app component
import {Component} from "@angular/core";
import {RouteConfig, ROUTER_DIRECTIVES, Route, Router} from "@angular/router-deprecated";
import {BoardComponent} from "./components/board/board";
import {BoardsComponent} from "./components/boards/boards";
import {ConfigComponent} from "./components/config/config";
import {ProgressErrorService} from "./services/progressErrorService";
import {RestUrlUtil} from "./common/RestUrlUtil";
import {VersionService} from "./services/versionService";

/** The current API version. It should match what is set in RestEndpoint.API_VERSION */
const VERSION:number = 1;

@Component({
    selector: 'my-app',
    providers: [VersionService],
    template: `

<div class="toolbar">
    <div class="toolbar-left">
        <span> <a [routerLink]="['/Boards']" class="toolbar-link">Boards</a></span>
        <!-- TODO Only display this if it is an admin -->
        <span> <a [routerLink]="['/Config']" class="toolbar-link">Config</a></span>
    </div>
    <div class="toolbar-right" [innerHTML]="completedMessage"></div>
</div>
<router-outlet></router-outlet>
<div class="wait-screen" [hidden]="hideProgress"> </div>
<div id="error-panel" [hidden]="!error" (click)="onClickErrorClose($event)">
    <div class="header">
            <div class="header-text">Error</div>
            <div class="header-close-button">
                <a href="close" class="close" (click)="onClickErrorClose($event)">X</a>
            </div>
    </div>
    <div *ngIf="!notLoggedIn" class="error-message" [innerHTML]="error"></div>
    <div *ngIf="notLoggedIn" class="error-message"><a href="{{loginUrl}}" class="error-message" (click)="onClickMessageLoginLink($event)">Log in</a> to Jira again.</div>
</div>

    `,
    directives: [ROUTER_DIRECTIVES, BoardComponent]
})
@RouteConfig([
    new Route({path: '/', component: BoardsComponent, name: 'Boards'}),
    new Route({path: '/board', component: BoardComponent, name: 'Board'}),
    new Route({path: '/boards', component: BoardsComponent, name: 'Boards'}),
    new Route({path: '/config', component: ConfigComponent, name: 'Config'})
])
export class App {
    _router:Router;
    _progressError:ProgressErrorService;
    loginUrl:string;

    constructor(router:Router, progressError:ProgressErrorService, versionService:VersionService) {
        this._router = router;
        this._progressError = progressError;
        this.loginUrl = RestUrlUtil.caclulateRestUrl("login.jsp");

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

        versionService.initialise(VERSION, progressError);
    }

    get hideProgress():boolean {
        return !this._progressError.displayProgressIcon();
    }

    get error():string {
        return this._progressError.error;
    }

    get notLoggedIn():boolean {
        return this._progressError.notLoggedIn;
    }

    get completedMessage():string {
        return this._progressError.completedMessage;
    }

    onClickErrorClose(event:MouseEvent):void {
        event.preventDefault();
        this._progressError.clearError();
    }

    onClickMessageLoginLink(event:MouseEvent):void {
        event.stopPropagation();
    }
}


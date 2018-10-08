//our root app component
import "../../../global.css";
import {Component, OnInit} from "@angular/core";
import {VersionService} from "../../services/versionService";
import {ProgressErrorService} from "../../services/progressErrorService";
import {AppHeaderService} from "../../services/appHeaderService";
import {RestUrlUtil} from "../../common/RestUrlUtil";

/** The current API version. It should match what is set in RestEndpoint.API_VERSION */
const VERSION: number = 2;

@Component({
    selector: 'my-app',
    providers: [VersionService],
    template : require('./app.html'),
    styles: [require('./app.css')]
})
export class AppComponent implements OnInit {
    _progressError: ProgressErrorService;
    loginUrl: string;

    overbaardUrl: string;
    attemptedReplacementUrl: string;
    private sunsetDismissed = false;

    constructor(progressError: ProgressErrorService, versionService: VersionService, appHeaderService: AppHeaderService) {
        this._progressError = progressError;
        this.loginUrl = RestUrlUtil.caclulateRestUrl("login.jsp");

        appHeaderService.disableBodyScrollbarsObservable.subscribe((disable: boolean) => {
            //Hack to hide the body scroll bars on the board page.
            //This is only really necessary on FireFox on linux, where the board's table
            //seems have extra width added to allow for the scrollbars on the board's divs
            document.getElementsByTagName("body")[0]["className"] = disable ? "no-scrollbars" : "";
        });


        versionService.initialise(VERSION, progressError);
        this.overbaardUrl = RestUrlUtil.calculateJiraUrl() + '/overbaard';
        this.attemptedReplacementUrl = RestUrlUtil.calculateOverbaardUrl();
    }

    ngOnInit(): void {
    }

    get hideProgress(): boolean {
        return !this._progressError.displayProgressIcon();
    }

    get error(): string {
        return this._progressError.error;
    }

    get notLoggedIn(): boolean {
        return this._progressError.notLoggedIn;
    }

    get completedMessage(): string {
        return this._progressError.completedMessage;
    }

    onClickErrorClose(event: MouseEvent): void {
        event.preventDefault();
        this._progressError.clearError();
    }

    onClickMessageLoginLink(event: MouseEvent): void {
        event.stopPropagation();
    }

    dismissSunset(event: MouseEvent) {
        event.preventDefault();
        this.sunsetDismissed = true;
    }
}


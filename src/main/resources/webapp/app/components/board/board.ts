import {Component, OnDestroy, OnInit} from "@angular/core";
import {RouteParams} from "@angular/router-deprecated";
import {IssuesService} from "../../services/issuesService";
import {BoardData} from "../../data/board/boardData";
import {IssueComponent} from "./issue/issue";
import {SwimlaneEntryComponent} from "./swimlaneEntry/swimlaneEntry";
import {PanelMenuComponent} from "./panelMenu/panelMenu";
import {IssueContextMenuComponent, IssueContextMenuData} from "./issueContextMenu/issueContextMenu";
import {ProgressErrorService} from "../../services/progressErrorService";
import {TitleFormatService} from "../../services/TitleFormatService";
import {BoardHeaderEntry, State} from "../../data/board/header";
import {IMap} from "../../common/map";
import {CharArrayRegistry} from "../../common/charArrayRegistry";
import Timer = NodeJS.Timer;


@Component({
    selector: 'board',
    providers: [IssuesService, BoardData],
    templateUrl: 'app/components/board/board.html',
    styleUrls: ['app/components/board/board.css'],
    directives: [IssueComponent, IssueContextMenuComponent, PanelMenuComponent, SwimlaneEntryComponent]
})
export class BoardComponent implements OnDestroy, OnInit {

    private boardCode:string;

    private issueContextMenuData:IssueContextMenuData;

    private _pollFailureCount:number = 0;

    private _currentTimeout:Timer;
    private _destroyed:boolean = false;

    /** The calculate height of the board body */
    private boardBodyHeight:number;
    /** The offset of the board, used to synchronize the offset of the headers as the board is scrolled */
    private boardLeftOffset:number = 0;

    private _defaultPollInterval:number = 30000;

    /** Cache all the char arrays used for the collapsed column labels so they are not recalculated all the time */
    private _collapsedColumnLabels:CharArrayRegistry = new CharArrayRegistry();

    constructor(private _issuesService:IssuesService,
                private _boardData:BoardData,
                private _progressError:ProgressErrorService,
                routeParams:RouteParams,
                title:TitleFormatService) {
        let queryString:IMap<string> = routeParams.params;
        this.boardCode = routeParams.get('board');
        title.setTitle("Board (" + this.boardCode + ")");

        this._boardData.setBacklogFromQueryParams(queryString);

        this.populateIssues(() => {
            //Loading filters does not work until the issue data is loaded
            _boardData.setFiltersFromQueryParams(queryString);
            this.setWindowSize();
        });

    }

    private populateIssues(issueDataLoaded:()=>void) {
        this._progressError.startProgress(true);
        this._issuesService.getIssuesData(this.boardCode, this._boardData.showBacklog).subscribe(
            data => {
                this.setIssueData(data);
                issueDataLoaded();
            },
            err => {
                this._progressError.setError(err);
            },
            () => {
                this.pollIssues();
                this._progressError.finishProgress();
            }
        );
    }

    ngOnInit():any {

        return null;
    }

    ngOnDestroy():any {
        this.clearPollTimeout();
        this._destroyed = true;
        return null;
    }

    private clearPollTimeout() {
        let timeout:Timer = this._currentTimeout;
        if (timeout) {
            clearTimeout(timeout);
        }
    }

    private setIssueData(issueData:any) {
        this._boardData.deserialize(this.boardCode, issueData);
        this.setWindowSize();
    }

    private pollIssues() {
        this._currentTimeout = setTimeout(()=>{this.doPoll()}, this._defaultPollInterval);
    }

    private doPoll() {
        if (this._destroyed) {
            return;
        }
        if (this._progressError.progress) {
            //Another request is already in progress (probably a move)
            //Just do another poll and return so we don't clash the error codes etc.
            this.pollIssues();
            return;
        }

        //Don't use the progress monitor for this background task.
        //Simply set the error in it if one happened
        this._issuesService.pollBoard(this.boardData)
            .subscribe(
                data => {
                    console.log("----> Received changes: " + JSON.stringify(data));
                    this.boardData.processChanges(data);
                    this.pollIssues();
                },
                err => {
                    this._pollFailureCount++;
                    console.log(err);
                    if (this._pollFailureCount < 3) {
                        this.pollIssues();
                    } else {
                        if (err.status === 401) {
                            this._progressError.setError(err);
                        } else {
                            this._progressError.setError("Connection to the board lost.");
                        }
                    }
                },
                () => {
                    this._pollFailureCount = 0;
                }
            );
    }


    private get visibleColumns():boolean[] {
        return this._boardData.headers.stateVisibilities;
    }

    private getCharArray(state:string):string[] {
        return this._collapsedColumnLabels.getCharArray(state);
    }

    private onResize(event : any) {
        this.setWindowSize();
    }

    private setWindowSize() {

        //The toolbar height is 30px
        const toolbarHeight = 30;

        //If we have one row of headers the height is 32px, for two rows the height is 62px
        let headersHeight = (this.bottomHeaders && this.bottomHeaders.length > 0) ? 62 : 32;
        this.boardBodyHeight = window.innerHeight - toolbarHeight - headersHeight;
    }

    private showIssueContextMenu(event:any) {
        this.issueContextMenuData = new IssueContextMenuData(event.issueId, event.x, event.y);
    }

    private hideMenus() {
        this._boardData.hideHideables();
        this.issueContextMenuData = null;
    }

    onCloseIssueContextMenu(event:any) {
        this.issueContextMenuData = null;
    }

    get boardData():BoardData {
        return this._boardData;
    }

    get topHeaders():BoardHeaderEntry[] {
        return this._boardData.headers.topHeaders;
    }

    get bottomHeaders():BoardHeaderEntry[] {
        return this._boardData.headers.bottomHeaders;
    }

    get backlogTopHeader():BoardHeaderEntry {
        return this._boardData.headers.backlogTopHeader;
    }

    get backlogBottomHeadersIfVisible():BoardHeaderEntry[] {
        if (this.backlogTopHeader && this.backlogTopHeader.visible) {
            return this._boardData.headers.backlogBottomHeaders;
        }
        return null;
    }

    get mainStates():State[] {
        return this._boardData.mainStates;
    }

    get backlogAndIsCollapsed():boolean {
        if (!this.backlogTopHeader) {
            return false;
        }
        return !this.backlogTopHeader.visible;
    }

    get backlogStatesIfVisible():State[] {
        if (this.backlogTopHeader && this.backlogTopHeader.visible) {
            return this._boardData.backlogStates;
        }
        return null;
    }

    toggleHeaderVisibility(header:BoardHeaderEntry) {
        if (header.isCategory && header.backlog) {
            this._boardData.toggleBacklog();
            this.toggleBacklog();
        }
        this._boardData.headers.toggleHeaderVisibility(header);
    }

    toggleBacklog() {
        this._progressError.startProgress(true);
        this._issuesService.getIssuesData(this.boardCode, this._boardData.showBacklog).subscribe(
            data => {
                this.boardData.processChanges(data);
            },
            err => {
                this._progressError.setError(err);
            },
            () => {
                this._progressError.finishProgress();
            }
        );
    }

    getTopLevelHeaderClass(header:BoardHeaderEntry):string {
        if (header.stateAndCategory) {
            if (header.visible) {
                return 'visible';
            } else {
                return 'collapsed';
            }
        }
        return '';
    }

    getColourForIndex(index:number) : string {
        let mod:number = index % 5;
        switch (mod) {
            case 0:
                return "red";
            case 1:
                return "orange";
            case 2:
                return "green";
            case 3:
                return "blue";
            case 4:
                return "violet";
        }

    }

    scrollTableBodyX(event:Event) {
        this.boardLeftOffset = event.target["scrollLeft"];
    }

}

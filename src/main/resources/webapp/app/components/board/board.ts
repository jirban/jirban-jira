import {Component, OnDestroy} from "@angular/core";
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
export class BoardComponent implements OnDestroy {

    private boardCode:string;

    private issueContextMenuData:IssueContextMenuData;

    /** The calculate height of the board body */
    private boardBodyHeight:number;
    /** The offset of the board, used to synchronize the offset of the headers as the board is scrolled */
    private boardLeftOffset:number = 0;

    /** Cache all the char arrays used for the collapsed column labels so they are not recalculated all the time */
    private _collapsedColumnLabels:CharArrayRegistry = new CharArrayRegistry();

    constructor(private _issuesService:IssuesService,
                private _boardData:BoardData,
                private _progressError:ProgressErrorService,
                routeParams:RouteParams,
                title:TitleFormatService) {
        console.log("Create board");
        let queryString:IMap<string> = routeParams.params;
        this.boardCode = routeParams.get('board');
        title.setTitle("Board (" + this.boardCode + ")");

        this._boardData.setBacklogFromQueryParams(queryString);

        this._issuesService.populateIssues(this.boardCode, () => {
            //Loading filters does not work until the issue data is loaded
            _boardData.setFiltersFromQueryParams(queryString);
            this.setWindowSize();
        })

        this._issuesService.loadHelpTexts(this.boardCode, this._boardData);
    }

    ngOnDestroy():any {
        this._issuesService.destroy();
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

    getPossibleStateHelp(header:BoardHeaderEntry):string {
        if (header.rows == 2) {
            return this.getStateHelp(header);
        }
        return null;
    }

    getStateHelp(header:BoardHeaderEntry):string {
        return this._boardData.helpTexts[header.name];
    }


    toggleHeaderVisibility(header:BoardHeaderEntry) {
        let previousBacklog:boolean = this.boardData.showBacklog;

        this._boardData.headers.toggleHeaderVisibility(header);

        if (this.boardData.showBacklog != previousBacklog) {
            this._issuesService.toggleBacklog();
        }
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

    onFocus(event:Event):void{
        this._issuesService.visible = true;
        console.log("Focus");
    }

    onBlur(event:Event):void{
        this._issuesService.visible = false;
    }
}




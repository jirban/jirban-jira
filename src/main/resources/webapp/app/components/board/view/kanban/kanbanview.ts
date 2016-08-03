import {Component, OnInit} from "@angular/core";
import {IssuesService} from "../../../../services/issuesService";
import {BoardData} from "../../../../data/board/boardData";
import {IssueComponent} from "../../issue/issue";
import {SwimlaneEntryComponent} from "../../swimlaneEntry/swimlaneEntry";
import {PanelMenuComponent} from "../../panelMenu/panelMenu";
import {IssueContextMenuComponent, IssueContextMenuData} from "../../issueContextMenu/issueContextMenu";
import {ProgressErrorService} from "../../../../services/progressErrorService";
import {TitleFormatService} from "../../../../services/TitleFormatService";
import {BoardHeaderEntry, State} from "../../../../data/board/header";
import {CharArrayRegistry} from "../../../../common/charArrayRegistry";
import {TOOLBAR_HEIGHT} from "../../../../common/constants";


@Component({
    selector: 'kanban-view',
    inputs: ["boardCode", "issuesService", "boardData"],
    templateUrl: 'app/components/board/view/kanban/kanbanview.html',
    styleUrls: ['app/components/board/view/kanban/kanbanview.css'],
    directives: [IssueComponent, IssueContextMenuComponent, PanelMenuComponent, SwimlaneEntryComponent]
})
export class KanbanViewComponent implements OnInit {

    //Inputs
    private _boardCode:string;
    private _issuesService:IssuesService;
    private _boardData:BoardData;

    private issueContextMenuData:IssueContextMenuData;

    /** The calculate height of the board body */
    private boardBodyHeight:number;
    /** The offset of the board, used to synchronize the offset of the headers as the board is scrolled */
    private boardLeftOffset:number = 0;

    /** Cache all the char arrays used for the collapsed column labels so they are not recalculated all the time */
    private _collapsedColumnLabels:CharArrayRegistry = new CharArrayRegistry();

    constructor(private _progressError:ProgressErrorService,
                private _title:TitleFormatService) {
        console.log("Create kanban view");
    }

    ngOnInit():any {
        this._title.setTitle("Kanban (" + this._boardCode + ")");
    }

    set issuesService(value:IssuesService) {
        this._issuesService = value;
    }

    set boardCode(value:string) {
        this._boardCode = value;
    }

    set boardData(value:BoardData) {
        console.log("Setting boardData " + value);
        this._boardData = value;
        this._boardData.registerInitializedCallback(() => {
            this.setWindowSize();
        });
    }

    get initialized():boolean {
        if (!this._boardData) {
            return false;
        }
        return this._boardData.initialized;
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

        //If we have one row of headers the height is 32px, for two rows the height is 62px
        let headersHeight = (this.bottomHeaders && this.bottomHeaders.length > 0) ? 62 : 32;
        this.boardBodyHeight = window.innerHeight - TOOLBAR_HEIGHT - headersHeight;
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
}




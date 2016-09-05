import {IssuesService} from "../../../services/issuesService";
import {BoardData} from "../../../data/board/boardData";
import {OnInit, EventEmitter} from "@angular/core";
import {ProgressErrorService} from "../../../services/progressErrorService";
import {AppHeaderService} from "../../../services/appHeaderService";
import {TOOLBAR_HEIGHT} from "../../../common/constants";
import {BoardHeaderEntry} from "../../../data/board/header";
import {IssueContextMenuData} from "../../../data/board/issueContextMenuData";

/**
 * Abstract base class for a board containing a fixed header.
 */
export abstract class FixedHeaderView implements OnInit {

    //Inputs
    protected _boardCode: string;
    protected _issuesService: IssuesService;
    protected _boardData: BoardData;

    /** The calculate height of the board body */
    private boardBodyHeight:number;
    /** The offset of the board, used to synchronize the offset of the headers as the board is scrolled */
    private boardLeftOffset:number = 0;

    private showIssueContextMenu:EventEmitter<IssueContextMenuData> = new EventEmitter<IssueContextMenuData>();


    constructor(private _progressError:ProgressErrorService,
                private _appHeaderService:AppHeaderService,
                private _titlePrefix:string) {
    }

    ngOnInit():any {
        this._appHeaderService.disableBodyScrollbars = true;
        this._appHeaderService.setTitle(this._titlePrefix + " (" + this._boardCode + ")");
    }

    protected setIssuesService(value:IssuesService) {
        console.log("Setting issuesService");
        this._issuesService = value;
    }

    protected setBoardCode(value:string) {
        console.log("Setting board code")
        this._boardCode = value;
    }

    protected setBoardData(value:BoardData) {
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

    get topHeaders():BoardHeaderEntry[] {
        return this._boardData.headers.topHeaders;
    }

    get bottomHeaders():BoardHeaderEntry[] {
        return this._boardData.headers.bottomHeaders;
    }

    get backlogTopHeader():BoardHeaderEntry {
        return this._boardData.headers.backlogTopHeader;
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

    private onResize(event : any) {
        this.setWindowSize();
    }

    private setWindowSize() {

        //If we have one row of headers the height is 32px, for two rows the height is 62px
        let headersHeight = (this.bottomHeaders && this.bottomHeaders.length > 0) ? 62 : 32;
        this.boardBodyHeight = window.innerHeight - TOOLBAR_HEIGHT - headersHeight;
    }

    scrollTableBodyX(event:Event) {
        this.boardLeftOffset = event.target["scrollLeft"];
    }

    protected onShowIssueContextMenu(event:IssueContextMenuData) {
        console.log("KBV: Propagating show context menu event");
        this.showIssueContextMenu.emit(event);
    }
}

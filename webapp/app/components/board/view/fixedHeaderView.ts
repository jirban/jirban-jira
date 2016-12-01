import {IssuesService} from "../../../services/issuesService";
import {BoardData} from "../../../data/board/boardData";
import {OnInit, EventEmitter} from "@angular/core";
import {ProgressErrorService} from "../../../services/progressErrorService";
import {AppHeaderService} from "../../../services/appHeaderService";
import {TOOLBAR_HEIGHT} from "../../../common/constants";
import {BoardHeaderEntry} from "../../../data/board/header";
import {IssueContextMenuData} from "../../../data/board/issueContextMenuData";
import {ParallelTaskMenuData} from "../../../data/board/parallelTaskMenuData";

/**
 * Abstract base class for a board containing a fixed header.
 */
export abstract class FixedHeaderView implements OnInit {

    //Inputs
    protected _boardCode: string;
    protected _issuesService: IssuesService;
    protected _boardData: BoardData;

    /** The calculate height of the board body */
    private _boardBodyHeight:string;
    /** The offset of the board, used to synchronize the offset of the headers as the board is scrolled */
    private boardLeftOffsetPx:string = "";
    private boardLeftOffset:number;


    private showIssueContextMenu:EventEmitter<IssueContextMenuData> = new EventEmitter<IssueContextMenuData>();
    private showParallelTaskMenu:EventEmitter<ParallelTaskMenuData> = new EventEmitter<ParallelTaskMenuData>();


    constructor(private _progressError:ProgressErrorService,
                private _appHeaderService:AppHeaderService,
                private _titlePrefix:string) {
    }

    ngOnInit():any {
        this._appHeaderService.disableBodyScrollbars = true;
        this._appHeaderService.setTitle(this._titlePrefix + " (" + this._boardCode + ")");
    }

    protected setIssuesService(value:IssuesService) {
        this._issuesService = value;
    }

    protected setBoardCode(value:string) {
        this._boardCode = value;
    }

    protected setBoardData(value:BoardData) {
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

    get boardBodyHeight(): string {
        return this._boardBodyHeight;
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
        //If we have one row of headers the height is 42px, for two rows the height is 82px
        let headersHeight = (this.bottomHeaders && this.bottomHeaders.length > 0) ? 82 : 42;
        let boardBodyHeight:number = window.innerHeight - TOOLBAR_HEIGHT - headersHeight;
        this._boardBodyHeight = boardBodyHeight + "px";
    }

    scrollTableBodyX(event:Event) {
        let boardLeftOffset:number = event.target["scrollLeft"] * -1;
        this.boardLeftOffset = boardLeftOffset;
        this.boardLeftOffsetPx = boardLeftOffset + "px";
    }

    protected onShowIssueContextMenu(event:IssueContextMenuData) {
        console.log("KBV: Propagating show context menu event");
        this.showIssueContextMenu.emit(event);
    }

    protected onShowParallelTaskMenu(event:ParallelTaskMenuData) {
        this.showParallelTaskMenu.emit(event);
    }
}

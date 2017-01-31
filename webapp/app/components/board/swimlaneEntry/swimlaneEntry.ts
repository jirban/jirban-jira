import {Component, EventEmitter} from "@angular/core";
import {BoardData} from "../../../data/board/boardData";
import {SwimlaneData} from "../../../data/board/swimlaneData";
import {State, BoardHeaderEntry} from "../../../data/board/header";
import {IssueContextMenuData} from "../../../data/board/issueContextMenuData";
import {ParallelTaskMenuData} from "../../../data/board/parallelTaskMenuData";


/**
 * This is here to be able to add a header and the contents for a swimlane
 * as several rows
 */
@Component({
    inputs: ['swimlaneIndex', 'boardData', 'swimlane', 'boardLeftOffset'],
    outputs: ['showIssueContextMenu', 'toggleBacklogVisibility', 'showParallelTaskMenu'],
    selector: 'swimlane-entry',
    templateUrl: './swimlaneEntry.html',
    styleUrls: ['../view/kanban/kanbanview.css', './swimlaneEntry.css']
})
export class SwimlaneEntryComponent {
    public swimlane : SwimlaneData;
    public boardData : BoardData;
    public swimlaneIndex : number;
    private _boardLeftOffsetPx:string;

    private showIssueContextMenu:EventEmitter<IssueContextMenuData> = new EventEmitter<IssueContextMenuData>();
    private showParallelTaskMenu:EventEmitter<ParallelTaskMenuData> = new EventEmitter<ParallelTaskMenuData>();
    private toggleBacklogVisibility:EventEmitter<any> = new EventEmitter();

    constructor() {
    }

    get displayEntry():boolean {
        if (this.swimlane.filtered) {
            return false;
        }
        if (this.swimlane.empty && this.boardData.hideEmptySwimlanes) {
            return false;
        }
        return true;
    }

    get boardLeftOffsetPx():string {
        return this._boardLeftOffsetPx;
    }

    set boardLeftOffset(value:number) {
        let i:number = 10 + -1 * value;
        this._boardLeftOffsetPx = i + "px";
    }

    private get collapsed() {
        return this.swimlane.collapsed;
    }

    private get boardStates() : State[] {
        return this.boardData.boardStates;
    }

    private toggleSwimlane(index:number) {
        this.boardData.toggleSwimlaneVisibility(index);
    }

    private onShowIssueContextMenu(event:IssueContextMenuData) {
        this.showIssueContextMenu.emit(event);
    }

    protected onShowParallelTaskMenu(event:ParallelTaskMenuData) {
        console.log("Swimlane: Propagating show parallel menu event");
        this.showParallelTaskMenu.emit(event);
    }

    private get visibleColumns() : boolean[] {
        return this.boardData.headers.stateVisibilities;
    }

    get backlogTopHeader():BoardHeaderEntry {
        return this.boardData.headers.backlogTopHeader;
    }

    get mainStates():State[] {
        return this.boardData.mainStates;
    }

    get backlogAndIsCollapsed():boolean {
        if (!this.backlogTopHeader) {
            return false;
        }
        return !this.backlogTopHeader.visible;
    }

    get backlogStatesIfVisible():State[] {
        if (this.backlogTopHeader && this.backlogTopHeader.visible) {
            return this.boardData.backlogStates;
        }
        return null;
    }

    get empty() {
        return this.swimlane.empty;
    }

    toggleBacklog(event:any) {
        this.toggleBacklogVisibility.emit(event);
    }
}

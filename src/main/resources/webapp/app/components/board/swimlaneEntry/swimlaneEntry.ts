import {Component, EventEmitter} from "@angular/core";
import {BoardData} from "../../../data/board/boardData";
import {IssueComponent} from "../issue/issue";
import {SwimlaneData} from "../../../data/board/issueTable";
import {State, BoardHeaderEntry} from "../../../data/board/header";
import {CharArrayRegistry} from "../../../common/charArrayRegistry";
import {IssueContextMenuData} from "../../../data/board/issueContextMenuData";


/**
 * This is here to be able to add a header and the contents for a swimlane
 * as several rows
 */
@Component({
    inputs: ['swimlaneIndex', 'boardData', 'swimlane', 'boardLeftOffset'],
    outputs: ['showIssueContextMenu', 'toggleBacklogVisibility'],
    selector: 'swimlane-entry',
    templateUrl: 'app/components/board/swimlaneEntry/swimlaneEntry.html',
    styleUrls: ['app/components/board/view/kanban/kanbanview.css', 'app/components/board/swimlaneEntry/swimlaneEntry.css'],
    directives: [IssueComponent]
})
export class SwimlaneEntryComponent {
    public swimlane : SwimlaneData;
    public boardData : BoardData;
    public swimlaneIndex : number;
    private _boardLeftOffset:number;
    private showIssueContextMenu:EventEmitter<IssueContextMenuData> = new EventEmitter<IssueContextMenuData>();
    private toggleBacklogVisibility:EventEmitter<any> = new EventEmitter();

    /** Cache all the char arrays used for the collapsed column labels so they are not recalculated all the time */
    private _collapsedColumnLabels:CharArrayRegistry = new CharArrayRegistry();

    constructor() {
    }

    get boardLeftOffset():number {
        return this._boardLeftOffset;
    }

    set boardLeftOffset(value:number) {
        this._boardLeftOffset = value;
    }

    private get boardStates() : State[] {
        return this.boardData.boardStates;
    }

    private getCharArray(state:string):string[] {
        let arr:string[] = [];
        for (let i:number = 0; i < state.length; i++) {
            let s = state.charAt(i);
            if (s == " ") {
            }
            arr.push(s);
        }
        return arr;
    }

    private toggleSwimlane(index:number) {
        this.boardData.toggleSwimlaneVisibility(index);
    }

    private onShowIssueContextMenu(event:IssueContextMenuData) {
        this.showIssueContextMenu.emit(event);
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

    toggleBacklog(event:any) {
        console.log("---> sl tb");
        this.toggleBacklogVisibility.emit(event);
    }
}

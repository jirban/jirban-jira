import {Component, EventEmitter, View} from 'angular2/core';
import {BoardData} from '../../../data/board/boardData';
import {IssueData} from '../../../data/board/issueData';
import {IssueComponent} from '../issue/issue'
import {SwimlaneData} from "../../../data/board/issueTable";

/**
 * This is here to be able to add a header and the contents for a swimlane
 * as several rows
 */
@Component({
    inputs: ['swimlaneIndex', 'boardData', 'swimlane', 'boardLeftOffset'],
    outputs: ['issueContextMenu'],
    selector: 'swimlane-entry'
})
@View({
    templateUrl: 'app/components/board/swimlaneEntry/swimlaneEntry.html',
    styleUrls: ['app/components/board//board.css', 'app/components/board/swimlaneEntry/swimlaneEntry.css'],
    directives: [IssueComponent]
})
export class SwimlaneEntryComponent {
    public swimlane : SwimlaneData;
    public boardData : BoardData;
    public swimlaneIndex : number;
    public boardLeftOffset:number;
    private issueContextMenu:EventEmitter<any> = new EventEmitter();

    constructor() {
    }

    private get boardStates() : string[] {
        return this.boardData.boardStates;
    }

    private toCharArray(state:string):string[] {
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

    private showIssueContextMenu(event:any) {
        this.issueContextMenu.emit(event);
    }

    private get visibleColumns() : boolean[] {
        return this.boardData.headers.stateVisibilities;
    }
}

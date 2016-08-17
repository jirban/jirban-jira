import {Component, EventEmitter} from "@angular/core";
import {BoardData} from "../../../data/board/boardData";

@Component({
    selector: 'health-panel',
    outputs: ['closeHealthPanel'],
    templateUrl: 'app/components/board/healthPanel/healthPanel.html',
    styleUrls: ['app/components/board/healthPanel/healthPanel.css'],
})
export class HealthPanelComponent {
    private closeHealthPanel:EventEmitter<any> = new EventEmitter();

    constructor(private boardData:BoardData) {
    }

    private get states() : string[] {
        if (!this.boardData.blacklist) {
            return [];
        }
        return this.boardData.blacklist.states;
    }

    private get issueTypes() : string[] {
        if (!this.boardData.blacklist) {
            return [];
        }
        return this.boardData.blacklist.issueTypes;
    }

    private get priorities() : string[] {
        if (!this.boardData.blacklist) {
            return [];
        }
        return this.boardData.blacklist.priorities;
    }

    private get issues() : string[] {
        if (!this.boardData.blacklist) {
            return [];
        }
        return this.boardData.blacklist.issues;
    }

    private formatUrl(issue:string) {
        return this.boardData.jiraUrl + "/browse/" + issue;
    }

    private onClickClose(event:MouseEvent) {
        this.closeHealthPanel.emit({});
        event.preventDefault();
    }

    private get rightOffset() : number {
        return this.boardData.blacklist ? 30 : 0;
    }
}

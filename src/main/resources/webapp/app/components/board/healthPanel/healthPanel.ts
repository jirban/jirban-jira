import {Component, EventEmitter, View} from 'angular2/core';
import {BoardData} from '../../../data/board/boardData';

@Component({
    selector: 'health-panel',
    outputs: ['closeHealthPanel']
})
@View({
    templateUrl: 'app/components/board/healthPanel/healthPanel.html',
    styleUrls: ['app/components/board/healthPanel/healthPanel.css'],
})
export class HealthPanelComponent {
    private _states : string[];
    private _issueTypes : string[];
    private _priorities : string[];

    private closeHealthPanel:EventEmitter<any> = new EventEmitter();

    constructor(private boardData:BoardData) {
    }

    private get states() : string[] {
        if (!this._states) {
            this._states = this.getNames("states");
        }
        return this._states;
    }

    private get issueTypes() : string[] {
        if (!this._issueTypes) {
            this._issueTypes = this.getNames("issue-types");
        }
        return this._issueTypes;
    }

    private get priorities() : string[] {
        if (!this._priorities) {
            this._priorities = this.getNames("priorities");
        }
        return this._priorities;
    }

    private getIssuesForState(name:string) : string[] {
        return this.getIssues("states", name);
    }


    private getIssuesForIssueType(name:string) : string[] {
        return this.getIssues("issue-types", name);
    }

    private getIssuesForPriority(name:string) : string[] {
        return this.getIssues("priorities", name);
    }

    private getNames(type:string) : string[] {
        let arr : string[] = [];
        if (this.boardData.missing) {
            for (let name in this.boardData.missing[type]) {
                arr.push(name);
            }
        }
        return arr;
    }

    private getIssues(type:string, name:string) : string[]{
        return this.boardData.missing[type][name].issues;
    }

    private formatUrl(issue:string) {
        return this.boardData.jiraUrl + "/browse/" + issue;
    }

    private onClickClose(event:MouseEvent) {
        this.closeHealthPanel.emit({});
        event.preventDefault();
    }
}

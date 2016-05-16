import {Component, EventEmitter} from "angular2/core";
import {IssueData} from "../../../data/board/issueData";

@Component({
    inputs: ['issue'],
    outputs: ['issueContextMenu'],
    selector: 'issue',
    templateUrl: 'app/components/board/issue/issue.html',
    styleUrls: ['app/components/board/issue/issue.css'],

})
export class IssueComponent {
    private issue : IssueData;
    private issueContextMenu:EventEmitter<any> = new EventEmitter();
    constructor() {
    }

    private get jiraUrl() : string {
        return this.issue.boardData.jiraUrl;
    }

    private get hideAssignee() : boolean {
        return !this.issue.boardData.issueDisplayDetails.assignee;
    }

    private get hideSummary() : boolean {
        return !this.issue.boardData.issueDisplayDetails.summary;
    }

    private get hideInfo() : boolean {
        return !this.issue.boardData.issueDisplayDetails.info;
    }

    private get hideLinkedIssues() : boolean {
        return !this.issue.boardData.issueDisplayDetails.linkedIssues;
    }

    private getStatusFraction(issue:IssueData) : string {
        //This should only be called for linked projects
        return issue.statusIndex + "/" + (issue.linkedProject.statesLength - 1);
    }

    private getStatusColour(issue:IssueData) : string {
        if (issue.statusIndex == 0) {
            //Progress has not been started
            return "red";
        }
        let length:number = issue.linkedProject.statesLength
        if (length - 1 == issue.statusIndex) {
            //It is fully done
            return "green";
        }
        //We are somewhere in the middle, for now return orange
        return "orange";
    }

    private showIssueContextMenuEvent(event : MouseEvent, issueId:string) {
        this.issueContextMenu.emit({
            x: event.clientX,
            y: event.clientY,
            issueId: issueId
        })
        event.preventDefault();
    }

    private defaultContextMenu(event:MouseEvent) {
        event.stopPropagation();
    }
}

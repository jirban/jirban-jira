import {Component, EventEmitter} from "@angular/core";
import {IssueData} from "../../../data/board/issueData";
import {IssueContextMenuData} from "../../../data/board/issueContextMenuData";

@Component({
    inputs: ['issue'],
    outputs: ['showIssueContextMenu'],
    selector: 'issue',
    templateUrl: './issue.html',
    styleUrls: ['./issue.css'],

})
export class IssueComponent {
    private issue : IssueData;
    private showIssueContextMenu:EventEmitter<IssueContextMenuData> = new EventEmitter<IssueContextMenuData>();
    constructor() {
    }

    private get jiraUrl() : string {
        return this.issue.boardData.jiraUrl;
    }

    private get showAssignee() : boolean {
        return this.issue.boardData.issueDisplayDetails.assignee;
    }

    private get showSummary() : boolean {
        return this.issue.boardData.issueDisplayDetails.summary;
    }

    private get showInfo() : boolean {
        return this.issue.boardData.issueDisplayDetails.info;
    }

    private get showLinkedIssues() : boolean {
        return this.issue.boardData.issueDisplayDetails.linkedIssues;
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

    private triggerShowIssueContextMenu(event : MouseEvent, issueId:string) {
        event.preventDefault();
        event.stopPropagation();
        console.log("Issue: Triggering show context menu event")
        this.showIssueContextMenu.emit(
            new IssueContextMenuData(issueId, event.clientX, event.clientY));

    }

    private defaultContextMenu(event:MouseEvent) {
        event.stopPropagation();
    }
}

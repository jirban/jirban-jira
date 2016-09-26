import {Component, EventEmitter} from "@angular/core";
import {IssueData} from "../../../data/board/issueData";
import {IssueContextMenuData} from "../../../data/board/issueContextMenuData";
import {ParallelTask} from "../../../data/board/parallelTask";
import {Indexed} from "../../../common/indexed";
import {ProgressColourService} from "../../../services/progressColourService";
import {Subscription} from "rxjs/Rx";
import {ParallelTaskMenuData} from "../../../data/board/parallelTaskMenuData";

@Component({
    inputs: ['issue'],
    outputs: ['showIssueContextMenu', 'showParallelTaskMenu'],
    selector: 'issue',
    templateUrl: './issue.html',
    styleUrls: ['./issue.css'],

})
export class IssueComponent {
    private issue : IssueData;
    private showIssueContextMenu:EventEmitter<IssueContextMenuData> = new EventEmitter<IssueContextMenuData>();
    private showParallelTaskMenu:EventEmitter<ParallelTaskMenuData> = new EventEmitter<ParallelTaskMenuData>();

    private _parallelTasks:Indexed<ParallelTask>;
    private _parallelTasksTitle:string;

    constructor(private _progressColourService:ProgressColourService) {
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

    private get parallelTasks():ParallelTask[] {
        let parallelTasks:Indexed<ParallelTask> = this.issue.boardData.boardProjects.forKey(this.issue.projectCode).parallelTasks;
        if (!parallelTasks) {
            return null;
        }
        return parallelTasks.array;
    }

    private parallelTaskStyle(taskCode:string):Object{
        let selectedOptionName = this.issue.parallelTaskOptions.forKey(taskCode);
        let parallelTask:ParallelTask
            = this.issue.boardData.boardProjects.forKey(this.issue.projectCode).parallelTasks.forKey(taskCode);
        let progress:number = parallelTask.options.indexOf(selectedOptionName);
        let style:Object = new Object();
        let length:number = parallelTask.options.array.length;
        style["background-color"] = this._progressColourService.getColour(progress, length);

        return style;
    }

    private get parallelTasksTitle() {
        if (!this._parallelTasksTitle) {
            let parallelTasks:ParallelTask[] = this.parallelTasks;
            if (!parallelTasks) {
                return null;
            }

            let subscription: Subscription = this.issue.changeObserver.subscribe(
                done => {
                    this._parallelTasksTitle = null;
                    subscription.unsubscribe();
                }
            );

            let title:string = "";
            for (let i:number = 0 ; i < parallelTasks.length ; i++) {
                if (i > 0) {
                    title += "\n";
                }
                title += parallelTasks[i].name + ": " + this.issue.parallelTaskOptions.forIndex(i);
            }
            this._parallelTasksTitle = title;
        }
        return this._parallelTasksTitle;
    }

    private getLinkedIssueStatusFraction(issue:IssueData) : string {
        //This should only be called for linked projects
        return issue.statusIndex + "/" + (issue.linkedProject.statesLength - 1);
    }

    private getLinkedIssueStatusColour(issue:IssueData) : string {
        return this._progressColourService.getColour(issue.statusIndex, issue.linkedProject.statesLength);
    }

    private triggerShowIssueContextMenu(event : MouseEvent, issueId:string) {
        event.preventDefault();
        event.stopPropagation();
        console.log("Issue: Triggering show context menu event");

        this.showIssueContextMenu.emit(
            new IssueContextMenuData(issueId, event.clientX, event.clientY));

    }

    private triggerShowParallelTaskMenu(event:MouseEvent, taskCode:string) {
        event.preventDefault();
        event.stopPropagation();
        console.log("Issue: Triggering show parallel task menu event");

        this.showParallelTaskMenu.emit(
            new ParallelTaskMenuData(this.issue, taskCode, event.clientX, event.clientY));
    }

    private defaultContextMenu(event:MouseEvent) {
        event.stopPropagation();
    }
}

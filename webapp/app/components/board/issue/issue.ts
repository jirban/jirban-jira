import {Component, EventEmitter, ChangeDetectionStrategy, OnDestroy, ChangeDetectorRef, NgZone} from "@angular/core";
import {IssueData, LinkedIssueData} from "../../../data/board/issueData";
import {IssueContextMenuData} from "../../../data/board/issueContextMenuData";
import {ParallelTask} from "../../../data/board/parallelTask";
import {Indexed} from "../../../common/indexed";
import {ProgressColourService} from "../../../services/progressColourService";
import {ParallelTaskMenuData} from "../../../data/board/parallelTaskMenuData";
import {Subscription} from "rxjs";
import {JiraMultiSelectFieldValue} from "../../../data/board/multiSelectNameOnlyValue";

@Component({
    inputs: ['issue'],
    outputs: ['showIssueContextMenu', 'showParallelTaskMenu'],
    selector: 'issue',
    templateUrl: './issue.html',
    styleUrls: ['./issue.css'],
    changeDetection: ChangeDetectionStrategy.OnPush

})
export class IssueComponent implements OnDestroy {
    private _issue : IssueData;

    //Events emitted
    private showIssueContextMenu:EventEmitter<IssueContextMenuData> = new EventEmitter<IssueContextMenuData>();
    private showParallelTaskMenu:EventEmitter<ParallelTaskMenuData> = new EventEmitter<ParallelTaskMenuData>();


    //Used to show titles in the issue cards
    private _currentTitleTimeout:any;
    private issueTitle:string;
    private parallelTasksTitle:string;
    private _linkedIssueTitleKey:string;
    private _linkedIssueTitle:string;

    //Subscriptions to changes
    private _filteredSubscription:Subscription;
    private _issueDisplayDetailsSubscription:Subscription;

    constructor(private _progressColourService:ProgressColourService,
                private _changeDetector:ChangeDetectorRef,
                private _zone:NgZone) {
    }

    set issue(issue:IssueData) {
        //Clear any current subscriptions
        this.unsubscribe();

        this._issue = issue;

        if (this._issue) {
            this._filteredSubscription = this._issue.filteredObservable.subscribe(
                done => {
                    this.viewUpdated();
                }
            );
            this._issueDisplayDetailsSubscription = this.issue.boardData.issueDisplayDetailsObservable.subscribe(
                done => {
                    this.viewUpdated();
                }
            );
        }
    }


    ngOnDestroy(): void {
        this.unsubscribe();
    }

    private unsubscribe():void {
        if (this._filteredSubscription) {
            this._filteredSubscription.unsubscribe();
        }
        if (this._issueDisplayDetailsSubscription) {
            this._issueDisplayDetailsSubscription.unsubscribe();
        }
    }

    private viewUpdated():void {
        this._zone.run(()=>{
            this._changeDetector.markForCheck();
        });
    }

    get issue(): IssueData {
        return this._issue;
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
        let parallelTasks:Indexed<ParallelTask> = this.issue.project.parallelTasks;
        if (!parallelTasks) {
            return null;
        }
        return parallelTasks.array;
    }

    private parallelTaskStyle(taskCode:string):Object{
        let selectedOptionName = this.issue.parallelTaskOptions.forKey(taskCode);
        let parallelTask:ParallelTask
            = this.issue.project.parallelTasks.forKey(taskCode);
        let progress:number = parallelTask.options.indexOf(selectedOptionName);
        let style:Object = new Object();
        let length:number = parallelTask.options.array.length;
        style["background-color"] = this._progressColourService.getColour(progress, length);

        return style;
    }

    private getLinkedIssueStatusFraction(issue:LinkedIssueData) : string {
        //This should only be called for linked projects
        return issue.statusIndex + "/" + (issue.project.statesLength - 1);
    }

    private getLinkedIssueStatusColour(issue:LinkedIssueData) : string {
        return this._progressColourService.getColour(issue.statusIndex, issue.project.statesLength);
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

    private showTitle(event:MouseEvent, type:string, extra?:Object) {
        event.stopPropagation();
        if (this._currentTitleTimeout) {
            clearTimeout(this._currentTitleTimeout);
        }
        this._currentTitleTimeout = setTimeout(()=>{
            //this.doHideTitles();
            if (type === 'issue') {
                this.issueTitle = this.calculateIssueTitle();
            } else if (type === 'parallel') {
                this.parallelTasksTitle = this.calculateParallelTasksTitle();
            } else if (type === 'linked') {
                let linked:LinkedIssueData = <LinkedIssueData>extra;
                this._linkedIssueTitleKey = linked.key;
                this._linkedIssueTitle = this.calculateLinkedIssueTitle(linked);
            }
            this.viewUpdated();
        }, 75);
    }

    private hideTitles(event:MouseEvent) {
        this.doHideTitles();
        if (this._currentTitleTimeout) {
            clearTimeout(this._currentTitleTimeout);
        }
        this.doHideTitles();
    }

    private doHideTitles() {
        if (this.issueTitle || this.parallelTasksTitle) {
            this.issueTitle = null;
            this.parallelTasksTitle = null;
            this.viewUpdated();
        }
    }

    private calculateIssueTitle():string{
        let title:string =
            this._issue.key + "\n" +
            this._issue.summary + "\n\n" +
            "State: " + this._issue.ownStatus + "\n" +
            "Priority: " + this._issue.priority.name + "\n" +
            "Type: " + this._issue.type.name;

        if (this.issue.components) {
            title += "\n" + this.createMultiSelectNameOnlyValueTitleString("Components", this.issue.components.array);
        }
        if (this.issue.labels) {
            title += "\n" + this.createMultiSelectNameOnlyValueTitleString("Labels", this.issue.labels.array);
        }
        if (this.issue.fixVersions) {
            title += "\n" + this.createMultiSelectNameOnlyValueTitleString("Fix Versions", this.issue.fixVersions.array);
        }

        return title;
    }

    private createMultiSelectNameOnlyValueTitleString(header:string, values:JiraMultiSelectFieldValue[]) {
        let result = header + ": ";
        let first:boolean = true;
        for (let value of values) {
            if (first) {
                first = false;
            } else {
                result += ", ";
            }
            result += value.name;
        }
        return result;
    }

    private calculateParallelTasksTitle():string {
        let parallelTasks:ParallelTask[] = this.parallelTasks;
        if (!parallelTasks) {
            return null;
        }

        let title:string = "";
        for (let i:number = 0 ; i < parallelTasks.length ; i++) {
            if (i > 0) {
                title += "\n";
            }
            title += parallelTasks[i].name + ": " + this._issue.parallelTaskOptions.forIndex(i);
        }
        return title;
    }

    private calculateLinkedIssueTitle(linked:LinkedIssueData):string {
        let title:string = linked.key + "\n" + linked.ownStatus + "\n" + linked.summary;
        return title;
    }

    private getLinkedIssueTitle(linked:LinkedIssueData):string {
        if (this._linkedIssueTitleKey == linked.key) {
            return this._linkedIssueTitle;
        }
        return null;
    }
}

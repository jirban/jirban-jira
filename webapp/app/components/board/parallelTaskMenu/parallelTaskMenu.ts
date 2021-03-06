import {Component, EventEmitter} from "@angular/core";
import {ParallelTaskMenuData} from "../../../data/board/parallelTaskMenuData";
import {ParallelTask} from "../../../data/board/parallelTask";
import {BoardProject} from "../../../data/board/project";
import {IssueData} from "../../../data/board/issueData";
import {ProgressColourService} from "../../../services/progressColourService";
import {IssuesService} from "../../../services/issuesService";
import {ProgressErrorService} from "../../../services/progressErrorService";

@Component({
    inputs: ['parallelTaskMenuData'],
    outputs: ['closeParallelTaskMenu'],
    selector: 'parallel-task-menu',
    templateUrl: './parallelTaskMenu.html',
    styleUrls: ['./parallelTaskMenu.css']
})
export class ParallelTaskMenuComponent {

    private _parallelTaskMenuData:ParallelTaskMenuData;
    private parallelTaskMenuPosition:Object;

    private _issue:IssueData;
    private _task:ParallelTask;
    private _taskIndex:number;
    private _originalOptionIndex:number;
    private _originalOptionName:string;

    private _activeOptionName:string;
    private _selectedOptionIndex:number;
    private _selectedOptionName:string;

    private closeParallelTaskMenu:EventEmitter<any> = new EventEmitter<any>();

    constructor(
        private _progressColourService:ProgressColourService,
        private _issuesService:IssuesService,
        private _progressError:ProgressErrorService) {
    }

    set parallelTaskMenuData(parallelTaskMenuData:ParallelTaskMenuData) {
        this._parallelTaskMenuData = parallelTaskMenuData;
        this.setWindowSize();

        this._issue = parallelTaskMenuData.issue;
        let project:BoardProject = this._issue.boardData.boardProjects.forKey(this._issue.projectCode);
        this._task = project.parallelTasks.forKey(parallelTaskMenuData.taskCode);
        this._taskIndex = project.parallelTasks.indexOf(parallelTaskMenuData.taskCode);
        let option:string = this._issue.parallelTaskOptions.forIndex(this._taskIndex);
        this._originalOptionIndex = this._task.options.indexOf(option);
        this._originalOptionName = option;
        console.log("---> " + this._originalOptionIndex);
    }

    getColour(optionIndex:number):string{
        return this._progressColourService.getColour(optionIndex, this._task.options.array.length);
    }

    isSelected(optionIndex:number):boolean{
        //console.log(optionIndex + "==" + this.currentOptionIndex + ": " + (optionIndex === this.currentOptionIndex));
        return optionIndex === this._originalOptionIndex;
    }

    getOptionName(optionIndex:number):string {
        return this._task.options.forIndex(optionIndex);
    }

    private get selectedOptionName() {
        if (this._selectedOptionName) {
            return this._selectedOptionName;
        }
        if (this._activeOptionName) {
            return this._activeOptionName;
        }
        return this._originalOptionName;
    }

    private get task():ParallelTask {
        return this._task;
    }

    getSelectedClass(optionIndex:number):string {
        if (optionIndex == this._selectedOptionIndex) {
            return 'selected';
        } if (optionIndex == this._originalOptionIndex) {
            return isNaN(this._selectedOptionIndex) ? 'selected' : 'original';
        } else {
            return 'unselected';
        }
    }

    onMouseOverOption(optionIndex:number) {
        this._activeOptionName = this._task.options.forIndex(optionIndex);
    }

    onMouseOutOption(optionIndex:number) {
        this._activeOptionName = null;
    }

    onSelectOption(event:MouseEvent, optionIndex:number) {
        event.preventDefault();
        this._selectedOptionIndex = optionIndex;
        this._selectedOptionName = this._task.options.forIndex(optionIndex);

        this._progressError.startProgress(true);
        this._issuesService.setParallelTaskOption(this._issue.key, this._taskIndex, this._selectedOptionIndex)
            .subscribe(
                data => {
                    //No data is returned, issuesService refreshes boardData for us
                    //TODO close this thing
                    this.closeParallelTaskMenu.emit({});
                },
                err => {
                    this._progressError.setError(err);
                },
                () => {
                    let msg = "Set " + this._task.name + " <a " +
                        "class='toolbar-message' href='" + this._issue.boardData.jiraUrl + "/browse/" + this._issue.key + "'>" +
                        this._issue.key + "</a> to " + this._selectedOptionName;
                    this._progressError.finishProgress(msg);
                }
            );
    }

    private setWindowSize() {
        this.parallelTaskMenuPosition = new Object();
        if (this._parallelTaskMenuData.x > 100) {
            this.parallelTaskMenuPosition["right"] = (window.innerWidth - this._parallelTaskMenuData.x).toString() + "px";
        } else {
            this.parallelTaskMenuPosition["left"] = this._parallelTaskMenuData.x.toString() + "px";
        }
        if (this._parallelTaskMenuData.y < window.innerHeight - 100) {
            this.parallelTaskMenuPosition["top"] = (this._parallelTaskMenuData.y + 10).toString() + "px";
        } else {
            this.parallelTaskMenuPosition["bottom"] = (window.innerHeight - this._parallelTaskMenuData.y + 10).toString() + "px";
        }
        console.log(this.parallelTaskMenuPosition);
    }
}
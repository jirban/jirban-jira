import {Component, EventEmitter, View,} from 'angular2/core';
import {Control, ControlGroup, FormBuilder, FORM_DIRECTIVES} from 'angular2/common';
import {Router, RouteParams} from 'angular2/router';

import {NO_ASSIGNEE, Assignee} from "../../../data/board/assignee";
import {BoardData} from '../../../data/board/boardData';
import {IssueDisplayDetails} from '../../../data/board/boardFilters';
import {IssueData} from '../../../data/board/issueData';
import {IssueType} from '../../../data/board/issueType';
import {Priority} from "../../../data/board/priority";

import {IMap} from '../../../common/map';
import {AbstractControl} from "angular2/common";

@Component({
    selector: 'control-panel',
    outputs: ['closeControlPanel']
})
@View({
    templateUrl: 'app/components/board/controlPanel/controlPanel.html',
    styleUrls: ['app/components/board/controlPanel/controlPanel.css'],
    directives: [FORM_DIRECTIVES]
})
export class ControlPanelComponent {
    private _swimlaneForm:ControlGroup;
    private _issueDetailForm:ControlGroup;
    private _projectFilterForm:ControlGroup;
    private _priorityFilterForm:ControlGroup;
    private _issueTypeFilterForm:ControlGroup;
    private _assigneeFilterForm:ControlGroup;

    private closeControlPanel:EventEmitter<any> = new EventEmitter();

    private noAssignee : string = NO_ASSIGNEE;

    constructor(private boardData:BoardData, private formBuilder: FormBuilder) {
    }

    private get swimlaneForm() : ControlGroup {
        if (this._swimlaneForm) {
            return this._swimlaneForm;
        }
        let form : ControlGroup =
            this.formBuilder.group({});
        form.addControl("swimlane", new Control(null));
        form.valueChanges
            .subscribe((value) => {
                this.updateSwimlane(value);
            });

        this._swimlaneForm = form;
        return this._swimlaneForm;
    }

    private get issueDetailForm() : ControlGroup {
        if (this._issueDetailForm) {
            return this._issueDetailForm;
        }
        let form : ControlGroup =
            this.formBuilder.group({});

        form.addControl("assignee", new Control(true));
        form.addControl("description", new Control(true));
        form.addControl("info", new Control(true));
        form.addControl("linked", new Control(true));
        form.valueChanges
            .subscribe((value) => {
                this.updateIssueDetail(value);
            });

        this._issueDetailForm = form;
        return this._issueDetailForm;
    }

    private get projectFilterForm() : ControlGroup {
        if (this._projectFilterForm) {
            return this._projectFilterForm;
        }
        let form : ControlGroup =
            this.formBuilder.group({});

        //Add the projects to the form
        for (let projectName of this.boardData.boardProjectCodes) {
            form.addControl(projectName, new Control(false));
        }
        form.valueChanges
            .subscribe((value) => {
                this.updateProjectFilter(value);
            });

        this._projectFilterForm = form;
        return this._projectFilterForm;
    }

    private get priorityFilterForm() : ControlGroup {
        if (this._priorityFilterForm) {
            return this._priorityFilterForm;
        }
        let form : ControlGroup =
            this.formBuilder.group({});

        for (let priority of this.priorities) {
            form.addControl(priority.name, new Control(false));
        }
        form.valueChanges
            .subscribe((value) => {
                this.updatePriorityFilter(value);
            });

        this._priorityFilterForm = form;
        return this._priorityFilterForm;
    }

    private get issueTypeFilterForm() : ControlGroup {
        if (this._issueTypeFilterForm) {
            return this._issueTypeFilterForm;
        }
        let form : ControlGroup =
            this.formBuilder.group({});

        for (let issueType of this.issueTypes) {
            form.addControl(issueType.name, new Control(false));
        }
        form.valueChanges
            .subscribe((value) => {
                this.updateIssueTypeFilter(value);
            });

        this._issueTypeFilterForm = form;
        return this._issueTypeFilterForm;
    }

    private get assigneeFilterForm() : ControlGroup {
        if (!this._assigneeFilterForm) {
            console.log("----> assignee form");

            let form:ControlGroup =
                this.formBuilder.group({});

            //The unassigned assignee and the ones configured in the project
            form.addControl(NO_ASSIGNEE, new Control(false));
            for (let assignee of this.assignees) {
                form.addControl(assignee.key, new Control(false));
            }
            form.valueChanges
                .subscribe((value) => {
                    this.updateAssigneeFilter(value);
                });

            this._assigneeFilterForm = form;
        } else if (this.boardData.getAndClearHasNewAssignees()) {
            //TODO look into an Observable instead
            console.log("----> checking assignee form");

            let form:ControlGroup = this._assigneeFilterForm;
            for (let assignee of this.assignees) {
                let control:AbstractControl = form.controls[assignee.key];
                if (!control) {
                    form.addControl(assignee.key, new Control(false));
                }
            }
        }
        return this._assigneeFilterForm;
    }

    private get assignees() : Assignee[] {
        return this.boardData.assignees.array;
    }

    private get priorities() : Priority[] {
        return this.boardData.priorities.array;
    }

    private get issueTypes() : IssueType[] {
        return this.boardData.issueTypes.array;
    }

    private get boardProjectCodes() : string[] {
        return this.boardData.boardProjectCodes;
    }

    private updateSwimlane(value:any) {
        console.log("Swimlane: " + JSON.stringify(value));
        this.boardData.swimlane = value.swimlane;
    }

    private updateIssueDetail(value:any) {
        console.log("Detail: " + JSON.stringify(value));
        this.boardData.updateIssueDetail(value.assignee, value.description, value.info, value.linked);
    }

    private updateProjectFilter(value:any) {
        console.log(JSON.stringify(value));
        this.boardData.updateProjectFilter(value);
    }

    private updatePriorityFilter(value:any) {
        console.log(JSON.stringify(value));
        this.boardData.updatePriorityFilter(value);
    }

    private updateIssueTypeFilter(value:any) {
        console.log(JSON.stringify(value));
        this.boardData.updateIssueTypeFilter(value);
    }

    private updateAssigneeFilter(value:any) {
        console.log(JSON.stringify(value));
        this.boardData.updateAssigneeFilter(value);
    }

    private onClickClose(event:MouseEvent) {
        this.closeControlPanel.emit({});
        event.preventDefault();
    }
}

import {Component, EventEmitter} from "angular2/core";
import {AbstractControl, Control, ControlGroup, FormBuilder, FORM_DIRECTIVES} from "angular2/common";
import {NO_ASSIGNEE, Assignee} from "../../../data/board/assignee";
import {NO_COMPONENT, JiraComponent} from "../../../data/board/component";
import {BoardData} from "../../../data/board/boardData";
import {IssueType} from "../../../data/board/issueType";
import {Priority} from "../../../data/board/priority";
import {IMap} from "../../../common/map";
import "rxjs/add/operator/debounceTime";
import {BoardFilters} from "../../../data/board/boardFilters";

@Component({
    selector: 'control-panel',
    outputs: ['closeControlPanel'],
    templateUrl: 'app/components/board/controlPanel/controlPanel.html',
    styleUrls: ['app/components/board/controlPanel/controlPanel.css'],
    directives: [FORM_DIRECTIVES]
})
export class ControlPanelComponent {

    private _controlForm:ControlGroup;
    private _assigneeFilterForm:ControlGroup;
    private _componentFilterForm:ControlGroup;

    private closeControlPanel:EventEmitter<any> = new EventEmitter();

    private noAssignee:string = NO_ASSIGNEE;
    private noComponent:string = NO_COMPONENT;

    private linkUrl:string;

    constructor(private boardData:BoardData, private formBuilder:FormBuilder) {
    }

    private get controlForm():ControlGroup {
        if (this._controlForm) {
            return this._controlForm;
        }

        let form:ControlGroup = this.formBuilder.group({});
        form.addControl("swimlane", new Control(this.boardData.swimlane));

        let detailForm:ControlGroup = this.formBuilder.group({});
        form.addControl("detail", detailForm);
        detailForm.addControl("assignee", new Control(this.boardData.issueDisplayDetails.assignee));
        detailForm.addControl("description", new Control(this.boardData.issueDisplayDetails.summary));
        detailForm.addControl("info", new Control(this.boardData.issueDisplayDetails.info));
        detailForm.addControl("linked", new Control(this.boardData.issueDisplayDetails.linkedIssues));

        let filters:BoardFilters = this.boardData.filters;

        let projectFilterForm:ControlGroup = this.formBuilder.group({});
        for (let projectName of this.boardData.boardProjectCodes) {
            projectFilterForm.addControl(projectName, new Control(filters.initialProjectValueForForm(projectName)));
        }

        let priorityFilterForm:ControlGroup = this.formBuilder.group({});
        for (let priority of this.priorities) {
            priorityFilterForm.addControl(priority.name, new Control(filters.initialPriorityValueForForm(priority.name)));
        }

        let issueTypeFilterForm:ControlGroup = this.formBuilder.group({});
        for (let issueType of this.issueTypes) {
            issueTypeFilterForm.addControl(issueType.name, new Control(filters.initialIssueTypeValueForForm(issueType.name)));
        }

        let assigneeFilterForm:ControlGroup = this.formBuilder.group({});
        //The unassigned assignee and the ones configured in the project
        assigneeFilterForm.addControl(NO_ASSIGNEE, new Control(filters.initialAssigneeValueForForm(NO_ASSIGNEE)));
        for (let assignee of this.assignees) {
            assigneeFilterForm.addControl(assignee.key, new Control(filters.initialAssigneeValueForForm(assignee.key)));
        }

        let componentFilterForm:ControlGroup = this.formBuilder.group({});
        //The unassigned assignee and the ones configured in the project
        componentFilterForm.addControl(NO_COMPONENT, new Control(filters.initialComponentValueForForm(NO_COMPONENT)));
        for (let component of this.components) {
            componentFilterForm.addControl(component.name, new Control(filters.initialComponentValueForForm(component.name)));
        }

        //Add to the main form
        form.addControl("project", projectFilterForm);
        form.addControl("priority", priorityFilterForm);
        form.addControl("issue-type", issueTypeFilterForm);
        form.addControl("assignee", assigneeFilterForm);
        form.addControl("component", componentFilterForm);

        let dirtyChecker:DirtyChecker = DirtyChecker.create(form);

        form.valueChanges
            .debounceTime(150) //A delay for when people clear the filters so that we only get one event sent since filters are costly to recalculate
            .subscribe((value) => {
                let dirty:IMap<boolean> = dirtyChecker.checkDirty(value);
                //Swimlane is necessary to update if dirty
                if (dirty["swimlane"]) {
                    this.updateSwimlane(value);
                }
                //Detail is cheap to update if dirty
                if (dirty["detail"]) {
                    this.updateIssueDetail(value["detail"]);
                }
                //Updating the filters is costly so do it all in one go
                if (dirty["project"] || dirty["priority"] || dirty["issue-type"] || dirty["assignee"] || dirty["component"]) {
                    this.updateFilters(value["project"], value["priority"], value["issue-type"], value["assignee"], value["component"]);
                }
                this.updateLinkUrl();
            });

        this._assigneeFilterForm = assigneeFilterForm;
        this._componentFilterForm = componentFilterForm;
        this._controlForm = form;
        this.updateLinkUrl();
        return this._controlForm;
    }


    private addControlAndRecordInitialValue(form:ControlGroup, initialStateRecorder:any, groupName:string, name:string, value:any) {
        form.addControl(name, new Control(value));

        let recorder:any = initialStateRecorder;
        if (groupName) {
            let groupRecorder = recorder[groupName];
            if (!groupRecorder) {
                recorder[groupName] = {};
                groupRecorder = recorder[groupName];
            }
            recorder = groupRecorder;
        }
        recorder[name] = value;
    }

    private clearFilter(event:MouseEvent, name:string) {
        event.preventDefault();
        let filter:ControlGroup = <ControlGroup>this._controlForm.controls[name];
        for (let key in filter.controls) {
            let control:Control = <Control>filter.controls[key];
            control.updateValue(false);
        }
    }

    private get assignees():Assignee[] {
        if (this.boardData.getAndClearHasNewAssignees()) {
            //TODO look into using an Observable for this
            for (let assignee of this.boardData.assignees.array) {
                if (!this._assigneeFilterForm.controls[assignee.key]) {
                    this._assigneeFilterForm.addControl(assignee.key, new Control(false));
                }
            }
        }
        return this.boardData.assignees.array;
    }

    private get components():JiraComponent[] {
        if (this.boardData.getAndClearHasNewComponents()) {
            //TODO look into using an Observable for this instead
            for (let component of this.boardData.components.array) {
                if (!this._componentFilterForm.controls[component.name]) {
                    this._componentFilterForm.addControl(component.name, new Control(false));
                }
            }
        }
        return this.boardData.components.array;
    }

    private get priorities():Priority[] {
        return this.boardData.priorities.array;
    }

    private get issueTypes():IssueType[] {
        return this.boardData.issueTypes.array;
    }

    private get boardProjectCodes():string[] {
        return this.boardData.boardProjectCodes;
    }

    private updateSwimlane(value:any) {
        this.boardData.swimlane = value.swimlane;
    }

    private updateIssueDetail(value:any) {
        this.boardData.updateIssueDetail(value.assignee, value.description, value.info, value.linked);
    }

    private updateFilters(project:any, priority:any, issueType:any, assignee:any, component:any) {
        this.boardData.updateFilters(project, priority, issueType, assignee, component);
    }

    private onClickClose(event:MouseEvent) {
        this.closeControlPanel.emit({});
        event.preventDefault();
    }

    private grabInitialFormValues(form:ControlGroup):any {
        let values:any = {};

        for (let key in form.controls) {
            let ac:AbstractControl = form.controls[key];
            if (ac instanceof Control) {
                values[key] = ac.value;
            } else {
                values[key] = this.grabInitialFormValues(<ControlGroup>ac);
            }
        }

        return values;
    }


    private updateLinkUrl() {
        let url:string = window.location.href;
        let index = url.lastIndexOf("?");
        if (index >= 0) {
            url = url.substr(0, index);
        }
        url = url + "?board=" + this.boardData.code;

        if (this.boardData.swimlane) {
            url += "&swimlane=" + this.boardData.swimlane;
        }

        url += this.boardData.issueDisplayDetails.createQueryStringParticle();
        url += this.boardData.filters.createQueryStringParticles();

        console.log(url);
        this.linkUrl = url;
    }
}

/**
 * Hack to work around the current lack of ability to reset the dirty status of a ControlGroup (or any control for that matter)
 */
class DirtyChecker {

    constructor(private _lastValues:any) {
    }

    static create(form:ControlGroup):DirtyChecker {
        let dirtyChecker:DirtyChecker = new DirtyChecker(DirtyChecker.initialize(form));
        return dirtyChecker;
    }

    private static initialize(form:ControlGroup):any {
        let values:any = {};

        for (let key in form.controls) {
            let ac:AbstractControl = form.controls[key];
            if (ac instanceof Control) {
                values[key] = ac.value;
            } else {
                values[key] = DirtyChecker.initialize(<ControlGroup>ac);
            }
        }
        return values;
    }

    checkDirty(value:any):IMap<boolean> {
        let dirty:IMap<boolean> = {};

        for (let key in value) {
            if (key === "swimlane") {
                let sl:string = value[key];
                if (sl == "") {
                    sl = null;
                }
                if (sl != this._lastValues["swimlane"]) {
                    dirty[key] = true;
                }
            } else {
                dirty[key] = this.isDirty(value[key], this._lastValues[key]);
            }
        }

        this._lastValues = value;
        return dirty;
    }

    private isDirty(value:any, last:any):boolean {
        let keys:string[] = [];
        for (let key in value) {
            keys.push(key);
        }

        keys = keys.sort();

        for (let key of keys) {
            if (value[key] != last[key]) {
                return true;
            }
        }
        return false;
    }
}


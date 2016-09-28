import {Component, EventEmitter} from "@angular/core";
import {Assignee} from "../../../data/board/assignee";
import {JiraComponent} from "../../../data/board/component";
import {BoardData} from "../../../data/board/boardData";
import {NONE, BoardFilters} from "../../../data/board/boardFilters";
import {IssueType} from "../../../data/board/issueType";
import {Priority} from "../../../data/board/priority";
import {IMap} from "../../../common/map";
import "rxjs/add/operator/debounceTime";
import {CustomFieldValues} from "../../../data/board/customField";
import {VIEW_KANBAN} from "../../../common/constants";
import {FormGroup, FormControl, AbstractControl} from "@angular/forms";
import {Indexed} from "../../../common/indexed";
import {ParallelTask} from "../../../data/board/parallelTask";

@Component({
    selector: 'control-panel',
    inputs: ['view', 'linkUrl'],
    outputs: ['closeControlPanel', 'filtersUpdated'],
    templateUrl: './controlPanel.html',
    styleUrls: ['./controlPanel.css']
})
export class ControlPanelComponent {

    private _controlForm:FormGroup;
    private _assigneeFilterForm:FormGroup;
    private _componentFilterForm:FormGroup;

    private closeControlPanel:EventEmitter<any> = new EventEmitter();
    private filtersUpdated:EventEmitter<any> = new EventEmitter<any>();

    private none:string = NONE;

    private filtersToDisplay:string = null;

    private filtersTooltip:string;
    private filterTooltips:IMap<string> = {};

    private view:string;

    private linkUrl:string;

    constructor(private boardData:BoardData) {
    }

    private get controlForm():FormGroup {
        if (this._controlForm) {
            return this._controlForm;
        }

        let form:FormGroup = new FormGroup({});
        form.addControl("swimlane", new FormControl(this.boardData.swimlane));

        let detailForm:FormGroup = new FormGroup({});
        detailForm.addControl("assignee", new FormControl(this.boardData.issueDisplayDetails.assignee));
        detailForm.addControl("description", new FormControl(this.boardData.issueDisplayDetails.summary));
        detailForm.addControl("info", new FormControl(this.boardData.issueDisplayDetails.info));
        detailForm.addControl("linked", new FormControl(this.boardData.issueDisplayDetails.linkedIssues));
        form.addControl("detail", detailForm);

        let filters:BoardFilters = this.boardData.filters;

        let projectFilterForm:FormGroup = new FormGroup({});
        for (let projectName of this.boardData.boardProjectCodes) {
            projectFilterForm.addControl(projectName, new FormControl(filters.initialProjectValueForForm(projectName)));
        }

        let priorityFilterForm:FormGroup = new FormGroup({});
        for (let priority of this.priorities) {
            priorityFilterForm.addControl(priority.name, new FormControl(filters.initialPriorityValueForForm(priority.name)));
        }

        let issueTypeFilterForm:FormGroup = new FormGroup({});
        for (let issueType of this.issueTypes) {
            issueTypeFilterForm.addControl(issueType.name, new FormControl(filters.initialIssueTypeValueForForm(issueType.name)));
        }

        let assigneeFilterForm:FormGroup = new FormGroup({});
        //The unassigned assignee and the ones configured in the project
        assigneeFilterForm.addControl(NONE, new FormControl(filters.initialAssigneeValueForForm(NONE)));
        for (let assignee of this.assignees) {
            assigneeFilterForm.addControl(assignee.key, new FormControl(filters.initialAssigneeValueForForm(assignee.key)));
        }

        let componentFilterForm:FormGroup = new FormGroup({});
        //The unassigned assignee and the ones configured in the project
        componentFilterForm.addControl(NONE, new FormControl(filters.initialComponentValueForForm(NONE)));
        for (let component of this.components) {
            componentFilterForm.addControl(component.name, new FormControl(filters.initialComponentValueForForm(component.name)));
        }

        //Add to the main form
        form.addControl("project", projectFilterForm);
        form.addControl("priority", priorityFilterForm);
        form.addControl("issue-type", issueTypeFilterForm);
        form.addControl("assignee", assigneeFilterForm);
        form.addControl("component", componentFilterForm);

        //Add the custom fields form(s) to the main form
        if (this.customFields.length > 0) {
            let customFieldsFilterForm:FormGroup = new FormGroup({});
            for (let customFieldValues of this.customFields) {
                let customFieldForm:FormGroup = new FormGroup({});
                customFieldForm.addControl(NONE, new FormControl(filters.initialCustomFieldValueForForm(customFieldValues.name, NONE)))
                for (let customFieldValue of customFieldValues.values.array) {
                    customFieldForm.addControl(customFieldValue.key, new FormControl(filters.initialCustomFieldValueForForm(customFieldValues.name, customFieldValue.key)));
                }
                customFieldsFilterForm.addControl(customFieldValues.name, customFieldForm);
            }
            form.addControl("custom-fields", customFieldsFilterForm);
        }

        //Add the parallel-tasks form to the main form
        if (this.hasParallelTasks()) {
            let parallelTasksFilterForm:FormGroup = new FormGroup({});
            for (let task of this.parallelTasks) {
                let options:Indexed<string> = task.options;
                let optionsForm:FormGroup = new FormGroup({});
                parallelTasksFilterForm.addControl(task.code, optionsForm);
                for (let option of options.array) {
                    optionsForm.addControl(
                        option,
                        new FormControl(filters.initialParallelTaskValueForForm(task.code, option)));
                }
            }
            form.addControl("parallel-tasks", parallelTasksFilterForm);
        }

        let lastValues = form.value;

        form.valueChanges
            .debounceTime(150) //A delay for when people clear the filters so that we only get one event sent since filters are costly to recalculate
            .subscribe((value) => {
                //console.log(JSON.stringify(value));

                //Swimlane is necessary to update if dirty
                if (this.isDirty("swimlane")) {
                    this.updateSwimlane(value);
                }
                //Detail is cheap to update if dirty
                if (this.isDirty("detail")) {
                    this.updateIssueDetail(detailForm, lastValues["detail"], value["detail"]);
                }

                if (this.isDirty("project") || this.isDirty("priority") || this.isDirty("issue-type")
                    || this.isDirty("assignee") || this.isDirty("component") || this.isDirty("custom-fields")
                    || this.isDirty("parallel-tasks")) {

                    let customFieldFormValues:IMap<any> = {};
                    let customFieldValue:any = value["custom-fields"];
                    for (let customFieldValues of this.customFields) {
                        customFieldFormValues[customFieldValues.name] = customFieldValue[customFieldValues.name];
                    }
                    let parallelTaskFormValues:IMap<any> = {};
                    let parallelTaskValue:any = value["parallel-tasks"];
                    for (let task of this.parallelTasks) {
                        parallelTaskFormValues[task.code] = parallelTaskValue[task.code];
                    }
                    this.updateFilters(
                        value["project"], value["priority"], value["issue-type"], value["assignee"],
                        value["component"], customFieldFormValues, parallelTaskFormValues);
                }
                this.updateLinkUrl();

                form.reset(value);
                lastValues = value;
            });

        this._assigneeFilterForm = assigneeFilterForm;
        this._componentFilterForm = componentFilterForm;
        this._controlForm = form;
        this.updateTooltips();

        return this._controlForm;
    }

    private addControlAndRecordInitialValue(form:FormGroup, initialStateRecorder:any, groupName:string, name:string, value:any) {
        form.addControl(name, new FormControl(value));

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


    private clearFilters(event:MouseEvent, name:string) {
        event.preventDefault();
        this.clearFilter(event, 'project');
        this.clearFilter(event, 'issue-type');
        this.clearFilter(event, 'priority');
        this.clearFilter(event, 'assignee');
        this.clearFilter(event, 'component');
        for (let customFieldValues of this.customFields) {
            this.clearFilter(event, "custom-fields");
        }
        for (let task of this.parallelTasks) {
            this.clearFilter(event, "parallel-tasks");
        }
    }

    private clearFilter(event:MouseEvent, name:string) {
        event.preventDefault();
        let complexForms:string[] = ["parallel-tasks", "custom-fields"];
        let complexForm:string = null;

        for (let test of complexForms) {
            if (name.startsWith(test)) {
                complexForm = test;
                break;
            }
        }

        if (!complexForm) {
            let filter:FormGroup = <FormGroup>this._controlForm.controls[name];
            this.clearFilterForm(filter);
        } else {
            if (name === complexForm) {
                let filter:FormGroup = <FormGroup>this._controlForm.controls[name];
                for (let key in filter.controls) {
                    let form:FormGroup = <FormGroup>filter.controls[key];
                    this.clearFilterForm(form);
                }
            } else {
                let filter:FormGroup = <FormGroup>this._controlForm.controls[complexForm];
                let formName:string = name.substr(complexForm.length + 1);
                this.clearFilterForm(<FormGroup>filter.controls[formName]);
            }
        }
    }

    private clearFilterForm(filterForm:FormGroup) {
        for (let key in filterForm.controls) {
            let control:FormControl = <FormControl>filterForm.controls[key];
            control.setValue(false);
            control.markAsDirty();
        }
    }

    private clearSwimlane(event:MouseEvent) {
        event.preventDefault();
        let swimlane:FormControl = <FormControl>this._controlForm.controls['swimlane'];
        swimlane.setValue(null);
        swimlane.markAsDirty();
    }

    private clearDetail(event:MouseEvent) {
        event.preventDefault();
        let group:FormGroup = <FormGroup>this._controlForm.controls['detail'];

        let control:FormControl = <FormControl>group.controls['assignee'];
        if (!control.value) {
            control.setValue(true);
            control.markAsDirty();
        }

        control = <FormControl>group.controls['description'];
        if (!control.value) {
            control.setValue(true);
            control.markAsDirty();
        }

        control = <FormControl>group.controls['info'];
        if (!control.value) {
            control.setValue(true);
            control.markAsDirty();
        }

        control = <FormControl>group.controls['linked'];
        if (!control.value) {
            control.setValue(true);
            control.markAsDirty();
        }
    }

    private get assignees():Assignee[] {
        if (this.boardData.getAndClearHasNewAssignees()) {
            //TODO look into using an Observable for this
            for (let assignee of this.boardData.assignees.array) {
                if (!this._assigneeFilterForm.controls[assignee.key]) {
                    this._assigneeFilterForm.addControl(assignee.key, new FormControl(false));
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
                    this._componentFilterForm.addControl(component.name, new FormControl(false));
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

    private hasParallelTasks():boolean {
        return this.boardData.parallelTasks.array.length > 0;
    }

    private get parallelTasks():ParallelTask[] {
        return this.boardData.parallelTasks.array;
    }

    private updateIssueDetail(detailForm:FormGroup, lastValue:any, value:any) {
        if (lastValue["description"] && !value["description"] && value["assignee"]) {
            //Description was deselected, but assignee is still set. Deselect assignee
            detailForm.controls["assignee"].setValue(false);
            //NB - this updates the value in dirty checker's lastValues, which is what we want
            value["assignee"] = false;
        } else if (!lastValue["assignee"] && value["assignee"] && !value["description"]) {
            //Assignee was selected, but description was not set. Select description
            detailForm.controls["description"].setValue(true);
            //NB - this updates the value in dirty checker's lastValues, which is what we want
            value["description"] = true;
        }
        this.boardData.updateIssueDetail(value.assignee, value.description, value.info, value.linked);
    }

    private updateFilters(project:any, priority:any, issueType:any, assignee:any, component:any,
                          customFieldValues:IMap<any>, parallelTaskFormValues:IMap<any>) {
        this.boardData.updateFilters(project, priority, issueType, assignee, component, customFieldValues, parallelTaskFormValues);
        this.updateTooltips();
    }

    private updateTooltips() {

        //When initialising the form, this will be false.
        let dirty = this.controlForm.dirty;

        let filters:BoardFilters = this.boardData.filters;
        if (!this._controlForm.dirty || this.isDirty("project")) {
            this.filterTooltips["project"] = this.createTooltipForFilter(filters.selectedProjectNames);

        }
        if (!dirty || this.isDirty('issue-type')) {
            this.filterTooltips["issue-type"] = this.createTooltipForFilter(filters.selectedIssueTypes);
        }
        if (!dirty || this.isDirty('priority')) {
            this.filterTooltips["priority"] = this.createTooltipForFilter(filters.selectedPriorityNames);
        }
        if (!dirty || this.isDirty('assignee')) {
            this.filterTooltips["assignee"] = this.createTooltipForFilter(filters.selectedAssignees);
        }
        if (!dirty || this.isDirty('component')) {
            this.filterTooltips["component"] = this.createTooltipForFilter(filters.selectedComponents);
        }

        for (let customFieldValues of this.customFields) {
            if (!dirty || this.isDirty("custom-fields", customFieldValues.name)) {
                let selected:IMap<string[]> = filters.selectedCustomFields;
                this.filterTooltips[customFieldValues.name] = this.createTooltipForFilter(selected[customFieldValues.name]);
            }
        }

        if (!dirty || this.isDirty("parallel-tasks")) {
            let parallelTasksTip:string = "";
            let selected:IMap<string[]> = filters.selectedParallelTasks;
            for (let paralellTask of this.parallelTasks) {
                let tip:string = this.createTooltipForFilter(selected[paralellTask.code]);
                if (tip) {
                    if (parallelTasksTip.length > 0) {
                        parallelTasksTip += "\n";
                    }
                    parallelTasksTip += paralellTask.code + ": " + tip;
                }
            }
            if (parallelTasksTip.length == 0) {
                parallelTasksTip = null;
            }
            this.filterTooltips["parallel-tasks"] = parallelTasksTip;
        }

        let filtersToolTip:string = "";
        let current:string = this.filterTooltips["project"];
        if (current) {
            filtersToolTip += "Projects:\n" + current;
        }

        current = this.filterTooltips["issue-type"];
        if (current) {
            if (filtersToolTip.length > 0) {
                filtersToolTip += "\n\n";
            }
            filtersToolTip += "Issue Types:\n" + current;
        }

        current = this.filterTooltips["priority"];
        if (current) {
            if (filtersToolTip.length > 0) {
                filtersToolTip += "\n\n";
            }
            filtersToolTip += "Priorities:\n" + current;
        }

        current = this.filterTooltips["assignee"];
        if (current) {
            if (filtersToolTip.length > 0) {
                filtersToolTip += "\n\n";
            }
            filtersToolTip += "Assignees:\n" + current;
        }

        current = this.filterTooltips["component"];
        if (current) {
            if (filtersToolTip.length > 0) {
                filtersToolTip += "\n\n";
            }
            filtersToolTip += "Components:\n" + current;
        }

        for (let customFieldValues of this.customFields) {
            current = this.filterTooltips[customFieldValues.name];
            if (current) {
                if (filtersToolTip.length > 0) {
                    filtersToolTip += "\n\n";
                }
                filtersToolTip += customFieldValues.name + ":\n" + current;
            }
        }

        current = this.filterTooltips["parallel-tasks"];
        if (current) {
            if (filtersToolTip.length > 0) {
                filtersToolTip += "\n\n";
            }
            filtersToolTip += "Parallel Tasks:\n" + current;
        }


        if (filtersToolTip.length > 0) {
            filtersToolTip += "\n\nClick 'Filters' to clear all filters";
        }
        
        this.filtersTooltip = filtersToolTip;
    }

    private createTooltipForParallelTaskFilter() {

    }

    private createTooltipForFilter(selectedNames:string[]):string {
        let tooltip:string = "";
        let first:boolean = true;
        for (let name of selectedNames) {
            if (first) {
                first = false;
            } else {
                tooltip += ", ";
            }
            tooltip += name;
        }
        return first ? null : tooltip;
    }

    private onClickClose(event:MouseEvent) {
        this.closeControlPanel.emit({});
        event.preventDefault();
    }

    private onSelectFiltersToDisplay(event:MouseEvent, filter:string) {
        event.preventDefault();
        this.filtersToDisplay = filter;
    }

    private grabInitialFormValues(form:FormGroup):any {
        let values:any = {};

        for (let key in form.controls) {
            let ac:AbstractControl = form.controls[key];
            if (ac instanceof FormControl) {
                values[key] = ac.value;
            } else {
                values[key] = this.grabInitialFormValues(<FormGroup>ac);
            }
        }

        return values;
    }


    private updateLinkUrl() {
        this.filtersUpdated.emit({});
    }



    private getFilterLinkClass(filterName:string) {
        let clazz:string = "filter";

        if (filterName === this.filtersToDisplay) {
            clazz += " selected";
        }
        if (this.filterTooltips[filterName]) {
            clazz += " hasFilters";
        }

        return clazz;
    }


    private get rightOffset() : string {
        //The offset if there is no health panel
        let offset:number = 75;
        if (this.boardData.blacklist) {
            //Make room for the health panel icon
            offset += 30;
        }
        return offset + "px";
    }

    private get customFields():CustomFieldValues[] {
        return this.boardData.customFields.array;
    }

    private hasCustomFields():boolean {
        return this.customFields.length > 0;
    }

    private get showSwimlane():boolean {
        return this.view == VIEW_KANBAN;
    }

    private isDirty(...keys:string[]):boolean{
        let formGroup:FormGroup = this._controlForm;
        for (let key of keys) {
            formGroup = <FormGroup>formGroup.controls[key];
            if (!formGroup) {
                return false;
            }
        }
        return formGroup.dirty;
    }
}


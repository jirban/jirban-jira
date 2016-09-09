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

@Component({
    selector: 'control-panel',
    inputs: ['view'],
    outputs: ['closeControlPanel'],
    templateUrl: './controlPanel.html',
    styleUrls: ['./controlPanel.css']
})
export class ControlPanelComponent {

    private _controlForm:FormGroup;
    private _assigneeFilterForm:FormGroup;
    private _componentFilterForm:FormGroup;

    private closeControlPanel:EventEmitter<any> = new EventEmitter();

    private none:string = NONE;

    private linkUrl:string;

    private filtersToDisplay:string = null;

    private filtersTooltip:string;
    private filterTooltips:IMap<string> = {};

    private view:string;

    constructor(private boardData:BoardData) {
    }

    private get controlForm():FormGroup {
        if (this._controlForm) {
            return this._controlForm;
        }

        let form:FormGroup = new FormGroup({});
        form.addControl("swimlane", new FormControl(this.boardData.swimlane));

        let detailForm:FormGroup = new FormGroup({});
        form.addControl("detail", detailForm);
        detailForm.addControl("assignee", new FormControl(this.boardData.issueDisplayDetails.assignee));
        detailForm.addControl("description", new FormControl(this.boardData.issueDisplayDetails.summary));
        detailForm.addControl("info", new FormControl(this.boardData.issueDisplayDetails.info));
        detailForm.addControl("linked", new FormControl(this.boardData.issueDisplayDetails.linkedIssues));

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
            for (let customFieldValues of this.customFields) {
                let customFieldForm:FormGroup = new FormGroup({});
                customFieldForm.addControl(NONE, new FormControl(filters.initialCustomFieldValueForForm(customFieldValues.name, NONE)))
                for (let customFieldValue of customFieldValues.values.array) {
                    customFieldForm.addControl(customFieldValue.key, new FormControl(filters.initialCustomFieldValueForForm(customFieldValues.name, customFieldValue.key)));
                }
                form.addControl(customFieldValues.name, customFieldForm);
            }
        }

        let dirtyChecker:DirtyChecker = DirtyChecker.create(form);

        form.valueChanges
            .debounceTime(150) //A delay for when people clear the filters so that we only get one event sent since filters are costly to recalculate
            .subscribe((value) => {
                let lastValues:any = dirtyChecker.lastValues;

                let dirty:IMap<boolean> = dirtyChecker.checkDirty(value);
                //Swimlane is necessary to update if dirty
                if (dirty["swimlane"]) {
                    this.updateSwimlane(value);
                }
                //Detail is cheap to update if dirty
                if (dirty["detail"]) {
                    this.updateIssueDetail(detailForm, lastValues["detail"], value["detail"]);
                }
                //Updating the filters is costly so do it all in one go
                let dirtyCustom:boolean = false;
                for (let customFieldValues of this.customFields) {
                    if (dirty[customFieldValues.name]) {
                        dirtyCustom = true;
                        break;
                    }
                }
                if (dirtyCustom || dirty["project"] || dirty["priority"] || dirty["issue-type"] || dirty["assignee"] || dirty["component"]) {
                    let customFieldFormValues:IMap<any> = {};
                    for (let customFieldValues of this.customFields) {
                        customFieldFormValues[customFieldValues.name] = value[customFieldValues.name];
                    }
                    this.updateFilters(dirty,
                        value["project"], value["priority"], value["issue-type"], value["assignee"], value["component"], customFieldFormValues);
                }
                this.updateLinkUrl();
            });

        this._assigneeFilterForm = assigneeFilterForm;
        this._componentFilterForm = componentFilterForm;
        this._controlForm = form;
        this.updateLinkUrl();
        this.updateTooltips();

        this.boardData.headers.stateVisibilitiesChangedObservable.subscribe((value:void) => {
            this.updateLinkUrl();
        });
        this.boardData.swimlaneVisibilityObservable.subscribe((value:void) => {
            this.updateLinkUrl();
        });

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
            this.clearFilter(event, customFieldValues.name);
        }
    }

    private clearFilter(event:MouseEvent, name:string) {
        event.preventDefault();
        let filter:FormGroup = <FormGroup>this._controlForm.controls[name];
        for (let key in filter.controls) {
            let control:FormControl = <FormControl>filter.controls[key];
            control.setValue(false);
        }
    }

    private clearSwimlane(event:MouseEvent) {
        event.preventDefault();
        let swimlane:FormControl = <FormControl>this._controlForm.controls['swimlane'];
        swimlane.setValue(null);
    }

    private clearDetail(event:MouseEvent) {
        event.preventDefault();
        let group:FormGroup = <FormGroup>this._controlForm.controls['detail'];

        let control:FormControl = <FormControl>group.controls['assignee'];
        control.setValue(true);

        control = <FormControl>group.controls['description'];
        control.setValue(true);

        control = <FormControl>group.controls['info'];
        control.setValue(true);

        control = <FormControl>group.controls['linked'];
        control.setValue(true);
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

    private updateFilters(dirty:IMap<boolean>, project:any, priority:any, issueType:any, assignee:any, component:any, customFieldValues:IMap<any>) {
        this.boardData.updateFilters(project, priority, issueType, assignee, component, customFieldValues);
        this.updateTooltips(dirty);
    }

    private updateTooltips(dirty?:IMap<boolean>) {
        let filters:BoardFilters = this.boardData.filters;
        if (!dirty || dirty["project"]) {
            this.filterTooltips["project"] = this.createTooltipForFilter(filters.selectedProjectNames);

        }
        if (!dirty || dirty['issue-type']) {
            this.filterTooltips["issue-type"] = this.createTooltipForFilter(filters.selectedIssueTypes);
        }
        if (!dirty || dirty['priority']) {
            this.filterTooltips["priority"] = this.createTooltipForFilter(filters.selectedPriorityNames);
        }
        if (!dirty || dirty['assignee']) {
            this.filterTooltips["assignee"] = this.createTooltipForFilter(filters.selectedAssignees);
        }
        if (!dirty || dirty['component']) {
            this.filterTooltips["component"] = this.createTooltipForFilter(filters.selectedComponents);
        }

        for (let customFieldValues of this.customFields) {
            if (!dirty || dirty[customFieldValues.name]) {
                let selected:IMap<string[]> = filters.selectedCustomFields;
                this.filterTooltips[customFieldValues.name] = this.createTooltipForFilter(selected[customFieldValues.name]);
            }
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

        if (filtersToolTip.length > 0) {
            filtersToolTip += "\n\nClick 'Filters' to clear all filters";
        }
        
        this.filtersTooltip = filtersToolTip;
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
        console.log("Update link url");
        let url:string = window.location.href;
        let index = url.lastIndexOf("?");
        if (index >= 0) {
            url = url.substr(0, index);
        }
        url = this.boardData.createQueryStringParticeles(url);
        if (this.view != VIEW_KANBAN) {
            url += "&view=" + this.view;
        }

        console.log(url);
        this.linkUrl = url;
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

    private get showSwimlane():boolean {
        return this.view == VIEW_KANBAN;
    }
}

/**
 * Hack to work around the current lack of ability to reset the dirty status of a ControlGroup (or any control for that matter)
 */
class DirtyChecker {

    constructor(private _lastValues:any) {
    }

    static create(form:FormGroup):DirtyChecker {
        let dirtyChecker:DirtyChecker = new DirtyChecker(DirtyChecker.initialize(form));
        return dirtyChecker;
    }

    private static initialize(form:FormGroup):any {
        let values:any = {};

        for (let key in form.controls) {
            let ac:AbstractControl = form.controls[key];
            if (ac instanceof FormControl) {
                values[key] = ac.value;
            } else {
                values[key] = DirtyChecker.initialize(<FormGroup>ac);
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

    get lastValues():any {
        return this._lastValues;
    }
}


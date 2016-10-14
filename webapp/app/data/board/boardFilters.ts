import {Assignee} from "./assignee";
import {Priority} from "./priority";
import {IssueData} from "./issueData";
import {Indexed} from "../../common/indexed";
import {IssueType} from "./issueType";
import {JiraComponent} from "./component";
import {IMap} from "../../common/map";
import {CustomFieldValues, CustomFieldValue} from "./customField";
import {BoardData} from "./boardData";
import {ParallelTask} from "./parallelTask";
import filter = require("core-js/fn/array/filter");

export const NONE:string = "$n$o$n$e$";

export class BoardFilters {
    private _projectFilter:any;
    private _priorityFilter:any;
    private _issueTypeFilter:any;
    private _assigneeFilter:any;
    private _componentFilter:any;
    private _componentFilterLength:number;
    private _customFieldValueFilters:IMap<any>;
    private _parallelTaskFilters:IMap<any>;
    private _projects:boolean = false;
    private _assignees:boolean = false;
    private _priorities:boolean = false;
    private _issueTypes:boolean = false;
    private _components:boolean = false;
    private _customFields:IMap<boolean> = {};
    private _parallelTasks:IMap<boolean> = {};
    private _selectedProjectNames:string[] = [];
    private _selectedPriorityNames:string[] = [];
    private _selectedIssueTypes:string[] = [];
    private _selectedAssignees:string[] = [];
    private _selectedComponents:string[] = [];
    private _selectedCustomFields:IMap<string[]>;
    private _selectedParallelTasks:IMap<string[]>;

    updateFilters(projectFilter:any, boardProjectCodes:string[],
                  priorityFilter:any, priorities:Indexed<Priority>,
                  issueTypeFilter:any, issueTypes:Indexed<IssueType>,
                  assigneeFilter:any, assignees:Indexed<Assignee>,
                  componentFilter:any, components:Indexed<JiraComponent>,
                  customFieldValueFilters:IMap<any>, customFields:Indexed<CustomFieldValues>,
                  parallelTaskFilters:IMap<any>, parallelTasks:Indexed<ParallelTask>) {

        this.setProjectFilter(projectFilter, boardProjectCodes);
        this.setPriorityFilter(priorityFilter, priorities);
        this.setIssueTypeFilter(issueTypeFilter, issueTypes);
        this.setAssigneeFilter(assigneeFilter, assignees);
        this.setComponentFilter(componentFilter, components);
        this.setCustomFieldValueFilters(customFieldValueFilters, customFields);
        this.setParallelTaskFilters(parallelTaskFilters, parallelTasks);
    }

    private setProjectFilter(filter:any, boardProjectCodes:string[]) {
        this._projectFilter = filter;
        this._projects = false;
        this._selectedProjectNames = [];
        if (boardProjectCodes) {
            for (let key of boardProjectCodes) {
                if (filter[key]) {
                    this._projects = true;
                    this._selectedProjectNames.push(key);
                }
            }
        }
    }

    private setPriorityFilter(filter:any, priorities:Indexed<Priority>) {
        this._priorityFilter = filter;
        this._priorities = false;
        this._selectedPriorityNames = [];
        if (priorities) {
            for (let priority of priorities.array) {
                if (filter[priority.name]) {
                    this._priorities = true;
                    this._selectedPriorityNames.push(priority.name)
                }
            }
        }
    }

    private setIssueTypeFilter(filter:any, issueTypes:Indexed<IssueType>) {
        this._issueTypeFilter = filter;
        this._issueTypes = false;
        this._selectedIssueTypes = [];
        if (issueTypes) {
            for (let issueType of issueTypes.array) {
                if (filter[issueType.name]) {
                    this._issueTypes = true;
                    this._selectedIssueTypes.push(issueType.name);
                }
            }
        }

    }

    private setAssigneeFilter(filter:any, assignees:Indexed<Assignee>) {
        this._assigneeFilter = filter;
        this._assignees = false;
        this._selectedAssignees = [];
        if (filter[NONE]) {
            this._assignees = true;
            this._selectedAssignees.push("None");
        }
        if (assignees) {
            for (let assignee of assignees.array) {
                if (filter[assignee.key]) {
                    this._assignees = true;
                    this._selectedAssignees.push(assignee.name);
                }
            }
        }
    }

    private setComponentFilter(filter:any, components:Indexed<JiraComponent>) {
        //Trim to only contain the visible ones in _componentFilter
        this._componentFilter = {};
        this._componentFilterLength = 0;
        this._components = false;
        this._selectedComponents = [];
        if (filter[NONE]) {
            this._components = true;
            this._componentFilter[NONE] = true;
            this._componentFilterLength = 1;
            this._selectedComponents.push("None");
        }

        if (components) {
            for (let component of components.array) {
                if (filter[component.name]) {
                    this._components = true;
                    this._componentFilter[component.name] = true;
                    this._componentFilterLength += 1;
                    this._selectedComponents.push(component.name);
                }
            }
        }
    }

    private setCustomFieldValueFilters(customFieldValueFilters:IMap<any>, customFields:Indexed<CustomFieldValues>) {
        this._customFieldValueFilters = {};
        this._customFields = {};
        this._selectedCustomFields = {};
        for (let cfvs of customFields.array) {
            let filter = customFieldValueFilters[cfvs.name];
            let hasFilters = false;
            let selected:string[] = [];
            if (!filter) {
                filter = {};
            } else {
                if (filter[NONE]) {
                    hasFilters = true;
                    selected.push("None")
                }
                for (let cfv of cfvs.values.array) {
                    if (filter[cfv.key]) {
                        hasFilters = true;
                        selected.push(cfv.displayValue);
                    }
                }
            }
            this._customFieldValueFilters[cfvs.name] = filter;
            this._customFields[cfvs.name] = hasFilters;
            this._selectedCustomFields[cfvs.name] = selected;

        }
    }

    private setParallelTaskFilters(parallelTaskFormFilters:IMap<any>, parallelTasks:Indexed<ParallelTask>) {
        if (!parallelTasks) {
            return;
        }
        this._parallelTaskFilters = {};
        this._parallelTasks = {};
        this._selectedParallelTasks = {};
        for (let pt of parallelTasks.array) {
            let filter = parallelTaskFormFilters[pt.code];
            let hasFilters = false;
            let selected:string[] = [];
            if (!filter) {
                filter = {};
            } else {
                for (let option of pt.options.array) {
                    if (filter[option]) {
                        hasFilters = true;
                        selected.push(option);
                    }
                }
            }
            this._parallelTaskFilters[pt.code] = filter;
            this._parallelTasks[pt.code] = hasFilters;
            this._selectedParallelTasks[pt.code] = selected;
        }
    }

    private hasCustomValueFilter(customFieldValueFilters:IMap<any>, name:string, key:string) {
        let filtersForField:any = customFieldValueFilters[name];
        if (!filtersForField) {
            return false;
        }
        return filtersForField[key];
    }


    filterIssue(issue:IssueData):boolean {
        if (this.filterProject(issue.projectCode)) {
            return true;
        }
        if (this.filterAssignee(issue.assignee ? issue.assignee.key : null)) {
            return true;
        }
        if (this.filterPriority(issue.priority.name)) {
            return true;
        }
        if (this.filterIssueType(issue.type.name)) {
            return true;
        }
        if (this.filterComponentAllComponents(issue.components)) {
            return true;
        }
        if (this.filterCustomFields(issue.customFields)) {
            return true;
        }
        if (this.filterParallelTasks(issue.parallelTaskOptions)) {
            return true;
        }

        return false;
    }

    initialProjectValueForForm(projectCode:string):boolean {
        if (!this._projects) {
            return false;
        }
        return this._projectFilter[projectCode];
    }

    filterProject(projectCode:string):boolean {
        if (this._projects) {
            return !this._projectFilter[projectCode];
        }
        return false;
    }

    initialAssigneeValueForForm(assigneeKey:string):boolean {
        if (!this._assignees) {
            return false;
        }
        return this._assigneeFilter[assigneeKey];
    }

    filterAssignee(assigneeKey:string):boolean {
        if (this._assignees) {
            return !this._assigneeFilter[assigneeKey ? assigneeKey : NONE]
        }
        return false;
    }

    initialPriorityValueForForm(priorityName:string):boolean {
        if (!this._priorities) {
            return false;
        }
        return this._priorityFilter[priorityName];
    }

    filterPriority(priorityName:string):boolean {
        if (this._priorities) {
            return !this._priorityFilter[priorityName];
        }
        return false;
    }

    initialIssueTypeValueForForm(issueTypeName:string):boolean {
        if (!this._issueTypes) {
            return false;
        }
        return this._issueTypeFilter[issueTypeName];
    }

    filterIssueType(issueTypeName:string):boolean {
        if (this._issueTypes) {
            return !this._issueTypeFilter[issueTypeName];
        }
        return false;
    }

    initialComponentValueForForm(componentKey:string):boolean {
        if (!this._components) {
            return false;
        }
        return this._componentFilter[componentKey];
    }

    initialCustomFieldValueForForm(customFieldName:string, customFieldKey:string):boolean {
        if (!this._customFields[customFieldName]) {
            return false;
        }
        let customField:any = this._customFieldValueFilters[customFieldName];
        if (!customField) {
            return false;
        }
        return customField[customFieldKey];
    }

    initialParallelTaskValueForForm(parallelTaskCode:string, optionName:string):boolean {
        if (!this._parallelTasks[parallelTaskCode]) {
            return false;
        }
        let paralellTask:any = this._parallelTaskFilters[parallelTaskCode];
        if (!paralellTask) {
            return false;
        }
        return paralellTask[optionName];
    }

    private filterComponentAllComponents(issueComponents:Indexed<JiraComponent>):boolean {
        if (this._components) {
            if (!issueComponents) {
                return !this._componentFilter[NONE];
            } else {
                if (this._componentFilterLength == 1 && this._componentFilter[NONE]) {
                    //All we want to match is no components, and we have some components so return that we
                    //should be filtered out
                    return true;
                }
                for (let component in this._componentFilter) {
                    if (component === NONE) {
                        //We have components and we are looking for some components, for this case ignore the
                        //no components filter
                        continue;
                    }
                    if (issueComponents.forKey(component)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    filterComponent(componentName:string):boolean {
        if (this._components) {
            return !this._componentFilter[componentName ? componentName : NONE]
        }
        return false;

    }

    filterCustomFields(customFields:IMap<CustomFieldValue>):boolean {
        for (let customFieldName in this._customFields) {
            if (this._customFields[customFieldName]) {
                let hadFilters:boolean = false;
                let match:boolean = false;
                let filter:any = this._customFieldValueFilters[customFieldName];
                for (let fieldName in filter) {
                    if (!filter[fieldName]) {
                        continue;
                    }
                    hadFilters = true;
                    if (fieldName === NONE) {
                        if (!customFields[customFieldName]){
                            match = true;
                            break;
                        }
                    } else {
                        let customFieldValue = customFields[customFieldName]
                        if (customFieldValue && filter[customFieldValue.key]) {
                            match = true;
                            break;
                        }
                    }
                }
                if (hadFilters && !match) {
                    return true;
                }
            }
        }
        return false;
    }

    filterCustomField(customFieldName:string, customFieldKey:string):boolean {
        if (this._customFields[customFieldName]) {
            let customField:any = this._customFieldValueFilters[customFieldName];
            if (customField) {
                return !customField[customFieldKey];
            }
        }
        return false;
    }

    filterParallelTasks(parallelTaskOptions:Indexed<string>) {
        for (let parallelTaskCode in this._parallelTasks) {
            if (this._parallelTasks[parallelTaskCode]) {
                if (!parallelTaskOptions) {
                    //Issue belongs to a project which does not have parallel task fields set up, so filter it since
                    //we have selected filtering on parallel task fields
                    return true;
                }
                let hadFilters:boolean = false;
                let match:boolean = false;
                let filter: any = this._parallelTaskFilters[parallelTaskCode];
                for (let option in filter) {
                    if (!filter[option]) {
                        continue;
                    }
                    hadFilters = true;
                    let selectedOption = parallelTaskOptions.forKey(parallelTaskCode);
                    if (selectedOption && filter[selectedOption]) {
                        match = true;
                        break;
                    }
                }
                if (hadFilters && !match) {
                    return true;
                }
            }
        }
        return false;
    }

    createFromQueryParams(boardData:BoardData, queryParams:IMap<string>,
                                 callback:(
                                     projectFilter:any,
                                     priorityFilter:any,
                                     issueTypeFilter:any,
                                     assigneeFilter:any,
                                     componentFilter:any,
                                     customFieldFilters:IMap<any>,
                                     parallelTaskFilters:IMap<any>
                                 )=>void):void {
        let projectFilter:any = this.parseBooleanFilter(queryParams, "project");
        let priorityFilter:any = this.parseBooleanFilter(queryParams, "priority");
        let issueTypeFilter:any = this.parseBooleanFilter(queryParams, "issue-type");
        let assigneeFilter:any = this.parseBooleanFilter(queryParams, "assignee");
        let componentFilter:any = this.parseBooleanFilter(queryParams, "component");

        let customFieldFilters:IMap<any> = {};
        for (let customFieldValues of boardData.customFields.array) {
            let customFilter:any = this.parseBooleanFilter(queryParams, "cf." + customFieldValues.name);
            customFieldFilters[customFieldValues.name] = customFilter;
        }
        let parallelTaskFilters:IMap<any> = {};
        for (let parallelTask of boardData.parallelTasks.array) {
            let parallelTaskFilter: any = this.parseBooleanFilter(queryParams, "pt." + parallelTask.code);
            parallelTaskFilters[parallelTask.code] = parallelTaskFilter;
        }

        callback(projectFilter, priorityFilter, issueTypeFilter, assigneeFilter, componentFilter, customFieldFilters, parallelTaskFilters);
    }

    parseBooleanFilter(queryParams:IMap<string>, name:string):any{
        let valueString:string = queryParams[name];
        if (valueString) {
            let jsonFilter:any = {};
            let values:string[] = valueString.split(",");
            for (let value of values) {
                value = decodeURIComponent(value);
                jsonFilter[value] = true;
            }
            return jsonFilter;
        }
        return {};
    }

    createQueryStringParticles() {
        let query = "";
        query += this.createQueryStringParticle("project", this._projects, this._projectFilter);
        query += this.createQueryStringParticle("priority", this._priorities, this._priorityFilter);
        query += this.createQueryStringParticle("issue-type", this._issueTypes, this._issueTypeFilter);
        query += this.createQueryStringParticle("assignee", this._assignees, this._assigneeFilter);
        query += this.createQueryStringParticle("component", this._components, this._componentFilter);
        for (let key in this._customFieldValueFilters) {
            let customField:boolean = this._customFields[key];
            let customFieldFilter:any = this._customFieldValueFilters[key];
            query += this.createQueryStringParticle("cf." + key, customField, customFieldFilter);
        }
        for (let key in this._parallelTaskFilters) {
            let parallelTask:boolean = this._parallelTasks[key];
            let parallelTaskFilter:any = this._parallelTaskFilters[key];
            query += this.createQueryStringParticle("pt." + key, parallelTask, parallelTaskFilter);
        }
        return query;
    }

    private createQueryStringParticle(name:string, hasFilter:boolean, filter:any) {
        let query:string = "";
        if (hasFilter) {
            let initialised:boolean = false;
            for (let key in filter) {
                if (filter[key]) {
                    if (!initialised) {
                        initialised = true;
                        query = "&" + name + "="
                    } else {
                        query += ","
                    }
                    query += encodeURIComponent(key);
                }
            }
        }
        return query;
    }

    get selectedProjectNames():string[] {
        return this._selectedProjectNames;
    }

    get selectedPriorityNames():string[] {
        return this._selectedPriorityNames;
    }

    get selectedIssueTypes():string[] {
        return this._selectedIssueTypes;
    }

    get selectedAssignees():string[] {
        return this._selectedAssignees;
    }

    get selectedComponents():string[] {
        return this._selectedComponents;
    }

    get selectedCustomFields():IMap<string[]> {
        return this._selectedCustomFields;
    }

    get selectedParallelTasks(): IMap<string[]> {
        return this._selectedParallelTasks;
    }
}

/**
 * The details to show for the issues
 */
export class IssueDisplayDetails {
    private _assignee:boolean = true;
    private _summary:boolean = true;
    private _info:boolean = true;
    private _linkedIssues:boolean = true;

    constructor(assignee:boolean = true, summary:boolean = true, info:boolean = true, linkedIssues:boolean = true) {
        this._assignee = assignee;
        this._summary = summary;
        this._info = info;
        this._linkedIssues = linkedIssues;
    }

    get assignee():boolean {
        return this._assignee;
    }

    get summary():boolean {
        return this._summary;
    }

    get info():boolean {
        return this._info;
    }

    get linkedIssues():boolean {
        return this._linkedIssues;
    }

    createQueryStringParticle() {
        let query = "";
        if (!this._assignee || !this._summary || !this._info || !this._linkedIssues) {
            let first:boolean = true;
            query = "&detail=";
            if (!this._assignee) {
                first = false;
                query += "assignee";
            }
            if (!this._summary) {
                if (!first) {
                    query += ",";
                } else {
                    first = false;
                }
                query += "description";
            }
            if (!this._info) {
                if (!first) {
                    query += ",";
                } else {
                    first = false;
                }
                query += "info";
            }
            if (!this._linkedIssues) {
                if (!first) {
                    query += ",";
                }
                query += "linked"
            }
        }
        return query;
    }

}
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
    private _projectFilter:SimpleFilter;
    private _priorityFilter:SimpleFilter;
    private _issueTypeFilter:SimpleFilter;
    private _assigneeFilter:SimpleFilter;
    private _componentFilter:ComponentFilter;
    private _customFieldValueFilters:MapFilter;
    private _parallelTaskFilters:MapFilter;

    updateFilters(projectFilter:any, boardProjectCodes:string[],
                  priorityFilter:any, priorities:Indexed<Priority>,
                  issueTypeFilter:any, issueTypes:Indexed<IssueType>,
                  assigneeFilter:any, assignees:Indexed<Assignee>,
                  componentFilter:any, components:Indexed<JiraComponent>,
                  customFieldValueFilters:IMap<any>, customFields:Indexed<CustomFieldValues>,
                  parallelTaskFilters:IMap<any>, parallelTasks:Indexed<ParallelTask>) {

        this._projectFilter = SimpleFilter.create(
            projectFilter,
            boardProjectCodes, {
                getKey : (value:string) => {return value;},
                getDisplayValue : (value:string) => {return value;}});

        this._priorityFilter = SimpleFilter.create(
            priorityFilter,
            this.getIndexedArray(priorities), {
                getKey : (value:Priority) => {return value.name;},
                getDisplayValue : (value:Priority) => {return value.name;}});

        this._issueTypeFilter = SimpleFilter.create(
            issueTypeFilter,
            this.getIndexedArray(issueTypes), {
                getKey : (value:IssueType) => {return value.name;},
                getDisplayValue : (value:IssueType) => {return value.name;}});

        this._assigneeFilter = SimpleFilter.create(
            assigneeFilter,
            this.getIndexedArray(assignees),{
                getKey : (value:Assignee) => {return value.key;},
                getDisplayValue : (value:Assignee) => {return value.name;}},
            true);

        this._componentFilter = ComponentFilter.create(componentFilter, this.getIndexedArray(components), {
                getKey : (value:JiraComponent) => {return value.name;},
                getDisplayValue : (value:JiraComponent) => {return value.name;}});

        this._customFieldValueFilters = MapFilter.create(customFieldValueFilters, customFields, {
                getKey : (parent:CustomFieldValues) => {return parent.name},
                getValues : (parent:CustomFieldValues) => {return parent.values.array}
            }, {
                getKey : (value:CustomFieldValue) => {return value.key},
                getDisplayValue : (value:CustomFieldValue) => {return value.displayValue}
            }, true);

        this._parallelTaskFilters = MapFilter.create(parallelTaskFilters, parallelTasks, {
            getKey : (parent:ParallelTask) => {return parent.code},
            getValues : (parent:ParallelTask) => {return parent.options.array}
        }, {
            getKey : (value:string) => {return value},
            getDisplayValue : (value:string) => {return value}
        });
    }

    private getIndexedArray<T>(indexed:Indexed<T>):T[] {
        if (indexed) {
            return indexed.array;
        }
        return null;
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
        return this._projectFilter.initialValueForForm(projectCode);
    }

    filterProject(projectCode:string):boolean {
        return this.filterSimple(this._projectFilter, projectCode);
    }

    initialAssigneeValueForForm(assigneeKey:string):boolean {
        return this._assigneeFilter.initialValueForForm(assigneeKey);
    }

    filterAssignee(assigneeKey:string):boolean {
        return this.filterSimple(this._assigneeFilter, assigneeKey);
    }

    initialPriorityValueForForm(priorityName:string):boolean {
        return this._priorityFilter.initialValueForForm(priorityName);
    }

    filterPriority(priorityName:string):boolean {
        return this.filterSimple(this._priorityFilter, priorityName);
    }

    initialIssueTypeValueForForm(issueTypeName:string):boolean {
        return this._issueTypeFilter.initialValueForForm(issueTypeName);
    }

    filterIssueType(issueTypeName:string):boolean {
        return this.filterSimple(this._issueTypeFilter, issueTypeName);
    }

    initialComponentValueForForm(componentKey:string):boolean {
        return this._componentFilter.initialValueForForm(componentKey);
    }

    initialCustomFieldValueForForm(customFieldName:string, customFieldKey:string):boolean {
        return this.initialMapValueForForm(this._customFieldValueFilters, customFieldName, customFieldKey);
    }

    initialParallelTaskValueForForm(parallelTaskCode:string, optionName:string):boolean {
        return this.initialMapValueForForm(this._parallelTaskFilters, parallelTaskCode, optionName);
    }

    private initialMapValueForForm(filter:MapFilter, filterName:string, key:string) {
        if (!filter) {
            return false;
        }
        return filter.initialValueForForm(filterName, key);
    }

    private filterComponentAllComponents(issueComponents:Indexed<JiraComponent>):boolean {
        if (!this._componentFilter) {
            return false;
        }
        return this._componentFilter.filterAll(issueComponents);
    }

    filterComponent(componentName:string):boolean {
        if (!this._componentFilter) {
            return false;
        }
        return this._componentFilter.doFilter(componentName);
    }

    filterCustomFields(customFields:IMap<CustomFieldValue>):boolean {
        if (!this._customFieldValueFilters) {
            return false;
        }
        return this._customFieldValueFilters.filterAll(customFields, {
            getKey : (value:CustomFieldValue) => {return value == null ? null : value.key}});
    }

    filterCustomField(customFieldName:string, customFieldKey:string):boolean {
        if (!this._customFieldValueFilters) {
            return false;
        }
        return this._customFieldValueFilters.filterSingle(customFieldName, customFieldKey);
    }

    filterParallelTasks(parallelTaskOptions:Indexed<string>) {
        if (!this._parallelTaskFilters) {
            return false;
        }
        let optionsMap:IMap<string>;
        if (parallelTaskOptions) {
            optionsMap = {};
            for (let parallelTaskCode in parallelTaskOptions.indices) {
                optionsMap[parallelTaskCode] = parallelTaskOptions.forKey(parallelTaskCode);
            }
        }

        return this._parallelTaskFilters.filterAll(optionsMap, {
            getKey : (value:string) => {return value}});
    }

    private filterSimple(filter:SimpleFilter, key:string):boolean {
        if (!filter) {
            return false;
        }
        return filter.doFilter(key);
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

        query += this.createQueryStringParticle("project", this._projectFilter);
        query += this.createQueryStringParticle("priority", this._priorityFilter);
        query += this.createQueryStringParticle("issue-type", this._issueTypeFilter);
        query += this.createQueryStringParticle("assignee", this._assigneeFilter);
        query += this.createQueryStringParticle("component", this._componentFilter);
        for (let key in this._customFieldValueFilters.filters) {
            let simpleFilter:SimpleFilter = this._customFieldValueFilters.filters[key];
            query += this.createQueryStringParticle("cf." + key, simpleFilter);
        }
        for (let key in this._parallelTaskFilters.filters) {
            let simpleFilter:SimpleFilter = this._parallelTaskFilters.filters[key];
            query += this.createQueryStringParticle("pt." + key, simpleFilter);
        }
        return query;
    }

    private createQueryStringParticle(name:string, simpleFilter:SimpleFilter) {
        let query:string = "";
        if (simpleFilter.anySelected) {
            let initialised:boolean = false;
            for (let key in simpleFilter.filter) {
                if (simpleFilter.filter[key]) {
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
        return this._projectFilter.selectedValues;
    }

    get selectedPriorityNames():string[] {
        return this._priorityFilter.selectedValues;
    }

    get selectedIssueTypes():string[] {
        return this._issueTypeFilter.selectedValues;
    }

    get selectedAssignees():string[] {
        return this._assigneeFilter.selectedValues;
    }

    get selectedComponents():string[] {
        return this._componentFilter.selectedValues;
    }

    get selectedCustomFields():IMap<string[]> {
        return this._customFieldValueFilters.selectedValues;
    }

    get selectedParallelTasks(): IMap<string[]> {
        return this._parallelTaskFilters.selectedValues;
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

class SimpleFilter {
    private _handleNone:boolean;
    protected _filter:any;
    protected _anySelected:boolean = false;
    protected _selectedValues:string[] = [];

    constructor(handleNone:boolean, filter:any, anySelected:boolean, selectedValues:string[]) {
        this._handleNone = handleNone;
        this._filter = filter;
        this._anySelected = anySelected;
        this._selectedValues = selectedValues;
    }

    get handleNone(): boolean {
        return this._handleNone;
    }

    get filter(): any {
        return this._filter;
    }

    get anySelected(): boolean {
        return this._anySelected;
    }

    get selectedValues(): string[] {
        return this._selectedValues;
    }

    initialValueForForm(key:string):boolean {
        if (!this._anySelected) {
            return false;
        }
        return this._filter[key];
    }

    doFilter(key:string):boolean {
        if (this._anySelected) {
            let useKey:string = key;
            if (!key && this._handleNone) {
                useKey = NONE;
            }
            return !this._filter[useKey];
        }
        return false;
    }


    static create<T>(filter:any, values:T[], valueAccessor:BoardValueAccessor<T>, handleNone:boolean = false):SimpleFilter {
        let anySelected = false;
        let selectedValues:string[] = [];
        if (handleNone) {
            if (filter[NONE]) {
                anySelected = true;
                selectedValues.push("None");
            }
        }
        if (values) {
            for (let value of values) {
                let key:string = valueAccessor.getKey(value);
                if (filter[key]) {
                    anySelected = true;
                    let displayValue:string = valueAccessor.getDisplayValue(value);
                    selectedValues.push(displayValue);
                }
            }
        }
        return new SimpleFilter(handleNone, filter, anySelected, selectedValues);
    }
}

class ComponentFilter extends SimpleFilter {
    private _filterLength: number;

    constructor(handleNone:boolean, filter:any, anySelected:boolean, selectedValues:string[]) {
        super(handleNone, filter, anySelected, selectedValues);
        this._filterLength = selectedValues.length;
    }

    filterAll(issueComponents:Indexed<JiraComponent>):boolean {
        if (this._anySelected) {
            if (!issueComponents) {
                return !this._filter[NONE];
            } else {
                if (this._filterLength == 1 && this._filter[NONE]) {
                    //All we want to match is no components, and we have some components so return that we
                    //should be filtered out
                    return true;
                }
                for (let component in this._filter) {
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

    static create<T>(filter: any, values: JiraComponent[], valueAccessor: BoardValueAccessor<JiraComponent>): ComponentFilter {
        let tmp: SimpleFilter = SimpleFilter.create(filter, values, valueAccessor, true);
        return new ComponentFilter(tmp.handleNone, tmp.filter, tmp.anySelected, tmp.selectedValues);
    }
}

class MapFilter {
    private _filters:IMap<SimpleFilter>;
    private _selectedValues:IMap<string[]>;

    constructor(filters: IMap<SimpleFilter>) {
        this._filters = filters;
        this._selectedValues = {};
        for (let key in filters) {
            let simpleFilter:SimpleFilter = filters[key];
            this._selectedValues[key] = simpleFilter.selectedValues;
        }
    }

    get filters() {
        return this._filters;
    }
    get selectedValues(): IMap<string[]> {
        return this._selectedValues;
    }

    initialValueForForm(filterName:string, key:string):boolean {
        let filter:SimpleFilter = this._filters[filterName];
        if (filter) {
            return filter.initialValueForForm(key);
        }
        return false;
    }

    filterSingle(filterName:string, key:string):boolean {
        let filter:SimpleFilter = this._filters[filterName];
        if (filter) {
            return filter.doFilter(key);
        }
    }

    filterAll<T>(values:IMap<T>, valueAccessor:IssueMapValueAccessor<T>):boolean {
        for (let name in this._filters) {
            let simpleFilter:SimpleFilter = this._filters[name];
            let key:string;
            if (values) {
                key = valueAccessor.getKey(values[name]);
            }
            if (!key) {
                if (simpleFilter.doFilter(null)) {
                    return true;
                }
            }
            if (simpleFilter.doFilter(key)) {
                return true;
            }
        }
        return false;
    }

    static create<T, V>(filters:IMap<any>, values:Indexed<T>,
                     mapAccessor:MapValueAccessor<T, V>, valueAccessor:BoardValueAccessor<V>, handleNone:boolean = false) : MapFilter {
        if (!values) {
            return;
        }
        let simpleFilters:IMap<SimpleFilter> = {};
        for (let value of values.array) {
            let name:string = mapAccessor.getKey(value);
            let filter:any = filters[name];
            if (!filter) {
                filter = {};
            }
            let childValues:V[] = mapAccessor.getValues(value);
            simpleFilters[name] = SimpleFilter.create(filter, childValues, valueAccessor, handleNone);
        }
        return new MapFilter(simpleFilters);
    }
}

interface MapValueAccessor<T, V> {
    getKey(parent:T):string;
    getValues(parent:T):V[];
}

interface BoardValueAccessor<T> {
    getKey(value:T):string;
    getDisplayValue(value:T):string;
}

interface IssueMapValueAccessor<T> {
    getKey(value:T):string;
}

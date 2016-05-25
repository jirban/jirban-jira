import {Assignee, NO_ASSIGNEE} from "./assignee";
import {Priority} from "./priority";
import {IssueData} from "./issueData";
import {Indexed} from "../../common/indexed";
import {IssueType} from "./issueType";
import {JiraComponent, NO_COMPONENT} from "./component";
import {IMap} from "../../common/map";

export class BoardFilters {
    private _projectFilter:any;
    private _priorityFilter:any;
    private _issueTypeFilter:any;
    private _assigneeFilter:any;
    private _componentFilter:any;
    private _componentFilterLength:number;
    private _projects:boolean = false;
    private _assignees:boolean = false;
    private _priorities:boolean = false;
    private _issueTypes:boolean = false;
    private _components:boolean = false;

    setProjectFilter(filter:any, boardProjectCodes:string[]) {
        this._projectFilter = filter;
        this._projects = false;
        if (boardProjectCodes) {
            for (let key of boardProjectCodes) {
                if (filter[key]) {
                    this._projects = true;
                    break;
                }
            }
        }
    }

    setPriorityFilter(filter:any, priorities:Indexed<Priority>) {
        this._priorityFilter = filter;
        this._priorities = false;
        if (priorities) {
            for (let priority of priorities.array) {
                if (filter[priority.name]) {
                    this._priorities = true;
                    break;
                }
            }
        }
    }

    setIssueTypeFilter(filter:any, issueTypes:Indexed<IssueType>) {
        this._issueTypeFilter = filter;
        this._issueTypes = false;
        if (issueTypes) {
            for (let issueType of issueTypes.array) {
                if (filter[issueType.name]) {
                    this._issueTypes = true;
                    break;
                }
            }
        }

    }

    setAssigneeFilter(filter:any, assignees:Indexed<Assignee>) {
        this._assigneeFilter = filter;
        this._assignees = false;
        if (filter[NO_ASSIGNEE]) {
            this._assignees = true;
        } else if (assignees) {
            for (let assignee of assignees.array) {
                if (filter[assignee.key]) {
                    this._assignees = true;
                    break;
                }
            }
        }
    }

    setComponentFilter(filter:any, components:Indexed<JiraComponent>) {
        //Trim to only contain the visible ones in _componentFilter
        this._componentFilter = {};
        this._componentFilterLength = 0;
        this._components = false;
        if (filter[NO_COMPONENT]) {
            this._components = true;
            this._componentFilter[NO_COMPONENT] = true;
            this._componentFilterLength = 1;
        }

        if (components) {
            for (let component of components.array) {
                if (filter[component.name]) {
                    this._components = true;
                    this._componentFilter[component.name] = true;
                    this._componentFilterLength += 1;
                }
            }
        }

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
            return !this._assigneeFilter[assigneeKey ? assigneeKey : NO_ASSIGNEE]
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

    private filterComponentAllComponents(issueComponents:Indexed<JiraComponent>):boolean {
        if (this._components) {
            if (!issueComponents) {
                return !this._componentFilter[NO_COMPONENT];
            } else {
                if (this._componentFilterLength == 1 && this._componentFilter[NO_COMPONENT]) {
                    //All we want to match is no components, and we have some components so return that we
                    //should be filtered out
                    return true;
                }
                for (let component in this._componentFilter) {
                    if (component === NO_COMPONENT) {
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
            return !this._componentFilter[componentName ? componentName : NO_COMPONENT]
        }
        return false;

    }

    createFromQueryParams(queryParams:IMap<string>,
                                 callback:(
                                     projectFilter:string,
                                     priorityFilter:string,
                                     issueTypeFilter:string,
                                     assigneeFilter:string,
                                     componentFilter:string)=>void):void {
        let projectFilter:any = this.parseBooleanFilter(queryParams, "project");
        let priorityFilter:any = this.parseBooleanFilter(queryParams, "priority");
        let issueTypeFilter:any = this.parseBooleanFilter(queryParams, "issue-type");
        let assigneeFilter:any = this.parseBooleanFilter(queryParams, "assignee");
        let componentFilter:any = this.parseBooleanFilter(queryParams, "component");

        callback(projectFilter, priorityFilter, issueTypeFilter, assigneeFilter, componentFilter);
    }

    parseBooleanFilter(queryParams:IMap<string>, name:string):any{
        let valueString:string = queryParams[name];
        if (valueString) {
            let jsonFilter:any = {};
            let values:string[] = valueString.split(",");
            for (let value of values) {
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
                    query += key;
                }
            }
        }
        return query;
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
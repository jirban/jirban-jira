import {Assignee, NO_ASSIGNEE} from "./assignee";
import {Priority} from "./priority";
import {IssueData} from "./issueData"
import {Indexed} from "../../common/indexed";
import {IssueType} from "./issueType";
import {JiraComponent} from "./component";
import {NO_COMPONENT} from "./component";
import {IMap} from "../../common/map";

export class BoardFilters {
    private _projectFilter:any;
    private _priorityFilter:any;
    private _issueTypeFilter:any;
    private _assigneeFilter:any;
    private _componentFilter:any;
    private _componentFilterLength:number;
    private projects:boolean = false;
    private assignees:boolean = false;
    private priorities:boolean = false;
    private issueTypes:boolean = false;
    private components:boolean = false;

    setProjectFilter(filter:any, boardProjectCodes:string[]) {
        this._projectFilter = filter;
        this.projects = false;
        if (boardProjectCodes) {
            for (let key of boardProjectCodes) {
                if (filter[key]) {
                    this.projects = true;
                    break;
                }
            }
        }

    }

    setPriorityFilter(filter:any, priorities:Indexed<Priority>) {
        this._priorityFilter = filter;
        this.priorities = false;
        if (priorities) {
            for (let priority of priorities.array) {
                if (filter[priority.name]) {
                    this.priorities = true;
                    break;
                }
            }
        }
    }

    setIssueTypeFilter(filter:any, issueTypes:Indexed<IssueType>) {
        this._issueTypeFilter = filter;
        this.issueTypes = false;
        if (issueTypes) {
            for (let issueType of issueTypes.array) {
                if (filter[issueType.name]) {
                    this.issueTypes = true;
                    break;
                }
            }
        }

    }

    setAssigneeFilter(filter:any, assignees:Indexed<Assignee>) {
        this._assigneeFilter = filter;
        this.assignees = false;
        if (filter[NO_ASSIGNEE]) {
            this.assignees = true;
        } else if (assignees) {
            for (let assignee of assignees.array) {
                if (filter[assignee.key]) {
                    this.assignees = true;
                    break;
                }
            }
        }
    }

    setComponentFilter(filter:any, components:Indexed<JiraComponent>) {
        //Trim to only contain the visible ones in _componentFilter
        this._componentFilter = {};
        this._componentFilterLength = 0;
        this.components = false;
        if (filter[NO_COMPONENT]) {
            this.components = true;
            this._componentFilter[NO_COMPONENT] = true;
            this._componentFilterLength = 1;
        }

        if (components) {
            for (let component of components.array) {
                if (filter[component.name]) {
                    this.components = true;
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

    filterProject(projectCode:string):boolean {
        if (this.projects) {
            return !this._projectFilter[projectCode];
        }
        return false;
    }

    filterAssignee(assigneeKey:string):boolean {
        if (this.assignees) {
            return !this._assigneeFilter[assigneeKey ? assigneeKey : NO_ASSIGNEE]
        }
        return false;
    }

    filterPriority(priorityName:string):boolean {
        if (this.priorities) {
            return !this._priorityFilter[priorityName];
        }
        return false;
    }

    filterIssueType(issueTypeName:string):boolean {
        if (this.issueTypes) {
            return !this._issueTypeFilter[issueTypeName];
        }
        return false;
    }

    private filterComponentAllComponents(issueComponents:Indexed<JiraComponent>):boolean {
        if (this.components) {
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
        if (this.components) {
            return !this._componentFilter[componentName ? componentName : NO_COMPONENT]
        }
        return false;

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
}
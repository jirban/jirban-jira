import {Assignee, NO_ASSIGNEE} from "./assignee";
import {Priority} from "./priority";
import {IssueData} from "./issueData"
import {Indexed} from "../../common/indexed";
import {IssueType} from "./issueType";

export class BoardFilters {
    private _projectFilter:any;
    private _priorityFilter:any;
    private _issueTypeFilter:any;
    private _assigneeFilter:any;
    private projects:boolean = false;
    private assignees:boolean = false;
    private priorities:boolean = false;
    private issueTypes:boolean = false;

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
        return false;
    }

    filterProject(projectCode:string):boolean {
        if (this.projects) {
            return !this._projectFilter[projectCode];
        }
        return false;
    }

    filterAssignee(assigneeKey:string) {
        if (this.assignees) {
            return !this._assigneeFilter[assigneeKey ? assigneeKey : NO_ASSIGNEE]
        }
        return false;
    }

    filterPriority(priorityName:string) {
        if (this.priorities) {
            return !this._priorityFilter[priorityName];
        }
        return false;
    }

    filterIssueType(issueTypeName:string) {
        if (this.issueTypes) {
            return !this._issueTypeFilter[issueTypeName];
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
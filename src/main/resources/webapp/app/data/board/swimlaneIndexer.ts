import {BoardData} from "./boardData";
import {BoardFilters} from "./boardFilters";
import {IssueData} from "./issueData";
import {IMap} from "../../common/map";
import {SwimlaneData} from "./issueTable";
import {Assignee} from "./assignee";

export interface SwimlaneIndexer {
    swimlaneTable : SwimlaneData[];
    swimlaneIndex(issue:IssueData):number;
    filter(swimlaneData:SwimlaneData):boolean;
    matchIssues(targetIssue:IssueData, issue:IssueData):boolean;
}

export class SwimlaneMatcher {
    constructor(
        private _targetIssue:IssueData,
        private _indexer:SwimlaneIndexer){
    }

    matchesSwimlane(issue:IssueData):boolean{
        return this._indexer.matchIssues(this._targetIssue, issue);
    }
}

export class SwimlaneIndexerFactory {
    createSwimlaneIndexer(swimlane:string, filters:BoardFilters, boardData:BoardData):SwimlaneIndexer {
        return this._createIndexer(swimlane, filters, boardData, true);
    }

    createSwimlaneMatcher(swimlane:string, targetIssue:IssueData):SwimlaneMatcher {
        let indexer:SwimlaneIndexer = this._createIndexer(swimlane, null, null, false);
        if (indexer == null) {
            return null;
        }
        return new SwimlaneMatcher(targetIssue, indexer);
    }

    private _createIndexer(swimlane:string, filters:BoardFilters, boardData:BoardData, initTable:boolean):SwimlaneIndexer {
        if (swimlane === "project") {
            return new ProjectSwimlaneIndexer(filters, boardData, initTable);
        } else if (swimlane === "priority") {
            return new PrioritySwimlaneIndexer(filters, boardData, initTable);
        } else if (swimlane === "issue-type") {
            return new IssueTypeSwimlaneIndexer(filters, boardData, initTable);
        } else if (swimlane === "assignee") {
            return new AssigneeSwimlaneIndexer(filters, boardData, initTable);
        }
        return null;
    }
}

abstract class BaseIndexer {
    protected _swimlaneTable:SwimlaneData[];
    constructor(
        protected _filters:BoardFilters,
        protected _boardData:BoardData) {
    }

    get swimlaneTable():SwimlaneData[] {
        return this._swimlaneTable;
    }
}

class ProjectSwimlaneIndexer extends BaseIndexer implements SwimlaneIndexer {
    private _indices:IMap<number> = {};

    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean) {
        super(filters, boardData);
        if (initTable) {
            let i:number = 0;
            for (let name of boardData.boardProjectCodes) {
                this._indices[name] = i;
                i++;
            }

            this._swimlaneTable = createTable(boardData, boardData.boardProjectCodes);
        }
    }

    swimlaneIndex(issue:IssueData):number {
        return this._indices[issue.projectCode];
    }

    filter(swimlaneData:SwimlaneData):boolean {
        return this._filters.filterProject(swimlaneData.name);
    }

    matchIssues(targetIssue:IssueData, issue:IssueData) : boolean {
        return targetIssue.projectCode === issue.projectCode;
    }
}

class PrioritySwimlaneIndexer extends BaseIndexer implements SwimlaneIndexer {
    private _swimlaneNames:string[] = [];

    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean) {
        super(filters, boardData);
        if (initTable) {
            let i:number = 0;
            for (let priority of boardData.priorities.array) {
                this._swimlaneNames.push(priority.name)
                i++;
            }

            this._swimlaneTable = createTable(boardData, this._swimlaneNames);
        }
    }

    swimlaneIndex(issue:IssueData):number {
        return this._boardData.priorities.indices[issue.priorityName];
    }

    filter(swimlaneData:SwimlaneData):boolean {
        return this._filters.filterPriority(swimlaneData.name);
    }

    matchIssues(targetIssue:IssueData, issue:IssueData) : boolean {
        return targetIssue.priority.name === issue.priority.name;
    }
}

class IssueTypeSwimlaneIndexer extends BaseIndexer implements SwimlaneIndexer {
    private _swimlaneNames:string[] = [];

    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean) {
        super(filters, boardData);
        if (initTable) {
            let i:number = 0;
            for (let issueType of boardData.issueTypes.array) {
                this._swimlaneNames.push(issueType.name)
                i++;
            }

            this._swimlaneTable = createTable(boardData, this._swimlaneNames);
        }
    }

    get swimlaneTable():SwimlaneData[] {
        return this._swimlaneTable;
    }

    swimlaneIndex(issue:IssueData):number {
        return this._boardData.issueTypes.indices[issue.typeName];
    }

    filter(swimlaneData:SwimlaneData):boolean {
        return this._filters.filterIssueType(swimlaneData.name);
    }

    matchIssues(targetIssue:IssueData, issue:IssueData) : boolean {
        return targetIssue.type.name === issue.type.name;
    }
}

class AssigneeSwimlaneIndexer extends BaseIndexer implements SwimlaneIndexer {
    private _swimlaneNames:string[] = [];

    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean) {
        super(filters, boardData);
        if (initTable) {
            let i:number = 0;
            for (let assignee of this._boardData.assignees.array) {
                this._swimlaneNames.push(assignee.name);
            }
            //Add an additional entry for the no assignee case
            this._swimlaneNames.push("None");

            this._swimlaneTable = createTable(boardData, this._swimlaneNames);
        }
    }

    get swimlaneTable():SwimlaneData[] {
        return this._swimlaneTable;
    }

    swimlaneIndex(issue:IssueData):number {
        if (!issue.assignee) {
            return this._swimlaneNames.length - 1;
        }
        return this._boardData.assignees.indices[issue.assignee.key];
    }

    filter(swimlaneData:SwimlaneData):boolean {
        let assigneeKey:string = null;
        if (swimlaneData.index < this._swimlaneNames.length - 1) {
            assigneeKey = this._boardData.assignees.forIndex(swimlaneData.index).key;
        }
        return this._filters.filterAssignee(assigneeKey);
    }

    matchIssues(targetIssue:IssueData, issue:IssueData):boolean {
        if (!targetIssue.assignee  && !issue.assignee) {
            return true;
        } else if (targetIssue.assignee && issue.assignee) {
            return targetIssue.assignee.key === issue.assignee.key;
        }
        return false;
    }
}

function createTable(boardData:BoardData, swimlaneNames:string[]) : SwimlaneData[] {
    let swimlaneTable:SwimlaneData[] = [];
    let slIndex:number = 0;
    for (let swimlaneName of swimlaneNames) {
        swimlaneTable.push(new SwimlaneData(boardData, swimlaneName, slIndex++));
    }
    return swimlaneTable;
}
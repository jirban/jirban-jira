import {BoardData} from "./boardData";
import {BoardFilters, NONE} from "./boardFilters";
import {IssueData} from "./issueData";
import {IMap} from "../../common/map";
import {SwimlaneData, SwimlaneDataBuilder} from "./swimlaneData";
import {CustomFieldValues, CustomFieldValue} from "./customField";
import {Indexed} from "../../common/indexed";
import {JiraMultiSelectFieldValue, JiraComponent, JiraLabel, JiraFixVersion} from "./multiSelectNameOnlyValue";

export interface SwimlaneIndexer {
    swimlaneBuilderTable : SwimlaneDataBuilder[];
    swimlaneIndex(issue:IssueData):number[];
    filter(swimlaneData:SwimlaneData):boolean;
}

export class SwimlaneIndexerFactory {
    createSwimlaneIndexer(swimlane:string, filters:BoardFilters, boardData:BoardData):SwimlaneIndexer {
        return this._createIndexer(swimlane, filters, boardData, true);
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
        } else if (swimlane === "component") {
            return new ComponentSwimlaneIndexer(filters, boardData, initTable);
        } else if (swimlane == "label") {
            return new LabelSwimlaneIndexer(filters, boardData, initTable);
        } else if (swimlane == "fix-version") {
            return new FixVersionSwimlaneIndexer(filters, boardData, initTable);
        } else if (swimlane) {
            if (boardData && boardData.customFields) {
                let cfvs:CustomFieldValues = boardData.customFields.forKey(swimlane);
                if (cfvs) {
                    return new CustomFieldSwimlaneIndexer(filters, boardData, initTable, swimlane, cfvs.values);
                }
            } else {
                console.log("Unknown swimlane '" + swimlane + "'. boardData:" + boardData);
            }
        }
        return null;
    }
}

abstract class BaseIndexer {
    protected _swimlaneTable:SwimlaneDataBuilder[];
    constructor(
        protected _filters:BoardFilters,
        protected _boardData:BoardData) {
    }

    get swimlaneBuilderTable():SwimlaneDataBuilder[] {
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

    swimlaneIndex(issue:IssueData):number[] {
        return [this._indices[issue.projectCode]];
    }

    filter(swimlaneData:SwimlaneData):boolean {
        return this._filters.filterProject(swimlaneData.name);
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

    swimlaneIndex(issue:IssueData):number[] {
        return [this._boardData.priorities.indices[issue.priorityName]];
    }

    filter(swimlaneData:SwimlaneData):boolean {
        return this._filters.filterPriority(swimlaneData.name);
    }
}

class IssueTypeSwimlaneIndexer extends BaseIndexer implements SwimlaneIndexer {
    private _swimlaneNames:string[] = [];

    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean) {
        super(filters, boardData);
        if (initTable) {
            for (let issueType of boardData.issueTypes.array) {
                this._swimlaneNames.push(issueType.name)
            }

            this._swimlaneTable = createTable(boardData, this._swimlaneNames);
        }
    }

    swimlaneIndex(issue:IssueData):number[] {
        return [this._boardData.issueTypes.indices[issue.typeName]];
    }

    filter(swimlaneData:SwimlaneData):boolean {
        return this._filters.filterIssueType(swimlaneData.name);
    }
}

class AssigneeSwimlaneIndexer extends BaseIndexer implements SwimlaneIndexer {
    private _swimlaneNames:string[] = [];

    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean) {
        super(filters, boardData);
        if (initTable) {
            for (let assignee of this._boardData.assignees.array) {
                this._swimlaneNames.push(assignee.name);
            }
            //Add an additional entry for the no assignee case
            this._swimlaneNames.push("None");

            this._swimlaneTable = createTable(boardData, this._swimlaneNames);
        }
    }

    swimlaneIndex(issue:IssueData):number[] {
        if (!issue.assignee) {
            return [this._swimlaneNames.length - 1];
        }
        return [this._boardData.assignees.indices[issue.assignee.key]];
    }

    filter(swimlaneData:SwimlaneData):boolean {
        let assigneeKey:string = null;
        if (swimlaneData.index < this._swimlaneNames.length - 1) {
            assigneeKey = this._boardData.assignees.forIndex(swimlaneData.index).key;
        }
        return this._filters.filterAssignee(assigneeKey);
    }
}


abstract class MultiSelectNameOnlyValueIndexer<T extends JiraMultiSelectFieldValue> extends BaseIndexer implements SwimlaneIndexer {
    private _swimlaneNames:string[] = [];

    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean) {
        super(filters, boardData);
        if (initTable) {
            for (let value of this.getBoardValues().array) {
                this._swimlaneNames.push(value.name);
            }
            //Add an additional entry for the no component case
            this._swimlaneNames.push("None");

            this._swimlaneTable = createTable(boardData, this._swimlaneNames);
        }
    }

    get swimlaneBuilderTable():SwimlaneDataBuilder[] {
        return this._swimlaneTable;
    }

    swimlaneIndex(issue:IssueData):number[] {
        let values:Indexed<T> = this.getIssueValues(issue);
        if (!values) {
            return [this._swimlaneNames.length - 1];
        }

        let lanes:number[] = new Array<number>(values.array.length);
        for (let i:number = 0 ; i < lanes.length ; i++) {
            lanes[i] = this.getBoardValues().indices[values.array[i].name];
        }
        return lanes;
    }

    filter(swimlaneData:SwimlaneData):boolean {
        let valueName:string = null;
        if (swimlaneData.index < this._swimlaneNames.length - 1) {
            valueName = this.getBoardValues().forIndex(swimlaneData.index).name;
        }
        return this.doFilter(valueName);
    }

    abstract getBoardValues():Indexed<T>;

    abstract getIssueValues(issue:IssueData):Indexed<T>;

    abstract doFilter(valueName:string):boolean;
}

class ComponentSwimlaneIndexer extends MultiSelectNameOnlyValueIndexer<JiraComponent> {
    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean) {
        super(filters, boardData, initTable);
    }

    getBoardValues(): Indexed<JiraComponent> {
        return this._boardData.components;
    }

    getIssueValues(issue: IssueData): Indexed<JiraComponent> {
        return issue.components;
    }

    doFilter(valueName: string):boolean {
        return this._filters.filterComponent(valueName);
    }
}

class LabelSwimlaneIndexer extends MultiSelectNameOnlyValueIndexer<JiraLabel> {
    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean) {
        super(filters, boardData, initTable);
    }

    getBoardValues(): Indexed<JiraLabel> {
        return this._boardData.labels;
    }

    getIssueValues(issue: IssueData): Indexed<JiraLabel> {
        return issue.labels;
    }

    doFilter(valueName: string):boolean {
        return this._filters.filterLabel(valueName);
    }
}

class FixVersionSwimlaneIndexer extends MultiSelectNameOnlyValueIndexer<JiraFixVersion> {
    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean) {
        super(filters, boardData, initTable);
    }

    getBoardValues(): Indexed<JiraFixVersion> {
        return this._boardData.fixVersions;
    }

    getIssueValues(issue: IssueData): Indexed<JiraFixVersion> {
        return issue.fixVersions;
    }

    doFilter(valueName: string):boolean {
        return this._filters.filterFixVersion(valueName);
    }
}

class CustomFieldSwimlaneIndexer extends BaseIndexer implements SwimlaneIndexer {
    private _customFieldName:string;
    private _customFieldValues:Indexed<CustomFieldValue>;
    private _swimlaneNames:string[] = [];

    constructor(filters:BoardFilters, boardData:BoardData, initTable:boolean, customFieldName:string, customFieldValues:Indexed<CustomFieldValue>) {
        super(filters, boardData);
        this._customFieldName = customFieldName;
        this._customFieldValues = customFieldValues;
        if (initTable) {
            let i:number = 0;
            for (let customFieldValue of customFieldValues.array) {
                this._swimlaneNames.push(customFieldValue.displayValue);
            }
            //Add an additional entry for the no match case
            this._swimlaneNames.push("None");

            this._swimlaneTable = createTable(boardData, this._swimlaneNames);
        }
    }

    swimlaneIndex(issue:IssueData):number[] {
        let customFieldValue:CustomFieldValue = issue.getCustomFieldValue(this._customFieldName);
        if (!customFieldValue) {
            //Put it into the 'None' bucket
            return [this._swimlaneNames.length - 1];
        }
        return [this._customFieldValues.indices[customFieldValue.key]];
    }

    filter(swimlaneData:SwimlaneData):boolean {
        let key:string = null;
        if (swimlaneData.index < this._swimlaneNames.length - 1) {
            key = this._customFieldValues.forIndex(swimlaneData.index).key;
        } else {
            key = NONE;
        }
        return this._filters.filterCustomField(this._customFieldName, key);
    }
}

function createTable(boardData:BoardData, swimlaneNames:string[]) : SwimlaneDataBuilder[] {
    let swimlaneTable:SwimlaneDataBuilder[] = [];
    let slIndex:number = 0;
    for (let swimlaneName of swimlaneNames) {
        swimlaneTable.push(new SwimlaneDataBuilder(boardData, swimlaneName, slIndex++));
    }
    return swimlaneTable;
}
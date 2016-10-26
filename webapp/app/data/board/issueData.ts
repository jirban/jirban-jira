import {Assignee} from "./assignee";
import {BoardData} from "./boardData";
import {Priority} from "./priority";
import {IssueType} from "./issueType";
import {BoardProject, Project, LinkedProject} from "./project";
import {IssueUpdate, IssueAdd} from "./change";
import {JiraComponent, JiraLabel, JiraFixVersion} from "./multiSelectNameOnlyValue";
import {Indexed} from "../../common/indexed";
import {CustomFieldValue} from "./customField";
import {IMap, IMapUtil} from "../../common/map";
import {Subject, Observable} from "rxjs/Rx";
import {ParallelTask} from "./parallelTask";
import {BoardFilters} from "./boardFilters";

/**
 * NB! This class is effectively immutable! When changing something, a new instance should be created.
 */
export abstract class IssueDataBase<P extends Project> {
    protected readonly _boardData:BoardData;
    protected readonly _key:string;
    protected readonly _project:P;
    protected readonly _summary:string;

    //The index within the issue's project's own states
    protected readonly _statusIndex:number;

    //Used to report the status in mouse-over a card
    //Note that projects will report their own jira project status rather than
    // the mapped board state in which they appear
    protected readonly _ownStatus:string;

    protected constructor(boardData:BoardData, key:string, project:P, summary:string,
                statusIndex:number) {
        this._boardData = boardData;
        this._key = key;
        this._project = project;
        this._statusIndex = statusIndex;
        this._summary = summary;
        this._ownStatus = this._project.getStateText(this._statusIndex);
    }

    get key():string {
        return this._key;
    }

    get projectCode():string {
        return this._project.code;
    }

    get summary():string {
        return this._summary;
    }

    get statusIndex():number {
        return this._statusIndex;
    }

    get boardData():BoardData {
        return this._boardData;
    }

    get project():P {
        return this._project;
    }

    get ownStatus():string {
        return this._ownStatus;
    }
}

/**
 * NB! THis class is effectively immutable! When changing something, a new instance should be created.
 *
 * The only exception to this is the _filtered field.
 *
 */
export class IssueData extends IssueDataBase<BoardProject> {
    private readonly _colour:string;
    private readonly _linked:LinkedIssueData[];
    private readonly _assignee:Assignee;
    private readonly _components:Indexed<JiraComponent>;
    private readonly _labels:Indexed<JiraLabel>
    private readonly _fixVersions:Indexed<JiraFixVersion>
    private readonly _priority:Priority;
    private readonly _type:IssueType;
    private readonly _customFields:IMap<CustomFieldValue>;
    private readonly _parallelTaskOptions:Indexed<string>;

    //Cached status fields
    private readonly _boardStatus:string;
    private readonly _boardStatusIndex:number = null;

    //This one is not immutable, but emits via the observable when it has been changed
    private _filtered:boolean = false;


    protected readonly _filteredSubject:Subject<boolean> = new Subject<boolean>();


    constructor(boardData:BoardData, key:string, project:BoardProject, colour:string, summary:string,
                assignee:Assignee,  components:Indexed<JiraComponent>, labels:Indexed<JiraLabel>,
                fixVersions:Indexed<JiraFixVersion>, priority:Priority, type:IssueType, statusIndex:number,
                linked:LinkedIssueData[], customFields:IMap<CustomFieldValue>, parallelTaskOptions:Indexed<string>) {
        super(boardData, key, project, summary, statusIndex);
        this._colour = colour;
        this._linked = linked;
        this._assignee = assignee;
        this._components = components;
        this._labels = labels;
        this._fixVersions = fixVersions;
        this._priority = priority;
        this._type = type;
        this._customFields = customFields;
        this._parallelTaskOptions = parallelTaskOptions;

        this._boardStatus = this.project.mapStateStringToBoard(this.project.states.forIndex(this.statusIndex));
        this._boardStatusIndex = this.project.mapStateIndexToBoardIndex(this.statusIndex);
    }

    static createFullRefresh(boardData:BoardData, input:any) : IssueData {
        return new BoardIssueDeserializer(boardData).deserialize(input);
    }

    static createFromChangeSet(boardData:BoardData, add:IssueAdd) : IssueData {
        return new BoardIssueDeserializer(boardData).createFromAddChange(add);
    }

    get filteredObservable():Observable<boolean> {
        return this._filteredSubject;
    }

    get colour():string {
        return this._colour;
    }

    get assignee():Assignee {
        return this._assignee;
    }

    get components():Indexed<JiraComponent> {
        return this._components;
    }

    get labels(): Indexed<JiraLabel> {
        return this._labels;
    }

    get fixVersions(): Indexed<JiraFixVersion> {
        return this._fixVersions;
    }

    get priority():Priority {
        return this._priority;
    }

    get type():IssueType {
        return this._type;
    }

    get customFields():IMap<CustomFieldValue> {
        return this._customFields;
    }

    get assigneeName():string {
        if (!this._assignee) {
            return "Unassigned";
        }
        return this._assignee.name;
    }

    get assigneeAvatar():string {
        if (!this._assignee) {
            //Return null, the issue.html will provide the replacement
            return null;
        }
        return this._assignee.avatar;
    }

    get assigneeInitials():string {
        if (!this._assignee) {
            return "None";
        }
        return this._assignee.initials;
    }

    get priorityName():string {
        return this._priority.name;
    }

    get priorityUrl():string {
        return this.boardData.jiraUrl + this._priority.icon;
    }

    get typeName():string {
        return this._type.name;
    }

    get typeUrl():string {
        return this.boardData.jiraUrl + this._type.icon;
    }

    getCustomFieldValue(name:string):CustomFieldValue {
        return this._customFields[name];
    }

    get parallelTaskOptions(): Indexed<string> {
        return this._parallelTaskOptions;
    }

    get filtered() : boolean {
        return this._filtered;
    }

    get boardStatus():string {
        return this._boardStatus;
    }

    get boardStatusIndex():number {
        return this._boardStatusIndex;
    }

    get linkedIssues():LinkedIssueData[] {
        return this._linked;
    }

    //!!! Don't call this outside of tests
    get customFieldNames():string[] {
        return IMapUtil.getSortedKeys(this._customFields);
    }

    filterIssue(filters:BoardFilters) {
        let old:boolean = this._filtered;
        this._filtered = filters.filterIssue(this);
        if (old != this._filtered) {
            this._filteredSubject.next(this._filtered);
        }
    }

    applyUpdate(update:IssueUpdate):IssueData {
        return new BoardIssueDeserializer(this.boardData).createFromUpdateChange(this, update);
    }
}

export class LinkedIssueData extends IssueDataBase<LinkedProject> {
    constructor(boardData:BoardData, key:string, project:LinkedProject, summary:string, statusIndex:number) {
        super(boardData, key, project, summary, statusIndex)
    }
}

abstract class IssueDeserializer {
    protected readonly _boardData:BoardData;

    protected _key: string;
    protected _projectCode: string;
    protected _statusIndex: number;
    protected _summary: string;

    constructor(boardData:BoardData) {
        this._boardData = boardData;
    }

    protected deserialize(input:any) {
        this._key = input.key;
        this._projectCode = this.productCodeFromKey(this._key);
        this._statusIndex = input.state;
        this._summary = input.summary;

    }

    protected productCodeFromKey(key:string) : string {
        let index:number = key.lastIndexOf("-");
        return key.substring(0, index);
    }
}

class BoardIssueDeserializer extends IssueDeserializer {
    private _project:BoardProject;
    private _assignee: Assignee;
    private _priority: Priority;
    private _type: IssueType;
    private _colour:string;
    private _components:Indexed<JiraComponent>;
    private _labels:Indexed<JiraLabel>;
    private _fixVersions:Indexed<JiraFixVersion>;
    private _customFields:IMap<CustomFieldValue> = {};
    private _parallelTaskOptions: Indexed<string>;
    private _linked:LinkedIssueData[];


    constructor(boardData:BoardData) {
        super(boardData);
    }

    deserialize(input:any):IssueData {
        super.deserialize(input);
        this._assignee = this._boardData.assignees.forIndex(input.assignee);
        this._priority = this._boardData.priorities.forIndex(input.priority);
        this._type = this._boardData.issueTypes.forIndex(input.type);
        this._project = this._boardData.boardProjects.forKey(this._projectCode);
        this._colour = this._project.colour;

        if (input["components"]) {
            this._components = new Indexed<JiraComponent>()
            for (let componentIndex of input["components"]) {
                let component:JiraComponent = this._boardData.components.forIndex(componentIndex);
                this._components.add(component.name, component);
            }
        }

        if (input["labels"]) {
            this._labels = new Indexed<JiraLabel>();
            for (let index of input["labels"]) {
                let label:JiraLabel = this._boardData.labels.forIndex(index);
                this._labels.add(label.name, label);
            }
        }

        if (input["fix-versions"]) {
            this._fixVersions = new Indexed<JiraFixVersion>();
            for (let index of input["fix-versions"]) {
                let fixVersion:JiraFixVersion = this._boardData.fixVersions.forIndex(index);
                this._fixVersions.add(fixVersion.name, fixVersion);
            }
        }

        let linkedIssues = input["linked-issues"];
        if (!!linkedIssues && linkedIssues.length > 0) {
            this._linked = [];
            for (let i:number = 0; i < linkedIssues.length; i++) {
                this._linked.push(new LinkedIssueDeserializer(this._boardData).deserialize(linkedIssues[i]));
            }
        }
        if (input["custom"]) {
            let customFields:any[] = input["custom"];
            for (let name in customFields) {
                let index = customFields[name];
                this._customFields[name] = this._boardData.getCustomFieldValueForIndex(name, index);
            }
        }

        if (this._project) {
            //Only board projects have this
            let parallelTasksInput: number[] = input["parallel-tasks"];
            this._parallelTaskOptions = this.deserializeParallelTasksArray(this._project, parallelTasksInput);
        }
        return this.build();
    }

    createFromAddChange(add:IssueAdd):IssueData {
        this._key = add.key;
        this._projectCode = this.productCodeFromKey(add.key);
        this._summary = add.summary;
        this._assignee = this._boardData.assignees.forKey(add.assignee);
        this._priority = this._boardData.priorities.forKey(add.priority);
        this._type = this._boardData.issueTypes.forKey(add.type);
        this._project = this._boardData.boardProjects.forKey(this._projectCode);
        this._colour = this._project.colour;
        this._statusIndex = this._project.getOwnStateIndex(add.state);

        if (add.components) {
            this._components = new Indexed<JiraComponent>();
            for (let name of add.components) {
                let component:JiraComponent = this._boardData.components.forKey(name);
                this._components.add(component.name, component);
            }
        }

        if (add.labels) {
            this._labels = new Indexed<JiraLabel>();
            for (let name of add.labels) {
                let label:JiraLabel = this._boardData.labels.forKey(name);
                this._labels.add(label.name, label);
            }
        }

        if (add.fixVersions) {
            this._fixVersions = new Indexed<JiraFixVersion>();
            for (let name of add.fixVersions) {
                let fixVersion:JiraFixVersion = this._boardData.fixVersions.forKey(name);
                this._fixVersions.add(fixVersion.name, fixVersion);
            }
        }

        if (add.customFieldValues) {
            for (let name in add.customFieldValues) {
                let fieldKey:string = add.customFieldValues[name];
                this._customFields[name] = this._boardData.getCustomFieldValueForKey(name, fieldKey);
            }
        }

        this._parallelTaskOptions = this.deserializeParallelTasksArray(this._project, add.parallelTaskValues);

        return this.build();
    }

    createFromUpdateChange(existing:IssueData, update:IssueUpdate):IssueData {
        //Get the existing data
        this._key = existing.key;
        this._projectCode = existing.projectCode;
        this._summary = existing.summary;
        this._assignee = existing.assignee;
        this._priority = existing.priority;
        this._type = existing.type;
        this._project = existing.project;
        this._colour = existing.colour;
        this._statusIndex = existing.statusIndex;
        this._components = existing.components;
        this._labels = existing.labels;
        this._fixVersions = existing.fixVersions;
        this._customFields = existing.customFields;
        this._parallelTaskOptions = existing.parallelTaskOptions;

        //Apply the changes
        if (update.type) {
            this._type = this._boardData.issueTypes.forKey(update.type);
        }
        if (update.priority) {
            this._priority = this._boardData.priorities.forKey(update.priority);
        }
        if (update.summary) {
            this._summary = update.summary;
        }
        if (update.state) {
            this._statusIndex = this._project.getOwnStateIndex(update.state);
        }
        if (update.unassigned) {
            this._assignee = null;
        } else if (update.assignee) {
            this._assignee = this._boardData.assignees.forKey(update.assignee);
        }
        if (update.clearedComponents) {
            this._components = null;
        } else if (update.components) {
            this._components = new Indexed<JiraComponent>();
            for (let name of update.components) {
                let component:JiraComponent = this._boardData.components.forKey(name);
                this._components.add(component.name, component);
            }
        }
        if (update.clearedLabels) {
            this._labels = null;
        } else if (update.labels) {
            this._labels = new Indexed<JiraLabel>();
            for (let name of update.labels) {
                let label:JiraLabel = this._boardData.labels.forKey(name);
                this._labels.add(label.name, label);
            }
        }
        if (update.clearedFixVersions) {
            this._fixVersions = null;
        } else if (update.fixVersions) {
            this._fixVersions = new Indexed<JiraFixVersion>();
            for (let name of update.fixVersions) {
                let fixVersion:JiraFixVersion = this._boardData.fixVersions.forKey(name);
                this._fixVersions.add(fixVersion.name, fixVersion);
            }
        }
        if (update.customFieldValues) {
            for (let key in update.customFieldValues) {
                let fieldKey:string = update.customFieldValues[key];
                if (!fieldKey) {
                    delete this._customFields[key];
                } else {
                    this._customFields[key] = this._boardData.getCustomFieldValueForKey(key, fieldKey);
                }
            }
        }
        if (update.parallelTaskValueUpdates) {
            for (let indexString in update.parallelTaskValueUpdates) {
                let taskIndex:number = parseInt(indexString);
                let parallelTask:ParallelTask = this._project.parallelTasks.forIndex(taskIndex);
                let option: string = parallelTask.getOptionForIndex(update.parallelTaskValueUpdates[indexString]);
                this._parallelTaskOptions.array[taskIndex] = option;
            }
        }

        let newIssue:IssueData = this.build();
        return newIssue;
    }

    private build():IssueData {
        return new IssueData(this._boardData, this._key, this._project, this._colour, this._summary,
            this._assignee, this._components, this._labels, this._fixVersions,
            this._priority, this._type, this._statusIndex, this._linked,
            this._customFields, this._parallelTaskOptions);

    }

    private deserializeParallelTasksArray(project:BoardProject, parallelTasksInput: number[]):Indexed<string> {
        let parallelTaskOptions: Indexed<string>;
        let projectParallelTasks = project.parallelTasks;

        if (parallelTasksInput) {
            parallelTaskOptions = new Indexed<string>();
            for (let i: number = 0; i < parallelTasksInput.length; i++) {
                let parallelTask: ParallelTask = projectParallelTasks.forIndex(i);
                let option: string = parallelTask.getOptionForIndex(parallelTasksInput[i]);
                parallelTaskOptions.add(parallelTask.code, option);
            }
        }
        return parallelTaskOptions;
    }
}

class LinkedIssueDeserializer extends IssueDeserializer {
    constructor(boardData:BoardData) {
        super(boardData);
    }

    deserialize(input:any):LinkedIssueData {
        super.deserialize(input);
        let project:LinkedProject = this._boardData.linkedProjects[this._projectCode];
        return new LinkedIssueData(this._boardData, this._key, project, this._summary, this._statusIndex);
    }
}
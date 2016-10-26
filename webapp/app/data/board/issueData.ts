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

export abstract class IssueDataBase<P extends Project> {
    protected _boardData:BoardData;
    protected _key:string;
    protected _project:P;
    protected _summary:string;

    //The index within the issue's project's own states
    protected _statusIndex:number;

    //Used to report the status in mouse-over a card
    //Note that projects will report their own jira project status rather than
    // the mapped board state in which they appear
    protected _ownStatus:string;

    constructor(boardData:BoardData, key:string, project:P, summary:string,
                statusIndex:number) {
        this._boardData = boardData;
        this._key = key;
        this._project = project;
        this._statusIndex = statusIndex;
        this._summary = summary;

        this.initCalculatedFields();
    }

    static createFullRefresh(boardData:BoardData, input:any) : IssueData {
        return new BoardIssueDeserializer(boardData).deserialize(input);
    }

    static createFromChangeSet(boardData:BoardData, add:IssueAdd) : IssueData {
        return new BoardIssueDeserializer(boardData).createFromAddChange(add);
    }

    //Plain getters
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

    set statusIndex(index:number) {
        this._statusIndex = index;
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

    private isDefined(index:number):boolean {
        if (index) {
            return true;
        }
        if (!isNaN(index)) {
            return true;
        }
        return false;
    }

    protected initCalculatedFields():void{
        this._ownStatus = this._project.getStateText(this._statusIndex);
    }
}

export class IssueData extends IssueDataBase<BoardProject> {
    private _colour:string;
    private _linked:LinkedIssueData[];
    private _assignee:Assignee;
    private _components:Indexed<JiraComponent>;
    private _labels:Indexed<JiraLabel>
    private _fixVersions:Indexed<JiraFixVersion>
    private _priority:Priority;
    private _type:IssueType;
    private _customFields:IMap<CustomFieldValue>;
    private _parallelTaskOptions:Indexed<string>;

    private _filtered:boolean = false;

    //Cached status fields
    private _boardStatus:string;
    private _boardStatusIndex:number = null;

    protected _issueChangedSubject:Subject<void> = new Subject<void>();


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

    get changeObserver():Observable<void> {
        return this._issueChangedSubject;
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

    filterIssue(filters:BoardFilters) {
        //console.log("-Filter " + this.key);
        this._filtered = filters.filterIssue(this);
    }


    protected initCalculatedFields():void {
        super.initCalculatedFields();
        this._boardStatus = this.project.mapStateStringToBoard(this.project.states.forIndex(this.statusIndex));
        this._boardStatusIndex = this.project.mapStateIndexToBoardIndex(this.statusIndex);

    }

    //!!! Don't call this outside of tests
    get customFieldNames():string[] {
        return IMapUtil.getSortedKeys(this._customFields);
    }

//Update functions
    applyUpdate(update:IssueUpdate) {
        if (update.type) {
            this._type = this.boardData.issueTypes.forKey(update.type);
        }
        if (update.priority) {
            this._priority = this.boardData.priorities.forKey(update.priority);
        }
        if (update.summary) {
            this._summary = update.summary;
        }
        if (update.state) {
            let project:BoardProject = this.project;
            this._statusIndex = project.getOwnStateIndex(update.state);
            this.initCalculatedFields();
        }
        if (update.unassigned) {
            this._assignee = null;
        } else if (update.assignee) {
            this._assignee = this.boardData.assignees.forKey(update.assignee);
        }

        if (update.clearedComponents) {
            this._components = null;
        } else if (update.components) {
            this._components = new Indexed<JiraComponent>();
            for (let name of update.components) {
                let component:JiraComponent = this.boardData.components.forKey(name);
                this._components.add(component.name, component);
            }
        }

        if (update.clearedLabels) {
            this._labels = null;
        } else if (update.labels) {
            this._labels = new Indexed<JiraLabel>();
            for (let name of update.labels) {
                let label:JiraLabel = this.boardData.labels.forKey(name);
                this._labels.add(label.name, label);
            }
        }

        if (update.clearedFixVersions) {
            this._fixVersions = null;
        } else if (update.fixVersions) {
            this._fixVersions = new Indexed<JiraFixVersion>();
            for (let name of update.fixVersions) {
                let fixVersion:JiraFixVersion = this.boardData.fixVersions.forKey(name);
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
                let parallelTask:ParallelTask = this.project.parallelTasks.forIndex(taskIndex);
                let option: string = parallelTask.getOptionForIndex(update.parallelTaskValueUpdates[indexString]);
                this._parallelTaskOptions.array[taskIndex] = option;
            }
        }

        this._issueChangedSubject.next(null);
    }
}

export class LinkedIssueData extends IssueDataBase<LinkedProject> {
    constructor(boardData:BoardData, key:string, project:LinkedProject, summary:string, statusIndex:number) {
        super(boardData, key, project, summary, statusIndex)
    }
}

abstract class IssueDeserializer {
    protected _boardData:BoardData;

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
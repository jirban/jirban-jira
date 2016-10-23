import {Assignee} from "./assignee";
import {BoardData} from "./boardData";
import {Priority} from "./priority";
import {IssueType} from "./issueType";
import {BoardProject, Project} from "./project";
import {IssueUpdate, IssueAdd} from "./change";
import {JiraComponent} from "./component";
import {Indexed} from "../../common/indexed";
import {CustomFieldValue} from "./customField";
import {IMap} from "../../common/map";
import {Subject, Observable} from "rxjs/Rx";
import {ParallelTask} from "./parallelTask";
import {BoardFilters} from "./boardFilters";

export class IssueData {
    private _boardData:BoardData;
    private _key:string;
    private _projectCode:string;
    private _colour:string;
    private _summary:string;
    private _assignee:Assignee;
    private _components:Indexed<JiraComponent>;
    private _priority:Priority;
    private _type:IssueType;
    private _customFields:IMap<CustomFieldValue>;

    //The index within the issue's project's own states
    private _statusIndex:number;
    private _linked:IssueData[];
    private _filtered:boolean = false;

    //Cached status fields
    private _boardStatus:string;
    private _boardStatusIndex:number = null;
    private _ownStatus:string;

    private _parallelTaskOptions:Indexed<string>;

    private _issueChangedSubject:Subject<void> = new Subject<void>();

    constructor(boardData:BoardData, key:string, projectCode:string, colour:string, summary:string,
                assignee:Assignee,  components:Indexed<JiraComponent>, priority:Priority, type:IssueType, statusIndex:number,
                linked:IssueData[], customFields:IMap<CustomFieldValue>, parallelTaskOptions:Indexed<string>) {
        this._boardData = boardData;
        this._key = key;
        this._projectCode = projectCode;
        this._statusIndex = statusIndex;
        this._summary = summary;
        this._assignee = assignee;
        this._components = components;
        this._priority = priority;
        this._type = type;
        this._colour = colour;
        this._linked = linked;
        this._customFields = customFields;
        this._parallelTaskOptions = parallelTaskOptions;
    }

    static createFullRefresh(boardData:BoardData, input:any) : IssueData {
        let key:string = input.key;
        let projectCode:string = IssueData.productCodeFromKey(key);
        let statusIndex:number = input.state;
        let summary:string = input.summary;
        let assignee:Assignee = boardData.assignees.forIndex(input.assignee);
        let priority:Priority = boardData.priorities.forIndex(input.priority);
        let type:IssueType = boardData.issueTypes.forIndex(input.type);

        let components:Indexed<JiraComponent>;
        if (input.components) {
            components = new Indexed<JiraComponent>()
            for (let componentIndex of input.components) {
                let component:JiraComponent =boardData.components.forIndex(componentIndex);
                components.add(component.name, component);
            }
        }

        let colour:string;
        let project:BoardProject = boardData.boardProjects.forKey(projectCode);
        if (project) {
            colour = project.colour;
        }

        let linked:IssueData[];
        let linkedIssues = input["linked-issues"];
        if (!!linkedIssues && linkedIssues.length > 0) {
            linked = [];
            for (let i:number = 0; i < linkedIssues.length; i++) {
                linked.push(IssueData.createFullRefresh(boardData, linkedIssues[i]));
            }
        }
        let customFieldValues:IMap<CustomFieldValue> = {};
        if (input["custom"]) {
            let customFields:any[] = input["custom"];
            for (let name in customFields) {
                let index = customFields[name];
                customFieldValues[name] = boardData.getCustomFieldValueForIndex(name, index);
            }
        }

        let parallelTaskOptions: Indexed<string>;
        if (project) {
            //Only board projects have this
            let parallelTasksInput: number[] = input["parallel-tasks"];
            parallelTaskOptions = IssueData.deserializeParallelTasksArray(project, parallelTasksInput);
        }

        return new IssueData(boardData, key, projectCode, colour, summary, assignee, components, priority,
            type, statusIndex, linked, customFieldValues, parallelTaskOptions);
    }

    static createFromChangeSet(boardData:BoardData, add:IssueAdd) {
        let projectCode:string = IssueData.productCodeFromKey(add.key);
        let assignee:Assignee = boardData.assignees.forKey(add.assignee);
        let priority:Priority = boardData.priorities.forKey(add.priority);
        let type:IssueType = boardData.issueTypes.forKey(add.type);
        let colour:string;
        let statusIndex:number;
        let project:BoardProject = boardData.boardProjects.forKey(projectCode);
        if (project) {
            colour = project.colour;
            statusIndex = project.getOwnStateIndex(add.state);
        }

        let components:Indexed<JiraComponent>;
        if (add.components) {
            components = new Indexed<JiraComponent>();
            for (let name of add.components) {
                let component:JiraComponent = boardData.components.forKey(name);
                components.add(component.name, component);
            }
        }

        let linked:IssueData[];//This does not get set from the events

        let customFieldValues:IMap<CustomFieldValue> = {};
        if (add.customFieldValues) {
            for (let name in add.customFieldValues) {
                let fieldKey:string = add.customFieldValues[name];
                customFieldValues[name] = boardData.getCustomFieldValueForKey(name, fieldKey);
            }
        }

        let parallelTaskOptions:Indexed<string> = IssueData.deserializeParallelTasksArray(project, add.parallelTaskValues);

        return new IssueData(boardData, add.key, projectCode, colour, add.summary, assignee,
            components, priority, type, statusIndex, linked, customFieldValues, parallelTaskOptions);
    }

    private static deserializeParallelTasksArray(project:BoardProject, parallelTasksInput: number[]):Indexed<string> {
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

    private static productCodeFromKey(key:string) : string {
        let index:number = key.lastIndexOf("-");
        return key.substring(0, index);
    }

    //Plain getters
    get changeObserver():Observable<void> {
        return this._issueChangedSubject;
    }
    get key():string {
        return this._key;
    }

    get projectCode():string {
        return this._projectCode;
    }

    get colour():string {
        return this._colour;
    }

    get summary():string {
        return this._summary;
    }

    get assignee():Assignee {
        return this._assignee;
    }

    get components():Indexed<JiraComponent> {
        return this._components;
    }

    get priority():Priority {
        return this._priority;
    }

    get type():IssueType {
        return this._type;
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

    get linkedIssues():IssueData[] {
        return this._linked;
    }

    get filtered() : boolean {
        return this._filtered;
    }

    get customFields():IMap<CustomFieldValue> {
        return this._customFields;
    }

    get parallelTaskOptions(): Indexed<string> {
        return this._parallelTaskOptions;
    }

//'Advanced'/'Nested' getters

    get boardStatus():string {
        //The state we map to in the board (only for board projects)
        if (!this._boardStatus) {
            let project:BoardProject = this._boardData.boardProjects.forKey(this._projectCode);
            let myStatusName:string = project.states.forIndex(this._statusIndex);
            this._boardStatus = project.mapStateStringToBoard(myStatusName);
        }
        return this._boardStatus;
    }

    get boardStatusIndex():number {
        if (this._boardStatusIndex == null) {
            let project:BoardProject = this._boardData.boardProjects.forKey(this._projectCode);
            this._boardStatusIndex = project.mapStateIndexToBoardIndex(this._statusIndex);
        }
        return this._boardStatusIndex;
    }

    get ownStatus():string {
        //Used to report the status in mouse-over a card
        //Note that projects will report their own jira project status rather than
        // the mapped board state in which they appear
        if (!this._ownStatus) {
            let project:Project = this._boardData.linkedProjects[this._projectCode];
            if (!project) {
                project = this._boardData.boardProjects.forKey(this._projectCode);
            }
            this._ownStatus = project.getStateText(this._statusIndex);
        }
        return this._ownStatus;
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
        return this._boardData.jiraUrl + this._priority.icon;
    }

    get typeName():string {
        return this._type.name;
    }

    get typeUrl():string {
        return this._boardData.jiraUrl + this._type.icon;
    }

    get linkedProject() : Project {
        return this._boardData.linkedProjects[this._projectCode];
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

    private get boardProject() : BoardProject {
        return this._boardData.boardProjects.forKey(this._projectCode);
    }

    //Update functions
    applyUpdate(update:IssueUpdate) {
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
            let project:BoardProject = this.boardProject;
            this._statusIndex = project.getOwnStateIndex(update.state);
            this._ownStatus = null;
            this._boardStatus = null;
            this._boardStatusIndex = null;
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
                let project:BoardProject = this._boardData.boardProjects.forKey(this._projectCode);
                if (project) {
                    let taskIndex:number = parseInt(indexString);
                    let parallelTask:ParallelTask = project.parallelTasks.forIndex(taskIndex);
                    let option: string = parallelTask.getOptionForIndex(update.parallelTaskValueUpdates[indexString]);
                    this._parallelTaskOptions.array[taskIndex] = option;
                }
            }
        }

        this._issueChangedSubject.next(null);
    }

    getCustomFieldValue(name:string):CustomFieldValue {
        return this._customFields[name];
    }

    filterIssue(filters:BoardFilters) {
        //console.log("-Filter " + this.key);
        this._filtered = filters.filterIssue(this);
    }
}
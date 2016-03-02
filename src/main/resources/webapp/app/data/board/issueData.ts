import {Assignee} from "./assignee";
import {BoardData} from "./boardData";
import {Priority} from "./priority";
import {IssueType} from "./issueType";
import {isNumber} from "angular2/src/facade/lang";
import {BoardProject, Project} from "./project";
import {IssueChange} from "./change";
import {JiraComponent} from "./component";
import {Indexed} from "../../common/indexed";


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
    //The index within the issue's project's own states
    private _statusIndex:number;
    private _linked:IssueData[];
    private _filtered:boolean = false;

    constructor(boardData:BoardData, key:string, projectCode:string, colour:string, summary:string,
                assignee:Assignee,  components:Indexed<JiraComponent>, priority:Priority, type:IssueType, statusIndex:number,
                linked:IssueData[]) {
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
        return new IssueData(boardData, key, projectCode, colour, summary, assignee, components, priority, type, statusIndex, linked);
    }

    static createFromChangeSet(boardData:BoardData, add:IssueChange) {
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
        return new IssueData(boardData, add.key, projectCode, colour, add.summary, assignee, components, priority, type, statusIndex, linked);

    }


    private static productCodeFromKey(key:string) : string {
        let index:number = key.lastIndexOf("-");
        return key.substring(0, index);
    }

    //Plain getters
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

    set filtered(filtered) {
        this._filtered = filtered;
    }


    //'Advanced'/'Nested' getters

    get boardStatus():string {
        //The state we map to in the board (only for board projects)
        let project:BoardProject = this._boardData.boardProjects.forKey(this._projectCode);
        let myStatusName:string = project.states.forIndex(this._statusIndex);
        let boardStatus:string = project.mapStateStringToBoard(myStatusName);
        return boardStatus;
    }

    get ownStatus():string {
        //Used to report the status in mouse-over a card
        //Note that projects will report their own jira project status rather than
        // the mapped board state in which they appear
        let project:Project = this._boardData.linkedProjects[this._projectCode];
        if (!project) {
            project = this._boardData.boardProjects.forKey(this._projectCode);
        }
        return project.getStateText(this._statusIndex);
    }

    get assigneeName():string {
        if (!this._assignee) {
            return "Unassigned";
        }
        return this._assignee.name;
    }

    get assigneeAvatar():string {
        if (!this._assignee) {
            return "images/person-4x.png";
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
        if (isNumber(index)) {
            return true;
        }
        return false;
    }

    private get boardProject() : BoardProject {
        return this._boardData.boardProjects.forKey(this._projectCode);
    }

    //Update functions
    applyUpdate(update:IssueChange) {
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
    }
}
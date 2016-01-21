import {Assignee} from "./assignee";
import {BoardData} from "./boardData";
import {Priority} from "./priority";
import {IssueType} from "./issueType";
import {isNumber} from "angular2/src/facade/lang";
import {BoardProject, Project} from "./project";


export class IssueData {
    private _boardData:BoardData;
    private _key:string;
    private _projectCode:string;
    private _colour:string;
    private _summary:string;
    private _assignee:Assignee;
    private _priority:Priority;
    private _type:IssueType;
    private _statusIndex:number;
    private _linked:IssueData[];
    private _filtered:boolean = false;

    constructor(boardData:BoardData, input:any) {
        this._boardData = boardData;
        this._key = input.key;
        let index:number = this._key.lastIndexOf("-");
        this._projectCode = this._key.substring(0, index);
        this._statusIndex = input.state;
        this._summary = input.summary;
        this._assignee = boardData.assignees.forIndex(input.assignee);
        this._priority = boardData.priorities.forIndex(input.priority);
        this._type = boardData.issueTypes.forIndex(input.type);

        let project:BoardProject = boardData.boardProjects.forKey(this._projectCode);
        if (project) {
            this._colour = project.colour;
        }

        let linkedIssues = input["linked-issues"];
        if (!!linkedIssues && linkedIssues.length > 0) {
            this._linked = [];
            for (let i:number = 0; i < linkedIssues.length; i++) {
                this._linked.push(new IssueData(boardData, linkedIssues[i]));
            }
        }
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
        //Note that the main projects which are not the 'owner', will report their own status rather than
        // the mapped owner column they appear in
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
        return this._priority.icon;
    }

    get typeName():string {
        return this._type.name;
    }

    get typeUrl():string {
        return this._type.icon;
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
}
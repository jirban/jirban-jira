import {Indexed} from "../../common/indexed";
import {IMap} from "../../common/map";
import {IssueData} from "./issueData";
import {IssueTable} from "./issueTable";
import ownKeys = Reflect.ownKeys;

/**
 * Registry of project related data got from the server
 */
export class Projects {

    private _owner:string;
    private _boardStates:Indexed<string>;
    private _boardProjects:Indexed<BoardProject> = new Indexed<BoardProject>();
    private _linkedProjects:IMap<LinkedProject> = {};
    private _boardProjectCodes:string[] = [];

    constructor(owner:string, boardStates:Indexed<string>, boardProjects:Indexed<BoardProject>, linkedProjects:IMap<LinkedProject>) {
        this._owner = owner;
        this._boardProjects = boardProjects;
        this._linkedProjects = linkedProjects;
        this._boardStates = boardStates;
        for (let key in boardProjects.indices) {
            this._boardProjectCodes.push(key);
        }
    }

    get owner():string {
        return this._owner;
    }

    get ownerProject():BoardProject {
        return this._boardProjects.forKey(this._owner);
    }

    get linkedProjects():IMap<LinkedProject> {
        return this._linkedProjects;
    }

    get boardProjects():Indexed<BoardProject> {
        return this._boardProjects;
    }

    get boardProjectCodes():string[] {
        return this._boardProjectCodes;
    }

    getValidMoveBeforeIssues(issueTable:IssueTable, swimlane:string, moveIssue:IssueData, toState:string) : IssueData[] {
        let project:BoardProject = this._boardProjects.forKey(moveIssue.projectCode);
        let toStateIndex:number = this._boardStates.indices[toState];
        return project.getValidMoveBeforeIssues(issueTable, swimlane, moveIssue, toStateIndex);
    }

    deleteIssues(deletedIssues:IssueData[]) {
        let issuesByProject:IMap<IMap<IssueData>> = {};
        for (let issue of deletedIssues) {
            let issues:IMap<IssueData> = issuesByProject[issue.projectCode];
            if (!issues) {
                issues = {};
                issuesByProject[issue.projectCode] = issues;
            }
            issues[issue.key] = issue;
        }

        for (let projectCode in issuesByProject) {
            this._boardProjects.forKey(projectCode).deleteIssues(issuesByProject[projectCode]);
        }
    }

    getBoardStateIndex(boardState:string) : number {
        let owner:BoardProject = this._boardProjects.forKey(this._owner);
        return owner.getOwnStateIndex(boardState);
    }

    canRank(projectCode:string):boolean {
        let project:BoardProject = this._boardProjects.forKey(projectCode);
        if (!project) {
            return false;
        }
        return project.canRank;
    }
}

/**
 * Base class for projects
 */
export abstract class Project {
    protected _code:string;
    protected _states:Indexed<string>;

    constructor(code:string, states:Indexed<string>) {
        this._code = code;
        this._states = states;
    }

    get code():string {
        return this._code;
    }

    get states():Indexed<string> {
        return this._states;
    }

    get statesLength() {
        return this._states.array.length;
    }

    getStateText(index:number) {
        return this._states.forIndex(index);
    }
}
/**
 * These are the projects whose issues will appear as cards on the board.
 */
export class BoardProject extends Project {
    private _boardStates:Indexed<string>;
    private _colour:string;
    private _canRank:boolean;
    //The list of issue keys in ranked order.
    private _rankedIssueKeys:string[];

    _boardStatesToProjectState:IMap<string> = {};
    _projectStatesToBoardState:IMap<string> = {};

    constructor(boardStates:Indexed<string>, code:string, canRank:boolean, colour:string, states:Indexed<string>,
                rankedIssueKeys:string[],
                boardStatesToProjectState:IMap<string>, projectStatesToBoardState:IMap<string>) {
        super(code, states);
        this._boardStates = boardStates;
        this._colour = colour;
        this._canRank = canRank;
        this._rankedIssueKeys = rankedIssueKeys;
        this._boardStatesToProjectState = boardStatesToProjectState;
        this._projectStatesToBoardState = projectStatesToBoardState;
    }

    get colour():string {
        return this._colour;
    }

    get canRank():boolean {
        return this._canRank;
    }

    get rankedIssueKeys():string[] {
        return this._rankedIssueKeys;
    }

    getValidMoveBeforeIssues(issueTable:IssueTable, swimlane:string, moveIssue:IssueData, toStateIndex:number) : IssueData[] {
        //TODO - reintroduce this
        return [];
        /*
        let issueKeys:string[] = this._issueKeys[toStateIndex];
        let validIssues:IssueData[] = [];
        let swimlaneMatcher:SwimlaneMatcher = new SwimlaneIndexerFactory().createSwimlaneMatcher(swimlane, moveIssue);
        for (let issueKey of issueKeys) {
            let issue:IssueData = issueTable.getIssue(issueKey);
            if (!swimlaneMatcher || swimlaneMatcher.matchesSwimlane(issue)) {
                validIssues.push(issue);
            }
        }
        return validIssues;
        */
    }

    getOwnStateIndex(state:string) : number {
        return this._states.indices[state];
    }

    /*
    deleteIssues(deletedIssuesByBoardState:IMap<IMap<IssueData>) {
        //TODO reintroduce this
        for (let boardState in deletedIssuesByBoardState) {
            let issueTableIndex:number = this._boardStates.indices[boardState];
            let deletedIssues:IMap<IssueData> = deletedIssuesByBoardState[boardState];
            let issueKeysForState:string[] = this._issueKeys[issueTableIndex];
            for (let i:number = 0 ; i < issueKeysForState.length ; ) {
                if (deletedIssues[issueKeysForState[i]]) {
                    issueKeysForState.splice(i, 1);
                } else {
                    i++;
                }
            }
        }
    }
     */
    deleteIssues(deletedIssues:IMap<IssueData>) {
        for (let i:number = 0 ; i < this._rankedIssueKeys.length ; ) {
            if (deletedIssues[this._rankedIssueKeys[i]]) {
                this._rankedIssueKeys.splice(i, 1);
            } else {
                i++;
            }
        }
    }

    isValidState(state:string):boolean {
        return !!this._boardStatesToProjectState[state];
    }


    mapStateStringToBoard(ownState:string):string {
        return this._projectStatesToBoardState[ownState];
    }


    mapBoardStateToOwnState(boardState:string):string {
        return this._boardStatesToProjectState[boardState];
    }

    mapStateStringToBoardIndex(ownState:string):number {
        let boardState:string = this.mapStateStringToBoard(ownState);
        return this._boardStates.indices[boardState];
    }

    mapStateIndexToBoardIndex(ownStateIndex:number):number {
        let ownState:string = this.states.forIndex(ownStateIndex);
        return this.mapStateStringToBoardIndex(ownState);
    }

    updateStateIssues(stateIndex:number, issueKeys:string[]) {
        //TODO reintroduce this
        //this._issueKeys[stateIndex] = issueKeys;
    }

}


/**
 * A linked projects whose issues are linked to from the main board projects.
 */
export class LinkedProject extends Project {

    constructor(code:string, states:Indexed<string>) {
        super(code, states);
    }
}

export class ProjectDeserializer {

    constructor(private _boardStates:Indexed<string>) {

    }

    deserialize(input:any) : Projects {
        let projectsInput = input.projects;

        let owner:string = projectsInput.owner;
        let mainProjectsInput:any = projectsInput.main;
        let boardProjects:Indexed<BoardProject> = this.deserializeBoardProjects(mainProjectsInput);
        let linkedProjects:IMap<LinkedProject> = this.deserializeLinkedProjects(projectsInput);
        let projects:Projects = new Projects(owner, this._boardStates, boardProjects, linkedProjects);
        return projects;
    }

    private deserializeLinkedProjects(projectsInput:any) : IMap<LinkedProject> {
        let linkedProjects:IMap<LinkedProject> = {};
        let linkedInput:any = projectsInput.linked;
        for (let key in linkedInput) {
            let entry = linkedInput[key];
            let states:Indexed<string> = this.deserializeStateArray(entry.states);
            linkedProjects[key] = new LinkedProject(key, states);
        }
        return linkedProjects;
    }

    private deserializeBoardProjects(mainProjectsInput:any) : Indexed<BoardProject> {
        let boardProjects:Indexed<BoardProject> = new Indexed<BoardProject>();
        boardProjects.indexMap(
            mainProjectsInput,
            (key, projectInput) => {
                let colour:string = projectInput.colour;
                let canRank:boolean = projectInput.rank;
                let ranked:string[] = projectInput.ranked;
                if (!ranked) {
                    ranked = [];
                }

                let boardStatesToProjectState:IMap<string> = {};
                let projectStatesToBoardState:IMap<string> = {};
                let stateLinksInput = projectInput["state-links"];
                for (let boardState in stateLinksInput) {
                    let projectState = stateLinksInput[boardState];
                    if (projectState) {
                        boardStatesToProjectState[boardState] = projectState;
                        projectStatesToBoardState[projectState] = boardState;
                    }
                }

                let states:Indexed<string> = new Indexed<string>();
                for (let boardState of this._boardStates.array) {
                    let ownState = boardStatesToProjectState[boardState];
                    if (ownState) {
                        states.add(ownState, ownState);
                    }
                }

                return new BoardProject(this._boardStates, key, canRank, colour, states, ranked,
                    boardStatesToProjectState, projectStatesToBoardState);
            }
        );
        return boardProjects;
    }

    private deserializeStateArray(statesInput:any) : Indexed<string> {
        let states:Indexed<string> = new Indexed<string>();
        states.indexArray(
            statesInput,
            (state) => {
                return state;
            },
            (state) => {
                return state;
            });
        return states;
    }
}

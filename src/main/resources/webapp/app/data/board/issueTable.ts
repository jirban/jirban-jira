import {Projects} from "./project";
import {BoardData} from "./boardData";
import {IssueData} from "./issueData";
import {IMap} from "../../common/map";
import {BoardFilters} from "./boardFilters";
import {SwimlaneIndexer, SwimlaneIndexerFactory} from './swimlaneIndexer';
import {Indexed} from "../../common/indexed";
import {BoardProject} from "./project";
import {stat} from "fs";
import {IssueUpdate} from "./change";

export class IssueTable {
    private _allIssues:Indexed<IssueData>;
    private _issueTable:IssueData[][];
    private _swimlaneTable:SwimlaneData[];
    private _totalIssuesByState:number[];

    /**
     * Called when first loading a board
     * @param _boardData
     * @param _projects
     * @param _filters
     * @param _swimlane
     * @param input
     */
    constructor(
                private _boardData:BoardData,
                private _projects:Projects,
                private _filters:BoardFilters,
                private _swimlane:string,
                input:any) {
        this.internalFullRefresh(input, true);
    }

    /**
     * Called when we receive the full table over the web socket
     * @param input
     */
    fullRefresh(projects:Projects, input:any) {
        this._projects = projects;
        this.internalFullRefresh(input, false);
    }

    get issueTable():IssueData[][] {
        return this._issueTable;
    }

    get swimlaneTable():SwimlaneData[] {
        return this._swimlaneTable;
    }

    get totalIssuesByState() : number[] {
        return this._totalIssuesByState;
    }

    set filters(filters:BoardFilters) {
        this._filters = filters;
        for (let issue of this._allIssues.array) {
            issue.filtered = this._filters.filterIssue(issue);
        }
        if (this._swimlane) {
            let indexer:SwimlaneIndexer = this.createSwimlaneIndexer();
            for (let swimlaneData of this._swimlaneTable) {
                swimlaneData.filtered = indexer.filter(swimlaneData);
            }
        }
    }

    set swimlane(swimlane:string) {
        this._swimlane = swimlane;
        this.createTable();
    }

    toggleSwimlaneVisibility(swimlaneIndex:number) {
        if (this._swimlaneTable) {
            this._swimlaneTable[swimlaneIndex].toggleVisibility();
        }
    }

    getIssue(issueKey:string) : IssueData {
        return this._allIssues.forKey(issueKey);
    }

    deleteIssues(issueKeys:string[]) {
        //TODO don't duplicate this across all the update functions
        let storedSwimlaneVisibilities:IMap<boolean> = this.storeSwimlaneVisibilities(false);

        let deletedIssues:IssueData[] = this._allIssues.deleteKeys(issueKeys);
        this._projects.deleteIssues(deletedIssues);

        //TODO don't duplicate this across all the update functions
        this.createTable();
        this.restoreSwimlaneVisibilities(storedSwimlaneVisibilities);

    }

    applyUpdates(issueUpdates:IssueUpdate[]) {
        //TODO don't duplicate this across all the update functions
        let storedSwimlaneVisibilities:IMap<boolean> = this.storeSwimlaneVisibilities(false);

        for (let update of issueUpdates) {
            let issue = this._allIssues.forKey(update.key);
            if (!issue) {
                console.log("Could not find issue to update " + update.key);
                return;
            }
            issue.applyUpdate(update);
        }

        //TODO don't duplicate this across all the update functions
        this.createTable();
        this.restoreSwimlaneVisibilities(storedSwimlaneVisibilities);

    }

    //moveIssue(issueKey:string, toState:string, beforeIssueKey:string) {
    //    let issue:IssueData = this._allIssues.forKey(issueKey);
    //    if (!issue) {
    //        return;
    //    }
    //
    //    let project:BoardProject = this._boardData.boardProjects.forKey(issue.projectCode);
    //    let affectedStateIndices:number[] =
    //        project.moveIssueAndGetAffectedStateIndices(this._projects, issue, toState, beforeIssueKey);
    //
    //    if (!this._swimlane) {
    //        for (let stateIndex of affectedStateIndices) {
    //            let counter:StateIssueCounter = new StateIssueCounter();
    //            let stateColumn:IssueData[] = this.createIssueTableStateColumn(stateIndex, counter);
    //            this._issueTable[stateIndex] = stateColumn;
    //            this._totalIssuesByState[stateIndex] = counter.count;
    //        }
    //    } else {
    //        let indexer:SwimlaneIndexer = this.createSwimlaneIndexer();
    //        for (let stateIndex of affectedStateIndices) {
    //            //Reset the state column of the swimlane table
    //            for (let data of this._swimlaneTable) {
    //                data.resetState(stateIndex);
    //            }
    //            let counter:StateIssueCounter = new StateIssueCounter();
    //            this.createSwimlaneTableStateColumn(indexer, this._swimlaneTable, stateIndex, counter);
    //            this._totalIssuesByState[stateIndex] = counter.count;
    //        }
    //    }
    //}

    private internalFullRefresh(input:any, initial:boolean) {
        let storedSwimlaneVisibilities:IMap<boolean> = this.storeSwimlaneVisibilities(initial);

        this._allIssues = new Indexed<IssueData>();
        this._allIssues.indexMap(
            input.issues,
            (key, data) => {
                return new IssueData(this._boardData, data);
            });
        this.createTable();

        this.restoreSwimlaneVisibilities(storedSwimlaneVisibilities);
    }

    private storeSwimlaneVisibilities(initial:boolean) : IMap<boolean> {
        let swimlaneVisibilities:IMap<boolean>;
        if (!initial && this._swimlane && this._swimlaneTable) {
            //Store the visibilities from the users collapsing swimlanes
            swimlaneVisibilities = {};
            for (let swimlane of this._swimlaneTable) {
                swimlaneVisibilities[swimlane.name] = swimlane.visible;
            }
        }
        return swimlaneVisibilities;
    }

    private restoreSwimlaneVisibilities(storedSwimlaneVisibilities:IMap<boolean>) {
        if (storedSwimlaneVisibilities) {
            //Restore the user defined visibilities
            for (let swimlane of this._swimlaneTable) {
                swimlane.restoreVisibility(storedSwimlaneVisibilities);
            }
        }
    }

    private createTable() {
        if (this._swimlane) {
            this._swimlaneTable = this.createSwimlaneTable();
            this._issueTable = null;
        } else {
            this._issueTable = this.createIssueTable();
            this._swimlaneTable = null;
        }
    }

    private createIssueTable() : IssueData[][] {
        let numStates = this._boardData.boardStates.length;
        this._totalIssuesByState = [numStates];
        let issueTable:IssueData[][] = new Array<IssueData[]>(numStates);

        //Now copy across the issues for each project for each state
        for (let stateIndex:number = 0 ; stateIndex < issueTable.length ; stateIndex++) {
            let counter:StateIssueCounter = new StateIssueCounter();
            let stateColumn = this.createIssueTableStateColumn(stateIndex, counter);
            this._totalIssuesByState[stateIndex] = counter.count;
            issueTable[stateIndex] = stateColumn;
        }
        return issueTable;
    }

    private createIssueTableStateColumn(stateIndex:number, counter:StateIssueCounter) : IssueData[] {
        let stateColumn:IssueData[] = [];
        for (let project of this._boardData.boardProjects.array) {
            let projectIssues:string[][] = project.issueKeys;
            let issueKeysForState:string[] = projectIssues[stateIndex];

            for (let index:number = 0; index < issueKeysForState.length; index++) {
                let issue:IssueData = this._allIssues.forKey(issueKeysForState[index]);
                stateColumn.push(issue);
                counter.increment();
                issue.filtered = this._filters.filterIssue(issue);
            }
        }
        return stateColumn;
    }

    private createSwimlaneTable() : SwimlaneData[] {
        let numStates = this._boardData.boardStates.length;
        this._totalIssuesByState = [numStates];
        let indexer:SwimlaneIndexer = this.createSwimlaneIndexer();
        let swimlaneTable:SwimlaneData[] = indexer.swimlaneTable;

        //Now copy across the issues for each project for each state
        for (let stateIndex:number = 0 ; stateIndex < this._boardData.boardStates.length ; stateIndex++) {
            let counter:StateIssueCounter = new StateIssueCounter();
            this.createSwimlaneTableStateColumn(indexer, swimlaneTable, stateIndex, counter);
            this._totalIssuesByState[stateIndex] = counter.count;
        }
        //Apply the filters to the swimlanes
        for (let swimlaneData of swimlaneTable) {
            swimlaneData.filtered = indexer.filter(swimlaneData);
        }
        return swimlaneTable;
    }

    private createSwimlaneTableStateColumn(indexer:SwimlaneIndexer, swimlaneTable:SwimlaneData[], stateIndex:number, counter:StateIssueCounter) {

        for (let project of this._boardData.boardProjects.array) {
            let projectIssues:string[][] = project.issueKeys;
            let issueKeysForState:string[] = projectIssues[stateIndex];

            for (let index:number = 0; index < issueKeysForState.length; index++) {
                let issue:IssueData = this._allIssues.forKey(issueKeysForState[index]);
                let swimlaneIndex = indexer.swimlaneIndex(issue);
                issue.filtered = this._filters.filterIssue(issue);
                let targetSwimlane:SwimlaneData = swimlaneTable[swimlaneIndex];
                targetSwimlane.issueTable[stateIndex].push(issue);
                counter.increment();
            }
        }
    }

    private createSwimlaneIndexer():SwimlaneIndexer {
        return new SwimlaneIndexerFactory().createSwimlaneIndexer(this._swimlane, this._filters, this._boardData);
    }
}


export class SwimlaneData {
    private _name:string;
    public issueTable:IssueData[][];
    private _visible:boolean = true;
    public filtered:boolean;
    private _index:number;

    constructor(private boardData:BoardData, name:string, index:number) {
        this._name = name;
        this._index = index;
        let states = boardData.boardStates.length;
        this.issueTable = new Array<IssueData[]>(states);
        for (let i:number = 0 ; i < states ; i++) {
            this.issueTable[i] = [];
        }
    }

    toggleVisibility() : void {
        this._visible = !this._visible;
    }

    get visible() {
        return this._visible;
    }

    get name() {
        return this._name;
    }

    get index() {
        return this._index;
    }

    resetState(stateIndex:number) {
        this.issueTable[stateIndex] = [];
    }

    restoreVisibility(savedVisibilities:IMap<boolean>) {
        //When restoring the visibility, take into account that new swimlanes would not have been saved,
        //and so do not appear in the map
        this._visible = !(savedVisibilities[this._name] == false);
    }
}

class StateIssueCounter {
    private _count:number = 0;

    increment() {
        this._count++;
    }

    get count():number {
        return this._count;
    }
}
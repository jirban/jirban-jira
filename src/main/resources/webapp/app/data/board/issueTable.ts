import {Projects, BoardProject} from "./project";
import {BoardData} from "./boardData";
import {IssueData} from "./issueData";
import {IMap} from "../../common/map";
import {BoardFilters} from "./boardFilters";
import {SwimlaneIndexer, SwimlaneIndexerFactory} from "./swimlaneIndexer";
import {Indexed} from "../../common/indexed";
import {ChangeSet, RankChange} from "./change";
import {Subject, Observable} from "rxjs/Rx";

export class IssueTable {
    private _allIssues:Indexed<IssueData>;
    private _issueTable:IssueData[][];
    private _swimlaneTable:SwimlaneData[];
    private _totalIssuesByState:number[];

    private _swimlaneVisibitilySubject:Subject<void> = new Subject<void>();

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
        this._swimlaneVisibitilySubject.next(null);
    }

    get swimlaneVisibilityObservable():Observable<void> {
        return this._swimlaneVisibitilySubject;
    }

    toggleSwimlaneVisibility(swimlaneIndex:number) {
        if (this._swimlaneTable) {
            this._swimlaneTable[swimlaneIndex].toggleVisibility();
        }
        this._swimlaneVisibitilySubject.next(null);
    }

    getIssue(issueKey:string) : IssueData {
        return this._allIssues.forKey(issueKey);
    }

    processTableChanges(boardData:BoardData, changeSet:ChangeSet) {
        let storedSwimlaneVisibilities:IMap<boolean> = this.storeSwimlaneVisibilities(false);

        //Delete from the "all issues table"
        let deletedIssues:IssueData[] = this._allIssues.deleteKeys(changeSet.deletedIssueKeys);

        if (changeSet.issueUpdates) {
            for (let update of changeSet.issueUpdates) {
                let issue = this._allIssues.forKey(update.key);
                if (!issue) {
                    console.log("Could not find issue to update " + update.key);
                    continue;
                }
            }
        }

        //Delete all the deleted issues from the project issue tables
        //This also includes all the issues that have been moved
        this._projects.deleteIssues(deletedIssues);



        //Now do the actual application of the updates
        if (changeSet.issueUpdates) {
            for (let update of changeSet.issueUpdates) {
                let issue = this._allIssues.forKey(update.key);
                if (!issue) {
                    console.log("Could not find issue to update " + update.key);
                    continue;
                }
                issue.applyUpdate(update);
            }
        }
        //Add all the created issues
        if (changeSet.issueAdds) {
            for (let add of changeSet.issueAdds) {
                let issue:IssueData = IssueData.createFromChangeSet(boardData, add);
                this._allIssues.add(issue.key, issue);
            }
        }

        //Now update the issue ranks
        if (changeSet.rankChanges) {
            //let ownerProject:BoardProject = this._projects.ownerProject;
            for (let projectCode in changeSet.rankChanges) {

                let projectRankChanges:RankChange[] = changeSet.rankChanges[projectCode];
                let project:BoardProject = this._projects.boardProjects.forKey(projectCode);
                project.updateIssueRanks(changeSet.rankedIssues, changeSet.rankChanges[projectCode]);
            }
        }

        this.createTable();
        this.restoreSwimlaneVisibilities(storedSwimlaneVisibilities);
    }

    private internalFullRefresh(input:any, initial:boolean) {
        let storedSwimlaneVisibilities:IMap<boolean> = this.storeSwimlaneVisibilities(initial);

        this._allIssues = new Indexed<IssueData>();
        this._allIssues.indexMap(
            input.issues,
            (key, data) => {
                return IssueData.createFullRefresh(this._boardData, data);
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
        let numStates = this._boardData.boardStateNames.length;

        this._totalIssuesByState = new Array<number>(numStates);

        let issueCounters = new Array<StateIssueCounter>(numStates);
        let issueTable:IssueData[][] = new Array<IssueData[]>(numStates);

        //Initialise the states
        for (let stateIndex:number = 0 ; stateIndex < numStates ; stateIndex++) {
            issueTable[stateIndex] = [];
            issueCounters[stateIndex] = new StateIssueCounter();
        }

        for (let boardProject of this._boardData.boardProjects.array) {
            for (let issueKey of boardProject.rankedIssueKeys) {
                let issue:IssueData = this._allIssues.forKey(issueKey);
                let boardStateIndex:number = boardProject.mapStateIndexToBoardIndex(issue.statusIndex);

                let issuesForState:IssueData[] = issueTable[boardStateIndex];
                issuesForState.push(issue);

                let issueCounter:StateIssueCounter = issueCounters[boardStateIndex];
                issueCounter.increment();
                issue.filtered = this._filters.filterIssue(issue);
            }
        }

        for (let stateIndex:number = 0 ; stateIndex < numStates ; stateIndex++) {
            this._totalIssuesByState[stateIndex] = issueCounters[stateIndex].count;
        }

        return issueTable;
    }

    private createSwimlaneTable() : SwimlaneData[] {
        let numStates = this._boardData.boardStateNames.length;

        this._totalIssuesByState = new Array<number>(numStates);

        let indexer:SwimlaneIndexer = this.createSwimlaneIndexer();
        let issueCounters = new Array<StateIssueCounter>(numStates);
        let swimlaneTable:SwimlaneData[] = indexer.swimlaneTable;

        //Initialise the states
        for (let stateIndex:number = 0 ; stateIndex < numStates ; stateIndex++) {
            //The swimlane indexer will take care of initialising its own issue tables
            issueCounters[stateIndex] = new StateIssueCounter();
        }

        for (let boardProject of this._boardData.boardProjects.array) {
            for (let issueKey of boardProject.rankedIssueKeys) {
                let issue:IssueData = this._allIssues.forKey(issueKey);
                let boardStateIndex:number = boardProject.mapStateIndexToBoardIndex(issue.statusIndex);

                let swimlaneIndices:number[] = indexer.swimlaneIndex(issue);
                for (let swimlaneIndex of swimlaneIndices) {
                    let targetSwimlane:SwimlaneData = swimlaneTable[swimlaneIndex];
                    targetSwimlane.issueTable[boardStateIndex].push(issue);

                }

                let issueCounter:StateIssueCounter = issueCounters[boardStateIndex];
                issueCounter.increment();
                issue.filtered = this._filters.filterIssue(issue);
            }
        }

        for (let stateIndex:number = 0 ; stateIndex < numStates ; stateIndex++) {
            this._totalIssuesByState[stateIndex] = issueCounters[stateIndex].count;
        }

        //Apply the filters to the swimlanes
        for (let swimlaneData of swimlaneTable) {
            swimlaneData.filtered = indexer.filter(swimlaneData);
        }

        return swimlaneTable;
    }

    private createSwimlaneIndexer():SwimlaneIndexer {
        return new SwimlaneIndexerFactory().createSwimlaneIndexer(this._swimlane, this._filters, this._boardData);
    }

    createQueryStringParticle():string{
        let qs:string = "";
        if (this._swimlane) {
            qs = "&swimlane=" + this._swimlane;

            let hiddenEntries:SwimlaneData[] = [];
            let visibleEntries:SwimlaneData[] = [];
            for (let sd of this._swimlaneTable) {
                if (sd.visible) {
                    visibleEntries.push(sd);
                } else {
                    hiddenEntries.push(sd);
                }
            }

            if (hiddenEntries.length == 0) {
                return qs;
            }

            let swimlanes:SwimlaneData[];
            if (hiddenEntries.length < visibleEntries.length) {
                qs += "&hidden-sl=";
                swimlanes = hiddenEntries;
            } else {
                qs += "&visible-sl=";
                swimlanes = visibleEntries;
            }

            let first:boolean = true;
            for (let sd of swimlanes) {
                if (first){
                    first = false;
                } else {
                    qs += ",";
                }
                qs += encodeURIComponent(sd.name);
            }
        }
        return qs;
    }

    setSwimlaneVisibilitiesFromQueryParams(queryParams:IMap<string>):void{
        let value:string;
        let visible:boolean = false;
        if (queryParams["hidden-sl"]) {
            value = queryParams["hidden-sl"];
        } else if (queryParams["visible-sl"]) {
            value = queryParams["visible-sl"];
            visible = true;
        }

        if (!value) {
            return;
        }

        let swimlaneNames:IMap<boolean> = {};
        for (let name of value.split(",")) {
            swimlaneNames[decodeURIComponent(name)] = true;
        }

        for (let sd of this._swimlaneTable) {
            if (swimlaneNames[sd.name]) {
                sd.visible = visible;
            } else {
                sd.visible = !visible;
            }
        }
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
        let states = boardData.boardStateNames.length;
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

    set visible(visible:boolean) {
        this._visible = visible;
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
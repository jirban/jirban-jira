import {IssueData} from "./issueData";
import {BoardData} from "./boardData";
import {IMap} from "../../common/map";

export class SwimlaneData {
    private _name:string;
    //TODO make this immutable at some stage
    public _issueTable:IssueData[][];
    private _collapsed:boolean = false;
    public filtered:boolean;
    private _index:number;
    private _empty:boolean;

    constructor(issueTable:IssueData[][], name:string, index:number, empty:boolean) {
        this._name = name;
        this._index = index;
        this._issueTable = issueTable;
        this._empty = empty;
        console.log("Swimlane " + name + " is empty " + empty);
    }

    toggleCollapsedStatus() : void {
        this._collapsed = !this._collapsed;
    }

    get collapsed():boolean {
        return this._collapsed;
    }

    set collapsed(collapsed:boolean) {
        this._collapsed = collapsed;
    }

    get name() {
        return this._name;
    }

    get index() {
        return this._index;
    }

    get issueTable() : IssueData[][] {
        return this._issueTable;
    }

    get empty(): boolean {
        return this._empty;
    }

    restoreCollapsedStatus(savedVisibilities:IMap<boolean>) {
        //When restoring the visibility, take into account that new swimlanes would not have been saved,
        //and so do not appear in the map
        this._collapsed = (savedVisibilities[this._name] == true);
    }
}

export class SwimlaneDataBuilder {
    private _name:string;
    private _issueTable:IssueData[][];
    private _index:number;
    private _empty:boolean = true;

    constructor(boardData:BoardData, name:string, index:number) {
        this._name = name;
        this._index = index;
        let states = boardData.boardStateNames.length;
        this._issueTable = new Array<IssueData[]>(states);
        for (let i:number = 0 ; i < states ; i++) {
            this._issueTable[i] = [];
        }
    }

    get name() {
        return this._name;
    }

    get index() {
        return this._index;
    }

    addIssue(index:number, issueData:IssueData) {
        this._issueTable[index].push(issueData);
        this._empty = false;
    }

    build() : SwimlaneData {
        return new SwimlaneData(this._issueTable, this._name, this._index, this._empty);
    }

}
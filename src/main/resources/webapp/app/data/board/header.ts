import {Indexed} from "../../common/indexed";
import {isNumber} from "angular2/src/facade/lang";
import {IMap} from "../../common/map";
import {BoardData} from "./boardData";
/**
 * Support for categorised state headers
 */
export class BoardHeaders {

    private _boardData:BoardData;
    private _boardStates:Indexed<string> = new Indexed<string>();
    private _topHeaders:BoardHeaderEntry[] = [];
    private _bottomHeaders:BoardHeaderEntry[] = [];

    private _stateVisibilities:boolean[];

    constructor(boardData:BoardData, boardStates:Indexed<string>, topHeaders:BoardHeaderEntry[], bottomHeaders:BoardHeaderEntry[], stateVisibilities:boolean[]) {
        this._boardData = boardData;
        this._boardStates = boardStates;
        this._topHeaders = topHeaders;
        this._bottomHeaders = bottomHeaders;

        this._stateVisibilities = stateVisibilities;
    }

    get categorised():boolean {
        return this._bottomHeaders.length > 0;
    }

    get topHeaders():BoardHeaderEntry[] {
        return this._topHeaders;
    }

    get bottomHeaders():BoardHeaderEntry[] {
        return this._bottomHeaders;
    }

    get boardStates():Indexed<string> {
        return this._boardStates;
    }
    get stateVisibilities():boolean[] {
        return this._stateVisibilities;
    }

    toggleHeaderVisibility(header:BoardHeaderEntry) {
        header.toggleVisibility();
    }

    static deserialize(boardData:BoardData, input:any):BoardHeaders {

        let boardStates:Indexed<string> = new Indexed<string>();
        let headers:string[] = input.headers;

        let indexedStates:State[] = [];
        let categories:Indexed<StateCategory> = new Indexed<StateCategory>();
        let index = 0;
        for (let state of input.states) {
            boardStates.add(state.name, state.name);

            let backlogState:boolean = index < boardData.backlogSize;

            let category:StateCategory;
            if (isNumber(state.header)) {
                let header:string = headers[state.header];
                category = BoardHeaders.getOrCreateStateCategory(categories, header, false);
            } else if (backlogState && boardData.backlogSize > 1) {
                category = BoardHeaders.getOrCreateStateCategory(categories, "Backlog", true);
            }
            let stateEntry = new State(boardData, state.name, indexedStates.length, backlogState, category);
            indexedStates.push(stateEntry);
            if (category) {
                category.states.push(stateEntry);
            }
            index++;
        }

        let stateVisibilities:boolean[];
        if (boardData && boardData.headers) {
            stateVisibilities = boardData.headers._stateVisibilities;
        } else {
            stateVisibilities = new Array<boolean>(boardStates.array.length);
            for (let i:number = 0 ; i < stateVisibilities.length ; i++) {
                stateVisibilities[i] = i >= boardData.backlogSize;
            }
        }


        let topHeaders:BoardHeaderEntry[] = [];
        let bottomHeaders:BoardHeaderEntry[] = [];
        let addedCategories:IMap<boolean> = {};
        for (let i:number = 0 ; i < indexedStates.length ; i++) {
            let indexedState:State = indexedStates[i];
            if (!indexedState.category) {
                topHeaders.push(new StateHeaderEntry(indexedState, stateVisibilities, 1, 2));
            } else {
                if (!addedCategories[indexedState.category.name]) {
                    topHeaders.push(new CategoryHeaderEntry(indexedState.category, stateVisibilities, indexedState.category.states.length, 1));
                    addedCategories[indexedState.category.name] = true;
                }
                bottomHeaders.push(new StateHeaderEntry(indexedState, stateVisibilities, 1, 1));
            }
        }

        return new BoardHeaders(boardData, boardStates, topHeaders, bottomHeaders, stateVisibilities);
    }

    static getOrCreateStateCategory(categories:Indexed<StateCategory>, header:string, backlog:boolean):StateCategory {
        let category:StateCategory = categories.forKey(header);
        if (!category) {
            category = new StateCategory(header, backlog);
            categories.add(header, category);
        }
        return category;
    }
}

class StateCategory {
    private _name:string;
    private _backlog:boolean;
    private _states:State[] = [];

    constructor(name:string, backlog:boolean) {
        this._name = name;
        this._backlog = backlog;
    }

    get name():string {
        return this._name;
    }

    get states():State[] {
        return this._states;
    }

    get totalIssues():number {
        let total:number = 0;
        for (let state of this._states) {
            total += state.totalIssues;
        }
        return total;
    }

    get backlog():boolean {
        return this._backlog;
    }

    isVisible(stateVisibilities:boolean[]):boolean {
        for (let state of this._states) {
            if (state.isVisible(stateVisibilities)) {
                return true;
            }
        }
        return false;
    }

    toggleVisibility(stateVisibilities:boolean[]) {
        //We set all the state visibilities to false. However, if they were all false, we set them all to true.
        let visible:boolean = false;
        for (let state of this._states) {
            let visibility:boolean = stateVisibilities[state.index];
            if (visibility) {
                visible = true;
            }
            stateVisibilities[state.index] = false;
        }

        if (!visible) {

            for (let state of this._states) {
                stateVisibilities[state.index] = true;
            }
        }
    }
}

class State {

    constructor(private _boardData:BoardData, private _name:string, private _index:number,
                private _backlog:boolean, private _category:StateCategory) {
    }

    get name():string {
        return this._name;
    }

    get category():StateCategory {
        return this._category;
    }

    get totalIssues():number {
        return this._boardData.totalIssuesByState[this._index];
    }

    get index():number {
        return this._index;
    }

    get backlog():boolean {
        return this._backlog;
    }

    isVisible(stateVisibilities:boolean[]):boolean{
        return stateVisibilities[this._index];
    }

    toggleVisibility(stateVisibilities:boolean[]) {
        stateVisibilities[this._index] = !stateVisibilities[this._index];
    }
}

export abstract class BoardHeaderEntry {
    protected _stateVisibilities:boolean[];
    private _cols:number;
    protected _rows:number;

    constructor(stateVisibilities:boolean[], cols:number, rows:number) {
        this._stateVisibilities = stateVisibilities;
        this._cols = cols;
        this._rows = rows;
    }

    get cols():number {
        return this._cols;
    }

    get rows():number {
        return this._rows;
    }

    get stateAndCategory():boolean {
        return false;
    }

    get name():string {
        //Abstract getters don't exist :(
        throw new Error("nyi");
    }

    get totalIssues():number {
        //Abstract getters don't exist :(
        throw new Error("nyi");
    }

    get visible():boolean {
        //Abstract getters don't exist :(
        throw new Error("nyi");
    }

    get backlog():boolean {
        //Abstract getters don't exist :(
        throw new Error("nyi");
    }

    abstract toggleVisibility();
}

class CategoryHeaderEntry extends BoardHeaderEntry {
    constructor(private _category:StateCategory, stateVisibilities:boolean[], cols:number, rows:number) {
        super(stateVisibilities, cols, rows);
    }

    get name():string {
        return this._category.name;
    }

    get totalIssues() : number {
        return this._category.totalIssues;
    }

    get visible():boolean {
        return this._category.isVisible(this._stateVisibilities);
    }

    get backlog():boolean{
        return this._category.backlog;
    }

    toggleVisibility() {
        this._category.toggleVisibility(this._stateVisibilities);
    }

}

class StateHeaderEntry extends BoardHeaderEntry {
    constructor(private _state:State, stateVisibilities:boolean[], cols:number, rows:number) {
        super(stateVisibilities, cols, rows);
    }

    get name():string {
        return this._state.name;
    }

    get totalIssues() : number {
        return this._state.totalIssues;
    }

    get stateAndCategory():boolean {
        return this._rows == 2;
    }

    get visible():boolean {
        return this._state.isVisible(this._stateVisibilities);
    }

    get backlog():boolean{
        return this._state.backlog;
    }

    toggleVisibility() {
        this._state.toggleVisibility(this._stateVisibilities);
    }
}
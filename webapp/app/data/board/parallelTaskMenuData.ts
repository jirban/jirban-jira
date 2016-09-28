import {IssueData} from "./issueData";
/**
 * The event raised to show the issue context menu
 *
 */
export class ParallelTaskMenuData {
    constructor(private _issue:IssueData,
                private _taskCode:string,
                private _x:number,
                private _y:number) {
    }

    get issue(): IssueData {
        return this._issue;
    }

    get taskCode(): string {
        return this._taskCode;
    }

    get x():number {
        return this._x;
    }

    get y():number {
        return this._y;
    }
}
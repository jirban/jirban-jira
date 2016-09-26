/**
 * The event raised to show the issue context menu
 *
 */
export class IssueContextMenuData {
    constructor(private _issueKey:string,
                private _x:number,
                private _y:number) {
    }

    get issueKey():string {
        return this._issueKey;
    }

    get x():number {
        return this._x;
    }

    get y():number {
        return this._y;
    }
}
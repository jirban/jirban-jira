/**
 * The event raised to show the issue context menu
 *
 */
export class IssueContextMenuData {
    private _x:string;
    private _y:string;
    constructor(private _issueKey:string,
                x:number,
                y:number) {
        this._x = x + "px";
        this._y = y + "px";
    }

    get issueKey():string {
        return this._issueKey;
    }

    get x():string {
        return this._x;
    }

    get y():string {
        return this._y;
    }
}
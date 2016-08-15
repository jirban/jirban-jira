export class AccessLogEntry {
    private _userKey:string;
    private _userName:string;
    private _board:string;
    private _time:string;

    constructor(userKey:string, userName:string, board:string, time:string) {
        this._userKey = userKey;
        this._userName = userName;
        this._board = board;
        this._time = time;
    }

    get userKey():string {
        return this._userKey;
    }

    get userName():string {
        return this._userName;
    }

    get board():string {
        return this._board;
    }

    get time():string {
        return this._time;
    }

    static deserialize(input:any):AccessLogEntry[]{
        let entries:AccessLogEntry[] = [];
        for (let index in input) {
            let entry:any = input[index];
            let userDetails:any = entry["user"];

            entries.push(
                new AccessLogEntry(
                    userDetails["key"],
                    userDetails["name"],
                    entry["board"],
                    entry["time"]));
        }

        return entries;
    }
}
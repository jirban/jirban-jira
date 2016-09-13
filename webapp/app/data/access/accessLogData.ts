import {Indexed} from "../../common/indexed";

export class AccessLogData {
    private _userAccessSummaries:UserAccessSummary[]
    private _entries:AccessLogEntry[];
    private _firstTime:string;
    private _lastTime:string;

    constructor(userAccessSummaries:UserAccessSummary[], entries:AccessLogEntry[], firstTime:string, lastTime:string) {
        this._userAccessSummaries = userAccessSummaries;
        this._entries = entries;
        this._firstTime = firstTime;
        this._lastTime = lastTime;
    }

    get userAccessSummaries(): UserAccessSummary[] {
        return this._userAccessSummaries;
    }

    get entries(): AccessLogEntry[] {
        return this._entries;
    }

    get firstTime(): string {
        return this._firstTime;
    }

    get lastTime(): string {
        return this._lastTime;
    }

    static deserialize(input:any):AccessLogData {
        let userAccesses:Indexed<UserAccessSummary> = new Indexed<UserAccessSummary>();

        let entries:AccessLogEntry[] = [];
        for (let index in input) {
            let entry:any = input[index];
            let userDetails:any = entry["user"];

            let userKey:string = userDetails["key"];
            let accessSummary:UserAccessSummary = userAccesses.forKey(userKey);
            if (!accessSummary) {
                accessSummary = new UserAccessSummary(new User(userKey, userDetails["name"]), 1);
                userAccesses.add(userKey, accessSummary);
            } else {
                accessSummary.increment();
            }

            let alEntry:AccessLogEntry =
                new AccessLogEntry(accessSummary.user, entry["board"], entry["time"]);
            entries.push(alEntry);
        }

        userAccesses.array.sort((u1:UserAccessSummary, u2:UserAccessSummary) => {return u1.userName.localeCompare(u2.userName)});

        let firstTime:string;
        let lastTime:string;
        if (entries.length > 0) {
            firstTime = entries[0].time;
            lastTime = entries[entries.length - 1].time;
        }
        return new AccessLogData(userAccesses.array, entries, firstTime, lastTime);
    }
}

export class AccessLogEntry {
    private _user:User;
    private _board:string;
    private _time:string;

    constructor(user:User, board:string, time:string) {
        this._user = user;
        this._board = board;
        this._time = time;
    }

    get userKey():string {
        return this._user.key;
    }

    get userName():string {
        return this._user.name;
    }

    get board():string {
        return this._board;
    }

    get time():string {
        return this._time;
    }
}

class User {
    private _key:string;
    private _name:string;

    constructor(key:string, name:string) {
        this._key = key;
        this._name = name;
    }

    get key(): string {
        return this._key;
    }

    get name(): string {
        return this._name;
    }
}

export class UserAccessSummary {
    private _user:User;
    private _count:number;

    constructor(user:User, count:number) {
        this._user = user;
        this._count = count;
    }

    get userKey():string {
        return this._user.key;
    }

    get userName():string {
        return this._user.name;
    }

    get count(): number {
        return this._count;
    }

    get user(): User {
        return this._user;
    }

    increment():void {
        this._count++;
    }
}
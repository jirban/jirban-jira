export class ChangeSet {
    private _view:number;

    private _issueAdds:IssueAdd[];
    private _issueUpdates:IssueUpdate[];
    private _issueDeletes:IssueDelete[];

    private _blacklistStates:string[];
    private _blacklistTypes:string[];
    private _blacklistPriorities:string[];
    private _blacklistIssues:string[];
    private _blacklistClearedIssues:string[];

    constructor(input:any) {
        let changes:any = input.changes;
        this._view = changes.view;

        let issues:any = changes.issues;
        if (issues) {
            let newIssues:any[] = issues["new"];
            let updatedIssues:any[] = issues.update;
            let deletedIssues:any[] = issues.delete;
            if (newIssues) {
                this._issueAdds = new IssueAdd[newIssues.length];
                for (let i:number = 0 ; i < newIssues.length ; i++) {
                    this._issueAdds[i] = IssueAdd.deserialize(newIssues[i]);
                }
            }
            if (updatedIssues) {
                this._issueUpdates = new IssueUpdate[updatedIssues.length];
                for (let i:number = 0 ; i < updatedIssues.length ; i++) {
                    this._issueUpdates[i] = IssueUpdate.deserialize(newIssues[i]);
                }
            }
            if (deletedIssues) {
                this._issueDeletes = new IssueDelete[deletedIssues.length];
                for (let i:number = 0 ; i < deletedIssues.length ; i++) {
                    this._issueDeletes[i] = IssueDelete.deserialize(deletedIssues[i]);
                }
            }
        }

        let blacklist:any = changes.blacklist;
        if (blacklist) {
            if (blacklist.states) {
                this._blacklistStates = blacklist.states;
            }
            if (blacklist["issue-types"]) {
                this._blacklistTypes = blacklist["issue-types"];
            }
            if (blacklist.priorities) {
                this._blacklistPriorities = blacklist.priorities;
            }
            if (blacklist.issues) {
                this._blacklistIssues = blacklist.issues;
            }
            if (blacklist["removed-issues"]) {
                this._blacklistClearedIssues = blacklist["removed-issues"];
            }
        }
    }


    get view():number {
        return this._view;
    }

    get issueAdds():IssueAdd[] {
        return this._issueAdds;
    }

    get issueUpdates():IssueUpdate[] {
        return this._issueUpdates;
    }

    get issueDeletes():IssueDelete[] {
        return this._issueDeletes;
    }

    get blacklistStates():string[] {
        return this._blacklistStates;
    }

    get blacklistTypes():string[] {
        return this._blacklistTypes;
    }

    get blacklistPriorities():string[] {
        return this._blacklistPriorities;
    }

    get blacklistIssues():string[] {
        return this._blacklistIssues;
    }

    get blacklistClearedIssues():string[] {
        return this._blacklistClearedIssues;
    }

    get blacklistChanges() : boolean {
        if (this._blacklistClearedIssues || this._blacklistIssues || this._blacklistPriorities ||
                this._blacklistStates || this._blacklistTypes ) {
            return true;
        }
        return false;
    }
}

export abstract class IssueChange {
    private _key:string;

    constructor(key:string) {
        this._key = key;
    }

    get key():string {
        return this._key;
    }
}

export abstract class IssueDetailChange extends IssueChange {
    private _type:string;
    private _priority:string;
    private _summary:string;
    private _state:string;

    constructor(key:string, type:string, priority:string, summary:string, state:string) {
        super(key);
        this._type = type;
        this._priority = priority;
        this._summary = summary;
        this._state = state;
    }

    get type():string {
        return this._type;
    }

    get priority():string {
        return this._priority;
    }

    get summary():string {
        return this._summary;
    }

    get state():string {
        return this._state;
    }
}

export class IssueAdd extends IssueDetailChange {
    constructor(key:string, type:string, priority:string, summary:string, state:string) {
        super(key, type, priority, summary, state);
    }

    static deserialize(input:any) : IssueAdd {
        //TODO state!!!
        return new IssueAdd(input.key, input.type, input.priority, input.summary, input.state);
    }
}

export class IssueUpdate extends IssueDetailChange {
    constructor(key:string, type:string, priority:string, summary:string, state:string) {
        super(key, type, priority, summary, state);
    }

    static deserialize(input:any) : IssueAdd {
        //TODO state!!!
        return new IssueUpdate(input.key, input.type, input.priority, input.summary, input.state);
    }
}

export class IssueDelete extends IssueChange {
    constructor(key:string) {
        super(key);
    }

    static deserialize(input:any) : IssueDelete {
        return new IssueDelete(input.key);
    }
}
import {Assignee} from "./assignee";
import {AssigneeDeserializer} from "./assignee";
import {Indexed} from "../../common/indexed";
import {BlacklistData} from "./blacklist";
export class ChangeSet {
    private _view:number;

    private _issueAdds:IssueChange[];
    private _issueUpdates:IssueChange[];
    private _issueDeletes:string[];

    private _addedAssignees:Assignee[];

    private _blacklistChange:BlacklistData;
    private _blacklistClearedIssues:string[];

    private _stateChanges:Indexed<Indexed<string[]>>;


    constructor(input:any) {
        let changes:any = input.changes;
        this._view = changes.view;

        let issues:any = changes.issues;
        if (issues) {
            let newIssues:any[] = issues["new"];
            let updatedIssues:any[] = issues.update;
            let deletedIssues:any[] = issues.delete;
            if (newIssues) {
                this._issueAdds = new Array<IssueChange>(newIssues.length);
                for (let i:number = 0 ; i < newIssues.length ; i++) {
                    this._issueAdds[i] = IssueChange.deserializeAdd(newIssues[i]);
                }
            }
            if (updatedIssues) {
                this._issueUpdates = new Array<IssueChange>(updatedIssues.length);
                for (let i:number = 0 ; i < updatedIssues.length ; i++) {
                    this._issueUpdates[i] = IssueChange.deserializeUpdate(updatedIssues[i]);
                }
            }
            if (deletedIssues) {
                this._issueDeletes = deletedIssues;
            }
        }

        if (changes.assignees) {
            this._addedAssignees = new AssigneeDeserializer().deserialize(changes).array;
        }

        let blacklist:any = changes.blacklist;
        if (blacklist) {
            this._blacklistChange = BlacklistData.fromInput(blacklist);
            if (blacklist["removed-issues"]) {
                this._blacklistClearedIssues = blacklist["removed-issues"];
            }
        }

        let stateChanges:any = changes.states;
        this._stateChanges = new Indexed<Indexed<string[]>>();
        this._stateChanges.indexMap(stateChanges, (projectCode:string, projectEntry:any) => {
           let projStates:Indexed<string[]> = new Indexed<string[]>();
            projStates.indexMap(projectEntry, (stateName:string, stateEntry:any) => {
                return stateEntry;
            });
            return projStates;
        });
    }


    get view():number {
        return this._view;
    }

    get issueAdds():IssueChange[] {
        return this._issueAdds;
    }

    get issueUpdates():IssueChange[] {
        return this._issueUpdates;
    }

    /**
     * Gets the issues which should be totally removed from the board
     */
    get deletedIssueKeys():string[] {
        let deleteKeys:string[] = [];
        if (this._blacklistChange && this._blacklistChange) {
            deleteKeys = deleteKeys.concat(this._blacklistChange.issues);
        }
        if (this._blacklistClearedIssues) {
            deleteKeys = deleteKeys.concat(this._blacklistClearedIssues);
        }
        if (this._issueDeletes) {
            deleteKeys = deleteKeys.concat(this._issueDeletes);
        }
        return deleteKeys;
    }

    get blacklistChanges() : boolean {
        if (this._blacklistClearedIssues || this._blacklistChange ) {
            return true;
        }
        return false;
    }

    get addedAssignees() : Assignee[] {
        return this._addedAssignees;
    }

    addToBlacklist(blacklist:BlacklistData) {
        blacklist.addChanges(this._blacklistChange);
    }

    get stateChanges():Indexed<Indexed<string[]>> {
        return this._stateChanges;
    }
}

export class IssueChange {
    private _key:string;
    private _type:string;
    private _priority:string;
    private _summary:string;
    private _state:string;
    private _assignee:string;

    constructor(key:string, type:string, priority:string, summary:string, state:string, assignee:string) {
        this._key = key;
        this._type = type;
        this._priority = priority;
        this._summary = summary;
        this._state = state;
        this._assignee = assignee;
    }

    get key():string {
        return this._key;
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

    get assignee():string {
        return this._assignee;
    }

    static deserializeAdd(input:any) : IssueChange {
        //TODO state!!!
        return new IssueChange(input.key, input.type, input.priority, input.summary, input.state, input.assignee);
    }

    static deserializeUpdate(input:any) : IssueChange {
        return new IssueChange(input.key, input.type, input.priority, input.summary, input.state, input.assignee);
    }

    static createDelete(input:any) : IssueChange {
        return null;
    }
}

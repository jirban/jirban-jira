import {Assignee, AssigneeDeserializer} from "./assignee";
import {Indexed} from "../../common/indexed";
import {BlacklistData} from "./blacklist";
import {JiraComponent, ComponentDeserializer} from "./component";
import {CustomFieldDeserializer, CustomFieldValues, CustomFieldValue} from "./customField";
import {IMap} from "../../common/map";
export class ChangeSet {
    private _view:number;

    private _issueAdds:IssueChange[];
    private _issueUpdates:IssueChange[];
    private _issueDeletes:string[];

    private _addedAssignees:Assignee[];
    private _addedComponents:JiraComponent[];
    private _addedCustomFields:Indexed<CustomFieldValues>;

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
        if (changes.components) {
            this._addedComponents = new ComponentDeserializer().deserialize(changes).array;
        }
        if (changes.custom) {
            this._addedCustomFields = new CustomFieldDeserializer().deserialize(changes);
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

    get addedComponents():JiraComponent[] {
        return this._addedComponents;
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
    private _components:string[];
    private _customFields:IMap<CustomFieldValue>;

    //Only set on update
    private _unassigned:boolean = false;
    private _clearedComponents:boolean = false;

    constructor(key:string, type:string, priority:string, summary:string, state:string, assignee:string,
                components:string[], customFields:IMap<CustomFieldValue>) {
        this._key = key;
        this._type = type;
        this._priority = priority;
        this._summary = summary;
        this._state = state;
        this._assignee = assignee;
        this._components = components;
        this._customFields = customFields;
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

    get components():string[] {
        return this._components;
    }

    get unassigned():boolean {
        return this._unassigned;
    }

    get clearedComponents():boolean {
        return this._clearedComponents;
    }

    get customFields():IMap<CustomFieldValue> {
        return this._customFields;
    }

    static deserializeAdd(input:any) : IssueChange {
        return new IssueChange(input.key, input.type, input.priority, input.summary,
            input.state, input.assignee, input.components, input.custom);
    }

    static deserializeUpdate(input:any) : IssueChange {
        // let clearedCustomFields:string[] = [];
        // let custom:Indexed<CustomFieldValue> = new Indexed<CustomFieldValue>();
        // if (input.custom) {
        //     for (let key in input.custom) {
        //         let customValue = input.custom[key];
        //         if (!customValue) {
        //             clearedCustomFields.push(key);
        //         } else {
        //             custom.add(key, new CustomFieldValue(key, customValue));
        //         }
        //     }
        // }

        let change:IssueChange =
            new IssueChange(
                input.key, input.type, input.priority, input.summary,
                input.state, input.assignee, input.components, input.custom);
        if (input.unassigned) {
            change._unassigned = true;
        }
        if (input["clear-components"]) {
            change._clearedComponents = true;
        }
        return change;
    }
}

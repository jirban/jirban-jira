
import {ChangeSet} from "./change";

export class BlacklistData {
    private _states:string[] = [];
    private _issueTypes:string[] = [];
    private _priorities:string[] = [];
    private _issues:string[] = [];

    static fromInput(input:any) : BlacklistData {
        if (!input) {
            return null;
        }
        let bd:BlacklistData = new BlacklistData();
        if (input.states) {
            bd._states = input.states;
        }
        if (input.priorities) {
            bd._priorities = input.priorities;
        }
        if (input["issue-types"]) {
            bd._issueTypes = input["issue-types"];
        }
        if (input.issues) {
            bd._issues = input.issues;
        }
        return bd;
    }

    addChangeSet(changeSet:ChangeSet) {
        changeSet.addToBlacklist(this);
    }

    get states():string[] {
        return this._states;
    }

    get issueTypes():string[] {
        return this._issueTypes;
    }

    get priorities():string[] {
        return this._priorities;
    }

    get issues():string[] {
        return this._issues;
    }

    addChanges(change:BlacklistData):void {
        if (change.issueTypes.length > 0) {
            this._issueTypes = this._issueTypes.concat(change.issueTypes.slice());
        }
        if (change.priorities) {
            this._priorities = this._priorities.concat(change.priorities.slice());
        }
        if (change.states) {
            this._states = this._states.concat(change.states.slice());
        }
        if (change.issues) {
            this._issues = this._issues.concat(change.issues.slice());
        }
    }
}
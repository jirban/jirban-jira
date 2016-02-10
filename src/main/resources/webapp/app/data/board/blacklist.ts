
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
        //Use slice to copy the arrays here to avoid side-effects
        if (changeSet.blacklistStates) {
            this._states = changeSet.blacklistStates.slice();
        }
        if (changeSet.blacklistPriorities) {
            this._priorities = changeSet.blacklistPriorities.slice();
        }
        if (changeSet.blacklistTypes) {
            this._issueTypes = changeSet.blacklistTypes.slice();
        }
        if (changeSet.blacklistIssues) {
            this._issues = changeSet.blacklistIssues.slice();
        }
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
}
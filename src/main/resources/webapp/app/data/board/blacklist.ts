

export class BlacklistData {
    private _states:string[] = [];
    private _issueTypes:string[] = [];
    private _priorities:string[] = [];
    private _issues:string[] = [];
    constructor(input:any) {
        if (!input) {
            return;
        }
        if (input.states) {
            this._states = input.states;
        }
        if (input.priorities) {
            this._priorities = input.priorities;
        }
        if (input["issue-types"]) {
            this._issueTypes = input["issue-types"];
        }
        if (input.issues) {
            this._issues = input.issues;
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
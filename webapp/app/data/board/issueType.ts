import {Indexed} from '../../common/indexed';

export class IssueType {
    private _name:string;
    private _icon:string;

    constructor(name:string, icon:string) {
        this._name = name;
        this._icon = icon;
    }

    get name():string {
        return this._name;
    }

    get icon():string {
        return this._icon;
    }
}

export class IssueTypeDeserializer {
    deserialize(input:any) : Indexed<IssueType> {
        let issueTypes:Indexed<IssueType> = new Indexed<IssueType>();
        issueTypes.indexArray(
            input["issue-types"],
            (entry)=> {
                return new IssueType(entry.name, entry.icon);
            },
            (priority) => {
                return priority.name;
            }
        );
        return issueTypes;
    }
}
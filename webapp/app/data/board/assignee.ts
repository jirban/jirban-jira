import {Indexed} from "../../common/indexed";
import {StringUtils} from "../../common/stringUtils";

export class Assignee {
    //A key suitable for using in the id field of an element
    private readonly _id:string;
    private readonly _key:string;
    private readonly _email:string;
    private readonly _avatar:string;
    private readonly _name:string;
    private readonly _initials:string;

    constructor(id:string, key:string, email:string, avatar:string, name:string) {
        this._id = id;
        this._key = key;
        this._email = email;
        this._avatar = avatar;
        this._name = name;
        this._initials = this.calculateInitials();
    }

    get key():string {
        return this._key;
    }

    get email():string {
        return this._email;
    }

    get avatar():string {
        return this._avatar;
    }

    get name():string {
        return this._name;
    }

    get initials():string {
        return this._initials;
    }

    private calculateInitials():string {
        let name:string = this._name;
        let arr:string[] = name.split(" ");
        if (arr.length == 1) {
            let ret:string = "";
            for (let i:number = 0; i < 3 && i < arr[0].length; i++) {
                let char:string = arr[0][i];
                if (i == 0) {
                    char = char.toUpperCase();
                } else {
                    char = char.toLowerCase();
                }
                ret = ret + char;
            }
            return ret;
        }
        let ret:string = "";
        for (let i:number = 0; i < 3 && i < arr.length; i++) {
            ret = ret + arr[i][0];
        }
        return ret.toUpperCase();
    }
}

export class AssigneeDeserializer {

    deserialize(input:any) : Indexed<Assignee> {
        let assignees:Indexed<Assignee> = new Indexed<Assignee>();
        assignees.indexArray(
            input.assignees,
            (entry)=> {
                let id:string = StringUtils.makeValidKey(entry.key);
                return new Assignee(id, entry.key, entry.email, entry.avatar, entry.name);
            },
            (assignee) => {
                return assignee.key;
            }
        );
        return assignees;
    }
}


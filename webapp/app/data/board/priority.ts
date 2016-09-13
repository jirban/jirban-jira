import {Indexed} from '../../common/indexed';

export class Priority {
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

export class PriorityDeserializer {
    deserialize(input:any) : Indexed<Priority> {
        let priorities:Indexed<Priority> = new Indexed<Priority>();
        priorities.indexArray(
            input.priorities,
            (entry)=> {
                return new Priority(entry.name, entry.icon);
            },
            (priority) => {
                return priority.name;
            }
        );
        return priorities;
    }
}


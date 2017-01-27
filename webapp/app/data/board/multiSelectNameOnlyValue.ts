import {Indexed} from "../../common/indexed";
import {StringUtils} from "../../common/stringUtils";

export class JiraMultiSelectFieldValue {
    //A key suitable for using in the id field of an element
    private readonly _id:string;
    private readonly _name:string;

    protected constructor(id:string, name:string) {
        this._id = id;
        this._name = name;
    }

    get id():string {
        return this._id;
    }
    get name():string {
        return this._name;
    }
}

export class JiraComponent extends JiraMultiSelectFieldValue {

    private constructor(id:string, name:string) {
        super(id, name);
    }

    static deserialize(input:any) : Indexed<JiraComponent> {
        return deserializeValues(input, "components", (key, name) => {return new JiraComponent(key, name)});

    }
}

export class JiraLabel extends JiraMultiSelectFieldValue {

    private constructor(id:string, name:string) {
        super(id, name);
    }

    static deserialize(input:any) : Indexed<JiraLabel> {
        return deserializeValues(input, "labels", (key, name) => {return new JiraLabel(key, name)});
    }
}

export class JiraFixVersion extends JiraMultiSelectFieldValue {

    private constructor(id:string, name:string) {
        super(id, name);
    }

    static deserialize(input:any) : Indexed<JiraLabel> {
        return deserializeValues(input, "fix-versions", (key, name) => {return new JiraFixVersion(key, name)});
    }
}

function deserializeValues<T extends JiraMultiSelectFieldValue>(input:any, valueName:string, factory:(key:string, name:string)=>T) : Indexed<JiraMultiSelectFieldValue>{
    let values:Indexed<T> = new Indexed<T>();
    values.indexArray(
        input[valueName],
        (name)=> {
            let key:string = StringUtils.makeValidKey(name);
            return factory(key, name);
        },
        (value) => {
            return value.name;
        }
    );
    return values;
}


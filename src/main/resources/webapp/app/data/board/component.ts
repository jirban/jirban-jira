import {Indexed} from "../../common/indexed";

export class Component {
    private _name:string;

    constructor(name:string) {
        this._name = name;
    }

    get name():string {
        return this._name;
    }
}

export class ComponentDeserializer {

    deserialize(input:any) : Indexed<Component> {
        let components:Indexed<Component> = new Indexed<Component>();
        components.indexArray(
            input.components,
            (entry)=> {
                return new Component(entry);
            },
            (component) => {
                return component.name;
            }
        );
        return components;
    }
}
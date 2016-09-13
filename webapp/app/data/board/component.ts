import {Indexed} from "../../common/indexed";

export class JiraComponent {
    private _name:string;

    constructor(name:string) {
        this._name = name;
    }

    get name():string {
        return this._name;
    }
}

export class ComponentDeserializer {

    deserialize(input:any) : Indexed<JiraComponent> {
        let components:Indexed<JiraComponent> = new Indexed<JiraComponent>();
        components.indexArray(
            input.components,
            (entry)=> {
                return new JiraComponent(entry);
            },
            (component) => {
                return component.name;
            }
        );
        return components;
    }
}
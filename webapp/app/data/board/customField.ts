import {Indexed} from "../../common/indexed";
export class CustomFieldValues {
    private _name:string;
    private _values:Indexed<CustomFieldValue>;

    constructor(name:string, values:Indexed<CustomFieldValue>) {
        this._name = name;
        this._values = values;
    }

    get name():string {
        return this._name;
    }

    get values():Indexed<CustomFieldValue> {
        return this._values;
    }
}

export class CustomFieldDeserializer {
    deserialize(input:any):Indexed<CustomFieldValues> {
        let indexedValues:Indexed<CustomFieldValues> = new Indexed<CustomFieldValues>();
        let custom:any = input["custom"];
        if (custom) {
            for (let name in custom) {
                let values:Indexed<CustomFieldValue> = new Indexed<CustomFieldValue>();
                values.indexArray(
                    custom[name],
                    (entry) => {
                        return new CustomFieldValue(entry["key"], entry["value"])
                    },
                    (value) => {
                        return value.key;
                    });
                indexedValues.add(name, new CustomFieldValues(name, values));
            }
        }
        return indexedValues;
    }
}

export class CustomFieldValue {
    private _key:string;
    private _displayValue:string;

    constructor(key:string, displayValue:string) {
        this._key = key;
        this._displayValue = displayValue;
    }

    get key():string {
        return this._key;
    }

    get displayValue():string {
        return this._displayValue;
    }
}

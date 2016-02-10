import {IMap} from './map';

/**
 * Container for an array, and a lookup of the array index by key
 */
export class Indexed<T> {
    private _array:T[] = []
    private _indices:IMap<number> = {};

    /**
     * Creates an index where the input is an array of entries
     * @param input the array input
     * @param factory function to create the entries of type T
     * @param keyValue function to get the key to index by
     */
    indexArray(input:any, factory:(entry:any)=>T, keyValue:(t:T)=>string) {
        let i = 0;
        for (let entry of input) {
            let value:T = factory(entry);
            let key:string = keyValue(value);
            this._array.push(value);
            this._indices[key] = i++;
        }
    }

    /**
     * Creates an index where the input is a map of entries
     * @param input the array input
     * @param factory function to create the entries of type T
     * @param keyValue function to get the key to index by
     */
    indexMap(input:any, factory:(key:string, entry:any)=>T) {
        let i = 0;
        for (let key in input) {
            let value:T = factory(key, input[key]);
            this._array.push(value);
            this._indices[key] = i++;
        }
    }

    forKey(key:string) : T {
        let index:number = this._indices[key];
        if (isNaN(index)) {
            return null;
        }
        return this._array[index];
    }

    forIndex(index:number) : T {
        return this._array[index];
    }

    /**
     * Deletes the entries with the selected keys
     * @param keys the keys to remove
     */
    deleteKeys(keys:string[]) {
        let deleted:string[] = [];
        let indices:number[] = [];
        for (let key of keys) {
            let index:number = this._indices[key];
            if (index) {
                delete this._indices[key];
                indices.push(index);
                keys.push(key);
            }
        }

        if (deleted.length > 0) {
            indices.sort();
            for (let i:number = indices.length - 1; i >= 0; i--) {
                this.array.splice(indices[i], 1);
            }
            return deleted;
        }
    }

    get array():T[] {
        return this._array;
    }

    get indices():IMap<number> {
        return this._indices;
    }
}

class KeyAndIndex {
    index:number;
    key:string;

    constructor(index:number, key:string) {
        this.index = index;
        this.key = key;
    }
}
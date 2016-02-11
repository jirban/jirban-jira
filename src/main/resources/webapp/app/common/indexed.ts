import {IMap} from './map';
import {isNumber} from "angular2/src/facade/lang";

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
     * @return the entries that were deleted
     */
    deleteKeys(keys:string[]) : T[] {
        if (keys.length == 0) {
            return [];
        }
        //Map the indices to keys
        let indicesToKeys:string[] = new Array<string>(this.array.length);
        for (let key in this._indices) {
            let index:number = this._indices[key];
            indicesToKeys[index] = key;
        }

        //Index the keys we want to delete
        let deleteKeys:IMap<boolean> = {};
        for (let key of keys) {
            deleteKeys[key] = true;
        }

        let deleted:T[] = [];
        let newArray:T[] = [];
        let newIndices:IMap<number> = {};
        let currentIndex:number = 0;
        for (let i:number = 0 ; i < this._array.length ; i++) {
            let key:string = indicesToKeys[i];
            if (deleteKeys[key]) {
                deleted.push(this._array[i]);
                continue;
            }
            newArray.push(this._array[i]);
            newIndices[key] = currentIndex++;
        }
        this._array = newArray;
        this._indices = newIndices;

        return deleted;
    }

    add(key:string, value:T) {
        if (!this._indices[key]) {
            let index = this.array.length;
            this.array.push(value);
            this.indices[key] = index;
        }
    }


    get array():T[] {
        return this._array;
    }

    get indices():IMap<number> {
        return this._indices;
    }
}

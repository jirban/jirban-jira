export interface IMap<T> {
    [name: string]: T;
}

export class IMapUtil {
    static getSortedKeys<T>(map:IMap<T>):string[] {
        if (map == null) {
            return null;
        }
        let keys:string[] = [];
        for (let key in map) {
            keys.push(key);
        }
        keys = keys.sort();
        return keys;
    }
}



import {IMap} from "./map";
export class CharArrayRegistry {
    private registry:IMap<string[]> = {};

    constructor() {
    }

    getCharArray(str:string):string[] {
        let chars:string[] = this.registry[str];
        if (!chars) {
            chars = this.toCharArray(str);
            this.registry[str] = chars;
        }
        return chars;
    }

    private toCharArray(str:string):string[] {
        let arr:string[] = [];
        for (let i:number = 0; i < str.length; i++) {
            let s = str.charAt(i);
            if (s == " ") {
            }
            arr.push(s);
        }
        return arr;
    }
}
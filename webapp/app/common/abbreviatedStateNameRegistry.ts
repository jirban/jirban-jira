import {IMap} from "./map";

export class AbbreviatedHeaderRegistry {
    private registry:IMap<string> = {};

    constructor() {
    }

    getAbbreviatedHeader(str:string):string {
        let abbreviated:string = this.registry[str];
        if (!abbreviated) {
            abbreviated = this.abbreviate(str);
            this.registry[str] = abbreviated;
        }
        return abbreviated;
    }

    private abbreviate(str:string):string {
        let words:string[] = str.split(" ");
        if (!words) {
            words = [str];
        }
        let abbreviated:string = "";
        for (let i:number = 0; i < words.length; i++) {
            let s = words[i].trim();
            if (s.length > 0) {
                abbreviated += s.charAt(0).toUpperCase();
            }
        }
        return abbreviated;
    }
}
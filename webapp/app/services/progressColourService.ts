import {IMap} from "../common/map";
import {Injectable} from "@angular/core";

@Injectable()
export class ProgressColourService {
    private _colourTables:IMap<string[]> = {};

    constructor() {
    }

    public getColour(index:number, length:number):string{
        let lengthStr: string = length.toString();
        let table:string[] = this._colourTables[lengthStr];

        if (table == null) {
            table = this.calculateColourTable(length);
            this._colourTables[lengthStr] = table;

        }
        return table[index];
    }

    private calculateColourTable(length:number):string[] {
        let odd:boolean = length%2 == 1;
        let len:number = length;
        if (!odd) {
            //Insert a fake half-way element to simplify the calculations
            len = length + 1;
        }
        let max = 255;
        let halfLength:number = Math.floor(len/2);

        let increment:number = max/2/halfLength;

        let table:string[] = new Array(length);
        let insertIndex = 0;

        for (let i:number = 0 ; i < len ; i++) {
            let red:number = 0;
            let green:number = 0;
            if (i === halfLength) {
                red = max;
                green = max;
                if (!odd) {
                    //Skip this fake element
                    continue;
                }
            } else if (i < halfLength) {
                red = max;
                green = i == 0 ? 0 : Math.round(max/2 + increment * i);
            } else {
                //The yellow to green part of the scale is a bit too shiny, so reduce the brightness
                // while keeping the red to green ratio
                let adjustment:number = 4/5;
                if (i == len - 1) {
                    red = 0;
                    green = 220;
                } else {
                    red = Math.round((max - increment * (i - halfLength)));
                    green = Math.round(max * adjustment);
                }
            }

            let colourString:string = "#" + this.toHex(red) + this.toHex(green) + "00";
            table[insertIndex] = colourString;
            //console.log(insertIndex + " " + colourString + " " + red + " " + green);
            insertIndex++;
        }
        return table;
    }

    private toHex(i:number):string {
        let s: string = i.toString(16);
        if (s.length == 1) {
            s = "0" + s;
        }
        return s;
    }

}
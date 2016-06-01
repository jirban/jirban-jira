import {Title} from "@angular/platform-browser";
import {Injectable} from "@angular/core";

@Injectable()
export class TitleFormatService {
    constructor(private _title:Title) {
    }

    setTitle(title:string) {
        this._title.setTitle("Jirban - " + title);
    }
}
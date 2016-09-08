import {Title} from "@angular/platform-browser";
import {Injectable} from "@angular/core";
import {Observable, Subject} from "rxjs/Rx";

@Injectable()
export class AppHeaderService {
    private _disableBodyScrollbars:boolean;
    private _disableBodyScrollbarsObservable:Subject<boolean> = new Subject<boolean>();
    constructor(private _title:Title) {
    }

    //Call this setter before calling setTitle(), setTitle() will reset the status once done
    set disableBodyScrollbars(disable:boolean) {
        this._disableBodyScrollbars = disable;
    }

    setTitle(title:string) {
        this._title.setTitle("Jirban - " + title);
        this._disableBodyScrollbarsObservable.next(this._disableBodyScrollbars);
        this._disableBodyScrollbars = false;
    }


    get disableBodyScrollbarsObservable():Observable<boolean> {
        return this._disableBodyScrollbarsObservable;
    }
}
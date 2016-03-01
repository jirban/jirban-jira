import {Injectable} from 'angular2/core';
import {Headers, Http, Response} from 'angular2/http';
import {Router} from 'angular2/router';
import {Observable} from 'rxjs/Observable';
import {RestUrlUtil} from "../common/RestUrlUtil";
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/timeout';
import {timeout} from "rxjs/operator/timeout";
import {ProgressErrorService} from "./progressErrorService";


@Injectable()
export class BoardsService {

    private timeout:number = 20000;

    constructor(private _http:Http) {
    }

    loadBoardsList(summaryOnly:boolean) : Observable<any> {
        let path:string = RestUrlUtil.caclulateRestUrl(summaryOnly ? 'rest/boards' : 'rest/boards?full=1');
        let ret:Observable<any> =
            this._http.get(path)
                .timeout(this.timeout, "The server did not respond in a timely manner for GET " + path)
                .map((res: Response) => res.json());

        return ret;
    }

    createBoard(json:string) : Observable<any> {
        let path:string = RestUrlUtil.caclulateRestUrl('rest/boards');
        let headers = new Headers();
        console.log("Saving board " + path);
        let ret:Observable<any> =
            this._http.post(path, json, {
                headers : headers
            })
                .timeout(this.timeout, "The server did not respond in a timely manner for POST " + path)
                .map((res: Response) => res.json());
        return ret;
    }

    saveBoard(id:number, json:string) : Observable<any> {
        let path:string = RestUrlUtil.caclulateRestUrl('rest/boards/' + id);
        let headers = new Headers();
        console.log("Saving board " + path);
        let ret:Observable<any> =
            this._http.put(path, json, {
                headers : headers
            })
                .timeout(this.timeout, "The server did not respond in a timely manner for PUT " + path)
                .map((res: Response) => res.json());
        return ret;
    }

    deleteBoard(id:number) : Observable<any> {
        let path:string = RestUrlUtil.caclulateRestUrl('rest/boards/' + id);
        let headers = new Headers();
        console.log("Deleting board " + path);
        let ret:Observable<any> =
            this._http.delete(path, {
                headers : headers
            })
                .timeout(this.timeout, "The server did not respond in a timely manner for DELETE " + path)
                .map((res: Response) => res.json());
        return ret;
    }
}
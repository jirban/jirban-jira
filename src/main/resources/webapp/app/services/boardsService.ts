import {Injectable} from 'angular2/core';
import {Headers, Http, Response} from 'angular2/http';
import {Router} from 'angular2/router';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import {getToken, hasToken} from '../services/authenticationHelper';
import {RestUrlUtil} from "../common/RestUrlUtil";


@Injectable()
export class BoardsService {
    constructor(private _http:Http, private _router:Router) {
    }

    loadBoardsList(summaryOnly:boolean) : Observable<any> {
        if (!hasToken()) {
            this._router.navigateByUrl("/login");
        }
        let token = getToken();
        let headers = new Headers();
        headers.append("Authorization", token);

        let path:string = RestUrlUtil.caclulateRestUrl(summaryOnly ? 'rest/boards' : 'rest/boards?full=1');
        let ret:Observable<any> =
            this._http.get(path, {
                headers : headers
            }).
            map((res: Response) => res.json());
        return ret;
    }

    createBoard(json:string) : Observable<any> {
        let path:string = RestUrlUtil.caclulateRestUrl('rest/boards');
        let headers = new Headers();
        console.log("Saving board " + path);
        let ret:Observable<any> =
            this._http.post(path, json, {
                headers : headers
            }).
            map((res: Response) => res.json());
        return ret;
    }

    saveBoard(id:number, json:string) : Observable<any> {
        let path:string = RestUrlUtil.caclulateRestUrl('rest/boards/' + id);
        let headers = new Headers();
        console.log("Saving board " + path);
        let ret:Observable<any> =
            this._http.put(path, json, {
                headers : headers
            }).
            map((res: Response) => res.json());
        return ret;
    }

    deleteBoard(id:number) : Observable<any> {
        let path:string = RestUrlUtil.caclulateRestUrl('rest/boards/' + id);
        let headers = new Headers();
        console.log("Deleting board " + path);
        let ret:Observable<any> =
            this._http.delete(path, {
                headers : headers
            }).
            map((res: Response) => res.json());
        return ret;
    }
}
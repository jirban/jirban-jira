//a simple service
import {Injectable} from "@angular/core";
import {Http, Response} from "@angular/http";
import {Observable} from "rxjs/Observable";
import "rxjs/add/operator/map";
import {RestUrlUtil} from "../common/RestUrlUtil";

@Injectable()
export class AccessLogService {
    private bigTimeout:number = 60000;
    private http : Http;

    constructor(http:Http) {
        this.http = http;
    }

    getAccessLog() : Observable<Response> {
        let url = 'rest/jirban/1.0/access-log';
        let path:string = RestUrlUtil.caclulateRestUrl(url);
        return this.http.get(path)
            .timeout(this.bigTimeout, "The server did not respond in a timely manner for GET " + path)
            .map(res => (<Response>res).json());
    }
}





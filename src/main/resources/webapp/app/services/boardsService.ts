import {Injectable} from 'angular2/core';
import {Headers, Http, Response} from 'angular2/http';
import {Router} from 'angular2/router';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import {getToken, hasToken} from '../services/authenticationHelper';


@Injectable()
export class BoardsService {
    boardsData : Observable<any>;
    router : Router;

    constructor(http :Http, router : Router) {
        if (!hasToken()) {
            router.navigateByUrl('/login');
        }
        let token = getToken();
        let headers = new Headers();
        headers.append("Authorization", token);

        let path : string = 'rest/boards.json'; //Real URL
        this.boardsData =
            http.get(path, {
                headers : headers
            }).
            map((res: Response) => res.json());
    }
}
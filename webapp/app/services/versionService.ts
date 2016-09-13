import {Http, Response} from "@angular/http";
import {RestUrlUtil} from "../common/RestUrlUtil";
import {Injectable} from "@angular/core";
import {ProgressErrorService} from "./progressErrorService";

@Injectable()
export class VersionService {

    private timeout:number = 20000;
    private _apiVersion:number;
    private _jirbanVersion:string;


    constructor(private _http:Http) {
    }


    get jirbanVersion():string {
        return this._jirbanVersion;
    }

    initialise(expectedVersion:number, progressError:ProgressErrorService):void {
        let path:string = RestUrlUtil.caclulateRestUrl('rest/jirban/1.0/version');
        this._http.get(path)
            .timeout(this.timeout, "The server did not respond in a timely manner for GET " + path)
            .map((res: Response) => res.json())
            .subscribe(
                data => {
                    let apiVersion:number = data["api-version"];
                    let jirbanVersion:string = data["jirban-version"];
                    if (!apiVersion || apiVersion != expectedVersion) {
                        progressError.setErrorString("You appear to be using an outdated/cached version of the client. " +
                            "Please empty your browser caches and reload this page.")
                    }
                    this._apiVersion = apiVersion;
                    this._jirbanVersion = jirbanVersion;
                },
                error => {progressError.setError(error)},
                () => {progressError.finishProgress()}
        )
    }
}
import {Component} from "@angular/core";
import {BoardsService} from "../../services/boardsService";
import {AppHeaderService} from "../../services/appHeaderService";
import {RestUrlUtil} from "../../common/RestUrlUtil";
import {Http, Headers} from "@angular/http";
import {ProgressErrorService} from "../../services/progressErrorService";
import {FormControl, FormGroup, Validators} from "@angular/forms";

/**
 * Backing class for functionality to explore the DB
 */
@Component({
    selector: 'boards',
    inputs: ['boards'],
    providers: [BoardsService],
    templateUrl: './dbexplorer.html'
})
export class DbExplorerComponent {
    private sqlForm:FormGroup;
    private error:string;
    private result:any;

    constructor(appHeaderService:AppHeaderService, private _http:Http) {
        appHeaderService.setTitle("DB Explorer");
        this.sqlForm = new FormGroup({
            "sql": new FormControl(null, Validators.required)
        });
    }


    executeSql() {
        this.error = null;
        let url = 'rest/jirban/1.0/db-explorer';
        let path:string = RestUrlUtil.caclulateRestUrl(url);

        let headers:Headers = new Headers();
        headers.append("Content-Type", "application/json");
        headers.append("Accept", "application/json");
        let payload:any = {sql: this.sqlForm.value.sql};

        return this._http.post(path, JSON.stringify(payload),  {headers : headers})

            //TODO figure out how to

            .timeout(60000)
            .subscribe(
                data => {
                    console.log(data);
                    this.result = JSON.parse(data["_body"]);
                },
                err => {
                    this.error = ProgressErrorService.parseErrorCodeAndString(err);
                    console.log(err);
                    this.result = null;
                }
            );
    }
}
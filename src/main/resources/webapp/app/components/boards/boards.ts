import {Component} from "@angular/core";
import {ROUTER_DIRECTIVES} from "@angular/router-deprecated";
import {BoardsService} from "../../services/boardsService";
import {ProgressErrorService} from "../../services/progressErrorService";
import {AppHeaderService} from "../../services/appHeaderService";
import {VersionService} from "../../services/versionService";
import {VIEW_RANK} from "../../common/constants";

@Component({
    selector: 'boards',
    inputs: ['boards'],
    providers: [BoardsService],
    templateUrl: 'app/components/boards/boards.html',
    directives: [ROUTER_DIRECTIVES]
})
export class BoardsComponent {
    private boards:any[];
    private rankViewParameter:string = VIEW_RANK;

    constructor(private _boardsService:BoardsService, private _versionService:VersionService, progressError:ProgressErrorService, appHeaderService:AppHeaderService) {
        appHeaderService.setTitle("List of boards");
        progressError.startProgress(true);
        _boardsService.loadBoardsList(true).subscribe(
            data => {
                console.log('Boards: Got data' + JSON.stringify(data));
                this.boards = data.configs;
            },
            err => {
                progressError.setError(err);
            },
            () => {
                progressError.finishProgress();
            });
    }

    get jirbanVersion():string {
        return this._versionService.jirbanVersion;
    }
}
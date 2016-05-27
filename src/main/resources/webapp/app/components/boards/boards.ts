import {Component} from "@angular/core";
import {ROUTER_DIRECTIVES} from "@angular/router-deprecated";
import {BoardsService} from "../../services/boardsService";
import {ProgressErrorService} from "../../services/progressErrorService";
import {TitleFormatService} from "../../services/titleFormatService";

@Component({
    selector: 'boards',
    inputs: ['boards'],
    providers: [BoardsService],
    templateUrl: 'app/components/boards/boards.html',
    directives: [ROUTER_DIRECTIVES]
})
export class BoardsComponent {
    private boards:any[];

    constructor(private _boardsService:BoardsService, progressError:ProgressErrorService, title:TitleFormatService) {
        title.setTitle("List of boards");
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
}
import {Component} from 'angular2/core';
import {ROUTER_DIRECTIVES, ROUTER_PROVIDERS, Router, RouterLink} from 'angular2/router';
import {BoardsService} from '../../services/boardsService';
import {BoardComponent} from '../board/board';
import {Observable} from "rxjs/Observable";
import {ProgressErrorService} from "../../services/progressErrorService";

@Component({
    selector: 'boards',
    inputs: ['boards'],
    providers: [BoardsService],
    templateUrl: 'app/components/boards/boards.html',
    directives: [ROUTER_DIRECTIVES]
})
export class BoardsComponent {
    private boards:any[];

    constructor(private _boardsService:BoardsService, progressError:ProgressErrorService) {
        progressError.startProgress(true);
        _boardsService.loadBoardsList(true).subscribe(
            data => {
                console.log('Boards: Got data' + JSON.stringify(data));
                this.boards = data;
            },
            err => {
                progressError.setError(err);
            },
            () => {
                progressError.finishProgress();
            });
    }
}
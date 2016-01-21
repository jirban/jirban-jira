import {Component, View} from 'angular2/core';
import {ROUTER_DIRECTIVES, ROUTER_PROVIDERS, Router, RouterLink} from 'angular2/router';
import {BoardsService} from '../../services/boardsService';
import {BoardComponent} from '../board/board';
import {clearToken,setToken} from '../../services/authenticationHelper';

@Component({
    selector: 'boards',
    inputs: ['boards'],
    providers: [BoardsService]
})
@View({
    templateUrl: 'app/components/boards/boards.html',
    directives: [ROUTER_DIRECTIVES]
})
export class BoardsComponent {
    private boards:any[]

    constructor(public boardsService:BoardsService, public router:Router) {
        boardsService.boardsData.subscribe(
            data => {
                console.log('Boards: Got data' + JSON.stringify(data));
                this.boards = data;
            },
            err => {
                console.log(err);
                //TODO logout locally if 401, and redirect to login
                //err seems to contain a complaint about the json marshalling of the empty body having gone wrong,
                //rather than about the auth problems

                //To be safe, go back to the error page
                clearToken();
                this.router.navigateByUrl('/login');
            },
            () => console.log('Board: done')
        );
    }


}
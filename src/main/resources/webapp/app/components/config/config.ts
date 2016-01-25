import {Component, View} from 'angular2/core';
import {ROUTER_DIRECTIVES, ROUTER_PROVIDERS, Router, RouterLink} from 'angular2/router';
import {BoardsService} from '../../services/boardsService';
import {BoardComponent} from '../board/board';
import {clearToken,setToken} from '../../services/authenticationHelper';
import {ControlGroup, FormBuilder} from "angular2/common";
import {Validators} from "angular2/common";
import {Control} from "angular2/common";

@Component({
    selector: 'boards',
    inputs: ['boards'],
    providers: [BoardsService]
})
@View({
    templateUrl: 'app/components/config/config.html',
    directives: [ROUTER_DIRECTIVES]
})
export class ConfigComponent {
    private boards:any[]
    private selected:number = -1;
    private edit:boolean = false;
    private deleting = false;

    private editForm:ControlGroup;
    private deleteForm:ControlGroup;
    private newForm:ControlGroup;

    constructor(private _boardsService:BoardsService, private _router:Router, private _formBuilder:FormBuilder) {
        this.loadBoards();
    }

    private loadBoards() {
        this._boardsService.loadBoardsList(false).subscribe(
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
                this._router.navigateByUrl('/login');
            },
            () => console.log('Board: done')
        );

        this.updateNewForm();
    }

    private updateNewForm() {
        this.newForm = this._formBuilder.group({
            "newJson": ["", Validators.nullValidator(null)] //TODO validate that is is valid json at least
        });
    }

    hasBoards() : boolean {
        return this.boards && this.boards.length > 0;
    }

    isSelected(id:number) : boolean {
        return id == this.selected;
    }

    getConfigJson(board:any) : string {
        let config:any = board.config;
        let json:string = JSON.stringify(config, null, 2);
        return json;
    }

    toggleBoard(event:MouseEvent, id:number) {
        this.edit = false;
        if (this.selected == id) {
            this.selected = -1;
        } else {
            this.selected = id;
        }
        event.preventDefault();
    }

    toggleEdit(event:MouseEvent, board?:any) {
        this.edit = !this.edit;
        if (this.edit) {
            this.editForm = this._formBuilder.group({
                "editJson": [this.getConfigJson(board), Validators.nullValidator(null)] //TODO validate that is is valid json at least
            });
        }
        event.preventDefault();
    }

    toggleDelete(event:MouseEvent, id:number) {
        this.deleting = !this.deleting;
        if (this.deleting) {
            this.deleteForm = this._formBuilder.group({
                "boardName": ['', Validators.nullValidator(null)] //TODO proper validation
            })
        }
        event.preventDefault();
    }

    deleteBoard() {
        this._boardsService.deleteBoard(this.selected)
            .subscribe(
                data => {
                    console.log("Deleted board");
                    this.boards = data;
                    this.edit = false;
                    this.deleting = false;
                },
                err => {
                    console.log(err);
                    //TODO error reporting
                },
                () => {}
            );
    }

    editBoard() {
        this._boardsService.saveBoard(this.selected, this.editForm.value.editJson)
            .subscribe(
                data => {
                    console.log("Edited board");
                    this.boards = data;
                    this.edit = false;
                },
                err => {
                    console.log(err);
                    //TODO error reporting
                },
                () => {}
            );
    }

    newBoard() {
        this._boardsService.createBoard(this.newForm.value.newJson)
            .subscribe(
                data => {
                    console.log("Saved new board");
                    this.boards = data;
                    this.updateNewForm();
                },
                err => {
                    console.log(err);
                    //TODO error reporting
                },
                () => {}
            );

    }


}
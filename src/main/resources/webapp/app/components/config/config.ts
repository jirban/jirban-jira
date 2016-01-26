import {Component, View} from 'angular2/core';
import {ROUTER_DIRECTIVES, ROUTER_PROVIDERS, Router, RouterLink} from 'angular2/router';
import {BoardsService} from '../../services/boardsService';
import {BoardComponent} from '../board/board';
import {clearToken,setToken} from '../../services/authenticationHelper';
import {ControlGroup, FormBuilder} from "angular2/common";
import {Validators} from "angular2/common";
import {Control} from "angular2/common";
import {Indexed} from "../../common/indexed";

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
    private _boards:Indexed<any>;
    private selected:number = -1;
    private edit:boolean = false;
    private deleting = false;

    private editForm:ControlGroup;
    private deleteForm:ControlGroup;
    private newForm:ControlGroup;
    private jsonErrorEdit:string = null;
    private jsonErrorCreate:string = null;

    constructor(private _boardsService:BoardsService, private _router:Router, private _formBuilder:FormBuilder) {
        this.loadBoards();
    }

    private loadBoards() {
        this._boardsService.loadBoardsList(false).subscribe(
            data => {
                console.log('Boards: Got data' + JSON.stringify(data));
                this._boards = this.indexBoard(data);
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

    private get boards() : any[] {
        if (!this._boards) {
            return [];
        }
        return this._boards.array;
    }

    private updateNewForm() {
        this.newForm = this._formBuilder.group({
            "newJson": ["", Validators.required]
        });
    }

    hasBoards() : boolean {
        return this._boards && this._boards.array.length > 0;
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
        this.clearJsonErrors();
        this.edit = false;
        if (this.selected == id) {
            this.selected = -1;
        } else {
            this.selected = id;
        }
        event.preventDefault();
    }

    toggleEdit(event:MouseEvent, board?:any) {
        this.clearJsonErrors();
        this.edit = !this.edit;
        if (this.edit) {
            this.editForm = this._formBuilder.group({
                "editJson": [this.getConfigJson(board), Validators.required]
            });
        }
        event.preventDefault();
    }

    toggleDelete(event:MouseEvent, id:number) {
        this.deleting = !this.deleting;
        if (this.deleting) {

            //I wasn't able to get 'this' working with the lambda below
            let component:ConfigComponent = this;

            this.deleteForm = this._formBuilder.group({
                "boardName": ['', Validators.compose([Validators.required, (control:Control) => {
                    if (component.selected) {
                        let board:any = component._boards.forKey(component.selected.toString());
                        if (board.name != control.value) {
                            return {"boardName" : true};
                        }
                    }
                    return null;
                }])]
            })
        }
        event.preventDefault();
    }

    deleteBoard() {
        this._boardsService.deleteBoard(this.selected)
            .subscribe(
                data => {
                    console.log("Deleted board");
                    this._boards = this.indexBoard(data);
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
        let value:string = this.editForm.value.editJson;
        if (!this.checkJson(value)) {
            this.jsonErrorEdit = "The contents must be valid json";
            return;
        }
        this._boardsService.saveBoard(this.selected, value)
            .subscribe(
                data => {
                    console.log("Edited board");
                    this._boards = this.indexBoard(data);
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
        let value:string = this.newForm.value.newJson;
        if (!this.checkJson(value)) {
            this.jsonErrorCreate = "The contents must be valid json";
            return;
        }
        this._boardsService.createBoard(this.newForm.value.newJson)
            .subscribe(
                data => {
                    console.log("Saved new board");
                    this._boards = this.indexBoard(data);
                    this.updateNewForm();
                },
                err => {
                    console.log(err);
                    //TODO error reporting
                },
                () => {}
            );
    }

    checkJson(value:string):boolean {
        try {
            JSON.parse(value);
            return true;
        } catch (e) {
            return false;
        }
    }

    private clearJsonErrors() {
        this.jsonErrorCreate = null;
        this.jsonErrorEdit = null;
    }

    private indexBoard(data:any) : Indexed<any> {
        let boards:Indexed<any> = new Indexed<any>();
        boards.indexArray(
            data,
            (entry) => entry,
            (board) => board.id);
        return boards;
    }
}



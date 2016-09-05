import {Component} from "@angular/core";
import {ControlGroup, FormBuilder, Validators, Control} from "@angular/common";
import {Indexed} from "../../common/indexed";
import {ProgressErrorService} from "../../services/progressErrorService";
import {AppHeaderService} from "../../services/appHeaderService";
import {VersionService} from "../../services/versionService";
import {BoardsService} from "../../services/boardsService";

@Component({
    selector: 'boards',
    inputs: ['boards'],
    providers: [BoardsService],
    templateUrl: 'app/components/config/config.html'
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

    private canEditCustomFieldId:boolean;
    private customFieldId:number;
    private _customFieldIdForm:ControlGroup;

    private configJson:string;

    constructor(private _boardsService:BoardsService, private _progressError:ProgressErrorService,
                private _formBuilder:FormBuilder, private _versionService:VersionService,
                appHeaderService:AppHeaderService) {
        this.loadBoards();
        appHeaderService.setTitle("Configuration of boardsc");
    }

    private loadBoards() {
        this._progressError.startProgress(true);
        this._boardsService.loadBoardsList(false).subscribe(
            data => {
                console.log('Boards: Got data' + JSON.stringify(data));
                this._boards = this.indexBoard(data.configs);
                this.canEditCustomFieldId = data["rank-custom-field"].edit;
                this.customFieldId = data["rank-custom-field"].id;
            },
            err => {
                this._progressError.setError(err);
            },
            () => {
                this._progressError.finishProgress();
            }
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

    toggleBoard(event:MouseEvent, id:number) {
        this.clearJsonErrors();
        this.edit = false;
        if (this.selected == id) {
            this.configJson = null;
            this.selected = -1;
        } else {
            this.selected = id;
            this._progressError.startProgress(true);
            console.log("Loading board config " + id);
            this._boardsService.loadBoardConfigJson(id)
                .subscribe(
                    data => {
                        this.configJson = this.formatAsJson(data);
                        console.log("Loaded config " + id);
                    },
                    err => {
                        this._progressError.setError(err);
                    },
                    () => {
                        this._progressError.finishProgress();
                    });
        }
        event.preventDefault();
    }

    toggleEdit(event:MouseEvent, board?:any) {
        this.clearJsonErrors();
        this.edit = !this.edit;
        if (this.edit) {
            this.editForm = this._formBuilder.group({
                "editJson": [this.configJson, Validators.required]
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
        this._progressError.startProgress(true);
        this._boardsService.deleteBoard(this.selected)
            .subscribe(
                data => {
                    console.log("Deleted board");
                    this._boards = this.indexBoard(data.configs);
                    this.edit = false;
                    this.deleting = false;
                },
                err => {
                    this._progressError.setError(err);
                },
                () => {
                    this._progressError.finishProgress();
                }
            );
    }

    editBoard() {
        let value:string = this.editForm.value.editJson;
        if (!this.checkJson(value)) {
            this.jsonErrorEdit = "The contents must be valid json";
            return;
        }
        this._progressError.startProgress(true);
        this._boardsService.saveBoard(this.selected, value)
            .subscribe(
                data => {
                    console.log("Edited board");
                    this._boards = this.indexBoard(data.configs);
                    //Reformat the json so it is indented the standard way
                    this.configJson = this.formatAsJson(JSON.parse(value));
                    this.edit = false;
                },
                err => {
                    this._progressError.setError(err);
                },
                () => {
                    this._progressError.finishProgress();
                }
            );
    }

    newBoard() {
        let value:string = this.newForm.value.newJson;
        if (!this.checkJson(value)) {
            this.jsonErrorCreate = "The contents must be valid json";
            return;
        }
        this._progressError.startProgress(true);
        this._boardsService.createBoard(value)
            .subscribe(
                data => {
                    console.log("Saved new board");
                    this._boards = this.indexBoard(data.configs);
                    this.updateNewForm();
                },
                err => {
                    this._progressError.setError(err);
                },
                () => {
                    this._progressError.finishProgress();
                }
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

    get customFieldIdForm():ControlGroup {
        if (!this._customFieldIdForm) {
            this._customFieldIdForm = this._formBuilder.group({
                "customFieldId": [this.customFieldId, Validators.pattern("[0-9]*")]
            });
        }
        return this._customFieldIdForm;
    }

    saveCustomFieldId() {
        this._progressError.startProgress(true);
        this._boardsService.saveRankCustomFieldId(this._customFieldIdForm.value.customFieldId)
            .subscribe(
                data => {
                    console.log("Saved new board");
                    this.customFieldId = this._customFieldIdForm.value.customFieldId;
                },
                err => {
                    this._progressError.setError(err);
                },
                () => {
                    this._progressError.finishProgress();
                }
            )
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

    private formatAsJson(data:any):string {
        return JSON.stringify(data, null, 2);
    }

    get jirbanVersion():string {
        return this._versionService.jirbanVersion;
    }
}



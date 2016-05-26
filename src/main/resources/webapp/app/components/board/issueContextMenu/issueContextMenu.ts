import {Component, EventEmitter} from "@angular/core";
import {BoardData} from "../../../data/board/boardData";
import {IssueData} from "../../../data/board/issueData";
import {IssuesService} from "../../../services/issuesService";
import {IssueComponent} from "../issue/issue";
import {ProgressErrorService} from "../../../services/progressErrorService";
import {State} from "../../../data/board/header";

@Component({
    inputs: ['data'],
    outputs: ['closeContextMenu'],
    selector: 'issue-context-menu',
    templateUrl: 'app/components/board/issueContextMenu/issueContextMenu.html',
    styleUrls: ['app/components/board/issueContextMenu/issueContextMenu.css'],
    directives: [IssueComponent]
})
export class IssueContextMenuComponent {
    private _data:IssueContextMenuData;
    private showContext:boolean = false;
    private issue:IssueData;
    private endIssue:boolean;
    private toState:string;
    private issuesForState:IssueData[];

    //Calculated dimensions
    private movePanelTop:number;
    private movePanelHeight:number;
    private movePanelLeft:number;
    private statesColumnHeight:number;

    private insertBeforeIssueKey:string;

    private move:boolean;

    private closeContextMenu:EventEmitter<any> = new EventEmitter();

    constructor(private _boardData:BoardData, private _issuesService:IssuesService, private _progressError:ProgressErrorService) {
        this.setWindowSize();
    }

    private set data(data:IssueContextMenuData) {
        this.showContext = !!data;
        this.move = false;
        this.toState = null;
        this.issue = null;
        this.endIssue = false;
        this.issuesForState = null;
        this.insertBeforeIssueKey = null;
        this._data = data;
        this.issue = null;
        if (data) {
            this.issue = this._boardData.getIssue(data.issueKey);
            this.toState = this.issue.boardStatus;
            this.issuesForState = this._boardData.getValidMoveBeforeIssues(this.issue.key, this.toState);
        }
    }

    private clearMoveMenu() {
        this.move = false;
    }

    private get data() {
        return this._data;
    }

    private get displayContextMenu() : boolean {
        return !!this._data && !!this.issue && this.showContext;
    }

    private get moveStates() : string[] {
        return this._boardData.boardStateNames;
    }

    private isValidMoveState(state:string) : boolean {
        //We can do a plain move to all states apart from ourselves
        return this._boardData.isValidStateForProject(this.issue.projectCode, state) && state != this.issue.boardStatus;
    }

    private isValidRankState(stateName:string) : boolean {
        if (!this._boardData.isValidStateForProject(this.issue.projectCode, stateName)) {
            return false;
        }
        let state:State = this._boardData.indexedBoardStates.forKey(stateName);
        if (state.done) {
            return false;
        }
        if (state.backlog && !this._boardData.showBacklog) {
            return false;
        }
        if (state.unordered) {
            return false;
        }
        return true;
    }

    private onShowMovePanel(event:MouseEvent) {
        event.preventDefault();
        this.showContext = false;
        this.move = true;
    }

    private onSelectMoveState(event:MouseEvent, toState:string) {
        //The user has selected to move to a state accepting the default ranking
        event.preventDefault();
        this.issuesForState = null;
        this.toState = toState;

        this.moveIssue(false, null);
    }

    private onSelectRankState(event:MouseEvent, toState:string) {
        //The user has selected the rank for state button, pull up the list of issues
        event.preventDefault();
        this.issuesForState = this._boardData.getValidMoveBeforeIssues(this.issue.key, toState);
        this.toState = toState;
    }

    private onSelectRankIssue(event:MouseEvent, beforeIssueKey:string) {
        console.log("onSelectMoveIssue - " + beforeIssueKey)
        event.preventDefault();
        this.insertBeforeIssueKey = beforeIssueKey;

        if (this.issue.key == beforeIssueKey) {
            //If we are moving to ourself just abort
            console.log("onSelectMoveIssue - key is self, returning")
            this.clearMoveMenu();
            return;
        }
        this.moveIssue(true, beforeIssueKey);
    }

    private moveIssue(rank:boolean, beforeIssueKey:string) {
        let beforeKey:string;;
        let afterKey:string;

        if (rank) {
            beforeKey = beforeIssueKey === "" ? null : beforeIssueKey;
            if (!beforeKey && this.issuesForState.length > 0) {
                afterKey = this.issuesForState[this.issuesForState.length - 1].key;
            }
            console.log("onSelectMoveIssue key - afterKey " + afterKey);
        }
        //Tell the server to move the issue. The actual move will come in via the board's polling mechanism.
        this._progressError.startProgress(true);
        this._issuesService.moveIssue(this._boardData, this.issue, this.toState, beforeKey, afterKey)
            .subscribe(
                data => {},
                error => {
                    this._progressError.setError(error);
                    this.clearMoveMenu();
                },
                () => {
                    this._progressError.finishProgress();
                    this.clearMoveMenu();
                }
            );
    }

    private onResize(event : any) {
        this.setWindowSize();
    }

    private setWindowSize() {
        let movePanelTop, movePanelHeight, movePanelLeft, statesColumnHeight : number = 0;

        //40px top and bottom padding if window is high enough, 5px otherwise
        let yPad = window.innerHeight > 350 ? 40 : 5;
        movePanelHeight = window.innerHeight - 2 * yPad;
        movePanelTop = window.innerHeight/2 - movePanelHeight/2;

        statesColumnHeight = movePanelHeight - 55;

        //css hardcodes the width as 720px;
        if (window.innerWidth > 720) {
            movePanelLeft = window.innerWidth/2 - 720/2;
        }
        this.movePanelTop = movePanelTop;
        this.movePanelHeight = movePanelHeight;
        this.movePanelLeft = movePanelLeft;
        this.statesColumnHeight = statesColumnHeight;
    }

    private isIssueSelected(issue:IssueData) : boolean {
        if (this.insertBeforeIssueKey) {
            return issue.key === this.insertBeforeIssueKey;
        }
        return this.issue.key == issue.key;
    }


    private onClickClose(event:MouseEvent) {
        this.closeContextMenu.emit({});
        event.preventDefault();
    }

    get boardData():BoardData {
        return this._boardData;
    }
}

export class IssueContextMenuData {
    constructor(private _issueKey:string,
                private _x:number,
                private _y:number) {
    }

    get issueKey():string {
        return this._issueKey;
    }

    get x():number {
        return this._x;
    }

    get y():number {
        return this._y;
    }
}


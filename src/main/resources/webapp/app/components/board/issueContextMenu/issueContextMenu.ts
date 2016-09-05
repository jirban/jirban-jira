import {Component, EventEmitter} from "@angular/core";
import {ControlGroup, FormBuilder, Validators} from "@angular/common";
import {BoardData} from "../../../data/board/boardData";
import {IssueData} from "../../../data/board/issueData";
import {IssuesService} from "../../../services/issuesService";
import {IssueComponent} from "../issue/issue";
import {ProgressErrorService} from "../../../services/progressErrorService";
import {Hideable} from "../../../common/hide";
import {IssueContextMenuData} from "../../../data/board/issueContextMenuData";
import {VIEW_RANK} from "../../../common/constants";
import {BoardProject} from "../../../data/board/project";

@Component({
    inputs: ['data', 'view'],
    outputs: ['closeContextMenu'],
    selector: 'issue-context-menu',
    templateUrl: 'app/components/board/issueContextMenu/issueContextMenu.html',
    styleUrls: ['app/components/board/issueContextMenu/issueContextMenu.css'],
    directives: [IssueComponent]
})
export class IssueContextMenuComponent implements Hideable {
    private _data:IssueContextMenuData;
    private _view:string;
    private showContext:boolean = false;
    private issue:IssueData;
    private toState:string;
    private canRank:boolean;

    //The name of the panel to show (they are 'move', 'comment', 'rank')
    private showPanel:string;

    //Calculated dimensions
    private movePanelTop:number;
    private movePanelHeight:number;
    private movePanelLeft:number;
    private statesColumnHeight:number;


    private commentForm:ControlGroup;
    private commentPanelLeft:number;

    private rankPanelTop:number;
    private rankPanelLeft:number;
    private rankPanelHeight:number;
    private rankedIssuesColumnHeight:number
    private rankedIssues:IssueData[];
    private rankBeforeKey:string;

    private closeContextMenu:EventEmitter<any> = new EventEmitter();

    constructor(private _boardData:BoardData, private _issuesService:IssuesService,
                private _progressError:ProgressErrorService, private _formBuilder:FormBuilder) {
        _boardData.registerHideable(this);
    }

    private set data(data:IssueContextMenuData) {
        this.showContext = !!data;
        this.showPanel = null;
        this.toState = null;
        this.issue = null;
        this._data = data;
        this.issue = null;
        this.rankedIssues = null;
        if (data) {
            this.issue = this._boardData.getIssue(data.issueKey);
            this.toState = this.issue.boardStatus;
            this.canRank = this._boardData.canRank(this.issue.projectCode);
        }
        this.setWindowSize();
    }

    set view(value: string) {
        this._view = value;
    }

    hide():void {
        this.hideAllMenus();
    }

    private hideAllMenus() {
        this.showContext = false;
        this.showPanel = null;
        this.commentForm = null;
        this.rankedIssues = null;
    }

    private get data() {
        return this._data;
    }

    private showRankMenuEntry() {
        return this.canRank && this._view === VIEW_RANK;
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

    private onShowMovePanel(event:MouseEvent) {
        console.log("on show move panel");
        event.preventDefault();
        this.hideAllMenus();
        this.showPanel = "move";
    }

    private onSelectMoveState(event:MouseEvent, toState:string) {
        //The user has selected to move to a state accepting the default ranking
        event.preventDefault();
        this.toState = toState;

        let beforeKey:string;;
        let afterKey:string;

        //Tell the server to move the issue. The actual move will come in via the board's polling mechanism.
        this._progressError.startProgress(true);
        this._issuesService.moveIssue(this._boardData, this.issue, this.toState)
            .subscribe(
                data => {},
                error => {
                    this._progressError.setError(error);
                    this.hideAllMenus();
                },
                () => {
                    let status:string = "<a " +
                    "class='toolbar-message' href='" + this._boardData.jiraUrl + "/browse/" + this.issue.key + "'>" +
                        this.issue.key + "</a> moved to '" + this.toState + "'";
                    this._progressError.finishProgress(status);
                    this.hideAllMenus();
                }
            );
    }


    private onShowCommentPanel(event:MouseEvent) {
        event.preventDefault();
        this.hideAllMenus();
        this.showPanel = "comment";
        this.commentForm = this._formBuilder.group({
            "comment": ["", Validators.required]
        });
    }

    private saveComment() {
        let comment:string = this.commentForm.value.comment;
        this._progressError.startProgress(true);
        this._issuesService.commentOnIssue(this._boardData, this.issue, comment)
            .subscribe(
                data => {
                    //No data is returned, issuesService refreshes boardData for us
                    this.hideAllMenus();
                },
                err => {
                    this._progressError.setError(err);
                },
                () => {
                    this._progressError.finishProgress(
                        "Comment made on issue <a " +
                        "class='toolbar-message' href='" + this._boardData.jiraUrl + "/browse/" + this.issue.key + "'>" +
                        this.issue.key + "</a>");
                }
            );

    }

    private onShowRankPanel(event:MouseEvent) {
        event.preventDefault();
        this.hideAllMenus();
        this.showPanel = "rank";
        this.rankBeforeKey = this.issue.key;
    }

    get rankedIssuesForIssueProject():IssueData[] {
        if (!this.rankedIssues) {
            let project:BoardProject = this._boardData.boardProjects.forKey(this.issue.projectCode);
            this.rankedIssues = [];
            for (let key of project.rankedIssueKeys) {
                this.rankedIssues.push(this._boardData.getIssue(key));
            }
        }
        return this.rankedIssues;
    }

    isIssueBeingRanked(issue:IssueData) {
        return this.issue.key === issue.key;
    }

    onClickRankBefore(event:MouseEvent, index:number) {
        event.preventDefault();
        let before:IssueData;
        let beforeKey:string;
        let afterKey:string;
        if (index >= 0) {
            before = this.rankedIssuesForIssueProject[index];
            this.rankBeforeKey = before.key;
            beforeKey = before.key;
            if (index > 0) {
                afterKey = this.rankedIssuesForIssueProject[index - 1].key;
            }

        } else {
            this.rankBeforeKey = null;
            afterKey = this.rankedIssuesForIssueProject[this.rankedIssuesForIssueProject.length - 1].key;
        }
        console.log("onClickRankBefore " + index + "; before: " + beforeKey + " ; after: " + afterKey);

        this._issuesService.performRerank(this.issue, beforeKey, afterKey)
            .subscribe(
                data => {
                    //No data is returned, issuesService refreshes boardData for us
                    this.hideAllMenus();
                },
                err => {
                    this._progressError.setError(err);
                },
                () => {
                    let msg = "Ranked <a " +
                        "class='toolbar-message' href='" + this._boardData.jiraUrl + "/browse/" + this.issue.key + "'>" +
                        this.issue.key + "</a> ";
                    if (index < 0) {
                        msg += " to the end";
                    } else {
                        msg += " before " + beforeKey;
                    }
                    this._progressError.finishProgress(msg);
                }
            );
    }


    private onResize(event : any) {
        this.setWindowSize();
    }

    private setWindowSize() {
        let movePanelTop:number, movePanelHeight:number, movePanelLeft:number, statesColumnHeight:number;
        let movePanelWidth:number = 410;

        //40px top and bottom padding if window is high enough, 5px otherwise
        let yPad = window.innerHeight > 350 ? 40 : 5;
        movePanelHeight = window.innerHeight - 2 * yPad;
        movePanelTop = window.innerHeight/2 - movePanelHeight/2;
        statesColumnHeight = movePanelHeight - 55;

        //css hardcodes the width as 410px;
        if (window.innerWidth > movePanelWidth) {
            movePanelLeft = window.innerWidth/2 - movePanelWidth/2;
        }
        this.movePanelTop = movePanelTop;
        this.movePanelHeight = movePanelHeight;
        this.movePanelLeft = movePanelLeft;
        this.statesColumnHeight = statesColumnHeight;

        this.commentPanelLeft = (window.innerWidth - 600)/2;

        this.rankPanelTop = movePanelTop;
        this.rankPanelHeight = movePanelHeight;
        this.rankPanelLeft = (window.innerWidth - 500)/2;
        this.rankedIssuesColumnHeight = statesColumnHeight;
    }

    private onClickClose(event:MouseEvent) {
        this.hideAllMenus();
        this.closeContextMenu.emit({});
        event.preventDefault();
    }

    get boardData():BoardData {
        return this._boardData;
    }
}


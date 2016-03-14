import {Component, View} from 'angular2/core';
import {Router, RouteParams} from 'angular2/router';
import {IssuesService} from '../../services/issuesService';
import {BoardData} from '../../data/board/boardData';
import {IssueData} from '../../data/board/issueData';
import {IssueComponent} from './issue/issue';
import {SwimlaneEntryComponent} from './swimlaneEntry/swimlaneEntry';
import {PanelMenuComponent} from "./panelMenu/panelMenu";
import {IssueContextMenuComponent, IssueContextMenuData} from "./issueContextMenu/issueContextMenu";
import {OnDestroy} from "angular2/core";
import {ProgressErrorService} from "../../services/progressErrorService";
import {BoardHeaderEntry, BoardHeaders} from "../../data/board/header";


@Component({
    selector: 'board',
    providers: [IssuesService, BoardData]
})
@View({
    templateUrl: 'app/components/board/board.html',
    styleUrls: ['app/components/board/board.css'],
    directives: [IssueComponent, IssueContextMenuComponent, PanelMenuComponent, SwimlaneEntryComponent]
})
export class BoardComponent implements OnDestroy {
    boardId:number;

    private boardHeight; //board + headers
    private boardBodyHeight; //board + headers
    private width;

    private issueContextMenuData:IssueContextMenuData;

    private _pollFailureCount:number = 0;

    private _currentTimeout;
    private _destroyed:boolean = false;

    constructor(private _issuesService:IssuesService, private _boardData:BoardData, private _progressError:ProgressErrorService, routeParams:RouteParams) {
        let boardId:string = routeParams.get('board');
        if (boardId) {
            this.boardId = Number(boardId);
        }

        this._progressError.startProgress(true);
        _issuesService.getIssuesData(this.boardId).subscribe(
            data => {
                this.setIssueData(data);
            },
            err => {
                this._progressError.setError(err);
            },
            () => {
                this.pollIssues();
                this._progressError.finishProgress();
            }
        );
        this.setWindowSize();

    }

    ngOnDestroy():any {
        //this.issuesService.closeWebSocket();
        let timeout:number = this._currentTimeout;
        if (timeout) {
            clearTimeout(timeout);
        }
        this._destroyed = true;
        return null;
    }

    private setIssueData(issueData:any) {
        this._boardData.deserialize(this.boardId, issueData);
        this.setWindowSize();
    }

    private pollIssues(timeout?:number) {
        let to:number = timeout ? timeout : 5000;
        this._currentTimeout = setTimeout(()=>{this.doPoll()}, to);
    }

    private doPoll() {
        if (this._destroyed) {
            return;
        }
        if (this._progressError.progress) {
            //Another request is already in progress (probably a move)
            //Just do another poll and return so we don't clash the error codes etc.
            this.pollIssues();
            return;
        }

        //Don't use the progress monitor for this background task.
        //Simply set the error in it if one happened
        this._issuesService.pollBoard(this.boardData)
            .subscribe(
                data => {
                    console.log("----> Received changes: " + JSON.stringify(data));
                    this.boardData.processChanges(data);
                    this.pollIssues();
                },
                err => {
                    this._pollFailureCount++;
                    console.log(err);
                    if (this._pollFailureCount < 3) {
                        this.pollIssues();
                    } else {
                        this._progressError.setError("Connection to the board lost.");
                    }
                },
                () => {
                    this._pollFailureCount = 0;
                }
            );
    }


    private get visibleColumns():boolean[] {
        return this._boardData.headers.stateVisibilities;
    }

    private toCharArray(state:string):string[] {
        let arr:string[] = [];
        for (let i:number = 0; i < state.length; i++) {
            let s = state.charAt(i);
            if (s == " ") {
            }
            arr.push(s);
        }
        return arr;
    }

    private get boardStates():string[] {
        return this._boardData.boardStates;
    }

    private onResize(event : any) {
        this.setWindowSize();
    }

    private setWindowSize() {
        //Whole height - toolbars - borders
        this.boardHeight = window.innerHeight - 30 - 4;

        //board height - header - borders
        let boardHeaders = 30;
        if (this._boardData.headers && this._boardData.headers.categorised) {
            boardHeaders *= 2;
        }
        this.boardBodyHeight = this.boardHeight - boardHeaders - 3;
        this.width = window.innerWidth - 2; //subtract width of border
    }

    private showIssueContextMenu(event:any) {
        this.issueContextMenuData = new IssueContextMenuData(event.issueId, event.x, event.y);
    }

    private hideMenus() {
        this._boardData.hideHideables();
        this.issueContextMenuData = null;
    }

    onCloseIssueContextMenu(event:any) {
        this.issueContextMenuData = null;
    }

    get boardData():BoardData {
        return this._boardData;
    }

    /**
     * If not categorised, only the top headers will exist.
     * @returns {boolean}
     */
    get categorised():boolean {
        return this._boardData.headers.categorised;
    }

    get topHeaders():BoardHeaderEntry[] {
        return this._boardData.headers.topHeaders;
    }

    get bottomHeaders():BoardHeaderEntry[] {
        return this._boardData.headers.bottomHeaders;
    }

    toggleHeaderVisibility(header:BoardHeaderEntry) {
        this._boardData.headers.toggleHeaderVisibility(header);
    }

    getTopLevelHeaderClass(header:BoardHeaderEntry):string {
        if (header.stateAndCategory) {
            if (header.visible) {
                return 'visible';
            } else {
                return 'collapsed';
            }
        }
        return '';
    }
}

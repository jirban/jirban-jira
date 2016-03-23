import {Component, ElementRef, OnDestroy, OnInit} from 'angular2/core';
import {Router, RouteParams} from 'angular2/router';
import {IssuesService} from '../../services/issuesService';
import {BoardData} from '../../data/board/boardData';
import {IssueData} from '../../data/board/issueData';
import {IssueComponent} from './issue/issue';
import {SwimlaneEntryComponent} from './swimlaneEntry/swimlaneEntry';
import {PanelMenuComponent} from "./panelMenu/panelMenu";
import {IssueContextMenuComponent, IssueContextMenuData} from "./issueContextMenu/issueContextMenu";
import {ProgressErrorService} from "../../services/progressErrorService";
import {BoardHeaderEntry, BoardHeaders, State} from "../../data/board/header";


@Component({
    selector: 'board',
    providers: [IssuesService, BoardData],
    templateUrl: 'app/components/board/board.html',
    styleUrls: ['app/components/board/board.css'],
    directives: [IssueComponent, IssueContextMenuComponent, PanelMenuComponent, SwimlaneEntryComponent]
})
export class BoardComponent implements OnDestroy, OnInit {

    private boardCode;
    private boardHeight; //board + headers
    private boardBodyHeight; //board + headers
    private width;

    private issueContextMenuData:IssueContextMenuData;

    private _pollFailureCount:number = 0;

    private _currentTimeout;
    private _destroyed:boolean = false;

    private boardLeftOffset:number = 0;

    constructor(private _issuesService:IssuesService,
                private _boardData:BoardData,
                private _progressError:ProgressErrorService, routeParams:RouteParams) {

        this.boardCode = routeParams.get('board');
        this.populateIssues();
        this.setWindowSize();
    }

    private populateIssues() {
        this._progressError.startProgress(true);
        this._issuesService.getIssuesData(this.boardCode, this._boardData.showBacklog).subscribe(
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
    }

    ngOnInit():any {

        return null;
    }

    ngOnDestroy():any {
        this.clearPollTimeout();
        this._destroyed = true;
        return null;
    }

    private clearPollTimeout() {
        let timeout:number = this._currentTimeout;
        if (timeout) {
            clearTimeout(timeout);
        }
    }

    private setIssueData(issueData:any) {
        this._boardData.deserialize(this.boardCode, issueData);
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

    private onResize(event : any) {
        this.setWindowSize();
    }

    private setWindowSize() {
        //Whole height - toolbars - borders
        this.boardHeight = window.innerHeight - 30 - 4;

        //board height - header - borders
        let boardHeaders = 30;
        if (this._boardData.headers && this._boardData.headers.bottomHeaders.length > 0) {
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

    get topHeaders():BoardHeaderEntry[] {
        return this._boardData.headers.topHeaders;
    }

    get bottomHeaders():BoardHeaderEntry[] {
        return this._boardData.headers.bottomHeaders;
    }

    get backlogTopHeader():BoardHeaderEntry {
        return this._boardData.headers.backlogTopHeader;
    }

    get backlogBottomHeadersIfVisible():BoardHeaderEntry[] {
        if (this.backlogTopHeader && this.backlogTopHeader.visible) {
            return this._boardData.headers.backlogBottomHeaders;
        }
        return null;
    }

    get nonBacklogStates():State[] {
        return this._boardData.nonBacklogStates;
    }

    get backlogAndIsCollapsed():boolean {
        if (!this.backlogTopHeader) {
            return false;
        }
        return !this.backlogTopHeader.visible;
    }

    get backlogStatesIfVisible():State[] {
        if (this.backlogTopHeader && this.backlogTopHeader.visible) {
            return this._boardData.backlogStates;
        }
        return null;
    }

    toggleHeaderVisibility(header:BoardHeaderEntry) {
        if (header.backlog) {
            this._boardData.toggleBacklog();
            this.toggleBacklog();
        }
        this._boardData.headers.toggleHeaderVisibility(header);
    }

    toggleBacklog() {
        this._progressError.startProgress(true);
        this._issuesService.getIssuesData(this.boardCode, this._boardData.showBacklog).subscribe(
            data => {
                this.boardData.processChanges(data);
            },
            err => {
                this._progressError.setError(err);
            },
            () => {
                this._progressError.finishProgress();
            }
        );
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

    getColourForIndex(index:number) : string {
        let mod:number = index % 5;
        switch (mod) {
            case 0:
                return "red";
            case 1:
                return "orange";
            case 2:
                return "green";
            case 3:
                return "blue";
            case 4:
                return "violet";
        }

    }

    scrollOuterX(event:Event) {
        this.boardLeftOffset = event.srcElement.scrollLeft;
    }
}

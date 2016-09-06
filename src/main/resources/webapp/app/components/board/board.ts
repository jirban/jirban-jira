import {Component, OnDestroy} from "@angular/core";
import {IssuesService} from "../../services/issuesService";
import {BoardData} from "../../data/board/boardData";
import {IMap} from "../../common/map";
import {VIEW_KANBAN, VIEW_RANK} from "../../common/constants";
import {IssueContextMenuData} from "../../data/board/issueContextMenuData";
import {ActivatedRoute} from "@angular/router";
import Timer = NodeJS.Timer;


/**
 * Holder component for the different board views.
 * Handles loading and polling of the issues/board data
 */
@Component({
    selector: 'board',
    providers: [IssuesService, BoardData],
    templateUrl: 'app/components/board/board.html'
})
export class BoardComponent implements OnDestroy {

    private boardCode:string;
    private view:string = VIEW_KANBAN;
    private issueContextMenuData:IssueContextMenuData;

    private _wasBacklogForced:boolean = false;

    constructor(private _issuesService:IssuesService,
                private _boardData:BoardData,
                route:ActivatedRoute) {
        console.log("Create board");
        let queryParams:IMap<string> = route.snapshot.queryParams;
        let code:string = queryParams['board'];
        if (!code) {
            return;
        }
        this.boardCode = code;

        let view = queryParams['view'];
        if (view) {
            this.view = view;
            if (view === VIEW_RANK) {
                this._wasBacklogForced = true;
            }
        }

        this._boardData.setBacklogFromQueryParams(queryParams, this._wasBacklogForced);

        this._issuesService.populateIssues(this.boardCode, () => {
            //Loading filters does not work until the issue data is loaded
            _boardData.setFiltersFromQueryParams(queryParams);
        })

        this._issuesService.loadHelpTexts(this.boardCode, this._boardData);
    }

    ngOnDestroy():any {
        this._issuesService.destroy();
    }

    get boardData():BoardData {
        return this._boardData;
    }

    get issuesService():IssuesService {
        return this._issuesService;
    }

    onFocus(event:Event):void{
        console.log("Focus");
        this._issuesService.visible = true;
    }

    onBlur(event:Event):void{
        console.log("Blur");
        this._issuesService.visible = false;
    }

    private hideMenus() {
        this._boardData.hideHideables();
        this.issueContextMenuData = null;
    }

    private onShowIssueContextMenu(issueContextMenuData:IssueContextMenuData) {
        console.log("Got event");
        this.issueContextMenuData = issueContextMenuData;
    }

    onCloseIssueContextMenu(event:any) {
        this.issueContextMenuData = null;
    }


    onToggleView(event:Event) {
        this.hideMenus();
        if (this.view === VIEW_KANBAN) {
            this.view = VIEW_RANK;
            this.forceBacklog();
        } else if (this.view === VIEW_RANK) {
            this.view = VIEW_KANBAN;
            this.unforceBacklog();

        } else {
            console.error("Unknown original view " + this.view);
        }
        console.log("View changed to " + this.view);
    }

    private forceBacklog() {
        if (!this._boardData.showBacklog && this._boardData.headers.backlogTopHeader) {
            //Load up the board again with the backlog
            this.toggleBacklog();
            this._wasBacklogForced = true;
        }
    }

    private unforceBacklog() {
        if (this._wasBacklogForced && this._boardData.headers.backlogTopHeader) {
            this.toggleBacklog();
            this._wasBacklogForced = false;
        }
    }

    private toggleBacklog() {
        console.log("Toggling backlog");
        this._boardData.headers.toggleHeaderVisibility(this._boardData.headers.backlogTopHeader);
        this._issuesService.toggleBacklog();
    }
}




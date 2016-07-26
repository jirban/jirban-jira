import {Component, OnDestroy} from "@angular/core";
import {IssuesService} from "../../services/issuesService";
import {BoardData} from "../../data/board/boardData";
import {SwimlaneEntryComponent} from "./swimlaneEntry/swimlaneEntry";
import {PanelMenuComponent} from "./panelMenu/panelMenu";
import {IssueContextMenuComponent} from "./issueContextMenu/issueContextMenu";
import {ProgressErrorService} from "../../services/progressErrorService";
import {AppHeaderService} from "../../services/appHeaderService";
import {IMap} from "../../common/map";
import {KanbanViewComponent} from "./view/kanban/kanbanview";
import {RankViewComponent} from "./view/rank/rankview";
import {VIEW_KANBAN, VIEW_RANK} from "../../common/constants";
import {IssueContextMenuData} from "../../data/board/issueContextMenuData";
import {Router, ActivatedRoute} from "@angular/router";
import Timer = NodeJS.Timer;


/**
 * Holder component for the different board views.
 * Handles loading and polling of the issues/board data
 */
@Component({
    selector: 'board',
    providers: [IssuesService, BoardData],
    templateUrl: 'app/components/board/board.html',
    directives: [KanbanViewComponent, RankViewComponent, IssueContextMenuComponent, PanelMenuComponent, SwimlaneEntryComponent]
})
export class BoardComponent implements OnDestroy {

    private boardCode:string;
    private view:string = VIEW_KANBAN;
    private issueContextMenuData:IssueContextMenuData;

    private _wasBacklogForced:boolean = false;

    constructor(private _issuesService:IssuesService,
                private _boardData:BoardData,
                private _progressError:ProgressErrorService,
                route:ActivatedRoute,
                router:Router,
                appHeader:AppHeaderService) {
        console.log("Create board");
        let params:IMap<string> = route.snapshot.params;
        let queryString:IMap<string> = router.routerState.snapshot.queryParams;

        this.boardCode = params['board'];
        appHeader.setTitle("Board (" + this.boardCode + ")");

        let view = appHeader['view'];
        if (view) {
            this.view = view;
            if (view === VIEW_RANK) {
                this._wasBacklogForced = true;
            }
        }

        this._boardData.setBacklogFromQueryParams(queryString);

        this._issuesService.populateIssues(this.boardCode, () => {
            //Loading filters does not work until the issue data is loaded
            _boardData.setFiltersFromQueryParams(queryString);
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




import {Component, OnDestroy} from "@angular/core";
import {RouteParams} from "@angular/router-deprecated";
import {IssuesService} from "../../services/issuesService";
import {BoardData} from "../../data/board/boardData";
import {SwimlaneEntryComponent} from "./swimlaneEntry/swimlaneEntry";
import {PanelMenuComponent} from "./panelMenu/panelMenu";
import {IssueContextMenuComponent} from "./issueContextMenu/issueContextMenu";
import {ProgressErrorService} from "../../services/progressErrorService";
import {TitleFormatService} from "../../services/TitleFormatService";
import {IMap} from "../../common/map";
import {KanbanViewComponent} from "./view/kanban/kanbanview";
import {RankViewComponent} from "./view/rank/rankview";
import {VIEW_KANBAN, VIEW_RANK} from "../../common/constants";
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

    constructor(private _issuesService:IssuesService,
                private _boardData:BoardData,
                private _progressError:ProgressErrorService,
                routeParams:RouteParams,
                title:TitleFormatService) {
        console.log("Create board");
        let queryString:IMap<string> = routeParams.params;
        this.boardCode = routeParams.get('board');
        title.setTitle("Board (" + this.boardCode + ")");

        let view = routeParams.get('view');
        if (view) {
            this.view = view;
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

    onToggleView(event:Event) {
        if (this.view === VIEW_KANBAN) {
            this.view = VIEW_RANK;
        } else if (this.view === VIEW_RANK) {
            this.view = VIEW_KANBAN;
        } else {
            console.error("Unknown original view " + this.view);
        }

        console.log("View changed to " + this.view);
    }
}




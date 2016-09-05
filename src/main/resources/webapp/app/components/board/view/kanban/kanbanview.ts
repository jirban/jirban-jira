import {Component} from "@angular/core";
import {BoardData} from "../../../../data/board/boardData";
import {ProgressErrorService} from "../../../../services/progressErrorService";
import {AppHeaderService} from "../../../../services/appHeaderService";
import {BoardHeaderEntry, State} from "../../../../data/board/header";
import {CharArrayRegistry} from "../../../../common/charArrayRegistry";
import {FixedHeaderView} from "../fixedHeaderView";
import {IssuesService} from "../../../../services/issuesService";


@Component({
    selector: 'kanban-view',
    inputs: ["boardCode", "issuesService", "boardData"],
    outputs: ["showIssueContextMenu"],
    templateUrl: 'app/components/board/view/kanban/kanbanview.html',
    styleUrls: ['app/components/board/view/kanban/kanbanview.css']
})
export class KanbanViewComponent extends FixedHeaderView {

    /** Cache all the char arrays used for the collapsed column labels so they are not recalculated all the time */
    private _collapsedColumnLabels:CharArrayRegistry = new CharArrayRegistry();

    constructor(_progressError:ProgressErrorService,
                _appHeaderService:AppHeaderService) {
        super(_progressError, _appHeaderService, "Kanban");
    }

    set issuesService(value:IssuesService) {
        super.setIssuesService(value);
    }

    set boardCode(value:string) {
        super.setBoardCode(value);
    }

    set boardData(value:BoardData) {
        super.setBoardData(value);
    }

    get boardData():BoardData {
        return this._boardData;
    }

    private get visibleColumns():boolean[] {
        return this._boardData.headers.stateVisibilities;
    }

    private getCharArray(state:string):string[] {
        return this._collapsedColumnLabels.getCharArray(state);
    }

    get backlogBottomHeadersIfVisible():BoardHeaderEntry[] {
        if (this.backlogTopHeader && this.backlogTopHeader.visible) {
            return this._boardData.headers.backlogBottomHeaders;
        }
        return null;
    }

    get mainStates():State[] {
        return this._boardData.mainStates;
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

    getPossibleStateHelp(header:BoardHeaderEntry):string {
        if (header.rows == 2) {
            return this.getStateHelp(header);
        }
        return null;
    }

    getStateHelp(header:BoardHeaderEntry):string {
        return this._boardData.helpTexts[header.name];
    }


    toggleHeaderVisibility(header:BoardHeaderEntry) {
        let previousBacklog:boolean = this.boardData.showBacklog;

        this._boardData.headers.toggleHeaderVisibility(header);

        if (this.boardData.showBacklog != previousBacklog) {
            this._issuesService.toggleBacklog();
        }
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




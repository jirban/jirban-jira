import {Component} from "@angular/core";
import {IssuesService} from "../../../../services/issuesService";
import {BoardData} from "../../../../data/board/boardData";
import {IssueComponent} from "../../issue/issue";
import {SwimlaneEntryComponent} from "../../swimlaneEntry/swimlaneEntry";
import {PanelMenuComponent} from "../../panelMenu/panelMenu";
import {IssueContextMenuComponent} from "../../issueContextMenu/issueContextMenu";
import {ProgressErrorService} from "../../../../services/progressErrorService";
import {AppHeaderService} from "../../../../services/appHeaderService";
import {BoardHeaderEntry, State} from "../../../../data/board/header";
import {AbbreviatedHeaderRegistry} from "../../../../common/abbreviatedStateNameRegistry";
import {IssueData} from "../../../../data/board/issueData";
import {FixedHeaderView} from "../fixedHeaderView";


@Component({
    selector: 'rank-view',
    inputs: ["boardCode", "issuesService", "boardData"],
    outputs: ["showIssueContextMenu"],
    templateUrl: 'app/components/board/view/rank/rankview.html',
    styleUrls: ['app/components/board/view/rank/rankview.css'],
    directives: [IssueComponent, IssueContextMenuComponent, PanelMenuComponent, SwimlaneEntryComponent]
})
export class RankViewComponent extends FixedHeaderView {

    private _abbreviatedHeaderRegistry:AbbreviatedHeaderRegistry = new AbbreviatedHeaderRegistry();

    constructor(_progressError:ProgressErrorService,
                _appHeaderService:AppHeaderService) {
        super(_progressError, _appHeaderService, "Rank");
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

    private getAbbreviatedHeader(state:string):string {
        return this._abbreviatedHeaderRegistry.getAbbreviatedHeader(state);
    }

    get boardData():BoardData {
        return this._boardData;
    }

    get rankedIssues():IssueData[] {
        return this._boardData.rankedIssues;
    }

    get backlogBottomHeaders():BoardHeaderEntry[] {
        if (this.backlogTopHeader) {
            return this._boardData.headers.backlogBottomHeaders;
        }
        return null;
    }

    get boardStates():State[] {
        return this._boardData.boardStates;
    }
}




import {Component} from "@angular/core";
import {IssuesService} from "../../../../services/issuesService";
import {BoardData} from "../../../../data/board/boardData";
import {ProgressErrorService} from "../../../../services/progressErrorService";
import {AppHeaderService} from "../../../../services/appHeaderService";
import {BoardHeaderEntry, State} from "../../../../data/board/header";
import {AbbreviatedHeaderRegistry} from "../../../../common/abbreviatedStateNameRegistry";
import {IssueData} from "../../../../data/board/issueData";
import {FixedHeaderView} from "../fixedHeaderView";


@Component({
    selector: 'rank-view',
    inputs: ["boardCode", "issuesService", "boardData"],
    outputs: ["showIssueContextMenu", "showParallelTaskMenu"],
    templateUrl: './rankview.html',
    styleUrls: ['./rankview.css']
})
export class RankViewComponent extends FixedHeaderView {

    private _abbreviatedHeaderRegistry:AbbreviatedHeaderRegistry = new AbbreviatedHeaderRegistry();
    private _mainStates:State[];

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
        if (!this._mainStates) {
            let boardStates:State[] = this._boardData.boardStates;
            this._mainStates = [];
            //Don't show the done states
            for (let state of boardStates) {
                if (!state.done) {
                    this._mainStates.push(state);
                }
            }
        }
        return this._mainStates;
    }
}




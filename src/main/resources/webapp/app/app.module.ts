import {NgModule} from "@angular/core";
import {BrowserModule, Title} from "@angular/platform-browser";
import {App} from "./app";
import {ReactiveFormsModule, FormsModule} from "@angular/forms";
import {ROUTING} from "./routes";
import {IssueContextMenuComponent} from "./components/board/issueContextMenu/issueContextMenu";
import {KanbanViewComponent} from "./components/board/view/kanban/kanbanview";
import {RankViewComponent} from "./components/board/view/rank/rankview";
import {PanelMenuComponent} from "./components/board/panelMenu/panelMenu";
import {IssueComponent} from "./components/board/issue/issue";
import {ControlPanelComponent} from "./components/board/controlPanel/controlPanel";
import {HealthPanelComponent} from "./components/board/healthPanel/healthPanel";
import {HttpModule} from "@angular/http";
import {SwimlaneEntryComponent} from "./components/board/swimlaneEntry/swimlaneEntry";
import {BoardsComponent} from "./components/boards/boards";
import {DbExplorerComponent} from "./components/dbexplorer/dbexplorer";
import {AccessLogViewComponent} from "./components/access/accessLogView";
import {BoardComponent} from "./components/board/board";
import {ProgressErrorService} from "./services/progressErrorService";
import {AppHeaderService} from "./services/appHeaderService";
import {LocationStrategy, APP_BASE_HREF, HashLocationStrategy} from "@angular/common";
import {ConfigComponent} from "./components/config/config";

@NgModule({
    // module dependencies
    imports: [ BrowserModule, ReactiveFormsModule, ROUTING, FormsModule, HttpModule],
    // components and directives
    declarations:
        [
            App,
            AccessLogViewComponent,
            BoardComponent,
            BoardsComponent,
            ControlPanelComponent,
            ConfigComponent,
            DbExplorerComponent,
            HealthPanelComponent,
            IssueComponent,
            IssueContextMenuComponent,
            KanbanViewComponent,
            PanelMenuComponent,
            RankViewComponent,
            SwimlaneEntryComponent],
    // root component
    bootstrap: [ App ],
    //Services
    providers: [
        AppHeaderService,
        ProgressErrorService,
        Title,
        {provide: LocationStrategy, useClass: HashLocationStrategy},
        {provide: APP_BASE_HREF, useValue: '../../app/'}]
})
export class AppModule { }

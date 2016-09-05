import {RouterModule, Routes} from "@angular/router";
import {BoardsComponent} from "./components/boards/boards";
import {ConfigComponent} from "./components/config/config";
import {DbExplorerComponent} from "./components/dbexplorer/dbexplorer";
import {BoardComponent} from "./components/board/board";
import {AccessLogViewComponent} from "./components/access/accessLogView";

const JIRBAN_ROUTES: Routes = [
    { path: '', redirectTo: '/boards', pathMatch: 'full'},
    { path: 'boards', component: BoardsComponent },
    { path: 'board', component: BoardComponent },
    { path: 'config', component: ConfigComponent },
    { path: 'access-log', component: AccessLogViewComponent },
    { path: 'dbexplorer', component: DbExplorerComponent }
];

export const ROUTING = RouterModule.forRoot(JIRBAN_ROUTES);
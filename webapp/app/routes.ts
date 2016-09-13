import {RouterModule, Routes} from "@angular/router";
import {AccessLogViewComponent} from "./components/access/accessLogView";
import {DbExplorerComponent} from "./components/dbexplorer/dbexplorer";
import {ConfigComponent} from "./components/config/config";
import {BoardsComponent} from "./components/boards/boards";
import {BoardComponent} from "./components/board/board";

const JIRBAN_ROUTES: Routes = [
    { path: '', redirectTo: '/boards', pathMatch: 'full'},
    { path: 'boards', component: BoardsComponent },
    { path: 'board', component: BoardComponent },
    { path: 'access-log', component: AccessLogViewComponent },
    { path: 'config', component: ConfigComponent },
    { path: 'dbexplorer', component: DbExplorerComponent }
];

export const ROUTING = RouterModule.forRoot(JIRBAN_ROUTES);
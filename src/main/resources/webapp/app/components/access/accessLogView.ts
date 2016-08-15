
import {Component} from "@angular/core";
import {AccessLogService} from "../../services/accessLogService";
import {ProgressErrorService} from "../../services/progressErrorService";
import {AccessLogEntry} from "../../data/access/accessLogEntry";

@Component({
    providers: [AccessLogService],
    selector: 'access-log',
    templateUrl: 'app/components/access/accessLogView.html'
})
export class AccessLogViewComponent {
    //This is a bit lazy, it would be nice to have a class
    entries:AccessLogEntry[] = [];

    constructor(progressError:ProgressErrorService, accessLogService:AccessLogService) {
        progressError.startProgress(true);
        accessLogService.getAccessLog()
            .subscribe(
                data => {
                    this.entries = AccessLogEntry.deserialize(data);
                },
                err => {
                    progressError.setError(err);
                },
                () => {
                    progressError.finishProgress();
                }
            )

    }

}
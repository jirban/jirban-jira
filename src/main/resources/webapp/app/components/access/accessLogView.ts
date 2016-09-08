
import {Component} from "@angular/core";
import {AccessLogService} from "../../services/accessLogService";
import {ProgressErrorService} from "../../services/progressErrorService";
import {AccessLogData} from "../../data/access/accessLogData";

@Component({
    providers: [AccessLogService],
    selector: 'access-log',
    templateUrl: './accessLogView.html'
})
export class AccessLogViewComponent {

    accessData:AccessLogData;

    constructor(progressError:ProgressErrorService, accessLogService:AccessLogService) {
        progressError.startProgress(true);
        accessLogService.getAccessLog()
            .subscribe(
                data => {
                    this.accessData = AccessLogData.deserialize(data);
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
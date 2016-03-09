import {Injectable} from "angular2/core";

/**
 * Shared state injected to places where we should display a progress bar/and or an error.
 */
@Injectable()
export class ProgressErrorService {

    private _blocking:boolean = true;
    private _progress:boolean = false;
    private _error:string;

    /**
     * Called to start displaying the progress indicator
     */
    startProgress(blocking:boolean):void {
        this._blocking = blocking;
        this._progress = true;
    }

    /**
     * Called to hide the progress indicator, i.e. our work is done
     */
    finishProgress():void {
        this._progress = false;
    }

    /**
     * Called to set an error to be displayed
     */
    setError(error:any) {
        console.log("Setting error: " + error);
        let err:string;
        if (error.status == 401) {
            err = "You do not appear to be logged in.";
        } else if (error.status == 403) {
            err = "You do not appear to have the necessary permissions.";
        } else if (error.status == 404) {
            err = "The requested resource could not be found.";
        } else {
            if (typeof error === "string") {
                err = error;
            } else if (typeof error === "object") {
                err = error.message;
                if (!err) {
                    let body = error._body;
                    if (body) {
                        body = JSON.parse(body);
                        if (body.message) {
                            err = body.message;
                        } else {
                            err = JSON.stringify(body);
                        }
                    } else {
                        err = JSON.stringify(error);
                    }
                }
            }
        }
        this._progress = false;
        this._error = err;
    }

    setErrorString(error:string) {
        console.log("Setting error: " + error);
        this._progress = false;
        this._error = error;
    }


    getError():string {
        return this._error;
    }

    /**
     * Clears the error
     */
    clearError():void {
        this._error = null;
    }

    /**
     * Return whether we are in progress or not
     * @returns {boolean} true if we are in progress
     */
    get progress():boolean {
        return this._progress;
    }

    displayProgressIcon():boolean {
        return this._progress && this._blocking;
    }
}
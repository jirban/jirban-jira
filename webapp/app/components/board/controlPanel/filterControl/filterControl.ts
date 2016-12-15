import {Component, ChangeDetectionStrategy, EventEmitter} from "@angular/core";

@Component({
    selector: 'filter-control',
    outputs: ['filterControlEvent'],
    templateUrl: './filterControl.html',
    styleUrls: ['./filterControl.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class FilterControlComponent {
    private filterControlEvent:EventEmitter<FilterControlAction> = new EventEmitter<FilterControlAction>();

    constructor() {
    }

    private onClickAll(event:MouseEvent):void {
        this.raiseEvent(FilterControlAction.ALL);
    }

    private onClickClear(event:MouseEvent):void {
        this.raiseEvent(FilterControlAction.CLEAR);
    }

    private onClickInvert(event:MouseEvent):void {
        this.raiseEvent(FilterControlAction.INVERT);
    }

    private raiseEvent(action:FilterControlAction) {
        console.log("Raising filter control event " + action);
        this.filterControlEvent.emit(action);
    }
}

export enum FilterControlAction {
    ALL, CLEAR, INVERT
}
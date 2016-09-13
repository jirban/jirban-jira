import {Component, EventEmitter} from "@angular/core";
import {BoardData} from "../../../data/board/boardData";
import {Hideable} from "../../../common/hide";

@Component({
    selector: 'panel-menu',
    inputs: ['view'],
    outputs: ['toggleView'],
    templateUrl: './panelMenu.html',
    styleUrls: ['./panelMenu.css']
})
export class PanelMenuComponent implements Hideable {
    private controlPanel:boolean = false;
    private healthPanel:boolean = false;
    private toggleView:EventEmitter<any> = new EventEmitter<any>();
    private view:boolean;

    constructor(private boardData:BoardData) {
        this.boardData.registerHideable(this);
    }

    private toggleControlPanel() {
        this.healthPanel = false;
        this.controlPanel = !this.controlPanel;
    }

    private toggleHealthPanel() {
        this.controlPanel = false;
        this.healthPanel = !this.healthPanel;
    }

    private toggleViewEvent():void{
        this.controlPanel = false;
        this.healthPanel = false;
        this.toggleView.emit({});
    }

    hide():void {
        this.controlPanel = false;
        this.healthPanel = false;
    }

    onCloseHealthPanel(event:Event) : void {
        this.healthPanel = false;
    }

    onCloseControlPanel(event:Event) : void {
        this.controlPanel = false;
    }
}

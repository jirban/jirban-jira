import {Component} from 'angular2/core';
import {BoardData} from '../../../data/board/boardData';
import {ControlPanelComponent} from '../controlPanel/controlPanel';
import {HealthPanelComponent} from "../healthPanel/healthPanel";
import {Hideable} from "../../../common/hide";

@Component({
    selector: 'panel-menu',
    templateUrl: 'app/components/board/panelMenu/panelMenu.html',
    styleUrls: ['app/components/board/panelMenu/panelMenu.css'],
    directives: [ControlPanelComponent, HealthPanelComponent]

})
export class PanelMenuComponent implements Hideable {
    private controlPanel:boolean = false;
    private healthPanel:boolean = false;

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

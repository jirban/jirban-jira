System.register(['angular2/core', '../../../data/board/boardData', '../controlPanel/controlPanel', "../healthPanel/healthPanel"], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, boardData_1, controlPanel_1, healthPanel_1;
    var PanelMenuComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (boardData_1_1) {
                boardData_1 = boardData_1_1;
            },
            function (controlPanel_1_1) {
                controlPanel_1 = controlPanel_1_1;
            },
            function (healthPanel_1_1) {
                healthPanel_1 = healthPanel_1_1;
            }],
        execute: function() {
            PanelMenuComponent = (function () {
                function PanelMenuComponent(boardData) {
                    this.boardData = boardData;
                    this.controlPanel = false;
                    this.healthPanel = false;
                    this.boardData.registerHideable(this);
                }
                PanelMenuComponent.prototype.toggleControlPanel = function () {
                    this.healthPanel = false;
                    this.controlPanel = !this.controlPanel;
                };
                PanelMenuComponent.prototype.toggleHealthPanel = function () {
                    this.controlPanel = false;
                    this.healthPanel = !this.healthPanel;
                };
                PanelMenuComponent.prototype.hide = function () {
                    this.controlPanel = false;
                    this.healthPanel = false;
                };
                PanelMenuComponent.prototype.onCloseHealthPanel = function (event) {
                    this.healthPanel = false;
                };
                PanelMenuComponent.prototype.onCloseControlPanel = function (event) {
                    this.controlPanel = false;
                };
                PanelMenuComponent = __decorate([
                    core_1.Component({
                        selector: 'panel-menu'
                    }),
                    core_1.View({
                        templateUrl: 'app/components/board/panelMenu/panelMenu.html',
                        styleUrls: ['app/components/board/panelMenu/panelMenu.css'],
                        directives: [controlPanel_1.ControlPanelComponent, healthPanel_1.HealthPanelComponent]
                    }), 
                    __metadata('design:paramtypes', [boardData_1.BoardData])
                ], PanelMenuComponent);
                return PanelMenuComponent;
            })();
            exports_1("PanelMenuComponent", PanelMenuComponent);
        }
    }
});
//# sourceMappingURL=panelMenu.js.map
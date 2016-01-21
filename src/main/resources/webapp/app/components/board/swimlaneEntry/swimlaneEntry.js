System.register(['angular2/core', '../issue/issue'], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, issue_1;
    var SwimlaneEntryComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (issue_1_1) {
                issue_1 = issue_1_1;
            }],
        execute: function() {
            /**
             * This is here to be able to add a header and the contents for a swimlane
             * as several rows
             */
            SwimlaneEntryComponent = (function () {
                function SwimlaneEntryComponent() {
                    this.issueContextMenu = new core_1.EventEmitter();
                }
                Object.defineProperty(SwimlaneEntryComponent.prototype, "boardStates", {
                    get: function () {
                        return this.boardData.boardStates;
                    },
                    enumerable: true,
                    configurable: true
                });
                SwimlaneEntryComponent.prototype.toCharArray = function (state) {
                    var arr = [];
                    for (var i = 0; i < state.length; i++) {
                        var s = state.charAt(i);
                        if (s == " ") {
                        }
                        arr.push(s);
                    }
                    return arr;
                };
                SwimlaneEntryComponent.prototype.toggleSwimlane = function (index) {
                    this.boardData.toggleSwimlaneVisibility(index);
                };
                SwimlaneEntryComponent.prototype.showIssueContextMenu = function (event) {
                    this.issueContextMenu.emit(event);
                };
                Object.defineProperty(SwimlaneEntryComponent.prototype, "visibleColumns", {
                    get: function () {
                        return this.boardData.visibleColumns;
                    },
                    enumerable: true,
                    configurable: true
                });
                SwimlaneEntryComponent = __decorate([
                    core_1.Component({
                        inputs: ['swimlaneIndex', 'boardData', 'swimlane'],
                        outputs: ['issueContextMenu'],
                        selector: 'swimlane-entry'
                    }),
                    core_1.View({
                        templateUrl: 'app/components/board/swimlaneEntry/swimlaneEntry.html',
                        styleUrls: ['app/components/board//board.css', 'app/components/board/swimlaneEntry/swimlaneEntry.css'],
                        directives: [issue_1.IssueComponent]
                    }), 
                    __metadata('design:paramtypes', [])
                ], SwimlaneEntryComponent);
                return SwimlaneEntryComponent;
            })();
            exports_1("SwimlaneEntryComponent", SwimlaneEntryComponent);
        }
    }
});
//# sourceMappingURL=swimlaneEntry.js.map
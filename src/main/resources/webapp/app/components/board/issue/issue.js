System.register(['angular2/core'], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1;
    var IssueComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            }],
        execute: function() {
            IssueComponent = (function () {
                function IssueComponent() {
                    this.issueContextMenu = new core_1.EventEmitter();
                }
                Object.defineProperty(IssueComponent.prototype, "jiraUrl", {
                    get: function () {
                        return this.issue.boardData.jiraUrl;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueComponent.prototype, "hideAssignee", {
                    get: function () {
                        return !this.issue.boardData.issueDisplayDetails.assignee;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueComponent.prototype, "hideSummary", {
                    get: function () {
                        return !this.issue.boardData.issueDisplayDetails.summary;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueComponent.prototype, "hideInfo", {
                    get: function () {
                        return !this.issue.boardData.issueDisplayDetails.info;
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(IssueComponent.prototype, "hideLinkedIssues", {
                    get: function () {
                        return !this.issue.boardData.issueDisplayDetails.linkedIssues;
                    },
                    enumerable: true,
                    configurable: true
                });
                IssueComponent.prototype.getStatusFraction = function (issue) {
                    //This should only be called for linked projects
                    return issue.statusIndex + "/" + (issue.linkedProject.statesLength - 1);
                };
                IssueComponent.prototype.getStatusColour = function (issue) {
                    if (issue.statusIndex == 0) {
                        //Progress has not been started
                        return "red";
                    }
                    var length = issue.linkedProject.statesLength;
                    if (length - 1 == issue.statusIndex) {
                        //It is fully done
                        return "green";
                    }
                    //We are somewhere in the middle, for now return orange
                    return "orange";
                };
                IssueComponent.prototype.showIssueContextMenuEvent = function (event, issueId) {
                    this.issueContextMenu.emit({
                        x: event.clientX,
                        y: event.clientY,
                        issueId: issueId
                    });
                    event.preventDefault();
                };
                IssueComponent.prototype.defaultContextMenu = function (event) {
                    event.stopPropagation();
                };
                IssueComponent = __decorate([
                    core_1.Component({
                        inputs: ['issue'],
                        outputs: ['issueContextMenu'],
                        selector: 'issue'
                    }),
                    core_1.View({
                        templateUrl: 'app/components/board/issue/issue.html',
                        styleUrls: ['app/components/board/issue/issue.css'],
                    }), 
                    __metadata('design:paramtypes', [])
                ], IssueComponent);
                return IssueComponent;
            })();
            exports_1("IssueComponent", IssueComponent);
        }
    }
});
//# sourceMappingURL=issue.js.map
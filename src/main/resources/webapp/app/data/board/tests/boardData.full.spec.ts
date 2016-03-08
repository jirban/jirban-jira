/// <reference path="../../../../node_modules/angular2/ts/typings/jasmine/jasmine.d.ts" />
import {BoardData} from "./../boardData";
import {TestBoardData} from "./testData";
import {Assignee} from "./../assignee";
import {Priority} from "./../priority";
import {Indexed} from "../../../common/indexed";
import {IssueType} from "./../issueType";
import {IssueTable} from "./../issueTable";
import {IssueData} from "./../issueData";
import {JiraComponent} from "../component";

/**
 * This tests application of the expected json onto the BoardData component on the client which is so central to the display of the board.
 * The inputs into this test are of the same formats that we are checking for the server side in
 * BoardManagerTest and BoardChangeRegistryTest.
 */
describe('BoardData tests', ()=> {
//Tests for the BoardData component which is so central to the display of the board
    describe('Load', () => {

        it ('Empty board', () => {
            let data:any = TestBoardData.create(TestBoardData.OWNER_ONLY_BOARD_PROJECTS, []);

            //Delete from the standard config to simulate a project with absolutely no issues
            delete data.components;
            delete data.assignees;
            data.projects.main.TDP.issues=[[], [], [], []];

            let boardData:BoardData = new BoardData();
            boardData.deserialize(1, data);
            expect(boardData.blacklist).toBeNull();
        });

        it('Full board; No blacklist', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.FULL_BOARD_PROJECTS, TestBoardData.FULL_BOARD_ISSUES));

            expect(boardData.view).toEqual(0);
            checkAssignees(boardData, "brian", "kabir");
            checkComponents(boardData, "First", "Second");
            checkStandardPriorities(boardData);
            checkStandardIssueTypes(boardData);

            expect(boardData.swimlane).not.toBeDefined();
            expect(boardData.swimlaneTable).toBeNull();

            checkStandardProjects(boardData);

            expect(boardData.boardStates.length).toBe(4);
            checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);

            checkIssueDatas(boardData, TestBoardData.EXPECTED_FULL_BOARD);

            expect(boardData.blacklist).toBeNull();
        });

        it('Full board; Blacklist', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.FULL_BOARD_PROJECTS, TestBoardData.FULL_BOARD_ISSUES, TestBoardData.STANDARD_BLACKLIST));

            expect(boardData.view).toEqual(0);

            checkAssignees(boardData, "brian", "kabir");
            checkComponents(boardData, "First", "Second");

            checkStandardPriorities(boardData);
            checkStandardIssueTypes(boardData);

            expect(boardData.swimlane).not.toBeDefined();
            expect(boardData.swimlaneTable).toBeNull();

            checkStandardProjects(boardData);

            expect(boardData.boardStates.length).toBe(4);
            checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);

            checkIssueDatas(boardData, TestBoardData.EXPECTED_FULL_BOARD);

            expect(boardData.blacklist).toBeDefined();
            checkEntries(boardData.blacklist.issues, "TDP-100", "TBG-101");
            checkEntries(boardData.blacklist.issueTypes, "BadIssueType");
            checkEntries(boardData.blacklist.priorities, "BadPriority");
            checkEntries(boardData.blacklist.states, "BadState");
        });


        it('Owner Project Only; No blacklist', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.OWNER_ONLY_BOARD_PROJECTS, TestBoardData.OWNER_ONLY_BOARD_ISSUES));

            expect(boardData.view).toEqual(0);
            checkAssignees(boardData, "brian", "kabir");
            checkComponents(boardData, "First", "Second");
            checkStandardPriorities(boardData);
            checkStandardIssueTypes(boardData);

            expect(boardData.swimlane).not.toBeDefined();
            expect(boardData.swimlaneTable).toBeNull();

            checkProjects(boardData, null, "TDP");

            expect(boardData.boardStates.length).toBe(4);
            checkBoardLayout(boardData, TestBoardData.EXPECTED_OWNER_ONLY_BOARD);

            checkIssueDatas(boardData, TestBoardData.EXPECTED_OWNER_ONLY_BOARD);

            expect(boardData.blacklist).toBeNull();
        });

        it('Non-owner issues only; No blacklist', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.NON_OWNER_ONLY_BOARD_PROJECTS, TestBoardData.NON_OWNER_ONLY_BOARD_ISSUES));

            expect(boardData.view).toEqual(0);
            checkAssignees(boardData, "brian", "kabir");
            checkComponents(boardData, "First", "Second");
            checkStandardPriorities(boardData);
            checkStandardIssueTypes(boardData);

            expect(boardData.swimlane).not.toBeDefined();
            expect(boardData.swimlaneTable).toBeNull();

            checkProjects(boardData, null, "TDP", "TBG");

            expect(boardData.boardStates.length).toBe(4);
            checkBoardLayout(boardData, TestBoardData.EXPECTED_NON_OWNER_ONLY_BOARD);

            checkIssueDatas(boardData, TestBoardData.EXPECTED_NON_OWNER_ONLY_BOARD);

            expect(boardData.blacklist).toBeNull();
        });
    });

    describe('No Change', () => {
        it('No change', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.PRE_CHANGE_BOARD_PROJECTS, TestBoardData.PRE_CHANGE_BOARD_ISSUES));
            let changes:any = {
                changes: {
                    view: 0
                }
            };
            boardData.processChanges(changes);

            expect(boardData.blacklist).toBeNull();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });
    });


    describe('New Blacklist', () => {
        var boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.PRE_CHANGE_BOARD_PROJECTS, TestBoardData.PRE_CHANGE_BOARD_ISSUES));
            expect(boardData.blacklist).toBeNull();
        });

        it('Board unaffected', () => {
            //The blacklist change contains issues not on the board (this should not happen in real life, but it is easy to test)

            let changes:any = {
                changes: {
                    view: 5,
                    blacklist: {
                        issues: ["TDP-50", "TBG-100"],
                        states: ["BadState1", "BadState2"],
                        priorities: ["BadPriority1", "BadPriority2"],
                        "issue-types": ["BadType1", "BadType2"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(5);
            checkEntries(boardData.blacklist.issueTypes, "BadType1", "BadType2");
            checkEntries(boardData.blacklist.priorities, "BadPriority1", "BadPriority2");
            checkEntries(boardData.blacklist.states, "BadState1", "BadState2");
            checkEntries(boardData.blacklist.issues, "TDP-50", "TBG-100");

            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });

        it('Add to blacklist', () => {
            //Issues added to the blacklist should be removed from the issue table

            let changes:any = {
                changes: {
                    view: 5,
                    blacklist: {
                        issues: ["TDP-1", "TBG-1"],
                        states: ["BadState"],
                        priorities: ["BadPriority"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(5);
            expect(boardData.blacklist.issueTypes.length).toBe(0);
            checkEntries(boardData.blacklist.priorities, "BadPriority");
            checkEntries(boardData.blacklist.states, "BadState");
            checkEntries(boardData.blacklist.issues, "TDP-1", "TBG-1");

            let layout:any = [[], ["TDP-2"], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });

        it('Remove from blacklist', () => {
            //Issues removed from the blacklist should be removed from the issue table if they exist
            //This can happen if the change set includes adding the issue to the black list, and then the issue is deleted

            let changes:any = {
                changes: {
                    view: 4,
                    blacklist: {
                        "removed-issues": ["TDP-2", "TBG-1", "TBG-1000"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(4);
            expect(boardData.blacklist.issueTypes.length).toBe(0);
            expect(boardData.blacklist.priorities.length).toBe(0);
            expect(boardData.blacklist.states.length).toBe(0);
            expect(boardData.blacklist.issues.length).toBe(0);

            let layout:any = [["TDP-1"], [], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });

        it('Remove from and add to blacklist', () => {
            //Combine the two above tests to make sure everything gets removed from the issue table
            let changes:any = {
                changes: {
                    view: 4,
                    blacklist: {
                        "issue-types" : ["BadType"],
                        issues: ["TDP-1"],
                        "removed-issues": ["TBG-1"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(4);
            expect(boardData.blacklist.priorities.length).toBe(0);
            expect(boardData.blacklist.states.length).toBe(0);
            checkEntries(boardData.blacklist.issueTypes, "BadType");
            checkEntries(boardData.blacklist.issues, "TDP-1");


            let layout:any = [[], ["TDP-2"], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });
    });

    describe('Existing Blacklist', () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.PRE_CHANGE_BOARD_PROJECTS, TestBoardData.PRE_CHANGE_BOARD_ISSUES, TestBoardData.STANDARD_BLACKLIST));
            expect(boardData.blacklist).toEqual(jasmine.anything());
        });

        it('Board unaffected', () => {
            //The blacklist change contains issues not on the board (this should not happen in real life, but it is easy to test)

            let changes:any = {
                changes: {
                    view: 5,
                    blacklist: {
                        issues: ["TDP-200", "TBG-200"],
                        states: ["BadState1", "BadState2"],
                        priorities: ["BadPriority1", "BadPriority2"],
                        "issue-types": ["BadType1", "BadType2"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(5);
            checkEntries(boardData.blacklist.issueTypes, "BadIssueType", "BadType1", "BadType2");
            checkEntries(boardData.blacklist.priorities, "BadPriority", "BadPriority1", "BadPriority2");
            checkEntries(boardData.blacklist.states, "BadState", "BadState1", "BadState2");
            checkEntries(boardData.blacklist.issues, "TDP-100", "TBG-101", "TDP-200", "TBG-200");

            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });

        it('Add to blacklist', () => {
            //Issues added to the blacklist should be removed from the issue table

            let changes:any = {
                changes: {
                    view: 5,
                    blacklist: {
                        issues: ["TDP-1", "TBG-1"],
                        states: ["BadStateA"],
                        priorities: ["BadPriorityA"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(5);
            checkEntries(boardData.blacklist.issueTypes, "BadIssueType");
            checkEntries(boardData.blacklist.priorities, "BadPriority", "BadPriorityA");
            checkEntries(boardData.blacklist.states, "BadState", "BadStateA");
            checkEntries(boardData.blacklist.issues, "TDP-100", "TBG-101", "TDP-1", "TBG-1");

            let layout:any = [[], ["TDP-2"], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });

        it('Remove from blacklist', () => {
            //Issues removed from the blacklist should be removed from the issue table if they exist
            //This can happen if the change set includes adding the issue to the black list, and then the issue is deleted

            let changes:any = {
                changes: {
                    view: 4,
                    blacklist: {
                        "removed-issues": ["TDP-2", "TBG-1", "TBG-1000"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(4);
            checkEntries(boardData.blacklist.issueTypes, "BadIssueType");
            checkEntries(boardData.blacklist.priorities, "BadPriority");
            checkEntries(boardData.blacklist.states, "BadState");
            checkEntries(boardData.blacklist.issues, "TDP-100", "TBG-101");

            let layout:any = [["TDP-1"], [], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });

        it('Remove from and add to blacklist', () => {
            //Combine the two above tests to make sure everything gets removed from the issue table
            let changes:any = {
                changes: {
                    view: 4,
                    blacklist: {
                        "issue-types" : ["BadTypeA"],
                        issues: ["TDP-1"],
                        "removed-issues": ["TBG-1", "TBG-1000"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(4);
            checkEntries(boardData.blacklist.issueTypes, "BadIssueType", "BadTypeA");
            checkEntries(boardData.blacklist.priorities, "BadPriority");
            checkEntries(boardData.blacklist.states, "BadState");
            checkEntries(boardData.blacklist.issues, "TDP-100", "TBG-101", "TDP-1");


            let layout:any = [[], ["TDP-2"], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });
    });

    describe('Delete issues', () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.PRE_CHANGE_BOARD_PROJECTS, TestBoardData.PRE_CHANGE_BOARD_ISSUES));
        });

        it('Delete issue', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "delete" : ["TDP-1"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [[], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });


        it('Delete issues', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "delete" : ["TDP-2", "TBG-1"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [["TDP-1"], [], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });

        it('Delete issue and add to backlog', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "delete" : ["TDP-2"]
                    },
                    blacklist: {
                        "issue-types" : ["BadTypeA"],
                        issues: ["TDP-1"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist.states.length).toBe(0);
            expect(boardData.blacklist.priorities.length).toBe(0);
            checkEntries(boardData.blacklist.issueTypes, "BadTypeA");

            let layout:any = [[], ["TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });

        it('Delete issue and remove from blacklist', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "delete" : ["TDP-2"]
                    },
                    blacklist: {
                        "removed-issues": ["TBG-1"]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);

            let layout:any = [["TDP-1"], [], [], []];
            checkBoardLayout(boardData, layout);
            checkIssueDatas(boardData, layout);
        });
    });

    describe('Update issues - no state change ; ', () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.PRE_CHANGE_BOARD_PROJECTS, TestBoardData.PRE_CHANGE_BOARD_ISSUES));
        });

        it ('Update issue type', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TDP-1",
                            type: "bug"
                        }]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TDP-1");
            expect(updatedIssue.key).toBe("TDP-1");
            checkBoardIssue(updatedIssue, "TDP-1", "bug", "highest", "brian", ["First"], "One");

        });

        it ('Update priority', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TDP-2",
                            priority: "low"
                        }]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TDP-2");
            expect(updatedIssue.key).toBe("TDP-2");
            checkBoardIssue(updatedIssue, "TDP-2", "bug", "low", "kabir", ["Second"], "Two");
        });

        it ('Update summary', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TBG-1",
                            summary: "Uno"
                        }]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TBG-1");
            expect(updatedIssue.key).toBe("TBG-1");
            checkBoardIssue(updatedIssue, "TBG-1", "task", "highest", "brian", ["First"], "Uno");
        });

        it ('Unassign', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TBG-1",
                            unassigned: true
                        }]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TBG-1");
            expect(updatedIssue.key).toBe("TBG-1");
            checkBoardIssue(updatedIssue, "TBG-1", "task", "highest", null, ["First"], "One");
        });

        it ('Update assignee (not new on board)', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TBG-1",
                            assignee: "kabir"
                        }]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TBG-1");
            expect(updatedIssue.key).toBe("TBG-1");
            checkBoardIssue(updatedIssue, "TBG-1", "task", "highest", "kabir", ["First"], "One");
        });

        it('Update assignee (new on board)', () => {

            checkAssignees(boardData, "brian", "kabir");
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TBG-1",
                            assignee: "jason"
                        }]
                    },
                    assignees : [{
                        key : "jason",
                        email : "jason@example.com",
                        avatar : "/avatars/jason.png",
                        name : "Jason Greene"
                    }]
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            checkAssignees(boardData, "brian", "jason", "kabir");
            checkComponents(boardData, "First", "Second");


            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TBG-1");
            expect(updatedIssue.key).toBe("TBG-1");
            checkBoardIssue(updatedIssue, "TBG-1", "task", "highest", "jason", ["First"], "One");
        });

        it ('Clear component', () => {
            checkComponents(boardData, "First", "Second");
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TBG-1",
                            "clear-components": true
                        }]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TBG-1");
            expect(updatedIssue.key).toBe("TBG-1");
            checkBoardIssue(updatedIssue, "TBG-1", "task", "highest", "brian", null, "One");
            checkComponents(boardData, "First", "Second");
        });

        it ('Update components (not new on board)', () => {
            checkComponents(boardData, "First", "Second");
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TBG-1",
                            components: ["Second"]
                        }]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TBG-1");
            expect(updatedIssue.key).toBe("TBG-1");
            checkBoardIssue(updatedIssue, "TBG-1", "task", "highest", "brian", ["Second"], "One");
            checkComponents(boardData, "First", "Second");
        });

        it('Update components (new on board)', () => {

            checkComponents(boardData, "First", "Second");
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TBG-1",
                            components: ["Third", "Fourth"]
                        }]
                    },
                    components : ["Third", "Fourth"]
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            checkComponents(boardData, "First", "Fourth", "Second", "Third");


            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TBG-1");
            expect(updatedIssue.key).toBe("TBG-1");
            checkBoardIssue(updatedIssue, "TBG-1", "task", "highest", "brian", ["Third", "Fourth"], "One");
        });

    });

    describe('Update issues - state change', () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.PRE_CHANGE_BOARD_PROJECTS, TestBoardData.PRE_CHANGE_BOARD_ISSUES));
        });

        it ('Update main project to populated state', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TDP-1",
                            type: "bug",
                            state: "TDP-B"
                        }]
                    },
                    states: {
                        TDP : {
                            "TDP-B" : ["TDP-1", "TDP-2"]
                        }
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [[], ["TDP-1", "TDP-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TDP-1");
            expect(updatedIssue.key).toBe("TDP-1");
            checkBoardIssue(updatedIssue, "TDP-1", "bug", "highest", "brian", ["First"], "One");
        });

        it ('Update main project to unpopulated state', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TDP-1",
                            type: "bug",
                            state: "TDP-C"
                        }]
                    },
                    states: {
                        TDP : {
                            "TDP-C" : ["TDP-1"]
                        }
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [[], ["TDP-2", "TBG-1"], ["TDP-1"], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TDP-1");
            expect(updatedIssue.key).toBe("TDP-1");
            checkBoardIssue(updatedIssue, "TDP-1", "bug", "highest", "brian", ["First"], "One");
        });

        it ('Update other project', () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [{
                            key: "TBG-1",
                            type: "bug",
                            state: "TBG-Y"
                        }]
                    },
                    states: {
                        TBG : {
                            "TBG-Y" : ["TBG-1"]
                        }
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let layout:any = [["TDP-1"], ["TDP-2"], ["TBG-1"], []];
            checkBoardLayout(boardData, layout);
            let updatedIssue:IssueData = checkIssueDatas(boardData, layout, "TBG-1");
            expect(updatedIssue.key).toBe("TBG-1");
            checkBoardIssue(updatedIssue, "TBG-1", "bug", "highest", "brian", ["First"], "One");
        });
    });


    describe("Create issue", () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.PRE_CHANGE_BOARD_PROJECTS, TestBoardData.PRE_CHANGE_BOARD_ISSUES));
        });

        it ("Main project to populated state - no new assignee", () => {
            checkAssignees(boardData, "brian", "kabir");
            checkComponents(boardData, "First", "Second");
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "new" : [{
                            key: "TDP-3",
                            state : "TDP-B",
                            summary : "Three",
                            priority : "high",
                            type : "bug",
                            assignee : "kabir"

                        }]
                    },
                    states: {
                        TDP : {
                            "TDP-B" : ["TDP-2", "TDP-3"]
                        }
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            checkAssignees(boardData, "brian", "kabir");


            let layout:any = [["TDP-1"], ["TDP-2", "TDP-3", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let createdIssue:IssueData = checkIssueDatas(boardData, layout, "TDP-3");
            expect(createdIssue.key).toBe("TDP-3");
            checkBoardIssue(createdIssue, "TDP-3", "bug", "high", "kabir", null, "Three");
        });

        it ("Main project to unpopulated state  - new assignee", () => {
            checkAssignees(boardData, "brian", "kabir");
            checkComponents(boardData, "First", "Second");
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "new" : [{
                            key: "TDP-3",
                            state : "TDP-C",
                            summary : "Three",
                            priority : "high",
                            type : "bug",
                            assignee : "jason"

                        }]
                    },
                    states: {
                        TDP : {
                            "TDP-C" : ["TDP-3"]
                        }
                    },
                    assignees : [{
                        key : "jason",
                        email : "jason@example.com",
                        avatar : "/avatars/jason.png",
                        name : "Jason Greene"
                    }]
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            checkAssignees(boardData, "brian", "jason", "kabir");
            checkComponents(boardData, "First", "Second");


            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], []];
            checkBoardLayout(boardData, layout);
            let createdIssue:IssueData = checkIssueDatas(boardData, layout, "TDP-3");
            expect(createdIssue.key).toBe("TDP-3");
            checkBoardIssue(createdIssue, "TDP-3", "bug", "high", "jason", null, "Three");
        });

        it ("Other project populated state", () => {
            checkAssignees(boardData, "brian", "kabir");
            checkComponents(boardData, "First", "Second");
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "new" : [{
                            key: "TBG-2",
                            state : "TBG-X",
                            summary : "Two",
                            priority : "high",
                            type : "bug",
                            assignee : "kabir"

                        }]
                    },
                    states: {
                        TBG : {
                            "TBG-X" : ["TBG-2", "TBG-1"]
                        }
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            checkAssignees(boardData, "brian", "kabir");
            checkComponents(boardData, "First", "Second");


            let layout:any = [["TDP-1"], ["TDP-2", "TBG-2", "TBG-1"], [], []];
            checkBoardLayout(boardData, layout);
            let createdIssue:IssueData = checkIssueDatas(boardData, layout, "TBG-2");
            expect(createdIssue.key).toBe("TBG-2");
            checkBoardIssue(createdIssue, "TBG-2", "bug", "high", "kabir", null, "Two");
        });

        it ("Other project unpopulated state", () => {
            checkAssignees(boardData, "brian", "kabir");
            checkComponents(boardData, "First", "Second");
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "new" : [{
                            key: "TBG-2",
                            state : "TBG-Y",
                            summary : "Two",
                            priority : "highest",
                            type : "feature",
                            assignee : "brian"

                        }]
                    },
                    states: {
                        TBG : {
                            "TBG-Y" : ["TBG-2"]
                        }
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            checkAssignees(boardData, "brian", "kabir");
            checkComponents(boardData, "First", "Second");


            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TBG-2"], []];
            checkBoardLayout(boardData, layout);
            let createdIssue:IssueData = checkIssueDatas(boardData, layout, "TBG-2");
            expect(createdIssue.key).toBe("TBG-2");
            checkBoardIssue(createdIssue, "TBG-2", "feature", "highest", "brian", null, "Two");
        });
    });

    //TODO a test involving several updates, deletes and state changes


    describe("Filter tests", () => {

        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.FULL_BOARD_PROJECTS, TestBoardData.FULL_BOARD_ISSUES));
        });

        it("Project", () => {
            boardData.updateFilters({"TDP": true}, {}, {}, {}, {});

            checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
            checkFiltered(boardData,
                ["TBG-1", "TBG-2", "TBG-3", "TBG-4"],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7"]);

            boardData.updateFilters({"TDP": true, "TBG": true}, {}, {}, {}, {});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

            boardData.updateFilters({"TBG": true}, {}, {}, {}, {});
            checkFiltered(boardData,
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7"],
                ["TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

            boardData.updateFilters({}, {}, {}, {}, {});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

        });

        it("Priority", () => {
            boardData.updateFilters({}, {"highest":true}, {}, {}, {});
            checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
            checkFiltered(boardData,
                ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                ["TDP-1", "TDP-5", "TBG-1"]);

            boardData.updateFilters({}, {"high":true}, {}, {}, {});
            checkFiltered(boardData,
                ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                ["TDP-2", "TDP-6", "TBG-2"]);

            boardData.updateFilters({}, {"highest":true, "high":true, "low":true, "lowest":true}, {}, {}, {});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

            boardData.updateFilters({}, {"low":true, "lowest":true}, {}, {}, {});
            checkFiltered(boardData,
                ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

            boardData.updateFilters({}, {}, {}, {}, {});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);
        });


        it("Issue Type", () => {
            boardData.updateFilters({}, {}, {"task":true}, {}, {});
            checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
            checkFiltered(boardData,
                ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                ["TDP-1", "TDP-5", "TBG-1"]);

            boardData.updateFilters({}, {}, {"bug":true}, {}, {});
            checkFiltered(boardData,
                ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                ["TDP-2", "TDP-6", "TBG-2"]);

            boardData.updateFilters({}, {}, {"task":true, "bug":true, "feature":true, "issue":true}, {}, {});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

            boardData.updateFilters({}, {}, {"feature":true, "issue":true}, {}, {});
            checkFiltered(boardData,
                ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

            boardData.updateFilters({}, {}, {}, {}, {});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);
        });

        it ("Assignee", () => {
            boardData.updateFilters({}, {}, {}, {"brian":true}, {});
            checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
            checkFiltered(boardData,
                ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                ["TDP-1", "TDP-5", "TBG-1"]);

            boardData.updateFilters({}, {}, {}, {"kabir":true}, {});
            checkFiltered(boardData,
                ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                ["TDP-2", "TDP-6", "TBG-2"]);

            boardData.updateFilters({}, {}, {}, {"$no$assignee":true, "brian":true, "kabir":true}, {});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

            boardData.updateFilters({}, {}, {}, {"$no$assignee":true}, {});
            checkFiltered(boardData,
                ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

            boardData.updateFilters({}, {}, {}, {}, {});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

        });

        it ("Component (single per issue)", () => {
            //Components are a bit different from all the filters, since an issue may have more than one component
            boardData.updateFilters({}, {}, {}, {}, {"First":true});
            checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
            checkFiltered(boardData,
                ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                ["TDP-1", "TDP-5", "TBG-1"]);

            boardData.updateFilters({}, {}, {}, {}, {"Second":true});
            checkFiltered(boardData,
                ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                ["TDP-2", "TDP-6", "TBG-2"]);

            boardData.updateFilters({}, {}, {}, {}, {"$no$component":true});
            checkFiltered(boardData,
                ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

            boardData.updateFilters({}, {}, {}, {}, {"$no$component":true, "First":true, "Second":true});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


            //Make sure that if selecting more than one component we get all the issues which have EITHER
            boardData.updateFilters({}, {}, {}, {}, {"First":true, "Second":true});
            checkFiltered(boardData,
                ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"],
                ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"]);


            boardData.updateFilters({}, {}, {}, {}, {});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


        });

        it ("Component (several per issue)", () => {
            //Components are a bit different from all the filters, since an issue may have more than one component

            //Add some components to some issues
            checkComponents(boardData, "First", "Second");
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [
                            {key: "TDP-3", components: ["First", "Second"]},
                            {key: "TDP-7", components: ["First", "Second"]},
                            {key: "TBG-3", components: ["First", "Second"]}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());
            boardData.updateFilters({}, {}, {}, {}, {"First":true});
            checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
            checkFiltered(boardData,
                ["TDP-2", "TDP-4", "TDP-6", "TBG-2", "TBG-4"],
                ["TDP-1", "TDP-3", "TDP-5", "TDP-7", "TBG-1", "TBG-3"]);

            boardData.updateFilters({}, {}, {}, {}, {"Second":true});
            checkFiltered(boardData,
                ["TDP-1", "TDP-4", "TDP-5", "TBG-1", "TBG-4"],
                ["TDP-2", "TDP-3", "TDP-6", "TDP-7", "TBG-2", "TBG-3"]);

            boardData.updateFilters({}, {}, {}, {}, {"$no$component":true});
            checkFiltered(boardData,
                ["TDP-1", "TDP-2", "TDP-3", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3"],
                ["TDP-4", "TBG-4"]);

            boardData.updateFilters({}, {}, {}, {}, {"$no$component":true, "First":true, "Second":true});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


            //Make sure that if selecting more than one component we get all the issues which have EITHER
            boardData.updateFilters({}, {}, {}, {}, {"First":true, "Second":true});
            checkFiltered(boardData,
                ["TDP-4", "TBG-4"],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3"]);


            boardData.updateFilters({}, {}, {}, {}, {});
            checkFiltered(boardData,
                [],
                ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);
        });
    });

    function checkEntries(value:string[], ...expected:string[]) {
        expect(value.length).toBe(expected.length);
        for (let ex of expected) {
            expect(value).toContain(ex);
        }
    }

    /**
     * This verifies the issues against the original setup for the board which uses 'calculable' settings.
     * When the board has changed, we can pass in an issue to skip. It will be returned, and manual
     * verification can happen.
     *
     * @param boardData the current board data
     * @param layout the board layout
     * @param skipKey the issue key to skip
     * @returns {IssueData} the issue that was skipped
     */
    function checkIssueDatas(boardData:BoardData, layout:string[][], skipKey?:string) : IssueData {
        //If 'skipKey' is set, we return the matching issue for manual checks
        let skippedIssue:IssueData;
        for (let i:number = 0; i < layout.length; i++) {
            for (let j:number = 0; j < layout[i].length; j++) {
                let issue:IssueData = boardData.getIssue(layout[i][j]);
                if (skipKey && skipKey == issue.key) {
                    skippedIssue = issue;
                    continue;
                }
                let id:number = getIdFromKey(issue.key);
                let mod4 = (id - 1) % 4;
                switch (mod4) {
                    case 0:
                        checkBoardIssue(issue, issue.key, "task", "highest", "brian", ["First"]);
                        break;
                    case 1:
                        checkBoardIssue(issue, issue.key, "bug", "high", "kabir", ["Second"]);
                        break;
                    case 2:
                        checkBoardIssue(issue, issue.key, "feature", "low", null, null);
                        break;
                    case 3:
                        checkBoardIssue(issue, issue.key, "issue", "lowest", null, null);
                        break;
                }
                checkIssueConvenienceMethods(issue);
            }
        }
        return skippedIssue;
    }

    function checkBoardIssue(issue:IssueData, key:string, type:string, priority:string, assignee:string, components:string[], summary?:string) {
        expect(issue.key).toEqual(key);
        checkBoardIssueType(issue.type, type);
        checkBoardPriority(issue.priority, priority);
        if (assignee) {
            checkBoardAssignee(issue.assignee, assignee);
        } else {
            expect(issue.assignee).not.toEqual(jasmine.anything());
        }

        if (components) {
            expect(issue.components).toEqual(jasmine.anything());
            checkBoardComponents(issue.components.array, components);
        } else {
            expect(issue.components).not.toEqual(jasmine.anything());
        }

        if (summary) {
            expect(issue.summary).toEqual(summary);
        }
        checkIssueConvenienceMethods(issue);
        checkIssueConvenienceMethods(issue);
    }



    function checkBoardLayout(boardData:BoardData, layout:string[][]) {
        let issueTable:IssueData[][] = boardData.issueTable;
        expect(issueTable.length).toBe(layout.length);
        for (let i:number = 0; i < layout.length; i++) {
            let columnData:IssueData[] = issueTable[i];
            let columnLayout:string[] = layout[i];

            expect(boardData.totalIssuesByState[i]).toBe(columnLayout.length);

            expect(columnData.length).toBe(columnLayout.length, "The length of column is different " + i);
            for (let j:number = 0; j < columnLayout.length; j++) {
                expect(columnData[j].key).toBe(columnLayout[j]);

                //Check the states are mapped property in both projects
                let issue:IssueData = columnData[j];
                if (issue.projectCode === "TDP") {
                    expect(issue.statusIndex).toBe(i);
                    if (i == 0) {
                        expect(issue.boardStatus).toBe("S-1");
                        expect(issue.ownStatus).toBe("TDP-A");
                    } else if (i == 1) {
                        expect(issue.boardStatus).toBe("S-2");
                        expect(issue.ownStatus).toBe("TDP-B");
                    } else if (i == 2) {
                        expect(issue.boardStatus).toBe("S-3");
                        expect(issue.ownStatus).toBe("TDP-C");
                    } else if (i == 3) {
                        expect(issue.boardStatus).toBe("S-4");
                        expect(issue.ownStatus).toBe("TDP-D");
                    } else {
                        fail("Bad TDP state index " + i);
                    }
                } else if (issue.projectCode === "TBG") {
                    expect(issue.statusIndex).toBe(i - 1);
                    if (i == 1) {
                        expect(issue.boardStatus).toBe("S-2");
                        expect(issue.ownStatus).toBe("TBG-X");
                    } else if (i == 2) {
                        expect(issue.boardStatus).toBe("S-3");
                        expect(issue.ownStatus).toBe("TBG-Y");
                    } else {
                        fail("Bad TBG state index " + i);
                    }
                } else {
                    fail("Bad project " + issue.projectCode);
                }
            }
        }
    }

    function getIdFromKey(issueKey:string):number {
        let index:number = issueKey.indexOf("-");
        expect(index).toBeGreaterThan(0);
        let sub:string = issueKey.substr(index + 1);
        return Number(sub);
    }


    function checkAssignees(boardData:BoardData, ...assignees:string[]) {
        expect(boardData.assignees.array.length).toBe(assignees.length);
        for (let i = 0; i < assignees.length; i++) {
            checkBoardAssignee(boardData.assignees.forIndex(i), assignees[i]);
        }
    }

    function checkComponents(boardData:BoardData, ...components:string[]) {
        expect(boardData.components.array.length).toBe(components.length);
        for (let i = 0 ; i < components.length ; i++) {
            expect(boardData.components.forIndex(i).name).toEqual(components[i]);
        }
    }

    function checkStandardPriorities(boardData:BoardData) {
        let priorities:Indexed<Priority> = boardData.priorities;
        expect(priorities.array.length).toEqual(4);
        checkBoardPriority(priorities.array[0], "highest");
        checkBoardPriority(priorities.array[1], "high");
        checkBoardPriority(priorities.array[2], "low");
        checkBoardPriority(priorities.array[3], "lowest");
    }

    function checkStandardIssueTypes(boardData:BoardData) {
        let issueTypes:Indexed<IssueType> = boardData.issueTypes;
        expect(issueTypes.array.length).toEqual(4);
        checkBoardIssueType(issueTypes.array[0], "task");
        checkBoardIssueType(issueTypes.array[1], "bug");
        checkBoardIssueType(issueTypes.array[2], "feature");
        checkBoardIssueType(issueTypes.array[3], "issue");
    }

    function checkBoardAssignee(assignee:Assignee, key:string) {
        expect(assignee.key).toEqual(key);
        expect(assignee.avatar).toEqual("/avatars/" + key + ".png");
        expect(assignee.email).toEqual(key + "@example.com");
        expect(assignee.name.toLowerCase()).toContain(key.toLowerCase());
    }

    function checkBoardComponents(components:JiraComponent[], keys:string[]) {
        expect(components.length).toEqual(keys.length);
        for (let i:number = 0 ; i < keys.length ; i++) {
            expect(keys).toContain(components[i].name);
        }
    }

    function checkStandardProjects(boardData:BoardData) {
        checkProjects(boardData, "TUP", "TDP", "TBG");
    }

    function checkProjects(boardData:BoardData, linkedProject:string, ...mainProjects:string[]) {
        expect(boardData.owner).toBe(mainProjects[0]);
        expect(boardData.boardProjects).not.toBeNull();
        expect(boardData.boardProjectCodes.length).toBe(mainProjects.length);
        for (let code of mainProjects) {
            expect(boardData.boardProjectCodes).toContain(code);
        }

        if (linkedProject) {
            expect(boardData.linkedProjects).toEqual(jasmine.anything());
            expect(boardData.linkedProjects[linkedProject]).toEqual(jasmine.anything());
        }
    }

    function checkBoardPriority(priority:Priority, name:string) {
        expect(priority.name).toEqual(name);
        expect(priority.icon).toEqual("/icons/priorities/" + name + ".png");
    }

    function checkBoardIssueType(type:IssueType, name:string) {
        expect(type.name).toEqual(name);
        expect(type.icon).toEqual("/icons/issue-types/" + name + ".png");
    }

    function checkIssueConvenienceMethods(issue:IssueData) {
        let assignee:Assignee = issue.assignee;
        if (!assignee) {
            expect(issue.assigneeAvatar).toBe("images/person-4x.png");
            expect(issue.assigneeInitials).toBe("None");
            expect(issue.assigneeName).toBe("Unassigned");
        } else {
            expect(issue.assigneeAvatar).toBe(assignee.avatar);
            expect(issue.assigneeInitials).toBe(assignee.initials);
            expect(issue.assigneeName).toBe(assignee.name);
        }

        let priority:Priority = issue.priority;
        expect(issue.priorityName).toBe(priority.name);
        expect(issue.priorityUrl).toBe(priority.icon);

        let issueType:IssueType = issue.type;
        expect(issue.typeName).toBe(issueType.name);
        expect(issue.typeUrl).toBe(issueType.icon);
    }

    function checkFiltered(boardData:BoardData, invisible:string[], visible:string[]) {
        for (let key of invisible) {
            let issue:IssueData = boardData.getIssue(key);
            expect(issue).toEqual(jasmine.anything());
            expect(issue.filtered).toBe(true, "for " + key);
        }
        for (let key of visible) {
            let issue:IssueData = boardData.getIssue(key);
            expect(issue).toEqual(jasmine.anything());
            expect(issue.filtered).toBe(false, "for " + key);
        }
    }

});

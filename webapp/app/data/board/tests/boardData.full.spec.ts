import {BoardData} from "./../boardData";
import {TestBoardData} from "./testData";
import {Assignee} from "./../assignee";
import {Priority} from "./../priority";
import {Indexed} from "../../../common/indexed";
import {IssueType} from "./../issueType";
import {IssueData} from "./../issueData";
import {JiraMultiSelectFieldValue} from "../multiSelectNameOnlyValue";
import {SwimlaneData} from "../swimlaneData";
import {CustomFieldValues, CustomFieldValue} from "../customField";
import {IMap, IMapUtil} from "../../../common/map";
import {BoardProject} from "../project";

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
            data.projects.main.TDP.ranked=[];

            let boardData:BoardData = new BoardData();
            boardData.deserialize("tst", data);
            expect(boardData.blacklist).toBeNull();
        });

        it('Full board; No blacklist', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize("tst",
                TestBoardData.create(TestBoardData.FULL_BOARD_PROJECTS, TestBoardData.FULL_BOARD_ISSUES));

            expect(boardData.view).toEqual(0);
            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .check();

            expect(boardData.swimlane).not.toBeDefined();
            expect(boardData.swimlaneTable).toBeNull();

            expect(boardData.boardStateNames.length).toBe(4);
            checkIssueDatas(checker, boardData, TestBoardData.EXPECTED_FULL_BOARD);

            expect(boardData.blacklist).toBeNull();
        });

        it('Full board; Blacklist', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize("tst",
                TestBoardData.create(TestBoardData.FULL_BOARD_PROJECTS, TestBoardData.FULL_BOARD_ISSUES, TestBoardData.STANDARD_BLACKLIST));

            expect(boardData.view).toEqual(0);

            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .check();

            expect(boardData.swimlane).not.toBeDefined();
            expect(boardData.swimlaneTable).toBeNull();

            expect(boardData.boardStateNames.length).toBe(4);
            checkIssueDatas(checker, boardData, TestBoardData.EXPECTED_FULL_BOARD);

            expect(boardData.blacklist).toBeDefined();
            checkEntries(boardData.blacklist.issues, "TDP-100", "TBG-101");
            checkEntries(boardData.blacklist.issueTypes, "BadIssueType");
            checkEntries(boardData.blacklist.priorities, "BadPriority");
            checkEntries(boardData.blacklist.states, "BadState");
        });


        it('Owner Project Only; No blacklist', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize("tst",
                TestBoardData.create(TestBoardData.OWNER_ONLY_BOARD_PROJECTS, TestBoardData.OWNER_ONLY_BOARD_ISSUES));

            expect(boardData.view).toEqual(0);
            let checker:BoardDataChecker =
                new BoardDataChecker(boardData)
                    .mainProjects("TDP")
                    .check();

            expect(boardData.swimlane).not.toBeDefined();
            expect(boardData.swimlaneTable).toBeNull();

            expect(boardData.boardStateNames.length).toBe(4);
            checkIssueDatas(checker, boardData, TestBoardData.EXPECTED_OWNER_ONLY_BOARD);

            expect(boardData.blacklist).toBeNull();
        });

        it('Non-owner issues only; No blacklist', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize("tst",
                TestBoardData.create(TestBoardData.NON_OWNER_ONLY_BOARD_PROJECTS, TestBoardData.NON_OWNER_ONLY_BOARD_ISSUES));

            expect(boardData.view).toEqual(0);
            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .mainProjects("TDP", "TBG")
                .check();

            expect(boardData.swimlane).not.toBeDefined();
            expect(boardData.swimlaneTable).toBeNull();

            expect(boardData.boardStateNames.length).toBe(4);
            checkIssueDatas(checker, boardData, TestBoardData.EXPECTED_NON_OWNER_ONLY_BOARD);

            expect(boardData.blacklist).toBeNull();
        });

        it('Full board with custom fields; No blacklist', () => {
            let boardData:BoardData = new BoardData();

            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.FULL_BOARD_PROJECTS;
            bd.issues = TestBoardData.getFullBoardCustomFieldIssues();;
            bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;

            boardData.deserialize("tst", bd.build());

            expect(boardData.view).toEqual(0);
            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .customField("Tester", "james", "James Perkins")
                .customField("Tester", "kabir", "Kabir Khan")
                .customField("Documenter", "kabir", "Kabir Khan")
                .customField("Documenter", "stuart", "Stuart Douglas")
                .check();

            expect(boardData.swimlane).not.toBeDefined();
            expect(boardData.swimlaneTable).toBeNull();

            expect(boardData.boardStateNames.length).toBe(4);
            checkIssueDatas(checker, boardData, TestBoardData.EXPECTED_FULL_BOARD);
            expect(boardData.blacklist).toBeNull();
        });
    });

    describe('No Change', () => {
        it('No change', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize("tst",
                TestBoardData.create(TestBoardData.PRE_CHANGE_BOARD_PROJECTS, TestBoardData.PRE_CHANGE_BOARD_ISSUES));
            let changes:any = {
                changes: {
                    view: 0
                }
            };
            boardData.processChanges(changes);

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            expect(boardData.blacklist).toBeNull();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
        });
    });


    describe('New Blacklist', () => {
        var boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize("tst",
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [[], ["TDP-2"], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], [], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
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


            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [[], ["TDP-2"], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
        });
    });

    describe('Existing Blacklist', () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize("tst",
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [[], ["TDP-2"], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], [], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
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


            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [[], ["TDP-2"], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
        });
    });

    describe('Delete issues', () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize("tst",
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [[], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], [], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [[], ["TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], [], ["TDP-3"], ["TDP-4"]];
            checkIssueDatas(checker, boardData, layout);
        });
    });

    describe('Update issues - no state change ; ', () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize("tst",
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            expect(updatedIssue.key).toBe("TDP-1");
            new IssueDataChecker(updatedIssue, "bug", "highest", "brian", "One")
                .key("TDP-1")
                .components("C1")
                .check();

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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-2");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            expect(updatedIssue.key).toBe("TDP-2");
            new IssueDataChecker(updatedIssue, "bug", "low", "kabir", "Two")
                .key("TDP-2")
                .components("C5")
                .check();
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TBG-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            expect(updatedIssue.key).toBe("TBG-1");
            new IssueDataChecker(updatedIssue, "task", "highest", "brian", "Uno")
                .key("TBG-1")
                .components("C1")
                .check();
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TBG-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            expect(updatedIssue.key).toBe("TBG-1");
            new IssueDataChecker(updatedIssue, "task", "highest", null, "One")
                .key("TBG-1")
                .components("C1")
                .check();
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TBG-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            expect(updatedIssue.key).toBe("TBG-1");
            new IssueDataChecker(updatedIssue, "task", "highest", "kabir", "One")
                .key("TBG-1")
                .components("C1")
                .check();
        });

        it('Update assignee (new on board)', () => {

            new BoardDataChecker(boardData).check();
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

            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .assignees("brian", "jason", "kabir")
                .check();


            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TBG-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            expect(updatedIssue.key).toBe("TBG-1");
            new IssueDataChecker(updatedIssue, "task", "highest", "jason", "One")
                .key("TBG-1")
                .components("C1")
                .check();
        });

        describe('Components', () => {
            it ('Clear', () => {
                new BoardDataChecker(boardData).check();
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

                let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
                let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
                let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TBG-1");
                expect(updatedIssues.array.length).toBe(1);
                let updatedIssue:IssueData = updatedIssues.forIndex(0);
                expect(updatedIssue.key).toBe("TBG-1");
                new IssueDataChecker(updatedIssue, "task", "highest", "brian", "One")
                    .key("TBG-1")
                    .check();
                new BoardDataChecker(boardData).check();
            });

            it ('Update (not new on board)', () => {
                new BoardDataChecker(boardData).check();
                let changes:any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update" : [{
                                key: "TBG-1",
                                components: ["C5"]
                            }]
                        }
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
                let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
                let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TBG-1");
                expect(updatedIssues.array.length).toBe(1);
                let updatedIssue:IssueData = updatedIssues.forIndex(0);
                expect(updatedIssue.key).toBe("TBG-1");
                new IssueDataChecker(updatedIssue, "task", "highest", "brian", "One")
                    .key("TBG-1")
                    .components("C5")
                    .check();
                new BoardDataChecker(boardData).check();
            });

            it('Update (new on board)', () => {

                new BoardDataChecker(boardData).check();
                let changes:any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update" : [{
                                key: "TBG-1",
                                components: ["C7", "C3"]
                            }]
                        },
                        components : ["C7", "C3"]
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                let checker:BoardDataChecker = new BoardDataChecker(boardData)
                    .components("C1", "C3", "C5", "C7")
                    .check();

                let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
                let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TBG-1");
                expect(updatedIssues.array.length).toBe(1);
                let updatedIssue:IssueData = updatedIssues.forIndex(0);
                expect(updatedIssue.key).toBe("TBG-1");
                new IssueDataChecker(updatedIssue, "task", "highest", "brian", "One")
                    .key("TBG-1")
                    .components("C7", "C3")
                    .check();
            });
        })

        describe('Labels', () => {
            it ('Clear', () => {
                new BoardDataChecker(boardData).check();
                let changes:any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update" : [{
                                key: "TDP-3",
                                "clear-labels": true
                            }]
                        }
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
                let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
                let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-3");
                expect(updatedIssues.array.length).toBe(1);
                let updatedIssue:IssueData = updatedIssues.forIndex(0);
                expect(updatedIssue.key).toBe("TDP-3");
                new IssueDataChecker(updatedIssue, "feature", "low", null, "Three")
                    .key("TDP-3")
                    .fixVersions("F5")
                    .check();
                new BoardDataChecker(boardData).check();
            });

            it ('Update (not new on board)', () => {
                new BoardDataChecker(boardData).check();
                let changes:any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update" : [{
                                key: "TDP-3",
                                labels: ["L5"]
                            }]
                        }
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
                let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
                let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-3");
                expect(updatedIssues.array.length).toBe(1);
                let updatedIssue:IssueData = updatedIssues.forIndex(0);
                expect(updatedIssue.key).toBe("TDP-3");
                new IssueDataChecker(updatedIssue, "feature", "low", null, "Three")
                    .key("TDP-3")
                    .labels("L5")
                    .fixVersions("F5")
                    .check();
                new BoardDataChecker(boardData).check();
            });

            it('Update (new on board)', () => {

                new BoardDataChecker(boardData).check();
                let changes:any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update" : [{
                                key: "TDP-3",
                                labels: ["L7", "L3"]
                            }]
                        },
                        labels : ["L7", "L3"]
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                let checker:BoardDataChecker = new BoardDataChecker(boardData)
                    .labels("L1", "L3", "L5", "L7")
                    .check();

                let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
                let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-3");
                expect(updatedIssues.array.length).toBe(1);
                let updatedIssue:IssueData = updatedIssues.forIndex(0);
                expect(updatedIssue.key).toBe("TDP-3");
                new IssueDataChecker(updatedIssue, "feature", "low", null, "Three")
                    .key("TDP-3")
                    .labels("L7", "L3")
                    .fixVersions("F5")
                    .check();
            });
        })

        describe('Fix Versions', () => {
            it ('Clear', () => {
                new BoardDataChecker(boardData).check();
                let changes:any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update" : [{
                                key: "TDP-3",
                                "clear-fix-versions": true
                            }]
                        }
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
                let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
                let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-3");
                expect(updatedIssues.array.length).toBe(1);
                let updatedIssue:IssueData = updatedIssues.forIndex(0);
                expect(updatedIssue.key).toBe("TDP-3");
                new IssueDataChecker(updatedIssue, "feature", "low", null, "Three")
                    .key("TDP-3")
                    .labels("L1")
                    .check();
                new BoardDataChecker(boardData).check();
            });

            it ('Update (not new on board)', () => {
                new BoardDataChecker(boardData).check();
                let changes:any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update" : [{
                                key: "TDP-3",
                                "fix-versions": ["F1"]
                            }]
                        }
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
                let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
                let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-3");
                expect(updatedIssues.array.length).toBe(1);
                let updatedIssue:IssueData = updatedIssues.forIndex(0);
                expect(updatedIssue.key).toBe("TDP-3");
                new IssueDataChecker(updatedIssue, "feature", "low", null, "Three")
                    .key("TDP-3")
                    .labels("L1")
                    .fixVersions("F1")
                    .check();
                new BoardDataChecker(boardData).check();
            });

            it('Update (new on board)', () => {

                new BoardDataChecker(boardData).check();
                let changes:any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update" : [{
                                key: "TDP-3",
                                "fix-versions": ["F7", "F3"]
                            }]
                        },
                        "fix-versions" : ["F7", "F3"]
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                let checker:BoardDataChecker = new BoardDataChecker(boardData)
                    .fixVersions("F1", "F3", "F5", "F7")
                    .check();

                let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
                let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-3");
                expect(updatedIssues.array.length).toBe(1);
                let updatedIssue:IssueData = updatedIssues.forIndex(0);
                expect(updatedIssue.key).toBe("TDP-3");
                new IssueDataChecker(updatedIssue, "feature", "low", null, "Three")
                    .key("TDP-3")
                    .labels("L1")
                    .fixVersions("F7", "F3")
                    .check();
            });
        })


        it('Clear custom field', () => {
            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.PRE_CHANGE_BOARD_PROJECTS;
            bd.issues = TestBoardData.getPreChangeCustomFieldIssues();;
            bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;
            boardData.deserialize("tst", bd.build());

            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        update: [{
                            key: "TDP-1",
                            custom: {
                                Tester: null}
                        }]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            //Board should still have all the custom fields
            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .customField("Tester", "james", "James Perkins")
                .customField("Tester", "kabir", "Kabir Khan")
                .customField("Documenter", "kabir", "Kabir Khan")
                .customField("Documenter", "stuart", "Stuart Douglas")
                .check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            new IssueDataChecker(updatedIssue, "task", "highest", "brian", "One")
                .key("TDP-1")
                .components("C1")
                .customField("Documenter", "kabir", "Kabir Khan")
                .check();

        });

        it('Clear custom fields', () => {
            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.PRE_CHANGE_BOARD_PROJECTS;
            bd.issues = TestBoardData.getPreChangeCustomFieldIssues();;
            bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;
            boardData.deserialize("tst", bd.build());

            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        update: [{
                            key: "TDP-1",
                            custom: {
                                Tester: null, Documenter: null}
                        }]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            //Board should still have all the custom fields
            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .customField("Tester", "james", "James Perkins")
                .customField("Tester", "kabir", "Kabir Khan")
                .customField("Documenter", "kabir", "Kabir Khan")
                .customField("Documenter", "stuart", "Stuart Douglas")
                .check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            new IssueDataChecker(updatedIssue, "task", "highest", "brian", "One")
                .key("TDP-1")
                .components("C1")
                .check();
        });

        it('Update custom field (not new on board)', () => {
            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.PRE_CHANGE_BOARD_PROJECTS;
            bd.issues = TestBoardData.getPreChangeCustomFieldIssues();;
            bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;
            boardData.deserialize("tst", bd.build());

            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        update: [{
                            key: "TDP-1",
                            custom: {
                                Tester: "kabir", Documenter: "stuart"}
                        }]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            //Board should still have all the custom fields
            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .customField("Tester", "james", "James Perkins")
                .customField("Tester", "kabir", "Kabir Khan")
                .customField("Documenter", "kabir", "Kabir Khan")
                .customField("Documenter", "stuart", "Stuart Douglas")
                .check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            new IssueDataChecker(updatedIssue, "task", "highest", "brian", "One")
                .key("TDP-1")
                .components("C1")
                .customField("Tester", "kabir", "Kabir Khan")
                .customField("Documenter", "stuart", "Stuart Douglas")
                .check();
        });

        it('Update custom field (new on board)', () => {
            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.PRE_CHANGE_BOARD_PROJECTS;
            bd.issues = TestBoardData.getPreChangeCustomFieldIssues();;
            bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;
            boardData.deserialize("tst", bd.build());

            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        update: [{
                            key: "TDP-1",
                            custom: {
                                Tester: "stuart", Documenter: "brian"}
                        }]
                    },
                    custom: {
                        Tester: [{key: "stuart", value: "Stuart Douglas"}],
                        Documenter: [{key: "brian", value: "Brian Stansberry"}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            //Board should still have all the custom fields
            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .customField("Tester", "james", "James Perkins")
                .customField("Tester", "kabir", "Kabir Khan")
                .customField("Tester", "stuart", "Stuart Douglas")
                .customField("Documenter", "brian", "Brian Stansberry")
                .customField("Documenter", "kabir", "Kabir Khan")
                .customField("Documenter", "stuart", "Stuart Douglas")
                .check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            new IssueDataChecker(updatedIssue, "task", "highest", "brian", "One")
                .key("TDP-1")
                .components("C1")
                .customField("Tester", "stuart", "Stuart Douglas")
                .customField("Documenter", "brian", "Brian Stansberry")
                .check();
        });

        it('Update parallel tasks', () => {
            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.getPreChangeParallelTasksProjects();
            bd.issues = TestBoardData.getPrechangeParallelTaskIssues();
            boardData.deserialize("tst", bd.build());

            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        update: [{
                            key: "TDP-1",
                            "parallel-tasks": {"1":3, "0":1}
                        }]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            new IssueDataChecker(updatedIssue, "task", "highest", "brian", "One")
                .key("TDP-1")
                .components("C1")
                .selectedParallelTaskOptions("us-In Progress", "ds-Done")
                .check();
        });
    });

    describe('Update issues - state change', () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize("tst",
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
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [[], ["TDP-1", "TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            expect(updatedIssue.key).toBe("TDP-1");
            new IssueDataChecker(updatedIssue, "bug", "highest", "brian", "One")
                .key("TDP-1")
                .components("C1")
                .check();

        });

        it ('Update main project to unpopulated state', () => {
            //Delete an issue first so that we have an unpopulated state
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "delete" : ["TDP-3"]
                    }
                }
            };
            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);


            changes = {
                changes: {
                    view: 2,
                    issues: {
                        "update" : [{
                            key: "TDP-1",
                            type: "bug",
                            state: "TDP-C"
                        }]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(2);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [[], ["TDP-2", "TBG-1"], ["TDP-1"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            expect(updatedIssue.key).toBe("TDP-1");
            new IssueDataChecker(updatedIssue, "bug", "highest", "brian", "One")
                .key("TDP-1")
                .components("C1")
                .check();
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
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2"], ["TDP-3", "TBG-1"], ["TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TBG-1");
            expect(updatedIssues.array.length).toBe(1);
            let updatedIssue:IssueData = updatedIssues.forIndex(0);
            expect(updatedIssue.key).toBe("TBG-1");
            new IssueDataChecker(updatedIssue, "bug", "highest", "brian", "One")
                .key("TBG-1")
                .components("C1")
                .check();
        });
    });

    describe("Update issue rank", () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize("tst",
                TestBoardData.create(TestBoardData.PRE_RERANK_BOARD_PROJECTS, TestBoardData.PRE_RERANK_BOARD_ISSUES));
            new BoardDataChecker(boardData).check();
        });


        it("Move end issue to start", () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: null,
                    rank: {
                        TDP: [{index: 0, key: "TDP-4"}]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            new BoardDataChecker(boardData).check();
            checkBoardRank(boardData, {TDP: ["TDP-4", "TDP-1", "TDP-2", "TDP-3"]})
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkBoardLayout(boardData, layout);

        });

        it("Move one middle issue to start", () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: null,
                    rank: {
                        TDP: [{index: 0, key: "TDP-3"}]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());
            new BoardDataChecker(boardData).check();

            new BoardDataChecker(boardData).check();
            checkBoardRank(boardData, {TDP: ["TDP-3", "TDP-1", "TDP-2", "TDP-4"]})
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkBoardLayout(boardData, layout);

        });

        it("Move start issue to end", () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: null,
                    rank: {
                        TDP: [{index: 3, key: "TDP-1"}]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());
            new BoardDataChecker(boardData).check();

            new BoardDataChecker(boardData).check();
            checkBoardRank(boardData, {TDP: ["TDP-2", "TDP-3", "TDP-4", "TDP-1"]})
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkBoardLayout(boardData, layout);

        });

        it("Move one middle issue to end", () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: null,
                    rank: {
                        TDP: [{index: 3, key: "TDP-3"}]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());
            new BoardDataChecker(boardData).check();


            checkBoardRank(boardData, {TDP: ["TDP-1", "TDP-2", "TDP-4", "TDP-3"]})
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkBoardLayout(boardData, layout);
        });

        it("Swap middle issues", () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: null,
                    rank: {
                        TDP: [{index: 1, key: "TDP-3"}]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            new BoardDataChecker(boardData).check();
            checkBoardRank(boardData, {TDP: ["TDP-1", "TDP-3", "TDP-2", "TDP-4"]})
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkBoardLayout(boardData, layout);
        });

        it("Rank two issues", () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: null,
                    rank: {
                        TDP: [
                            {index: 1, key: "TDP-3"},
                            {index: 3, key: "TDP-2"}]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            new BoardDataChecker(boardData).check();
            checkBoardRank(boardData, {TDP: ["TDP-1", "TDP-3", "TDP-4", "TDP-2"]})
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkBoardLayout(boardData, layout);
        });

        it("Rank all issues", () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: null,
                    rank: {
                        TDP: [
                            {index: 0, key: "TDP-3"},
                            {index: 1, key: "TDP-4"},
                            {index: 2, key: "TDP-1"},
                            {index: 3, key: "TDP-2"}]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            new BoardDataChecker(boardData).check();
            checkBoardRank(boardData, {TDP: ["TDP-3", "TDP-4", "TDP-1", "TDP-2"]})
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkBoardLayout(boardData, layout);
        });

        it("Combine issue delete with rank", () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "delete" : ["TDP-1"]
                    },
                    rank: {
                        TDP: [{index: 0, key: "TDP-3"}]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            new BoardDataChecker(boardData).check();
            checkBoardRank(boardData, {TDP: ["TDP-3", "TDP-2", "TDP-4"]})
            let layout:any = [[], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            checkBoardLayout(boardData, layout);
        });

        it ("State updates, deletes and adds", () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "delete" : ["TDP-3"],
                        "update" : [{
                            key: "TDP-2",
                            type: "issue",
                            state: "TDP-D"
                        }],
                        "new": [{
                            key: "TDP-5",
                            state : "TDP-D",
                            summary : "Five",
                            priority : "high",
                            type : "bug",
                            assignee : "kabir"

                        }]
                    },

                    rank: {
                        TDP: [
                            {index: 0, key: "TDP-5"},
                            {index: 1, key: "TDP-2"}]
                    }
                }
            }

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            new BoardDataChecker(boardData).check();
            checkBoardRank(boardData, {TDP: ["TDP-5", "TDP-2", "TDP-1", "TDP-4"]})

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TBG-1"], [], ["TDP-5", "TDP-2", "TDP-4"]];
            let updatedIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-2", "TDP-5");
            expect(updatedIssues.array.length).toBe(2);
            let updatedIssue:IssueData = updatedIssues.forKey("TDP-2");
            new IssueDataChecker(updatedIssue, "issue", "high", "kabir", "Two")
                .key("TDP-2")
                .components("C5")
                .check();
            updatedIssue = updatedIssues.forKey("TDP-5");
            new IssueDataChecker(updatedIssue, "bug", "high", "kabir", "Five")
                .key("TDP-5")
                .check();

        });
    });

    describe("Create issue", () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize("tst",
                TestBoardData.create(TestBoardData.PRE_CHANGE_BOARD_PROJECTS, TestBoardData.PRE_CHANGE_BOARD_ISSUES));
            new BoardDataChecker(boardData).check();
        });

        it ("Main project to populated state - no new assignee", () => {
            new BoardDataChecker(boardData).check();
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "new" : [{
                            key: "TDP-5",
                            state : "TDP-B",
                            summary : "Five",
                            priority : "high",
                            type : "bug",
                            assignee : "kabir"

                        }]
                    },
                    rank: {
                        TDP: [{index: 2, key: "TDP-5"}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TDP-5", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let createdIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-5");
            expect(createdIssues.array.length).toBe(1);
            let createdIssue:IssueData = createdIssues.forIndex(0);
            expect(createdIssue.key).toBe("TDP-5");
            new IssueDataChecker(createdIssue, "bug", "high", "kabir", "Five")
                .key("TDP-5")
                .check();
        });

        it ("Main project to unpopulated state  - new assignee", () => {
            //Delete an issue first so that we have an unpopulated state
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "delete" : ["TDP-3"]
                    }
                }
            };
            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);

            changes = {
                changes: {
                    view: 2,
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
                    assignees : [{
                        key : "jason",
                        email : "jason@example.com",
                        avatar : "/avatars/jason.png",
                        name : "Jason Greene"
                    }],
                    rank: {
                        TDP: [{index: 2, key: "TDP-3"}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(2);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .assignees("brian", "jason", "kabir")
                .check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let createdIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-3");
            expect(createdIssues.array.length).toBe(1);
            let createdIssue:IssueData = createdIssues.forIndex(0);

            expect(createdIssue.key).toBe("TDP-3");
            new IssueDataChecker(createdIssue, "bug", "high", "jason", "Three")
                .key("TDP-3")
                .check();
        });

        it ("Other project populated state", () => {
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
                    rank: {
                        TBG: [{index: 1, key: "TBG-2"}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1", "TBG-2"], ["TDP-3"], ["TDP-4"]];
            let createdIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TBG-2");
            expect(createdIssues.array.length).toBe(1);
            let createdIssue:IssueData = createdIssues.forIndex(0);
            expect(createdIssue.key).toBe("TBG-2");
            new IssueDataChecker(createdIssue, "bug", "high", "kabir", "Two")
                .key("TBG-2")
                .check();
        });

        it ("Other project unpopulated state", () => {
            //Delete an issue first so that we have an unpopulated state
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "delete" : ["TDP-3"]
                    }
                }
            };
            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);

            new BoardDataChecker(boardData).check();
            changes = {
                changes: {
                    view: 2,
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
                    rank: {
                        TBG: [{index: 1, key: "TBG-2"}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(2);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TBG-2"], ["TDP-4"]];
            let createdIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TBG-2");
            expect(createdIssues.array.length).toBe(1);
            let createdIssue:IssueData = createdIssues.forIndex(0);
            new IssueDataChecker(createdIssue, "feature", "highest", "brian", "Two")
                .key("TBG-2")
                .check();
        });

        it('Main project to populated state - existing custom fields', () => {
            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.PRE_CHANGE_BOARD_PROJECTS;
            bd.issues = TestBoardData.getPreChangeCustomFieldIssues();
            bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;
            boardData.deserialize("tst", bd.build());

            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "new" : [{
                            key: "TDP-5",
                            state : "TDP-B",
                            summary : "Five",
                            priority : "high",
                            type : "bug",
                            assignee : "kabir",
                            components: ["C1"],
                            labels: ["L1"],
                            "fix-versions" : ["F1"],
                            custom: {
                                Tester: "kabir",
                                Documenter: "stuart"}
                        }]
                    },
                    rank: {
                        TDP: [{index: 2, key: "TDP-5"}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            //Board should still have all the custom fields
            let checker:BoardDataChecker = new BoardDataChecker(boardData)
                .customField("Tester", "james", "James Perkins")
                .customField("Tester", "kabir", "Kabir Khan")
                .customField("Documenter", "kabir", "Kabir Khan")
                .customField("Documenter", "stuart", "Stuart Douglas")
                .check();
            let layout:any = [["TDP-1"], ["TDP-2", "TDP-5", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let createdIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-5");
            expect(createdIssues.array.length).toBe(1);
            let createdIssue:IssueData = createdIssues.forIndex(0);
            new IssueDataChecker(createdIssue, "bug", "high", "kabir", "Five")
                .key("TDP-5")
                .components("C1")
                .labels("L1")
                .fixVersions("F1")
                .customField("Tester", "kabir", "Kabir Khan")
                .customField("Documenter", "stuart", "Stuart Douglas")
                .check();
        });

        it ("With parallel tasks", () => {
            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.getPreChangeParallelTasksProjects();
            bd.issues = TestBoardData.getPrechangeParallelTaskIssues();
            boardData.deserialize("tst", bd.build());

            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        new : [{
                            key: "TDP-5",
                            state : "TDP-B",
                            summary : "Five",
                            priority : "high",
                            type : "bug",
                            assignee : "kabir",
                            components : ["C1"],
                            labels : ["L5"],
                            "fix-versions" : ["F5"],
                            "parallel-tasks": [3, 0]
                        }]
                    },
                    rank: {
                        TDP: [{index: 2, key: "TDP-5"}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TDP-5", "TBG-1"], ["TDP-3"], ["TDP-4"]];
            let createdIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-5");
            expect(createdIssues.array.length).toBe(1);
            let createdIssue:IssueData = createdIssues.forIndex(0);
            new IssueDataChecker(createdIssue, "bug", "high", "kabir", "Five")
                .key("TDP-5")
                .components("C1")
                .labels("L5")
                .fixVersions("F5")
                .selectedParallelTaskOptions("us-Done", "ds-TODO")
                .check();
        });

        it ("Several issues", () => {
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "new" : [{
                                key: "TDP-5",
                                state : "TDP-C",
                                summary : "Five",
                                priority : "high",
                                type : "bug",
                                assignee : "brian"
                            },
                            {
                                key: "TDP-6",
                                state : "TDP-D",
                                summary : "Six",
                                priority : "low",
                                type : "issue",
                                assignee : "kabir"
                            },
                            {
                                key: "TBG-2",
                                state : "TBG-Y",
                                summary : "Two",
                                priority : "highest",
                                type : "feature",
                                assignee : "brian"

                        }]
                    },
                    rank: {
                        TDP: [{index: 4, key: "TDP-5"}, {index: 5, key: "TDP-6"}],
                        TBG: [{index: 1, key: "TBG-2"}]
                    }
                }
            };
            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            let checker:BoardDataChecker = new BoardDataChecker(boardData).check();
            let layout:any = [["TDP-1"], ["TDP-2", "TBG-1"], ["TDP-3", "TDP-5", "TBG-2"], ["TDP-4", "TDP-6"]];
            let createdIssues:Indexed<IssueData> = checkIssueDatas(checker, boardData, layout, "TDP-5", "TDP-6", "TBG-2");
            expect(createdIssues.array.length).toBe(3);
            let createdIssue:IssueData = createdIssues.forKey("TDP-5");
            new IssueDataChecker(createdIssue, "bug", "high", "brian", "Five")
                .key("TDP-5")
                .check();
            createdIssue = createdIssues.forKey("TDP-6");
            new IssueDataChecker(createdIssue, "issue", "low", "kabir", "Six")
                .key("TDP-6")
                .check();
            createdIssue = createdIssues.forKey("TBG-2");
            new IssueDataChecker(createdIssue, "feature", "highest", "brian", "Two")
                .key("TBG-2")
                .check();
        });
    });

    //TODO a test involving several updates, deletes and state changes


    describe("Filter tests", () => {

        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize("tst",
                TestBoardData.create(TestBoardData.FULL_BOARD_PROJECTS, TestBoardData.FULL_BOARD_ISSUES));
        });

        describe("Simple", () => {

            it("Project", () => {
                new FilterBuilder(boardData)
                    .setProjectFilter({"TDP": true})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TBG-1", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7"]);

                new FilterBuilder(boardData)
                    .setProjectFilter({"TDP": true, "TBG": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setProjectFilter({"TBG": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7"],
                    ["TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

            });

            it("Priority", () => {
                new FilterBuilder(boardData)
                    .setPriorityFilter({"highest": true})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-5", "TBG-1"]);

                new FilterBuilder(boardData)
                    .setPriorityFilter({"high": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                    ["TDP-2", "TDP-6", "TBG-2"]);

                new FilterBuilder(boardData)
                    .setPriorityFilter({"highest": true, "high":true, "low": true, "lowest":true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setPriorityFilter({"low": true, "lowest":true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);
            });


            it("Issue Type", () => {
                new FilterBuilder(boardData)
                    .setIssueTypeFilter({"task":true})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-5", "TBG-1"]);

                new FilterBuilder(boardData)
                    .setIssueTypeFilter({"bug":true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                    ["TDP-2", "TDP-6", "TBG-2"]);

                new FilterBuilder(boardData)
                    .setIssueTypeFilter({"task":true, "bug":true, "feature":true, "issue":true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setIssueTypeFilter({"feature":true, "issue":true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);
            });

            it ("Assignee", () => {
                new FilterBuilder(boardData)
                    .setAssigneeFilter({"brian":true})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-5", "TBG-1"]);

                new FilterBuilder(boardData)
                    .setAssigneeFilter({"kabir":true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                    ["TDP-2", "TDP-6", "TBG-2"]);

                new FilterBuilder(boardData)
                    .setAssigneeFilter({"$n$o$n$e$":true, "brian":true, "kabir":true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setAssigneeFilter({"$n$o$n$e$":true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

            });
        });

        describe("Map", () => {

            it("Component (single per issue)", () => {
                //Components are a bit different from the other filters, since an issue may have more than one component
                new FilterBuilder(boardData)
                    .setComponentFilter({"C1": true})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-5", "TBG-1"]);

                new FilterBuilder(boardData)
                    .setComponentFilter({"C5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                    ["TDP-2", "TDP-6", "TBG-2"]);

                new FilterBuilder(boardData)
                    .setComponentFilter({"$n$o$n$e$": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setComponentFilter({"$n$o$n$e$": true, "C1": true, "C5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


                //Make sure that if selecting more than one component we get all the issues which have EITHER
                new FilterBuilder(boardData)
                    .setComponentFilter({"C1": true, "C5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"]);


                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


            });

            it("Component (several per issue)", () => {
                //Components are a bit different from the other filters, since an issue may have more than one component

                //Add some components to some issues
                new BoardDataChecker(boardData)
                    .linkedProjects("TUP")
                    .check();
                let changes: any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update": [
                                {key: "TDP-3", components: ["C1", "C5"]},
                                {key: "TDP-4", components: ["C7"]},
                                {key: "TDP-7", components: ["C1", "C5"]},
                                {key: "TBG-3", components: ["C1", "C5"]}]
                        },
                        components: ["C7"]
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                new BoardDataChecker(boardData)
                    .linkedProjects("TUP")
                    .components("C1", "C5", "C7")
                    .check();
                new FilterBuilder(boardData)
                    .setComponentFilter({"C1": true})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-2", "TDP-4", "TDP-6", "TBG-2", "TBG-4"],
                    ["TDP-1", "TDP-3", "TDP-5", "TDP-7", "TBG-1", "TBG-3"]);

                new FilterBuilder(boardData)
                    .setComponentFilter({"C5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-4", "TDP-5", "TBG-1", "TBG-4"],
                    ["TDP-2", "TDP-3", "TDP-6", "TDP-7", "TBG-2", "TBG-3"]);

                new FilterBuilder(boardData)
                    .setComponentFilter({"$n$o$n$e$": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3"],
                    ["TBG-4"]);

                new FilterBuilder(boardData)
                    .setComponentFilter({"$n$o$n$e$": true, "C1": true, "C5": true, "C7": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


                //Make sure that if selecting more than one component we get all the issues which have EITHER
                new FilterBuilder(boardData)
                    .setComponentFilter({"C1": true, "C5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-4", "TBG-4"],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3"]);

                new FilterBuilder(boardData)
                    .setComponentFilter({"$n$o$n$e$": true, "C1": true, "C5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-4"],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);
            });

            it("Label (single per issue)", () => {
                //Components are a bit different from the other filters, since an issue may have more than one component
                new FilterBuilder(boardData)
                    .setLabelFilter({"L1": true})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-4", "TDP-5", "TDP-6", "TBG-1", "TBG-2", "TBG-4"],
                    ["TDP-3", "TDP-7", "TBG-3"]);

                new FilterBuilder(boardData)
                    .setLabelFilter({"L5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-5", "TDP-6", "TBG-1", "TBG-2", "TBG-3"],
                    ["TDP-4", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setLabelFilter({"$n$o$n$e$": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"]);

                new FilterBuilder(boardData)
                    .setLabelFilter({"$n$o$n$e$": true, "L1": true, "L5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


                //Make sure that if selecting more than one component we get all the issues which have EITHER
                new FilterBuilder(boardData)
                    .setLabelFilter({"L1": true, "L5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);


                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);
            });

            it("Label (several per issue)", () => {
                //Components are a bit different from the other filters, since an issue may have more than one component

                //Add some components to some issues
                new BoardDataChecker(boardData)
                    .linkedProjects("TUP")
                    .check();
                let changes: any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update": [
                                {key: "TDP-1", labels: ["L1", "L5"]},
                                {key: "TDP-2", labels: ["L7"]},
                                {key: "TDP-5", labels: ["L1", "L5"]},
                                {key: "TBG-1", labels: ["L1", "L5"]}]
                        },
                        labels: ["L7"]
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                new BoardDataChecker(boardData)
                    .linkedProjects("TUP")
                    .labels("L1", "L5", "L7")
                    .check();
                new FilterBuilder(boardData)
                    .setLabelFilter({"L1": true})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-2", "TDP-4", "TDP-6", "TBG-2", "TBG-4"],
                    ["TDP-1", "TDP-3", "TDP-5", "TDP-7", "TBG-1", "TBG-3"]);

                new FilterBuilder(boardData)
                    .setLabelFilter({"L5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-2", "TDP-3", "TDP-6", "TDP-7", "TBG-2", "TBG-3"],
                    ["TDP-1", "TDP-4", "TDP-5", "TBG-1", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setLabelFilter({"$n$o$n$e$": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                    ["TDP-6", "TBG-2"]);

                new FilterBuilder(boardData)
                    .setLabelFilter({"$n$o$n$e$": true, "L1": true, "L5": true, "L7": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


                //Make sure that if selecting more than one label we get all the issues which have EITHER
                new FilterBuilder(boardData)
                    .setLabelFilter({"L1": true, "L5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-2", "TDP-6", "TBG-2"],
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setLabelFilter({"$n$o$n$e$": true, "L1": true, "L5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-2"],
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);
            });

            it("Fix Version (single per issue)", () => {
                //Components are a bit different from the other filters, since an issue may have more than one component
                new FilterBuilder(boardData)
                    .setFixVersionFilter({"F1": true})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3"],
                    ["TDP-4", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setFixVersionFilter({"F5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-4", "TDP-5", "TDP-6", "TBG-1", "TBG-2", "TBG-4"],
                    ["TDP-3", "TBG-3"]);

                new FilterBuilder(boardData)
                    .setFixVersionFilter({"$n$o$n$e$": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"]);

                new FilterBuilder(boardData)
                    .setFixVersionFilter({"$n$o$n$e$": true, "F1": true, "F5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


                //Make sure that if selecting more than one component we get all the issues which have EITHER
                new FilterBuilder(boardData)
                    .setFixVersionFilter({"F1": true, "F5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);


                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);
            });

            it("Label (several per issue)", () => {
                //Components are a bit different from the other filters, since an issue may have more than one component

                //Add some components to some issues
                new BoardDataChecker(boardData)
                    .linkedProjects("TUP")
                    .check();
                let changes: any = {
                    changes: {
                        view: 1,
                        issues: {
                            "update": [
                                {key: "TDP-1", "fix-versions": ["F1", "F5"]},
                                {key: "TDP-2", "fix-versions": ["F7"]},
                                {key: "TDP-5", "fix-versions": ["F1", "F5"]},
                                {key: "TBG-1", "fix-versions": ["F1", "F5"]}]
                        },
                        "fix-versions": ["F7"]
                    }
                };

                boardData.processChanges(changes);
                expect(boardData.view).toBe(1);
                expect(boardData.blacklist).not.toEqual(jasmine.anything());

                new BoardDataChecker(boardData)
                    .linkedProjects("TUP")
                    .fixVersions("F1", "F5", "F7")
                    .check();
                new FilterBuilder(boardData)
                    .setFixVersionFilter({"F1": true})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-2", "TDP-3", "TDP-6", "TDP-7", "TBG-2", "TBG-3"],
                    ["TDP-1", "TDP-4", "TDP-5", "TBG-1", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setFixVersionFilter({"F5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-2", "TDP-4", "TDP-6", "TBG-2", "TBG-4"],
                    ["TDP-1", "TDP-3", "TDP-5", "TDP-7", "TBG-1", "TBG-3"]);

                new FilterBuilder(boardData)
                    .setFixVersionFilter({"$n$o$n$e$": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                    ["TDP-6", "TBG-2"]);

                new FilterBuilder(boardData)
                    .setFixVersionFilter({"$n$o$n$e$": true, "F1": true, "F5": true, "F7": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


                //Make sure that if selecting more than one label we get all the issues which have EITHER
                new FilterBuilder(boardData)
                    .setFixVersionFilter({"F1": true, "F5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-2", "TDP-6", "TBG-2"],
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setFixVersionFilter({"$n$o$n$e$": true, "F1": true, "F5": true})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-2"],
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);
            });

            it("Custom Field - Tester", () => {
                let bd: TestBoardData = new TestBoardData();
                bd.projects = TestBoardData.FULL_BOARD_PROJECTS;
                bd.issues = TestBoardData.getFullBoardCustomFieldIssues();
                bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;
                boardData.deserialize("tst", bd.build());

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"james": true}})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-5", "TBG-1"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"kabir": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                    ["TDP-2", "TDP-6", "TBG-2"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"james": false, "kabir": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                    ["TDP-2", "TDP-6", "TBG-2"]);

                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

                //None
                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"$n$o$n$e$": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"$n$o$n$e$": true, "kabir": true, "james": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"kabir": true, "james": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"]);

            });


            it("Custom Field - Documenter", () => {
                let bd: TestBoardData = new TestBoardData();
                bd.projects = TestBoardData.FULL_BOARD_PROJECTS;
                bd.issues = TestBoardData.getFullBoardCustomFieldIssues();
                bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;
                boardData.deserialize("tst", bd.build());

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Documenter": {"kabir": true}})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-5", "TBG-1"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Documenter": {"stuart": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                    ["TDP-2", "TDP-6", "TBG-2"]);

                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

                //None
                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Documenter": {"$n$o$n$e$": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Documenter": {"$n$o$n$e$": true, "kabir": true, "stuart": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Documenter": {"kabir": true, "stuart": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"]);
            });

            it("Custom Field - Documenter+Tester", () => {
                let bd: TestBoardData = new TestBoardData();
                bd.projects = TestBoardData.FULL_BOARD_PROJECTS;
                bd.issues = TestBoardData.getFullBoardCustomFieldIssues();
                bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;
                boardData.deserialize("tst", bd.build());

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"james": true}, "Documenter": {"kabir": true}})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-2", "TDP-3", "TDP-4", "TDP-6", "TDP-7", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-5", "TBG-1"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"kabir": true}, "Documenter": {"stuart": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-3", "TDP-4", "TDP-5", "TDP-7", "TBG-1", "TBG-3", "TBG-4"],
                    ["TDP-2", "TDP-6", "TBG-2"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"james": true}, "Documenter": {"stuart": true}})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"],
                    []);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"kabir": true}, "Documenter": {"kabir": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"],
                    []);

                //None
                new FilterBuilder(boardData)
                    .setCustomFieldFilter({"Tester": {"$n$o$n$e$": true}, "Documenter": {"$n$o$n$e$": true}})
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"],
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({
                        "Tester": {"$n$o$n$e$": true, "kabir": true, "james": true},
                        "Documenter": {"$n$o$n$e$": true, "kabir": true, "stuart": true}
                    })
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);

                new FilterBuilder(boardData)
                    .setCustomFieldFilter({
                        "Tester": {"kabir": true, "james": true},
                        "Documenter": {"kabir": true, "stuart": true}
                    })
                    .buildAndFilterBoardData();
                checkFiltered(boardData,
                    ["TDP-3", "TDP-4", "TDP-7", "TBG-3", "TBG-4"],
                    ["TDP-1", "TDP-2", "TDP-5", "TDP-6", "TBG-1", "TBG-2"]);

            });

            it("Parallel tasks", () => {
                let bd: TestBoardData = new TestBoardData();
                bd.projects = TestBoardData.getFullBoardParallelTaskProjects();
                bd.issues = TestBoardData.getFullBoardParallelTaskIssues();
                boardData.deserialize("tst", bd.build());

                new FilterBuilder(boardData)
                    .setParallelTasksFilter({"US": {"us-Done": true}})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-4"]);


                new FilterBuilder(boardData)
                    .setParallelTasksFilter({"DS": {"ds-Done": true}})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-4", "TDP-5", "TDP-6", "TBG-1", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-3", "TDP-7"]);

                new FilterBuilder(boardData)
                    .setParallelTasksFilter({"US": {"us-Review": true}, "DS": {"ds-Done": true}})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-4", "TDP-5", "TDP-6", "TBG-1", "TBG-2", "TBG-3", "TBG-4"],
                    ["TDP-3", "TDP-7"]);

                new FilterBuilder(boardData)
                    .setParallelTasksFilter({"US": {"us-Review": true}, "DS": {"ds-Review": true}})
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"],
                    []);


                new FilterBuilder(boardData)
                    .buildAndFilterBoardData();
                checkBoardLayout(boardData, TestBoardData.EXPECTED_FULL_BOARD);
                checkFiltered(boardData,
                    [],
                    ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7", "TBG-1", "TBG-2", "TBG-3", "TBG-4"]);


            });
        });
    });

    describe("Swimlane test", () => {
        let boardData:BoardData;
        beforeEach(() => {
            boardData = new BoardData();
            boardData.deserialize("tst",
                TestBoardData.create(TestBoardData.FULL_BOARD_PROJECTS, TestBoardData.FULL_BOARD_ISSUES));
        });

        it ("Project", () => {
            boardData.swimlane = "project";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "TDP",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TDP-2", "TDP-6"],
                        ["TDP-3", "TDP-7"],
                        ["TDP-4"]]
                },
                {
                    "name": "TBG",
                    table: [
                        [],
                        ["TBG-1", "TBG-3"],
                        ["TBG-2", "TBG-4"],
                        []]
                });
        });

        it ("Priority", () => {
            boardData.swimlane = "priority";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "highest",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1"],
                        [],
                        []]
                },
                {
                    "name": "high",
                    table: [
                        [],
                        ["TDP-2", "TDP-6"],
                        ["TBG-2"],
                        []]
                },
                {
                    "name": "low",
                    table: [
                        [],
                        ["TBG-3"],
                        ["TDP-3", "TDP-7"],
                        []]
                },
                {
                    "name": "lowest",
                    table: [
                        [],
                        [],
                        ["TBG-4"],
                        ["TDP-4"]]
                }
            );
        });

        it ("Issue Type", () => {
            boardData.swimlane = "issue-type";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "task",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1"],
                        [],
                        []]
                },
                {
                    "name": "bug",
                    table: [
                        [],
                        ["TDP-2", "TDP-6"],
                        ["TBG-2"],
                        []]
                },
                {
                    "name": "feature",
                    table: [
                        [],
                        ["TBG-3"],
                        ["TDP-3", "TDP-7"],
                        []]
                },
                {
                    "name": "issue",
                    table: [
                        [],
                        [],
                        ["TBG-4"],
                        ["TDP-4"]]
                }
            );
        });

        it ("Assignee", () => {
            boardData.swimlane = "assignee";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "Brian Stansberry",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1"],
                        [],
                        []]
                },
                {
                    "name": "Kabir Khan",
                    table: [
                        [],
                        ["TDP-2", "TDP-6"],
                        ["TBG-2"],
                        []]
                },
                {
                    "name": "None",
                    table: [
                        [],
                        ["TBG-3"],
                        ["TDP-3", "TDP-7", "TBG-4"],
                        ["TDP-4"]]
                }
            );
        });

        it ("Component (single per issue)", () => {
            //Components are a bit different from the other swimlane selectors, since an issue may have more than one component
            boardData.swimlane = "component";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "C1",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1"],
                        [],
                        []]
                },
                {
                    "name": "C5",
                    table: [
                        [],
                        ["TDP-2", "TDP-6"],
                        ["TBG-2"],
                        []]
                },
                {
                    "name": "None",
                    table: [
                        [],
                        ["TBG-3"],
                        ["TDP-3", "TDP-7", "TBG-4"],
                        ["TDP-4"]]
                }
            );
        });

        it ("Component (several per issue)", () => {
            //Components are a bit different from the other swimlane selectors, since an issue may have more than one component

            //Add some components to some issues
            new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .check();
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [
                            {key: "TDP-3", components: ["C1", "C5"]},
                            {key: "TDP-7", components: ["C1", "C5"]},
                            {key: "TBG-3", components: ["C1", "C5"]}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .check();
            boardData.swimlane = "component";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "C1",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1", "TBG-3"],
                        ["TDP-3", "TDP-7"],
                        []]
                },
                {
                    "name": "C5",
                    table: [
                        [],
                        ["TDP-2", "TDP-6", "TBG-3"],
                        ["TDP-3", "TDP-7", "TBG-2"],
                        []]
                },
                {
                    "name": "None",
                    table: [
                        [],
                        [],
                        ["TBG-4"],
                        ["TDP-4"]]
                }
            );
        });

        it ("Label (single per issue)", () => {
            //Labels are a bit different from the other swimlane selectors, since an issue may have more than one component
            boardData.swimlane = "label";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "L1",
                    table: [
                        [],
                        ["TBG-3"],
                        ["TDP-3", "TDP-7"],
                        []]
                },
                {
                    "name": "L5",
                    table: [
                        [],
                        [],
                        ["TBG-4"],
                        ["TDP-4"]]
                },
                {
                    "name": "None",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TDP-2", "TDP-6", "TBG-1"],
                        ["TBG-2"],
                        []]
                }
            );
        });

        it ("Label (several per issue)", () => {
            //Labels aree a bit different from the other swimlane selectors, since an issue may have more than one component

            //Add some components to some issues
            new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .check();
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [
                            {key: "TDP-1", labels: ["L1", "L5"]},
                            {key: "TDP-5", labels: ["L1", "L5"]},
                            {key: "TBG-1", labels: ["L1", "L5"]}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .check();
            boardData.swimlane = "label";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "L1",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1", "TBG-3"],
                        ["TDP-3", "TDP-7"],
                        []]
                },
                {
                    "name": "L5",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1"],
                        ["TBG-4"],
                        ["TDP-4"]]
                },
                {
                    "name": "None",
                    table: [
                        [],
                        ["TDP-2", "TDP-6"],
                        ["TBG-2"],
                        []]
                }
            );
        });

        it ("Fix Version (single per issue)", () => {
            //Labels are a bit different from the other swimlane selectors, since an issue may have more than one component
            boardData.swimlane = "fix-version";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "F1",
                    table: [
                        [],
                        [],
                        ["TBG-4"],
                        ["TDP-4"]]
                },
                {
                    "name": "F5",
                    table: [
                        [],
                        ["TBG-3"],
                        ["TDP-3", "TDP-7"],
                        []]
                },
                {
                    "name": "None",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TDP-2", "TDP-6", "TBG-1"],
                        ["TBG-2"],
                        []]
                }
            );
        });

        it ("Label (several per issue)", () => {
            //Labels aree a bit different from the other swimlane selectors, since an issue may have more than one component

            //Add some components to some issues
            new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .check();
            let changes:any = {
                changes: {
                    view: 1,
                    issues: {
                        "update" : [
                            {key: "TDP-1", "fix-versions": ["F1", "F5"]},
                            {key: "TDP-5", "fix-versions": ["F1", "F5"]},
                            {key: "TBG-1", "fix-versions": ["F1", "F5"]}]
                    }
                }
            };

            boardData.processChanges(changes);
            expect(boardData.view).toBe(1);
            expect(boardData.blacklist).not.toEqual(jasmine.anything());

            new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .check();
            boardData.swimlane = "fix-version";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "F1",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1"],
                        ["TBG-4"],
                        ["TDP-4"]]
                },
                {
                    "name": "F5",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1", "TBG-3"],
                        ["TDP-3", "TDP-7"],
                        []]
                },
                {
                    "name": "None",
                    table: [
                        [],
                        ["TDP-2", "TDP-6"],
                        ["TBG-2"],
                        []]
                }
            );
        });

        it ("Custom field 'Tester'", () => {
            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.FULL_BOARD_PROJECTS;
            bd.issues = TestBoardData.getFullBoardCustomFieldIssues();
            bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;
            boardData.deserialize("tst", bd.build());
            new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .customField("Tester", "james", "James Perkins")
                .customField("Tester", "kabir", "Kabir Khan")
                .customField("Documenter", "kabir", "Kabir Khan")
                .customField("Documenter", "stuart", "Stuart Douglas")
                .check();
            boardData.swimlane = "Tester";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "James Perkins",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1"],
                        [],
                        []]
                },
                {
                    "name": "Kabir Khan",
                    table: [
                        [],
                        ["TDP-2", "TDP-6"],
                        ["TBG-2"],
                        []]
                },
                {
                    "name": "None",
                    table: [
                        [],
                        ["TBG-3"],
                        ["TDP-3", "TDP-7", "TBG-4"],
                        ["TDP-4"]]
                }
            );
        });

        it ("Custom field 'Documenter'", () => {
            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.FULL_BOARD_PROJECTS;
            bd.issues = TestBoardData.getFullBoardCustomFieldIssues();
            bd.custom = TestBoardData.STANDARD_CUSTOM_FIELDS;
            boardData.deserialize("tst", bd.build());
            new BoardDataChecker(boardData)
                .linkedProjects("TUP")
                .customField("Tester", "james", "James Perkins")
                .customField("Tester", "kabir", "Kabir Khan")
                .customField("Documenter", "kabir", "Kabir Khan")
                .customField("Documenter", "stuart", "Stuart Douglas")
                .check();
            boardData.swimlane = "Documenter";

            let table:SwimlaneData[] = boardData.swimlaneTable;

            checkBoardSwimlaneLayout(boardData,
                {
                    "name": "Kabir Khan",
                    table: [
                        ["TDP-1", "TDP-5"],
                        ["TBG-1"],
                        [],
                        []]
                },
                {
                    "name": "Stuart Douglas",
                    table: [
                        [],
                        ["TDP-2", "TDP-6"],
                        ["TBG-2"],
                        []]
                },
                {
                    "name": "None",
                    table: [
                        [],
                        ["TBG-3"],
                        ["TDP-3", "TDP-7", "TBG-4"],
                        ["TDP-4"]]
                }
            );
        });
    });
});


function checkBoardSwimlaneLayout(boardData:BoardData, ...layouts:any[]) {
    let swimlanes:SwimlaneData[] = boardData.swimlaneTable;
    expect(swimlanes).toEqual(jasmine.anything());
    expect(swimlanes.length).toEqual(layouts.length);

    for (let i:number = 0 ; i < swimlanes.length ; i++) {
        let swimlane:SwimlaneData = swimlanes[i];
        let layout:any = layouts[i];
        expect(swimlane.name).toEqual(layout.name);
        expect(swimlane.issueTable.length).toEqual(layout.table.length);

        console.log(swimlane.name);
        for (let j:number = 0 ; j < layout.table.length ; j++) {
            let swimlaneIssues:IssueData[] = swimlane.issueTable[j];
            let layoutKeys:string[] = layout.table[j];
            expect(layoutKeys.length).toEqual(swimlaneIssues.length);

            for (let k:number = 0 ; k < layoutKeys.length ; k++) {
                let issue:IssueData = swimlaneIssues[k];
                expect(issue.key).toEqual(layoutKeys[k]);
            }
        }
    }
}


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
 * @param skipKey the issue keys to skip
 * @returns the issues that were skipped
 */
function checkIssueDatas(config:IssueDataCheckerConfig, boardData:BoardData, layout:string[][], ...skipKeys:string[]) : Indexed<IssueData> {

    checkBoardLayout(boardData, layout);

    let skippedKeys:IMap<boolean> = {};
    for (let key of skipKeys) {
        skippedKeys[key] = true;
    }

    let skippedIssues: Indexed<IssueData> = new Indexed<IssueData>();

    for (let i:number = 0; i < layout.length; i++) {
        for (let j:number = 0; j < layout[i].length; j++) {
            let issue: IssueData = boardData.getIssue(layout[i][j]);
            if (skippedKeys[issue.key]) {
                //If the issue was skipped, we return the issues for manual checks
                skippedIssues.add(issue.key, issue);
                continue;
            }

            let id: number = getIdFromKey(issue.key);
            let mod4 = (id - 1) % 4;

            let checker: IssueDataChecker;
            let summary: string = DEFAULT_SUMMARIES[id - 1];
            switch (mod4) {
                case 0:
                    checker = new IssueDataChecker(issue, "task", "highest", "brian", summary)
                        .components("C1");
                    if (config.customFields) {
                        checker.customField("Tester", "james", "James Perkins")
                            .customField("Documenter", "kabir", "Kabir Khan")
                    }
                    break;
                case 1:
                    checker = new IssueDataChecker(issue, "bug", "high", "kabir", summary)
                        .components("C5");
                    if (config.customFields) {
                        checker.customField("Tester", "kabir", "Kabir Khan")
                            .customField("Documenter", "stuart", "Stuart Douglas");
                    }
                    break;
                case 2:
                    checker = new IssueDataChecker(issue, "feature", "low", null, summary)
                        .labels("L1")
                        .fixVersions("F5")
                    break;
                case 3:
                    checker = new IssueDataChecker(issue, "issue", "lowest", null, summary)
                        .labels("L5")
                        .fixVersions("F1")
                    break;
            }
            checker.check();
        }
    }
    return skippedIssues;
}

function checkBoardLayout(boardData:BoardData, layout:string[][]) {
    expect(boardData.swimlaneTable).not.toEqual(jasmine.anything());
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

function checkBoardRank(boardData:BoardData, ranks:any) {
    for (let projectCode in ranks) {
        let expectedOrder:string[] = ranks[projectCode];

        let project:BoardProject = boardData.boardProjects.forKey(projectCode);
        let order:string[] = project.rankedIssueKeys;
        expect(order.length).toBe(expectedOrder.length);
        for (let i = 0 ; i < order.length ; i++) {
            expect(order[i]).toBe(expectedOrder[i]);
        }
    }
}

function getIdFromKey(issueKey:string):number {
    let index:number = issueKey.indexOf("-");
    expect(index).toBeGreaterThan(0);
    let sub:string = issueKey.substr(index + 1);
    return Number(sub);
}

function checkIssueConvenienceMethods(issue:IssueData) {
    let assignee:Assignee = issue.assignee;
    if (!assignee) {
        expect(issue.assigneeAvatar).toBeNull();
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

class FilterBuilder {
    private _boardData:BoardData;

    private _projectFilter:any = {};
    private _priorityFilter:any = {};
    private _issueTypeFilter:any = {};
    private _assigneeFilter:any = {};
    private _componentFilter:any = {};
    private _labelFilter:any = {};
    private _fixVersionFilter:any = {};
    private _customFieldValueFilters:IMap<any> = {};
    private _parallelTaskFilters:IMap<any> = {};

    constructor(boardData: BoardData) {
        this._boardData = boardData;
    }

    setProjectFilter(projectFilter:any):FilterBuilder {
        this._projectFilter = projectFilter;
        return this;
    }

    setPriorityFilter(priorityFilter:any):FilterBuilder {
        this._priorityFilter = priorityFilter;
        return this;
    }

    setIssueTypeFilter(issueTypeFilter:any):FilterBuilder {
        this._issueTypeFilter = issueTypeFilter;
        return this;
    }

    setAssigneeFilter(assigneeFilter:any):FilterBuilder {
        this._assigneeFilter = assigneeFilter;
        return this;
    }

    setComponentFilter(componentFilter:any):FilterBuilder {
        this._componentFilter = componentFilter;
        return this;
    }

    setLabelFilter(labelFilter:any):FilterBuilder {
        this._labelFilter = labelFilter;
        return this;
    }

    setFixVersionFilter(fixVersionFilter:any):FilterBuilder {
        this._fixVersionFilter = fixVersionFilter;
        return this;
    }

    setCustomFieldFilter(customFieldFilter:IMap<any>):FilterBuilder{
        this._customFieldValueFilters = customFieldFilter;
        return this;
    }

    setParallelTasksFilter(parallelTasksFilter:IMap<any>):FilterBuilder {
        this._parallelTaskFilters = parallelTasksFilter;
        return this;
    }

    buildAndFilterBoardData():void{
        this._boardData.updateFilters(this._projectFilter, this._priorityFilter, this._issueTypeFilter,
            this._assigneeFilter, this._componentFilter, this._labelFilter, this._fixVersionFilter,
            this._customFieldValueFilters, this._parallelTaskFilters);
    }
}

interface IssueDataCheckerConfig {
    readonly customFields:IMap<CustomFieldValue[]>;
}

class BoardDataChecker implements IssueDataCheckerConfig {
    private _boardData:BoardData;
    private _assignees:string[] = ["brian", "kabir"];
    private _components:string[] = ["C1", "C5"];
    private _labels:string[] = ["L1", "L5"];
    private _fixVersions:string[] = ["F1", "F5"];
    private _customFields:IMap<CustomFieldValue[]>;
    private _linkedProjects:string[] = [];
    private _mainProjects:string[] = ["TDP", "TBG"];


    constructor(boardData:BoardData) {
        this._boardData = boardData;
    }

    assignees(...assignees:string[]):BoardDataChecker {
        this._assignees = assignees;
        return this;
    }

    components(...components:string[]):BoardDataChecker {
        this._components = components;
        return this;
    }

    labels(...labels:string[]):BoardDataChecker {
        this._labels = labels;
        return this;
    }

    fixVersions(...fixVersions:string[]):BoardDataChecker {
        this._fixVersions = fixVersions;
        return this;
    }

    customField(field:string, key:string, displayValue:string):BoardDataChecker {
        if (!this._customFields) {
            this._customFields = {};
        }
        let fieldArray:CustomFieldValue[] = this._customFields[field];
        if (!this._customFields[field]) {
            fieldArray = [];
            this._customFields[field] = fieldArray;
        }
        fieldArray.push(new CustomFieldValue(key, displayValue));
        return this;
    }

    linkedProjects(...linkedProjects:string[]):BoardDataChecker {
        if (!linkedProjects) {
            this._linkedProjects = [];
        } else {
            this._linkedProjects = linkedProjects;
        }
        return this;
    }

    mainProjects(...mainProjects:string[]):BoardDataChecker {
        this._mainProjects = mainProjects;
        return this;
    }


    get customFields(): IMap<CustomFieldValue[]> {
        return this._customFields;
    }

    check():BoardDataChecker {
        this.checkProjects();
        this.checkAssignees();
        this.checkComponents();
        this.checkLabels();
        this.checkFixVersions();
        this.checkStandardPriorities();
        this.checkStandardIssueTypes();
        this.checkCustomFields();
        return this;
    }

    private checkAssignees():void {
        expect(this._boardData.assignees.array.length).toBe(this._assignees.length);
        for (let i = 0; i < this._assignees.length; i++) {
            DataChecker.checkAssignee(this._boardData.assignees.forIndex(i), this._assignees[i]);
        }
    }

    private checkComponents(): void {
        expect(this._boardData.components.array.length).toBe(this._components.length);
        for (let i = 0; i < this._components.length; i++) {
            expect(this._boardData.components.forIndex(i).name).toEqual(this._components[i]);
        }
    }

    private checkLabels() : void {
        expect(this._boardData.labels.array.length).toBe(this._labels.length);
        for (let i = 0; i < this._labels.length; i++) {
            expect(this._boardData.labels.forIndex(i).name).toEqual(this._labels[i]);
        }
    }

    private checkFixVersions() : void {
        expect(this._boardData.fixVersions.array.length).toBe(this._fixVersions.length);
        for (let i = 0; i < this._fixVersions.length; i++) {
            expect(this._boardData.fixVersions.forIndex(i).name).toEqual(this._fixVersions[i]);
        }
    }

    private checkStandardPriorities() {
        let priorities:Indexed<Priority> = this._boardData.priorities;
        expect(priorities.array.length).toEqual(4);
        DataChecker.checkPriority(priorities.array[0], "highest");
        DataChecker.checkPriority(priorities.array[1], "high");
        DataChecker.checkPriority(priorities.array[2], "low");
        DataChecker.checkPriority(priorities.array[3], "lowest");
    }

    private checkStandardIssueTypes() {
        let issueTypes:Indexed<IssueType> = this._boardData.issueTypes;
        expect(issueTypes.array.length).toEqual(4);
        DataChecker.checkIssueType(issueTypes.array[0], "task");
        DataChecker.checkIssueType(issueTypes.array[1], "bug");
        DataChecker.checkIssueType(issueTypes.array[2], "feature");
        DataChecker.checkIssueType(issueTypes.array[3], "issue");
    }

    private checkProjects() {
        expect(this._boardData.owner).toBe(this._mainProjects[0]);
        expect(this._boardData.boardProjects).not.toBeNull();
        expect(this._boardData.boardProjectCodes.length).toBe(this._mainProjects.length);
        for (let code of this._mainProjects) {
            expect(this._boardData.boardProjectCodes).toContain(code);
        }

        if (this._linkedProjects) {
            expect(this._boardData.linkedProjects).toEqual(jasmine.anything());
            let linked:string[] = IMapUtil.getSortedKeys(this._boardData.linkedProjects);
            expect(linked.length).toEqual(this._linkedProjects.length);
            for (let linkedProject of this._linkedProjects) {
                expect(this._boardData.linkedProjects[linkedProject]).toEqual(jasmine.anything());
            }
        }
    }

    private checkCustomFields() {
        let boardFields:Indexed<CustomFieldValues> = this._boardData.customFields;
        //this should always be initialised, but will be empty if there are no fields
        expect(boardFields).toEqual(jasmine.anything());
        if (!this._customFields) {
            expect(boardFields.array).toEqual([]);
        } else {
            let expectedFieldNames:string[] = IMapUtil.getSortedKeys(this._customFields);
            expect(boardFields.sortedKeys).toEqual(expectedFieldNames);

            for (let fieldName of expectedFieldNames) {
                let boardValues:CustomFieldValue[] = boardFields.forKey(fieldName).values.array;
                let expectedValues: CustomFieldValue[] = this._customFields[fieldName];
                expect(boardValues.length).toBe(expectedValues.length);
                for (let i:number = 0 ; i < boardValues.length ; i++) {
                    expect(boardValues[i].key).toEqual(expectedValues[i].key);
                    expect(boardValues[i].displayValue).toEqual(expectedValues[i].displayValue);
                }
            }
        }
    }
}

class IssueDataChecker {
    private _issue:IssueData;
    private _key:string;
    private _type:string;
    private _priority:string;
    private _assignee:string;
    private _summary:string;
    private _components:string[];
    private _labels:string[];
    private _fixVersions:string[];
    private _customFields:IMap<CustomFieldValue>;
    private _selectedParallelTaskOptions:string[]



    constructor(issue: IssueData, type: string, priority: string, assignee: string, summary:string) {
        this._issue = issue;
        this._key = issue.key;
        this._type = type;
        this._priority = priority;
        this._assignee = assignee;
        this._summary = summary;
    }

    key(key:string) : IssueDataChecker {
        this._key = key;
        return this;
    }

    components(...components:string[]) : IssueDataChecker {
        this._components = components;
        return this;
    }

    labels(...labels:string[]) : IssueDataChecker {
        this._labels = labels;
        return this;
    }

    fixVersions(...fixVersions:string[]) : IssueDataChecker {
        this._fixVersions = fixVersions;
        return this;
    }

    customField(field:string, key:string, displayValue:string) : IssueDataChecker {
        if (!this._customFields) {
            this._customFields = {};
        }
        this._customFields[field] = new CustomFieldValue(key, displayValue);
        return this;
    }

    selectedParallelTaskOptions(...selectedOptions:string[]) : IssueDataChecker {
        this._selectedParallelTaskOptions = selectedOptions;
        return this;
    }

    check() {
        expect(this._issue.key).toEqual(this._key);
        DataChecker.checkIssueType(this._issue.type, this._type);
        DataChecker.checkPriority(this._issue.priority, this._priority);
        if (this._assignee) {
            DataChecker.checkAssignee(this._issue.assignee, this._assignee);
        } else {
            expect(this._issue.assignee).not.toEqual(jasmine.anything());
        }

        if (this._components) {
            this.checkMultiSelectFieldValues(this._issue.components.array, this._components);

        } else {
            expect(this._issue.components).not.toEqual(jasmine.anything());
        }

        if (this._labels) {
            this.checkMultiSelectFieldValues(this._issue.labels.array, this._labels);
        } else {
            expect(this._issue.labels).not.toEqual(jasmine.anything());
        }

        if (this._fixVersions) {
            this.checkMultiSelectFieldValues(this._issue.fixVersions.array, this._fixVersions);
        } else {
            expect(this._issue.fixVersions).not.toEqual(jasmine.anything(), this._issue.key);
        }

        if (this._summary) {
            expect(this._issue.summary).toEqual(this._summary);
        }

        if (this._customFields) {
            let issueFieldNames:string[] = this._issue.customFieldNames;
            let expectedFieldNames:string[] = IMapUtil.getSortedKeys(this._customFields);
            expect(expectedFieldNames).toEqual(issueFieldNames);

            for (let fieldName of issueFieldNames) {
                let customField:CustomFieldValue = this._issue.getCustomFieldValue(fieldName);
                let expectedField:CustomFieldValue = this._customFields[fieldName];
                expect(customField).toEqual(jasmine.anything());
                expect(customField.key).toEqual(expectedField.key);
                expect(customField.displayValue).toEqual(expectedField.displayValue);
            }
        } else {
            expect(this._issue.customFieldNames).toEqual(jasmine.anything());
        }

        if (this._selectedParallelTaskOptions) {
            let options:Indexed<string> = this._issue.parallelTaskOptions;
            expect(options).toEqual(jasmine.anything());
            expect(options.array.length).toEqual(this._selectedParallelTaskOptions.length);
            for (let i = 0 ; i < this._selectedParallelTaskOptions.length ; i++) {
                expect(options.array[i]).toEqual(this._selectedParallelTaskOptions[i]);
            }
        } else {
            expect(this._selectedParallelTaskOptions).not.toEqual(jasmine.anything());
        }

        checkIssueConvenienceMethods(this._issue);
    }

    private checkMultiSelectFieldValues(issueValues:JiraMultiSelectFieldValue[], keys:string[]) {
        expect(issueValues).toEqual(jasmine.anything());
        expect(issueValues.length).toEqual(keys.length);
        for (let i:number = 0 ; i < keys.length ; i++) {
            expect(keys).toContain(issueValues[i].name);
        }
    }
}

class DataChecker {
    static checkAssignee(assignee:Assignee, key:string) {
        expect(assignee.key).toEqual(key);
        expect(assignee.avatar).toEqual("/avatars/" + key + ".png");
        expect(assignee.email).toEqual(key + "@example.com");
        expect(assignee.name.toLowerCase()).toContain(key.toLowerCase());
    }

    static checkPriority(priority:Priority, name:string) {
        expect(priority.name).toEqual(name);
        expect(priority.icon).toEqual("/icons/priorities/" + name + ".png");
    }

    static checkIssueType(type:IssueType, name:string) {
        expect(type.name).toEqual(name);
        expect(type.icon).toEqual("/icons/issue-types/" + name + ".png");
    }
}

const DEFAULT_SUMMARIES: string[] = ["One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten"];

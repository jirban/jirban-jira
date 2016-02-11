import {BoardData} from "./../boardData";
import {TestBoardData} from "./testData";
import {Assignee} from "./../assignee";
import {Priority} from "./../priority";
import {Indexed} from "../../../common/indexed";
import {IssueType} from "./../issueType";
import {IssueTable} from "./../issueTable";
import {IssueData} from "./../issueData";

describe('BoardData tests', ()=> {
//Tests for the BoardData component which is so central to the display of the board
    describe('Load', () => {

        it('Full board; No blacklist', () => {
            let boardData:BoardData = new BoardData();
            boardData.deserialize(1,
                TestBoardData.create(TestBoardData.FULL_BOARD_PROJECTS, TestBoardData.FULL_BOARD_ISSUES));

            expect(boardData.view).toEqual(0);
            checkAssignees(boardData, "brian", "kabir");
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
            let bd:TestBoardData = new TestBoardData();
            bd.projects = TestBoardData.FULL_BOARD_PROJECTS;
            bd.issues = TestBoardData.FULL_BOARD_ISSUES;
            bd.blacklist = TestBoardData.STANDARD_BLACKLIST;

            let boardData:BoardData = new BoardData();
            boardData.deserialize(1, bd.build());

            expect(boardData.view).toEqual(0);

            checkAssignees(boardData, "brian", "kabir");

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
                        "removed-issues": ["TDP-2", "TBG-1"]
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

        xit('Remove from and add to blacklist', () => {
            //Combine the two above tests to make sure everything gets removed from the issue table
            fail("NYI");
        });
    });

    function checkEntries(value:string[], ...expected:string[]) {
        expect(value.length).toBe(expected.length);
        for (let ex of expected) {
            expect(value).toContain(ex);
        }
    }

    function checkIssueDatas(boardData:BoardData, layout:string[][]) {
        for (let i:number = 0; i < layout.length; i++) {
            for (let j:number = 0; j < layout[i].length; j++) {
                let issue:IssueData = boardData.getIssue(layout[i][j]);
                let id:number = getIdFromKey(issue.key);
                let mod4 = (id - 1) % 4;
                switch (mod4) {
                    case 0:
                        checkBoardIssueType(issue.type, "task");
                        checkBoardPriority(issue.priority, "highest");
                        checkBoardAssignee(issue.assignee, "brian");
                        break;
                    case 1:
                        checkBoardIssueType(issue.type, "bug");
                        checkBoardPriority(issue.priority, "high");
                        checkBoardAssignee(issue.assignee, "kabir");
                        break;
                    case 2:
                        checkBoardIssueType(issue.type, "feature");
                        checkBoardPriority(issue.priority, "low");
                        expect(issue.assignee).not.toBeDefined();
                        break;
                    case 3:
                        checkBoardIssueType(issue.type, "issue");
                        checkBoardPriority(issue.priority, "lowest");
                        expect(issue.assignee).not.toBeDefined();
                        break;
                }
                checkIssueConvenienceMethods(issue);
            }
        }
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
                        expect(issue.boardStatus).toBe("TDP-A");
                        expect(issue.ownStatus).toBe("TDP-A");
                    } else if (i == 1) {
                        expect(issue.boardStatus).toBe("TDP-B");
                        expect(issue.ownStatus).toBe("TDP-B");
                    } else if (i == 2) {
                        expect(issue.boardStatus).toBe("TDP-C");
                        expect(issue.ownStatus).toBe("TDP-C");
                    } else if (i == 3) {
                        expect(issue.boardStatus).toBe("TDP-D");
                        expect(issue.ownStatus).toBe("TDP-D");
                    } else {
                        fail("Bad TDP state index " + i);
                    }
                } else if (issue.projectCode === "TBG") {
                    expect(issue.statusIndex).toBe(i - 1);
                    if (i == 1) {
                        expect(issue.boardStatus).toBe("TDP-B");
                        expect(issue.ownStatus).toBe("TBG-X");
                    } else if (i == 2) {
                        expect(issue.boardStatus).toBe("TDP-C");
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
});

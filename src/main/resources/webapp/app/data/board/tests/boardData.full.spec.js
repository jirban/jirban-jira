System.register(["./../boardData", "./testData"], function(exports_1) {
    var boardData_1, testData_1;
    function checkEntries(value) {
        var expected = [];
        for (var _i = 1; _i < arguments.length; _i++) {
            expected[_i - 1] = arguments[_i];
        }
        expect(value.length).toBe(expected.length);
        for (var _a = 0; _a < expected.length; _a++) {
            var ex = expected[_a];
            expect(value).toContain(ex);
        }
    }
    function checkIssueDatas(boardData, layout) {
        for (var i = 0; i < layout.length; i++) {
            for (var j = 0; j < layout[i].length; j++) {
                var issue = boardData.getIssue(layout[i][j]);
                var id = getIdFromKey(issue.key);
                var mod4 = (id - 1) % 4;
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
    function checkBoardLayout(boardData, layout) {
        var issueTable = boardData.issueTable;
        expect(issueTable.length).toBe(layout.length);
        for (var i = 0; i < layout.length; i++) {
            var columnData = issueTable[i];
            var columnLayout = layout[i];
            console.log(columnData);
            console.log(columnLayout);
            console.log(columnData.length);
            console.log(columnLayout.length);
            //expect(boardData.totalIssuesByState[i]).toBe(columnLayout.length);
            expect(columnData.length).toBe(columnLayout.length, "The length of column is different " + i);
            //console.log(columnData.length === columnLayout.length);
            //expect(columnData.length).toEqual(columnLayout.length);
            //expect(columnData.length === columnLayout.length).toBe(true);
            for (var j = 0; j < columnLayout.length; j++) {
                expect(columnData[j].key).toBe(columnLayout[j]);
                //Check the states are mapped property in both projects
                var issue = columnData[j];
                if (issue.projectCode === "TDP") {
                    expect(issue.statusIndex).toBe(i);
                    if (i == 0) {
                        expect(issue.boardStatus).toBe("TDP-A");
                        expect(issue.ownStatus).toBe("TDP-A");
                    }
                    else if (i == 1) {
                        expect(issue.boardStatus).toBe("TDP-B");
                        expect(issue.ownStatus).toBe("TDP-B");
                    }
                    else if (i == 2) {
                        expect(issue.boardStatus).toBe("TDP-C");
                        expect(issue.ownStatus).toBe("TDP-C");
                    }
                    else if (i == 3) {
                        expect(issue.boardStatus).toBe("TDP-D");
                        expect(issue.ownStatus).toBe("TDP-D");
                    }
                    else {
                        fail("Bad TDP state index " + i);
                    }
                }
                else if (issue.projectCode === "TBG") {
                    expect(issue.statusIndex).toBe(i - 1);
                    if (i == 1) {
                        expect(issue.boardStatus).toBe("TDP-B");
                        expect(issue.ownStatus).toBe("TBG-X");
                    }
                    else if (i == 2) {
                        expect(issue.boardStatus).toBe("TDP-C");
                        expect(issue.ownStatus).toBe("TBG-Y");
                    }
                    else {
                        fail("Bad TBG state index " + i);
                    }
                }
                else {
                    fail("Bad project " + issue.projectCode);
                }
            }
        }
    }
    function getIdFromKey(issueKey) {
        var index = issueKey.indexOf("-");
        expect(index).toBeGreaterThan(0);
        var sub = issueKey.substr(index + 1);
        return Number(sub);
    }
    function checkAssignees(boardData) {
        var assignees = [];
        for (var _i = 1; _i < arguments.length; _i++) {
            assignees[_i - 1] = arguments[_i];
        }
        expect(boardData.assignees.array.length).toBe(assignees.length);
        for (var i = 0; i < assignees.length; i++) {
            checkBoardAssignee(boardData.assignees.forIndex(i), assignees[i]);
        }
    }
    function checkStandardPriorities(boardData) {
        var priorities = boardData.priorities;
        expect(priorities.array.length).toEqual(4);
        checkBoardPriority(priorities.array[0], "highest");
        checkBoardPriority(priorities.array[1], "high");
        checkBoardPriority(priorities.array[2], "low");
        checkBoardPriority(priorities.array[3], "lowest");
    }
    function checkStandardIssueTypes(boardData) {
        var issueTypes = boardData.issueTypes;
        expect(issueTypes.array.length).toEqual(4);
        checkBoardIssueType(issueTypes.array[0], "task");
        checkBoardIssueType(issueTypes.array[1], "bug");
        checkBoardIssueType(issueTypes.array[2], "feature");
        checkBoardIssueType(issueTypes.array[3], "issue");
    }
    function checkBoardAssignee(assignee, key) {
        expect(assignee.key).toEqual(key);
        expect(assignee.avatar).toEqual("/avatars/" + key + ".png");
        expect(assignee.email).toEqual(key + "@example.com");
        expect(assignee.name.toLowerCase()).toContain(key.toLowerCase());
    }
    function checkStandardProjects(boardData) {
        checkProjects(boardData, "TUP", "TDP", "TBG");
    }
    function checkProjects(boardData, linkedProject) {
        var mainProjects = [];
        for (var _i = 2; _i < arguments.length; _i++) {
            mainProjects[_i - 2] = arguments[_i];
        }
        expect(boardData.owner).toBe(mainProjects[0]);
        expect(boardData.boardProjects).not.toBeNull();
        expect(boardData.boardProjectCodes.length).toBe(mainProjects.length);
        for (var _a = 0; _a < mainProjects.length; _a++) {
            var code = mainProjects[_a];
            expect(boardData.boardProjectCodes).toContain(code);
        }
        if (linkedProject) {
            expect(boardData.linkedProjects).toEqual(jasmine.anything());
            expect(boardData.linkedProjects[linkedProject]).toEqual(jasmine.anything());
        }
    }
    function checkBoardPriority(priority, name) {
        expect(priority.name).toEqual(name);
        expect(priority.icon).toEqual("/icons/priorities/" + name + ".png");
    }
    function checkBoardIssueType(type, name) {
        expect(type.name).toEqual(name);
        expect(type.icon).toEqual("/icons/issue-types/" + name + ".png");
    }
    function checkIssueConvenienceMethods(issue) {
        var assignee = issue.assignee;
        if (!assignee) {
            expect(issue.assigneeAvatar).toBe("images/person-4x.png");
            expect(issue.assigneeInitials).toBe("None");
            expect(issue.assigneeName).toBe("Unassigned");
        }
        else {
            expect(issue.assigneeAvatar).toBe(assignee.avatar);
            expect(issue.assigneeInitials).toBe(assignee.initials);
            expect(issue.assigneeName).toBe(assignee.name);
        }
        var priority = issue.priority;
        expect(issue.priorityName).toBe(priority.name);
        expect(issue.priorityUrl).toBe(priority.icon);
        var issueType = issue.type;
        expect(issue.typeName).toBe(issueType.name);
        expect(issue.typeUrl).toBe(issueType.icon);
    }
    return {
        setters:[
            function (boardData_1_1) {
                boardData_1 = boardData_1_1;
            },
            function (testData_1_1) {
                testData_1 = testData_1_1;
            }],
        execute: function() {
            //Tests for the BoardData component which is so central to the display of the board
            describe('Load Board', function () {
                it('Full board; No blacklist', function () {
                    var boardData = new boardData_1.BoardData();
                    boardData.deserialize(1, testData_1.TestBoardData.create(testData_1.TestBoardData.FULL_BOARD_PROJECTS, testData_1.TestBoardData.FULL_BOARD_ISSUES));
                    expect(boardData.view).toEqual(0);
                    checkAssignees(boardData, "brian", "kabir");
                    checkStandardPriorities(boardData);
                    checkStandardIssueTypes(boardData);
                    expect(boardData.swimlane).not.toBeDefined();
                    expect(boardData.swimlaneTable).toBeNull();
                    checkStandardProjects(boardData);
                    expect(boardData.boardStates.length).toBe(4);
                    checkBoardLayout(boardData, testData_1.TestBoardData.EXPECTED_FULL_BOARD);
                    checkIssueDatas(boardData, testData_1.TestBoardData.EXPECTED_FULL_BOARD);
                    expect(boardData.blacklist).toBeNull();
                });
                it('Full board; Blacklist', function () {
                    var bd = new testData_1.TestBoardData();
                    bd.projects = testData_1.TestBoardData.FULL_BOARD_PROJECTS;
                    bd.issues = testData_1.TestBoardData.FULL_BOARD_ISSUES;
                    bd.blacklist = testData_1.TestBoardData.STANDARD_BLACKLIST;
                    var boardData = new boardData_1.BoardData();
                    boardData.deserialize(1, bd.build());
                    expect(boardData.view).toEqual(0);
                    checkAssignees(boardData, "brian", "kabir");
                    checkStandardPriorities(boardData);
                    checkStandardIssueTypes(boardData);
                    expect(boardData.swimlane).not.toBeDefined();
                    expect(boardData.swimlaneTable).toBeNull();
                    checkStandardProjects(boardData);
                    expect(boardData.boardStates.length).toBe(4);
                    checkBoardLayout(boardData, testData_1.TestBoardData.EXPECTED_FULL_BOARD);
                    checkIssueDatas(boardData, testData_1.TestBoardData.EXPECTED_FULL_BOARD);
                    expect(boardData.blacklist).toBeDefined();
                    checkEntries(boardData.blacklist.issues, "TDP-100", "TBG-101");
                    checkEntries(boardData.blacklist.issueTypes, "BadIssueType");
                    checkEntries(boardData.blacklist.priorities, "BadPriority");
                    checkEntries(boardData.blacklist.states, "BadState");
                });
                it('Owner Project Only; No blacklist', function () {
                    var boardData = new boardData_1.BoardData();
                    boardData.deserialize(1, testData_1.TestBoardData.create(testData_1.TestBoardData.OWNER_ONLY_BOARD_PROJECTS, testData_1.TestBoardData.OWNER_ONLY_BOARD_ISSUES));
                    expect(boardData.view).toEqual(0);
                    checkAssignees(boardData, "brian", "kabir");
                    checkStandardPriorities(boardData);
                    checkStandardIssueTypes(boardData);
                    expect(boardData.swimlane).not.toBeDefined();
                    expect(boardData.swimlaneTable).toBeNull();
                    checkProjects(boardData, null, "TDP");
                    expect(boardData.boardStates.length).toBe(4);
                    checkBoardLayout(boardData, testData_1.TestBoardData.EXPECTED_OWNER_ONLY_BOARD);
                    checkIssueDatas(boardData, testData_1.TestBoardData.EXPECTED_OWNER_ONLY_BOARD);
                    expect(boardData.blacklist).toBeNull();
                });
                it('Non-owner issues only; No blacklist', function () {
                    var boardData = new boardData_1.BoardData();
                    boardData.deserialize(1, testData_1.TestBoardData.create(testData_1.TestBoardData.NON_OWNER_ONLY_BOARD_PROJECTS, testData_1.TestBoardData.NON_OWNER_ONLY_BOARD_ISSUES));
                    expect(boardData.view).toEqual(0);
                    checkAssignees(boardData, "brian", "kabir");
                    checkStandardPriorities(boardData);
                    checkStandardIssueTypes(boardData);
                    expect(boardData.swimlane).not.toBeDefined();
                    expect(boardData.swimlaneTable).toBeNull();
                    checkProjects(boardData, null, "TDP", "TBG");
                    expect(boardData.boardStates.length).toBe(4);
                    checkBoardLayout(boardData, testData_1.TestBoardData.EXPECTED_NON_OWNER_ONLY_BOARD);
                    checkIssueDatas(boardData, testData_1.TestBoardData.EXPECTED_NON_OWNER_ONLY_BOARD);
                    expect(boardData.blacklist).toBeNull();
                });
            });
            describe('No Change', function () {
                it('No change', function () {
                    var boardData = new boardData_1.BoardData();
                    boardData.deserialize(1, testData_1.TestBoardData.create(testData_1.TestBoardData.PRE_CHANGE_BOARD_PROJECTS, testData_1.TestBoardData.PRE_CHANGE_BOARD_ISSUES));
                    var changes = {
                        changes: {
                            view: 0
                        }
                    };
                    boardData.processChanges(changes);
                    expect(boardData.blacklist).toBeNull();
                    var layout = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
                    checkBoardLayout(boardData, layout);
                    checkIssueDatas(boardData, layout);
                });
            });
            describe('New Blacklist ', function () {
                it('Board unaffected', function () {
                    var boardData = new boardData_1.BoardData();
                    boardData.deserialize(1, testData_1.TestBoardData.create(testData_1.TestBoardData.PRE_CHANGE_BOARD_PROJECTS, testData_1.TestBoardData.PRE_CHANGE_BOARD_ISSUES));
                    expect(boardData.blacklist).toBeNull();
                    //These issues are not part of the board
                    var changes = {
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
                    var layout = [["TDP-1"], ["TDP-2", "TBG-1"], [], []];
                    checkBoardLayout(boardData, layout);
                    checkIssueDatas(boardData, layout);
                });
                it('Affects board issues', function () {
                    var boardData = new boardData_1.BoardData();
                    boardData.deserialize(1, testData_1.TestBoardData.create(testData_1.TestBoardData.PRE_CHANGE_BOARD_PROJECTS, testData_1.TestBoardData.PRE_CHANGE_BOARD_ISSUES));
                    expect(boardData.blacklist).toBeNull();
                    //These issues are not part of the board
                    var changes = {
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
                    var layout = [[], ["TDP-2"], [], []];
                    checkBoardLayout(boardData, layout);
                    checkIssueDatas(boardData, layout);
                });
            });
        }
    }
});
//# sourceMappingURL=boardData.full.spec.js.map
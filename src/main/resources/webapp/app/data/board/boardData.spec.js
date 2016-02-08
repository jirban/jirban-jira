System.register(["./boardData", "./test-data/test-board-data"], function(exports_1) {
    var boardData_1, test_board_data_1;
    function checkBoardLayout(boardData, layout) {
        var issueTable = boardData.issueTable;
        expect(issueTable.length).toBe(layout.length);
        for (var i = 0; i < layout.length; i++) {
            var columnData = issueTable[i];
            var columnLayout = layout[i];
            expect(columnData.length).toBe(columnLayout.length);
            for (var j = 0; j < columnLayout.length; j++) {
                expect(columnData[j].key).toBe(columnLayout[j]);
                var issueData = columnData[j];
                if (issueData.projectCode === "TDP") {
                    expect(issueData.statusIndex).toBe(i);
                }
                else if (issueData.projectCode === "TBG") {
                    expect(issueData.statusIndex).toBe(i - 1);
                    expect(issueData.statusIndex).toBeLessThan(2);
                }
            }
        }
    }
    function checkAssignees(boardData) {
        var assignees = [];
        for (var _i = 1; _i < arguments.length; _i++) {
            assignees[_i - 1] = arguments[_i];
        }
        expect(boardData.assignees.array.length).toBe(assignees.length);
        for (var i = 0; i < assignees.length; i++) {
            var name_1 = void 0;
            switch (assignees[i]) {
                case "brian":
                    name_1 = "Brian Stansberry";
                    break;
                case "kabir":
                    name_1 = "Kabir Khan";
                    break;
                default:
                    fail("Unknown name");
            }
            checkBoardAssignee(boardData.assignees.forIndex(i), assignees[i], name_1);
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
        expect(issueTypes.array.length).toEqual(3);
        checkBoardIssueType(issueTypes.array[0], "task");
        checkBoardIssueType(issueTypes.array[1], "bug");
        checkBoardIssueType(issueTypes.array[2], "feature");
    }
    function checkBoardAssignee(assignee, key, name) {
        expect(assignee.key).toEqual(key);
        expect(assignee.avatar).toEqual("/avatars/" + key + ".png");
        expect(assignee.email).toEqual(key + "@example.com");
        expect(assignee.name).toEqual(name);
    }
    function checkStandardProjects(boardData) {
        expect(boardData.owner).toBe("TDP");
        expect(boardData.boardProjects).not.toBeNull();
        expect(boardData.boardProjectCodes).toContain("TDP", "TBG");
        expect(boardData.linkedProjects).not.toBeNull();
        expect(boardData.linkedProjects["TUP"]).not.toBeNull();
    }
    function checkBoardPriority(priority, name) {
        expect(priority.name).toEqual(name);
        expect(priority.icon).toEqual("/icons/priorities/" + name + ".png");
    }
    function checkBoardIssueType(type, name) {
        expect(type.name).toEqual(name);
        expect(type.icon).toEqual("/icons/issue-types/" + name + ".png");
    }
    return {
        setters:[
            function (boardData_1_1) {
                boardData_1 = boardData_1_1;
            },
            function (test_board_data_1_1) {
                test_board_data_1 = test_board_data_1_1;
            }],
        execute: function() {
            //Tests for the BoardData component which is so central to the display of the board
            describe('Load Board Test', function () {
                var boardData;
                beforeEach(function () {
                    boardData = new boardData_1.BoardData();
                    boardData.deserialize(1, JSON.parse(test_board_data_1.TestBoardData.BASE_BOARD));
                });
                it('Simple load', function () {
                    expect(boardData.view).toEqual(0);
                    checkAssignees(boardData, "brian", "kabir");
                    checkStandardPriorities(boardData);
                    checkStandardIssueTypes(boardData);
                    expect(boardData.swimlane).not.toBeDefined();
                    expect(boardData.swimlaneTable).toBeNull();
                    checkStandardProjects(boardData);
                    expect(boardData.boardStates.length).toBe(4);
                    checkBoardLayout(boardData, test_board_data_1.TestBoardData.EXPECTED_BASE_BOARD);
                });
            });
        }
    }
});
//# sourceMappingURL=boardData.spec.js.map
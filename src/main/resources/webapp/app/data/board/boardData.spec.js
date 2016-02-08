System.register(["./boardData", "./test-data/test-board-data"], function(exports_1) {
    var boardData_1, test_board_data_1;
    function checkBoardAssignee(assignee, key, name) {
        expect(assignee.key).toEqual(key);
        expect(assignee.avatar).toEqual("/avatars/" + key + ".png");
        expect(assignee.email).toEqual(key + "@example.com");
        expect(assignee.name).toEqual(name);
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
                it('Initial Testing', function () {
                    expect(boardData.view).toEqual(0);
                    var assignees = boardData.assignees;
                    expect(assignees.array.length).toEqual(2);
                    checkBoardAssignee(assignees.array[0], "brian", "Brian Stansberry");
                    checkBoardAssignee(assignees.array[1], "kabir", "Kabir Khan");
                    var priorities = boardData.priorities;
                    expect(priorities.array.length).toEqual(4);
                    checkBoardPriority(priorities.array[0], "highest");
                    checkBoardPriority(priorities.array[1], "high");
                    checkBoardPriority(priorities.array[2], "low");
                    checkBoardPriority(priorities.array[3], "lowest");
                    var issueTypes = boardData.issueTypes;
                    expect(issueTypes.array.length).toEqual(3);
                    checkBoardIssueType(issueTypes.array[0], "task");
                    checkBoardIssueType(issueTypes.array[1], "bug");
                    checkBoardIssueType(issueTypes.array[2], "feature");
                    expect(boardData.owner).toBe("TDP");
                    fail("TODO More");
                });
            });
            //All this can go, it is examples which seem useful for reference
            describe('Test JSON', function () {
                //let fileJson:any;
                //beforeEach(() => {
                //    fileJson = JSON.parse(fs.readFileSync("./boardData.json"));
                //});
                //
                //it('File JSON', () => {
                //    let o:any = JSON.parse(json);
                //    expect(o.file).toEqual(3211);
                //});
                it('Hardcode some JSON', function () {
                    var json = "\n        {\n            \"testing\": 123\n        }";
                    var o = JSON.parse(json);
                    expect(o.testing).toEqual(123);
                });
            });
            describe("A spec using beforeEach and afterEach", function () {
                var foo = 0;
                beforeEach(function () {
                    foo += 1;
                });
                afterEach(function () {
                    foo = 0;
                });
                it("is just a function, so it can contain any code", function () {
                    expect(foo).toEqual(1);
                });
                it("can have more than one expectation", function () {
                    expect(foo).toEqual(1);
                    expect(true).toEqual(true);
                });
            });
            describe("jasmine.any", function () {
                //jasmine.any takes a constructor or “class” name as an expected value. It returns true if the constructor matches the constructor of the actual value.
                it("matches any value", function () {
                    expect({}).toEqual(jasmine.any(Object));
                    expect(12).toEqual(jasmine.any(Number));
                });
                describe("when used with a spy", function () {
                    it("is useful for comparing arguments", function () {
                        var foo = jasmine.createSpy('foo');
                        foo(12, function () {
                            return true;
                        });
                        expect(foo).toHaveBeenCalledWith(jasmine.any(Number), jasmine.any(Function));
                    });
                });
            });
            describe("jasmine.anything", function () {
                //jasmine.anything returns true if the actual value is not null or undefined.
                it("matches anything", function () {
                    expect(1).toEqual(jasmine.anything());
                });
                describe("when used with a spy", function () {
                    it("is useful when the argument can be ignored", function () {
                        var foo = jasmine.createSpy('foo');
                        foo(12, function () {
                            return false;
                        });
                        expect(foo).toHaveBeenCalledWith(12, jasmine.anything());
                    });
                });
            });
        }
    }
});
//# sourceMappingURL=boardData.spec.js.map
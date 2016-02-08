import {BoardData} from "./boardData";
import {TestBoardData} from "./test-data/test-board-data";
import {Assignee} from "./assignee";
import {Priority} from "./priority";
import {Indexed} from "../../common/indexed";
import {IssueType} from "./issueType";

//Tests for the BoardData component which is so central to the display of the board

describe('Load Board Test', () => {
    let boardData:BoardData;
    beforeEach(() => {
        boardData = new BoardData();
        boardData.deserialize(1, JSON.parse(TestBoardData.BASE_BOARD));
    });

    it('Initial Testing', () => {
        expect(boardData.view).toEqual(0);

        let assignees:Indexed<Assignee> = boardData.assignees;
        expect(assignees.array.length).toEqual(2);
        checkBoardAssignee(assignees.array[0], "brian", "Brian Stansberry");
        checkBoardAssignee(assignees.array[1], "kabir", "Kabir Khan");

        let priorities:Indexed<Priority> = boardData.priorities;
        expect(priorities.array.length).toEqual(4);
        checkBoardPriority(priorities.array[0], "highest");
        checkBoardPriority(priorities.array[1], "high");
        checkBoardPriority(priorities.array[2], "low");
        checkBoardPriority(priorities.array[3], "lowest");

        let issueTypes:Indexed<IssueType> = boardData.issueTypes;
        expect(issueTypes.array.length).toEqual(3);
        checkBoardIssueType(issueTypes.array[0], "task");
        checkBoardIssueType(issueTypes.array[1], "bug");
        checkBoardIssueType(issueTypes.array[2], "feature");

        expect(boardData.owner).toBe("TDP");

        fail("TODO More");

    });
});

function checkBoardAssignee(assignee:Assignee, key:string, name:String) {
    expect(assignee.key).toEqual(key);
    expect(assignee.avatar).toEqual("/avatars/" + key + ".png");
    expect(assignee.email).toEqual(key + "@example.com");
    expect(assignee.name).toEqual(name);
}

function checkBoardPriority(priority:Priority, name:string) {
    expect(priority.name).toEqual(name);
    expect(priority.icon).toEqual("/icons/priorities/" + name + ".png");
}

function checkBoardIssueType(type:IssueType, name:string) {
    expect(type.name).toEqual(name);
    expect(type.icon).toEqual("/icons/issue-types/" + name + ".png");
}


//All this can go, it is examples which seem useful for reference

describe('Test JSON', () => {
    //let fileJson:any;
    //beforeEach(() => {
    //    fileJson = JSON.parse(fs.readFileSync("./boardData.json"));
    //});
    //
    //it('File JSON', () => {
    //    let o:any = JSON.parse(json);
    //    expect(o.file).toEqual(3211);
    //});

    it('Hardcode some JSON', () => {
        let json:string = `
        {
            "testing": 123
        }`;
        let o:any = JSON.parse(json);

        expect(o.testing).toEqual(123);
    });

});


describe("A spec using beforeEach and afterEach", () => {
    var foo = 0;

    beforeEach(() => {
        foo += 1;
    });

    afterEach(() => {
        foo = 0;
    });

    it("is just a function, so it can contain any code", () => {
        expect(foo).toEqual(1);
    });

    it("can have more than one expectation", () => {
        expect(foo).toEqual(1);
        expect(true).toEqual(true);
    });
});

describe("jasmine.any", function() {
    //jasmine.any takes a constructor or “class” name as an expected value. It returns true if the constructor matches the constructor of the actual value.
    it("matches any value", function() {
        expect({}).toEqual(jasmine.any(Object));
        expect(12).toEqual(jasmine.any(Number));
    });

    describe("when used with a spy", function() {
        it("is useful for comparing arguments", function() {
            var foo = jasmine.createSpy('foo');
            foo(12, function() {
                return true;
            });

            expect(foo).toHaveBeenCalledWith(jasmine.any(Number), jasmine.any(Function));
        });
    });
});

describe("jasmine.anything", function() {
    //jasmine.anything returns true if the actual value is not null or undefined.
    it("matches anything", function() {
        expect(1).toEqual(jasmine.anything());
    });

    describe("when used with a spy", function() {
        it("is useful when the argument can be ignored", function() {
            var foo = jasmine.createSpy('foo');
            foo(12, function() {
                return false;
            });

            expect(foo).toHaveBeenCalledWith(12, jasmine.anything());
        });
    });
});
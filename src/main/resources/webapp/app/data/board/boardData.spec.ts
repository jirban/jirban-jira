import {BoardData} from "./boardData";
import {TestBoardData} from "./test-data/test-board-data";
import {Assignee} from "./assignee";
import {Priority} from "./priority";
import {Indexed} from "../../common/indexed";
import {IssueType} from "./issueType";
import {IssueTable} from "./issueTable";
import {IssueData} from "./issueData";

//Tests for the BoardData component which is so central to the display of the board

describe('Load Board Test', () => {
    let boardData:BoardData;
    beforeEach(() => {
        boardData = new BoardData();
        boardData.deserialize(1, JSON.parse(TestBoardData.BASE_BOARD));
    });

    it('Simple load', () => {
        expect(boardData.view).toEqual(0);

        checkAssignees(boardData, "brian", "kabir");

        checkStandardPriorities(boardData);
        checkStandardIssueTypes(boardData);

        expect(boardData.swimlane).not.toBeDefined();
        expect(boardData.swimlaneTable).toBeNull();

        checkStandardProjects(boardData);

        expect(boardData.boardStates.length).toBe(4);
        checkBoardLayout(boardData, TestBoardData.EXPECTED_BASE_BOARD);


    });
});

function checkBoardLayout(boardData:BoardData, layout:string[][]) {
    let issueTable:IssueData[][] = boardData.issueTable;
    expect(issueTable.length).toBe(layout.length);
    for (let i:number = 0 ; i < layout.length ; i++) {
        let columnData:IssueData[] = issueTable[i];
        let columnLayout:string[] = layout[i];
        expect(columnData.length).toBe(columnLayout.length);
        for (let j:number = 0 ; j < columnLayout.length ; j++) {
            expect(columnData[j].key).toBe(columnLayout[j]);

            let issueData:IssueData = columnData[j];
            if (issueData.projectCode === "TDP") {
                expect(issueData.statusIndex).toBe(i);
            } else if (issueData.projectCode === "TBG") {
                expect(issueData.statusIndex).toBe(i - 1);
                expect(issueData.statusIndex).toBeLessThan(2);
            }
        }
    }
}



function checkAssignees(boardData:BoardData, ...assignees:string[]) {
    expect(boardData.assignees.array.length).toBe(assignees.length);
    for (let i = 0 ; i < assignees.length ; i++) {
        let name:string;
        switch (assignees[i]) {
            case "brian":
                name = "Brian Stansberry";
                break;
            case "kabir" :
                name = "Kabir Khan";
                break;
            default:
                fail("Unknown name");
        }
        checkBoardAssignee(boardData.assignees.forIndex(i), assignees[i], name);
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
    expect(issueTypes.array.length).toEqual(3);
    checkBoardIssueType(issueTypes.array[0], "task");
    checkBoardIssueType(issueTypes.array[1], "bug");
    checkBoardIssueType(issueTypes.array[2], "feature");
}

function checkBoardAssignee(assignee:Assignee, key:string, name:String) {
    expect(assignee.key).toEqual(key);
    expect(assignee.avatar).toEqual("/avatars/" + key + ".png");
    expect(assignee.email).toEqual(key + "@example.com");
    expect(assignee.name).toEqual(name);
}

function checkStandardProjects(boardData:BoardData) {
    expect(boardData.owner).toBe("TDP");
    expect(boardData.boardProjects).not.toBeNull();
    expect(boardData.boardProjectCodes).toContain("TDP", "TBG");

    expect(boardData.linkedProjects).not.toBeNull();
    expect(boardData.linkedProjects["TUP"]).not.toBeNull();
}

function checkBoardPriority(priority:Priority, name:string) {
    expect(priority.name).toEqual(name);
    expect(priority.icon).toEqual("/icons/priorities/" + name + ".png");
}

function checkBoardIssueType(type:IssueType, name:string) {
    expect(type.name).toEqual(name);
    expect(type.icon).toEqual("/icons/issue-types/" + name + ".png");
}
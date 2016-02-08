export class TestBoardData {
    public static BASE_BOARD:string = `
{
    "view" : 0,
    "assignees" : [
        {
            "key" : "brian",
            "email" : "brian@example.com",
            "avatar" : "/avatars/brian.png",
            "name" : "Brian Stansberry"
        },
        {
            "key" : "kabir",
            "email" : "kabir@example.com",
            "avatar" : "/avatars/kabir.png",
            "name" : "Kabir Khan"
        }
    ],
    "priorities" : [
        {
            "name" : "highest",
            "icon" : "/icons/priorities/highest.png"
        },
        {
            "name" : "high",
            "icon" : "/icons/priorities/high.png"
        },
        {
            "name" : "low",
            "icon" : "/icons/priorities/low.png"
        },
        {
            "name" : "lowest",
            "icon" : "/icons/priorities/lowest.png"
        }
    ],
    "issue-types" : [
        {
            "name" : "task",
            "icon" : "/icons/issue-types/task.png"
        },
        {
            "name" : "bug",
            "icon" : "/icons/issue-types/bug.png"
        },
        {
            "name" : "feature",
            "icon" : "/icons/issue-types/feature.png"
        }
    ],
    "projects" : {
        "owner" : "TDP",
        "main" : {
            "TDP" : {
                "states" : [
                    "TDP-A",
                    "TDP-B",
                    "TDP-C",
                    "TDP-D"
                ],
                "colour" : "#4667CA",
                "issues" : [
                    [
                        "TDP-1",
                        "TDP-5"
                    ],
                    [
                        "TDP-2",
                        "TDP-6"
                    ],
                    [
                        "TDP-3",
                        "TDP-7"
                    ],
                    ["TDP-4"]
                ]
            },
            "TBG" : {
                "states" : [
                    "TBG-X",
                    "TBG-Y"
                ],
                "colour" : "#CA6746",
                "state-links" : {
                    "TDP-A" : null,
                    "TDP-B" : "TBG-X",
                    "TDP-C" : "TBG-Y",
                    "TDP-D" : null
                },
                "issues" : [
                    [],
                    [
                        "TBG-1",
                        "TBG-3"
                    ],
                    [
                        "TBG-2",
                        "TBG-4"
                    ],
                    []
                ]
            }
        },
        "linked" : {"TUP" : {"states" : [
            "TUP-A",
            "TUP-B",
            "TUP-C"
        ]}}
    },
    "issues" : {
        "TBG-2" : {
            "key" : "TBG-2",
            "state" : 1,
            "summary" : "Two",
            "priority" : 1,
            "type" : 1,
            "assignee" : 1
        },
        "TBG-3" : {
            "key" : "TBG-3",
            "state" : 0,
            "summary" : "Three",
            "priority" : 2,
            "type" : 2
        },
        "TBG-1" : {
            "key" : "TBG-1",
            "state" : 0,
            "summary" : "One",
            "priority" : 0,
            "type" : 0,
            "assignee" : 1
        },
        "TDP-7" : {
            "key" : "TDP-7",
            "state" : 2,
            "summary" : "Seven",
            "priority" : 2,
            "type" : 2
        },
        "TDP-6" : {
            "key" : "TDP-6",
            "state" : 1,
            "summary" : "Six",
            "priority" : 1,
            "type" : 1,
            "assignee" : 1
        },
        "TDP-5" : {
            "key" : "TDP-5",
            "state" : 0,
            "summary" : "Five",
            "priority" : 0,
            "type" : 0,
            "assignee" : 1
        },
        "TDP-4" : {
            "key" : "TDP-4",
            "state" : 3,
            "summary" : "Four",
            "priority" : 3,
            "type" : 0,
            "assignee" : 0
        },
        "TDP-3" : {
            "key" : "TDP-3",
            "state" : 2,
            "summary" : "Three",
            "priority" : 2,
            "type" : 0,
            "assignee" : 1
        },
        "TDP-2" : {
            "key" : "TDP-2",
            "state" : 1,
            "summary" : "Two",
            "priority" : 1,
            "type" : 0,
            "assignee" : 1
        },
        "TDP-1" : {
            "key" : "TDP-1",
            "state" : 0,
            "summary" : "One",
            "priority" : 0,
            "type" : 0,
            "assignee" : 1
        },
        "TBG-4" : {
            "key" : "TBG-4",
            "state" : 1,
            "summary" : "Four",
            "priority" : 3,
            "type" : 0,
            "assignee" : 1
        }
    }
}`;

public static EXPECTED_BASE_BOARD:string[][] =
    [
    ["TDP-1", "TDP-5"],
    ["TDP-2", "TDP-6", "TBG-1", "TBG-3"],
    ["TDP-3", "TDP-7", "TBG-2", "TBG-4"],
    ["TDP-4"]];

}
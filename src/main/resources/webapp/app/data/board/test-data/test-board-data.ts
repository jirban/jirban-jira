export class TestBoardData {

    public view:number = 0;
    public assignees:string[] = TestBoardData.STANDARD_ASSIGNEES;
    public priorities:string[] = TestBoardData.STANDARD_PRIORITIES;
    public issueTypes:string[] = TestBoardData.STANDARD_ISSUE_TYPES;
    public projects:string;
    public issues:string;
    public blacklist:string;

    public build() : any {
        let json:any = {};
        json["view"] = this.view;
        json["assignees"] = TestBoardData.parseArray(this.assignees);
        json["priorities"] = TestBoardData.parseArray(this.priorities);
        json["issue-types"] = TestBoardData.parseArray(this.issueTypes);
        json["projects"] = JSON.parse(this.projects);
        json["issues"] = JSON.parse(this.issues);
        if (this.blacklist) {
            json["blacklist"] = JSON.parse(this.blacklist);
        }
        console.log(JSON.stringify(json, null, 2));
        return json;
    }

    public static create(projects:string, issues:string) : any {
        let bd:TestBoardData = new TestBoardData();
        bd.projects = projects;
        bd.issues = issues;
        return bd.build();
    }

    private static parseArray(arr:string[]) : any[] {
        let ret:any[] = [];
        for (let s of arr) {
            ret.push(JSON.parse(s));
        }
        return ret;
    }

    public static STANDARD_ASSIGNEES:string[] = [
        `{
            "key" : "brian",
            "email" : "brian@example.com",
            "avatar" : "/avatars/brian.png",
            "name" : "Brian Stansberry"
        }`,
        `{
            "key" : "kabir",
            "email" : "kabir@example.com",
            "avatar" : "/avatars/kabir.png",
            "name" : "Kabir Khan"
        }`];

    public static STANDARD_PRIORITIES:string[] = [
        `{
            "name" : "highest",
            "icon" : "/icons/priorities/highest.png"
        }`,
        `{
            "name" : "high",
            "icon" : "/icons/priorities/high.png"
        }`,
        `{
            "name" : "low",
            "icon" : "/icons/priorities/low.png"
        }`,
        `{
            "name" : "lowest",
            "icon" : "/icons/priorities/lowest.png"
        }`];

    public static STANDARD_ISSUE_TYPES:string[] = [
        `{
            "name" : "task",
            "icon" : "/icons/issue-types/task.png"
        }`,
        `{
            "name" : "bug",
            "icon" : "/icons/issue-types/bug.png"
        }`,
        `{
            "name" : "feature",
            "icon" : "/icons/issue-types/feature.png"
        }`,
        `{
            "name" : "issue",
            "icon" : "/icons/issue-types/issue.png"
        }`];

    public static STANDARD_BLACKLIST:string = `
        {
            "states": [
              "BadState"
            ],
            "priorities": [
              "BadPriority"
            ],
            "issue-types": [
              "BadIssueType"
            ],
            "issues": [
              "TDP-100",
              "TBG-101"
            ]
        }`;

    public static EXPECTED_BASE_BOARD:string[][] =
    [
        ["TDP-1", "TDP-5"],
        ["TDP-2", "TDP-6", "TBG-1", "TBG-3"],
        ["TDP-3", "TDP-7", "TBG-2", "TBG-4"],
        ["TDP-4"]
    ];

    public static FULL_BASE_BOARD_PROJECTS:string = `
    {
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
    }`;

    public static FULL_BASE_BOARD_ISSUES:string =`
    {
        "TDP-1" : {
            "key" : "TDP-1",
            "state" : 0,
            "summary" : "One",
            "priority" : 0,
            "type" : 0,
            "assignee" : 0
        },
        "TDP-2" : {
            "key" : "TDP-2",
            "state" : 1,
            "summary" : "Two",
            "priority" : 1,
            "type" : 1,
            "assignee" : 1
        },
        "TDP-3" : {
            "key" : "TDP-3",
            "state" : 2,
            "summary" : "Three",
            "priority" : 2,
            "type" : 2
        },
        "TDP-4" : {
            "key" : "TDP-4",
            "state" : 3,
            "summary" : "Four",
            "priority" : 3,
            "type" : 3
        },
        "TDP-5" : {
            "key" : "TDP-5",
            "state" : 0,
            "summary" : "Five",
            "priority" : 0,
            "type" : 0,
            "assignee" : 0
        },
        "TDP-6" : {
            "key" : "TDP-6",
            "state" : 1,
            "summary" : "Six",
            "priority" : 1,
            "type" : 1,
            "assignee" : 1
        },
        "TDP-7" : {
            "key" : "TDP-7",
            "state" : 2,
            "summary" : "Seven",
            "priority" : 2,
            "type" : 2
        },
        "TBG-1" : {
            "key" : "TBG-1",
            "state" : 0,
            "summary" : "One",
            "priority" : 0,
            "type" : 0,
            "assignee" : 0
        },
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
        "TBG-4" : {
            "key" : "TBG-4",
            "state" : 1,
            "summary" : "Four",
            "priority" : 3,
            "type" : 3
        }
    }`;

}




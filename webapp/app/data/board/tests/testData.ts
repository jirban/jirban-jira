export class TestBoardData {

    public view:number = 0;
    public states:any[] = [{name:"S-1"}, {name:"S-2"}, {name:"S-3"}, {name:"S-4"}];
    public assignees:any[] = TestBoardData.STANDARD_ASSIGNEES;
    public components:string[] = TestBoardData.STANDARD_COMPONENTS;
    public labels:string[] = TestBoardData.STANDARD_LABELS;
    public fixVersions:string[] = TestBoardData.STANDARD_FIX_VERSIONS;
    public priorities:any[] = TestBoardData.STANDARD_PRIORITIES;
    public issueTypes:any[] = TestBoardData.STANDARD_ISSUE_TYPES;
    public projects:any;
    public issues:any;
    public blacklist:any;
    public custom:any;

    public build() : any {
        let json:any = {};
        json["view"] = this.view;
        //We use a clone of the objects. Otherwise tests that share a setup and modify the issue tables will pollute subsequent tests
        json["states"] = TestBoardData.clone(this.states);
        if (this.assignees) {
            json["assignees"] = TestBoardData.clone(this.assignees);
        } else {
            json["assignees"] = [];
        }
        if (this.components && this.components.length > 0) {
            json["components"] = TestBoardData.clone(this.components);
        }
        if (this.labels && this.labels.length > 0) {
            json["labels"] = TestBoardData.clone(this.labels);
        }
        if (this.fixVersions && this.fixVersions.length > 0) {
            json["fix-versions"] = TestBoardData.clone(this.fixVersions);
        }
        if (this.priorities && this.priorities.length > 0) {
            json["priorities"] = TestBoardData.clone(this.priorities);
        } else {
            json["priorities"] = [];
        }
        if (this.issueTypes) {
            json["issue-types"] = TestBoardData.clone(this.issueTypes);
        } else {
            json["issue-types"] = [];
        }
        if (!this.projects) {
            throw new Error("No projects were set up");
        }
        json["projects"] = TestBoardData.clone(this.projects);
        if (this.issues) {
            json["issues"] = TestBoardData.clone(this.issues);
        } else {
            json["issues"] = {};
        }
        if (this.blacklist) {
            json["blacklist"] = TestBoardData.clone(this.blacklist);
        }
        if (this.custom) {
            json["custom"] = TestBoardData.clone(this.custom);
        }
        //console.log(JSON.stringify(json, null, 2));
        return json;
    }

    public static create(projects:any, issues:any, blacklist?:any) : any {
        let bd:TestBoardData = new TestBoardData();
        bd.projects = projects;
        bd.issues = issues;
        if (blacklist) {
            bd.blacklist = blacklist;
        }
        return bd.build();
    }

    static clone(input:any):any{
        return JSON.parse(JSON.stringify(input));
    }

    public static readonly STANDARD_ASSIGNEES:any[] = [
        {
            key : "brian",
            email : "brian@example.com",
            avatar : "/avatars/brian.png",
            name : "Brian Stansberry"
        },
        {
            key : "kabir",
            email : "kabir@example.com",
            avatar : "/avatars/kabir.png",
            name : "Kabir Khan"
        }];

    public static readonly STANDARD_COMPONENTS: string[] = ["C1", "C5"];

    public static readonly STANDARD_LABELS: any = ["L1", "L5"];

    public static readonly STANDARD_FIX_VERSIONS: any = ["F1", "F5"];

    public static readonly STANDARD_PRIORITIES:any[] = [
        {
            name : "highest",
            icon : "/icons/priorities/highest.png"
        },
        {
            name : "high",
            icon : "/icons/priorities/high.png"
        },
        {
            name : "low",
            icon : "/icons/priorities/low.png"
        },
        {
            name : "lowest",
            icon : "/icons/priorities/lowest.png"
        }];

    public static readonly STANDARD_ISSUE_TYPES:any[] = [
        {
            name : "task",
            icon : "/icons/issue-types/task.png"
        },
        {
            name : "bug",
            icon : "/icons/issue-types/bug.png"
        },
        {
            name : "feature",
            icon : "/icons/issue-types/feature.png"
        },
        {
            name : "issue",
            icon : "/icons/issue-types/issue.png"
        }];

    public static readonly STANDARD_BLACKLIST:any =
        {
            states: [
              "BadState"
            ],
            priorities: [
              "BadPriority"
            ],
            "issue-types": [
              "BadIssueType"
            ],
            issues: [
              "TDP-100",
              "TBG-101"
            ]
        };

    public static readonly STANDARD_CUSTOM_FIELDS:any =
        {
            Tester: [
                {
                    key: "james",
                    value: "James Perkins"
                },
                {
                    key: "kabir",
                    value: "Kabir Khan"
                }
            ],
            Documenter: [
                {
                    key: "kabir",
                    value: "Kabir Khan"
                },
                {
                    key: "stuart",
                    value: "Stuart Douglas"
                }
            ]
        }


    // 'Full' board ////////////

    public static readonly EXPECTED_FULL_BOARD:string[][] =
    [
        ["TDP-1", "TDP-5"],
        ["TDP-2", "TDP-6", "TBG-1", "TBG-3"],
        ["TDP-3", "TDP-7", "TBG-2", "TBG-4"],
        ["TDP-4"]
    ];

    public static readonly FULL_BOARD_PROJECTS:any =
    {
        owner : "TDP",
        main : {
            TDP : {
                "state-links" : {
                    "S-1": "TDP-A",
                    "S-2": "TDP-B",
                    "S-3": "TDP-C",
                    "S-4": "TDP-D"
                },
                colour : "#4667CA",
                ranked : ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7"]
            },
            TBG : {
                states : [
                    "TBG-X",
                    "TBG-Y"
                ],
                colour : "#CA6746",
                "state-links" : {
                    "S-2" : "TBG-X",
                    "S-3" : "TBG-Y",
                },
                ranked: ["TBG-1", "TBG-2", "TBG-3", "TBG-4"]
            }
        },
        linked : {TUP : {states : [
            "TUP-A",
            "TUP-B",
            "TUP-C"
        ]}}
    };

    public static readonly FULL_BOARD_ISSUES:any =
    {
        "TDP-1" : {
            key : "TDP-1",
            state : 0,
            summary : "One",
            priority : 0,
            type : 0,
            assignee : 0,
            components: [0]
        },
        "TDP-2" : {
            key : "TDP-2",
            state : 1,
            summary : "Two",
            priority : 1,
            type : 1,
            assignee : 1,
            components: [1]
        },
        "TDP-3" : {
            key : "TDP-3",
            state : 2,
            summary : "Three",
            priority : 2,
            type : 2,
            labels : [0],
            "fix-versions" : [1]
        },
        "TDP-4" : {
            key : "TDP-4",
            state : 3,
            summary : "Four",
            priority : 3,
            type : 3,
            labels : [1],
            "fix-versions" : [0]
        },
        "TDP-5" : {
            key : "TDP-5",
            state : 0,
            summary : "Five",
            priority : 0,
            type : 0,
            assignee : 0,
            components: [0]
        },
        "TDP-6" : {
            key : "TDP-6",
            state : 1,
            summary : "Six",
            priority : 1,
            type : 1,
            assignee : 1,
            components: [1]
        },
        "TDP-7" : {
            key : "TDP-7",
            state : 2,
            summary : "Seven",
            priority : 2,
            type : 2,
            labels : [0],
            "fix-versions" : [1]
        },
        "TBG-1" : {
            key : "TBG-1",
            state : 0,
            summary : "One",
            priority : 0,
            type : 0,
            assignee : 0,
            components: [0]
        },
        "TBG-2" : {
            key : "TBG-2",
            state : 1,
            summary : "Two",
            priority : 1,
            type : 1,
            assignee : 1,
            components: [1]
        },
        "TBG-3" : {
            key : "TBG-3",
            state : 0,
            summary : "Three",
            priority : 2,
            type : 2,
            labels : [0],
            "fix-versions" : [1]
        },
        "TBG-4" : {
            key : "TBG-4",
            state : 1,
            summary : "Four",
            priority : 3,
            type : 3,
            labels : [1],
            "fix-versions" : [0]
        }
    };

    // 'Owner' only board /////////
    public static readonly EXPECTED_OWNER_ONLY_BOARD:string[][] =
        [
            ["TDP-1", "TDP-5"],
            ["TDP-2", "TDP-6"],
            ["TDP-3", "TDP-7"],
            ["TDP-4"]
        ];

    public static readonly OWNER_ONLY_BOARD_PROJECTS:any =
    {
        owner : "TDP",
        main : {
            TDP : {
                "state-links" : {
                    "S-1": "TDP-A",
                    "S-2": "TDP-B",
                    "S-3": "TDP-C",
                    "S-4": "TDP-D"
                },
                colour : "#4667CA",
                ranked : ["TDP-1", "TDP-2", "TDP-3", "TDP-4", "TDP-5", "TDP-6", "TDP-7"]
            }
        }
    };

    public static readonly OWNER_ONLY_BOARD_ISSUES:any =
    {
        "TDP-1" : {
            key : "TDP-1",
            state : 0,
            summary : "One",
            priority : 0,
            type : 0,
            assignee : 0,
            components: [0]
        },
        "TDP-2" : {
            key : "TDP-2",
            state : 1,
            summary : "Two",
            priority : 1,
            type : 1,
            assignee : 1,
            components: [1]
        },
        "TDP-3" : {
            key : "TDP-3",
            state : 2,
            summary : "Three",
            priority : 2,
            type : 2,
            labels : [0],
            "fix-versions" : [1]
        },
        "TDP-4" : {
            key : "TDP-4",
            state : 3,
            summary : "Four",
            priority : 3,
            type : 3,
            labels : [1],
            "fix-versions" : [0]
        },
        "TDP-5" : {
            key : "TDP-5",
            state : 0,
            summary : "Five",
            priority : 0,
            type : 0,
            assignee : 0,
            components: [0]
        },
        "TDP-6" : {
            key : "TDP-6",
            state : 1,
            summary : "Six",
            priority : 1,
            type : 1,
            assignee : 1,
            components: [1]
        },
        "TDP-7" : {
            key : "TDP-7",
            state : 2,
            summary : "Seven",
            priority : 2,
            type : 2,
            labels : [0],
            "fix-versions" : [1]
        }
    };

    // 'Non-Owner' only board ///////////
    public static readonly EXPECTED_NON_OWNER_ONLY_BOARD:string[][] =
        [
            [],
            ["TBG-1", "TBG-3"],
            ["TBG-2", "TBG-4"],
            []
        ];

    public static readonly NON_OWNER_ONLY_BOARD_PROJECTS:any =
    {
        owner : "TDP",
        main : {
            TDP : {
                "state-links" : {
                    "S-1": "TDP-A",
                    "S-2": "TDP-B",
                    "S-3": "TDP-C",
                    "S-4": "TDP-D"
                },
                colour : "#4667CA",
                issues : [
                    [],
                    [],
                    [],
                    []
                ]
            },
            TBG : {
                states : [
                    "TBG-X",
                    "TBG-Y"
                ],
                colour : "#CA6746",
                "state-links" : {
                    "S-2" : "TBG-X",
                    "S-3" : "TBG-Y",
                },
                ranked : ["TBG-1", "TBG-2", "TBG-3", "TBG-4"]
            }
        }
    };

    public static readonly NON_OWNER_ONLY_BOARD_ISSUES:any =
    {
        "TBG-1" : {
            key : "TBG-1",
            state : 0,
            summary : "One",
            priority : 0,
            type : 0,
            assignee : 0,
            components: [0]
        },
        "TBG-2" : {
            key : "TBG-2",
            state : 1,
            summary : "Two",
            priority : 1,
            type : 1,
            assignee : 1,
            components: [1]
        },
        "TBG-3" : {
            key : "TBG-3",
            state : 0,
            summary : "Three",
            priority : 2,
            type : 2,
            labels : [0],
            "fix-versions" : [1]
        },
        "TBG-4" : {
            key : "TBG-4",
            state : 1,
            summary : "Four",
            priority : 3,
            type : 3,
            labels : [1],
            "fix-versions" : [0]
        }
    };

    // Less data in boards for change tests //////////

    public static readonly PRE_CHANGE_BOARD_PROJECTS:any =
    {
        owner : "TDP",
        main : {
            TDP : {
                "state-links" : {
                    "S-1": "TDP-A",
                    "S-2": "TDP-B",
                    "S-3": "TDP-C",
                    "S-4": "TDP-D"
                },
                colour : "#4667CA",
                ranked : ["TDP-1", "TDP-2", "TDP-3", "TDP-4"],
            },
            TBG : {
                colour : "#CA6746",
                "state-links" : {
                    "S-2" : "TBG-X",
                    "S-3" : "TBG-Y"
                },
                ranked : ["TBG-1"]
            }
        }
    };

    public static readonly PRE_CHANGE_BOARD_ISSUES:any =
        {
            "TDP-1" : {
                key : "TDP-1",
                state : 0,
                summary : "One",
                priority : 0,
                type : 0,
                assignee : 0,
                components: [0]
            },
            "TDP-2" : {
                key : "TDP-2",
                state : 1,
                summary : "Two",
                priority : 1,
                type : 1,
                assignee : 1,
                components: [1]
            },
            "TDP-3" : {
                key : "TDP-3",
                state : 2,
                summary : "Three",
                priority : 2,
                type : 2,
                labels : [0],
                "fix-versions" : [1]
            },
            "TDP-4" : {
                key : "TDP-4",
                state : 3,
                summary : "Four",
                priority : 3,
                type : 3,
                labels : [1],
                "fix-versions" : [0]
            },
            "TBG-1" : {
                key : "TBG-1",
                state : 0,
                summary : "One",
                priority : 0,
                type : 0,
                assignee : 0,
                components: [0]
            }
        };

    // Less data in boards for change tests //////////

    public static readonly PRE_RERANK_BOARD_PROJECTS:any = TestBoardData.PRE_CHANGE_BOARD_PROJECTS;

    public static readonly PRE_RERANK_BOARD_ISSUES:any = TestBoardData.PRE_CHANGE_BOARD_ISSUES;

    static getFullBoardCustomFieldIssues():any {
        let data:any = TestBoardData.clone(TestBoardData.FULL_BOARD_ISSUES);

        TestBoardData.addCustomFields(data, "TDP-1", "Tester", 0);
        TestBoardData.addCustomFields(data, "TDP-1", "Documenter", 0);
        TestBoardData.addCustomFields(data, "TDP-2", "Tester", 1);
        TestBoardData.addCustomFields(data, "TDP-2", "Documenter", 1);
        TestBoardData.addCustomFields(data, "TDP-5", "Tester", 0);
        TestBoardData.addCustomFields(data, "TDP-5", "Documenter", 0);
        TestBoardData.addCustomFields(data, "TDP-6", "Tester", 1);
        TestBoardData.addCustomFields(data, "TDP-6", "Documenter", 1);

        TestBoardData.addCustomFields(data, "TBG-1", "Tester", 0);
        TestBoardData.addCustomFields(data, "TBG-1", "Documenter", 0);
        TestBoardData.addCustomFields(data, "TBG-2", "Tester", 1);
        TestBoardData.addCustomFields(data, "TBG-2", "Documenter", 1);

        return data;
    }

    static getPreChangeCustomFieldIssues():any {
        let data:any = TestBoardData.clone(TestBoardData.PRE_CHANGE_BOARD_ISSUES);

        TestBoardData.addCustomFields(data, "TDP-1", "Tester", 0);
        TestBoardData.addCustomFields(data, "TDP-1", "Documenter", 0);
        TestBoardData.addCustomFields(data, "TDP-2", "Tester", 1);
        TestBoardData.addCustomFields(data, "TDP-2", "Documenter", 1);

        TestBoardData.addCustomFields(data, "TBG-1", "Tester", 0);
        TestBoardData.addCustomFields(data, "TBG-1", "Documenter", 0);

        
        return data;
    }

    static addCustomFields(issues:any, issue:string, name:string, index:number) {
        if (!issues[issue]["custom"]) {
            issues[issue]["custom"] = {};
        }
        issues[issue]["custom"][name] = index;
    }

    static getPreChangeParallelTasksProjects():any {
        let data:any = TestBoardData.clone(TestBoardData.PRE_CHANGE_BOARD_PROJECTS);

        data["main"]["TDP"]["parallel-tasks"] = [{
            name: "Upstream",
            display: "US",
            options: ["us-TODO", "us-In Progress", "us-Review", "us-Done"]
        }, {
            name: "Downstream",
            display: "DS",
            options: ["ds-TODO", "ds-In Progress", "ds-Review", "ds-Done"]
        }];

        return data;
    }

    static getPrechangeParallelTaskIssues():any {
        let data:any = TestBoardData.clone(TestBoardData.PRE_CHANGE_BOARD_ISSUES);

        data["TDP-1"]["parallel-tasks"] = [0, 0];
        data["TDP-2"]["parallel-tasks"] = [1, 1];

        return data;
    }


    static getFullBoardParallelTaskProjects():any{
        let data:any = TestBoardData.clone(TestBoardData.FULL_BOARD_PROJECTS);

        data["main"]["TDP"]["parallel-tasks"] = [{
            name: "Upstream",
            display: "US",
            options: ["us-TODO", "us-In Progress", "us-Review", "us-Done"]
        }, {
            name: "Downstream",
            display: "DS",
            options: ["ds-TODO", "ds-In Progress", "ds-Review", "ds-Done"]
        }];

        return data;
    }

    static getFullBoardParallelTaskIssues():any {
        let data:any = TestBoardData.clone(TestBoardData.FULL_BOARD_ISSUES);

        data["TDP-1"]["parallel-tasks"] = [0, 1];
        data["TDP-2"]["parallel-tasks"] = [1, 2];
        data["TDP-3"]["parallel-tasks"] = [2, 3];
        data["TDP-4"]["parallel-tasks"] = [3, 0];
        data["TDP-5"]["parallel-tasks"] = [0, 1];
        data["TDP-6"]["parallel-tasks"] = [1, 2];
        data["TDP-7"]["parallel-tasks"] = [2, 3];

        return data;
    }
}




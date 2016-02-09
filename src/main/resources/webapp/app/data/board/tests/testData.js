System.register([], function(exports_1) {
    var TestBoardData;
    return {
        setters:[],
        execute: function() {
            TestBoardData = (function () {
                function TestBoardData() {
                    this.view = 0;
                    this.assignees = TestBoardData.STANDARD_ASSIGNEES;
                    this.priorities = TestBoardData.STANDARD_PRIORITIES;
                    this.issueTypes = TestBoardData.STANDARD_ISSUE_TYPES;
                }
                TestBoardData.prototype.build = function () {
                    var json = {};
                    json["view"] = this.view;
                    json["assignees"] = TestBoardData.parseArray(this.assignees);
                    json["priorities"] = TestBoardData.parseArray(this.priorities);
                    json["issue-types"] = TestBoardData.parseArray(this.issueTypes);
                    json["projects"] = JSON.parse(this.projects);
                    json["issues"] = JSON.parse(this.issues);
                    if (this.blacklist) {
                        json["blacklist"] = JSON.parse(this.blacklist);
                    }
                    //console.log(JSON.stringify(json, null, 2));
                    return json;
                };
                TestBoardData.create = function (projects, issues) {
                    var bd = new TestBoardData();
                    bd.projects = projects;
                    bd.issues = issues;
                    return bd.build();
                };
                TestBoardData.parseArray = function (arr) {
                    var ret = [];
                    for (var _i = 0; _i < arr.length; _i++) {
                        var s = arr[_i];
                        ret.push(JSON.parse(s));
                    }
                    return ret;
                };
                TestBoardData.STANDARD_ASSIGNEES = [
                    "{\n            \"key\" : \"brian\",\n            \"email\" : \"brian@example.com\",\n            \"avatar\" : \"/avatars/brian.png\",\n            \"name\" : \"Brian Stansberry\"\n        }",
                    "{\n            \"key\" : \"kabir\",\n            \"email\" : \"kabir@example.com\",\n            \"avatar\" : \"/avatars/kabir.png\",\n            \"name\" : \"Kabir Khan\"\n        }"];
                TestBoardData.STANDARD_PRIORITIES = [
                    "{\n            \"name\" : \"highest\",\n            \"icon\" : \"/icons/priorities/highest.png\"\n        }",
                    "{\n            \"name\" : \"high\",\n            \"icon\" : \"/icons/priorities/high.png\"\n        }",
                    "{\n            \"name\" : \"low\",\n            \"icon\" : \"/icons/priorities/low.png\"\n        }",
                    "{\n            \"name\" : \"lowest\",\n            \"icon\" : \"/icons/priorities/lowest.png\"\n        }"];
                TestBoardData.STANDARD_ISSUE_TYPES = [
                    "{\n            \"name\" : \"task\",\n            \"icon\" : \"/icons/issue-types/task.png\"\n        }",
                    "{\n            \"name\" : \"bug\",\n            \"icon\" : \"/icons/issue-types/bug.png\"\n        }",
                    "{\n            \"name\" : \"feature\",\n            \"icon\" : \"/icons/issue-types/feature.png\"\n        }",
                    "{\n            \"name\" : \"issue\",\n            \"icon\" : \"/icons/issue-types/issue.png\"\n        }"];
                TestBoardData.STANDARD_BLACKLIST = "\n        {\n            \"states\": [\n              \"BadState\"\n            ],\n            \"priorities\": [\n              \"BadPriority\"\n            ],\n            \"issue-types\": [\n              \"BadIssueType\"\n            ],\n            \"issues\": [\n              \"TDP-100\",\n              \"TBG-101\"\n            ]\n        }";
                // 'Full' board ////////////
                TestBoardData.EXPECTED_FULL_BOARD = [
                    ["TDP-1", "TDP-5"],
                    ["TDP-2", "TDP-6", "TBG-1", "TBG-3"],
                    ["TDP-3", "TDP-7", "TBG-2", "TBG-4"],
                    ["TDP-4"]
                ];
                TestBoardData.FULL_BOARD_PROJECTS = "\n    {\n        \"owner\" : \"TDP\",\n        \"main\" : {\n            \"TDP\" : {\n                \"states\" : [\n                    \"TDP-A\",\n                    \"TDP-B\",\n                    \"TDP-C\",\n                    \"TDP-D\"\n                ],\n                \"colour\" : \"#4667CA\",\n                \"issues\" : [\n                    [\n                        \"TDP-1\",\n                        \"TDP-5\"\n                    ],\n                    [\n                        \"TDP-2\",\n                        \"TDP-6\"\n                    ],\n                    [\n                        \"TDP-3\",\n                        \"TDP-7\"\n                    ],\n                    [\"TDP-4\"]\n                ]\n            },\n            \"TBG\" : {\n                \"states\" : [\n                    \"TBG-X\",\n                    \"TBG-Y\"\n                ],\n                \"colour\" : \"#CA6746\",\n                \"state-links\" : {\n                    \"TDP-A\" : null,\n                    \"TDP-B\" : \"TBG-X\",\n                    \"TDP-C\" : \"TBG-Y\",\n                    \"TDP-D\" : null\n                },\n                \"issues\" : [\n                    [],\n                    [\n                        \"TBG-1\",\n                        \"TBG-3\"\n                    ],\n                    [\n                        \"TBG-2\",\n                        \"TBG-4\"\n                    ],\n                    []\n                ]\n            }\n        },\n        \"linked\" : {\"TUP\" : {\"states\" : [\n            \"TUP-A\",\n            \"TUP-B\",\n            \"TUP-C\"\n        ]}}\n    }";
                TestBoardData.FULL_BOARD_ISSUES = "\n    {\n        \"TDP-1\" : {\n            \"key\" : \"TDP-1\",\n            \"state\" : 0,\n            \"summary\" : \"One\",\n            \"priority\" : 0,\n            \"type\" : 0,\n            \"assignee\" : 0\n        },\n        \"TDP-2\" : {\n            \"key\" : \"TDP-2\",\n            \"state\" : 1,\n            \"summary\" : \"Two\",\n            \"priority\" : 1,\n            \"type\" : 1,\n            \"assignee\" : 1\n        },\n        \"TDP-3\" : {\n            \"key\" : \"TDP-3\",\n            \"state\" : 2,\n            \"summary\" : \"Three\",\n            \"priority\" : 2,\n            \"type\" : 2\n        },\n        \"TDP-4\" : {\n            \"key\" : \"TDP-4\",\n            \"state\" : 3,\n            \"summary\" : \"Four\",\n            \"priority\" : 3,\n            \"type\" : 3\n        },\n        \"TDP-5\" : {\n            \"key\" : \"TDP-5\",\n            \"state\" : 0,\n            \"summary\" : \"Five\",\n            \"priority\" : 0,\n            \"type\" : 0,\n            \"assignee\" : 0\n        },\n        \"TDP-6\" : {\n            \"key\" : \"TDP-6\",\n            \"state\" : 1,\n            \"summary\" : \"Six\",\n            \"priority\" : 1,\n            \"type\" : 1,\n            \"assignee\" : 1\n        },\n        \"TDP-7\" : {\n            \"key\" : \"TDP-7\",\n            \"state\" : 2,\n            \"summary\" : \"Seven\",\n            \"priority\" : 2,\n            \"type\" : 2\n        },\n        \"TBG-1\" : {\n            \"key\" : \"TBG-1\",\n            \"state\" : 0,\n            \"summary\" : \"One\",\n            \"priority\" : 0,\n            \"type\" : 0,\n            \"assignee\" : 0\n        },\n        \"TBG-2\" : {\n            \"key\" : \"TBG-2\",\n            \"state\" : 1,\n            \"summary\" : \"Two\",\n            \"priority\" : 1,\n            \"type\" : 1,\n            \"assignee\" : 1\n        },\n        \"TBG-3\" : {\n            \"key\" : \"TBG-3\",\n            \"state\" : 0,\n            \"summary\" : \"Three\",\n            \"priority\" : 2,\n            \"type\" : 2\n        },\n        \"TBG-4\" : {\n            \"key\" : \"TBG-4\",\n            \"state\" : 1,\n            \"summary\" : \"Four\",\n            \"priority\" : 3,\n            \"type\" : 3\n        }\n    }";
                // 'Owner' only board /////////
                TestBoardData.EXPECTED_OWNER_ONLY_BOARD = [
                    ["TDP-1", "TDP-5"],
                    ["TDP-2", "TDP-6"],
                    ["TDP-3", "TDP-7"],
                    ["TDP-4"]
                ];
                TestBoardData.OWNER_ONLY_BOARD_PROJECTS = "\n    {\n        \"owner\" : \"TDP\",\n        \"main\" : {\n            \"TDP\" : {\n                \"states\" : [\n                    \"TDP-A\",\n                    \"TDP-B\",\n                    \"TDP-C\",\n                    \"TDP-D\"\n                ],\n                \"colour\" : \"#4667CA\",\n                \"issues\" : [\n                    [\n                        \"TDP-1\",\n                        \"TDP-5\"\n                    ],\n                    [\n                        \"TDP-2\",\n                        \"TDP-6\"\n                    ],\n                    [\n                        \"TDP-3\",\n                        \"TDP-7\"\n                    ],\n                    [\"TDP-4\"]\n                ]\n            }\n        }\n    }";
                TestBoardData.OWNER_ONLY_BOARD_ISSUES = "\n    {\n        \"TDP-1\" : {\n            \"key\" : \"TDP-1\",\n            \"state\" : 0,\n            \"summary\" : \"One\",\n            \"priority\" : 0,\n            \"type\" : 0,\n            \"assignee\" : 0\n        },\n        \"TDP-2\" : {\n            \"key\" : \"TDP-2\",\n            \"state\" : 1,\n            \"summary\" : \"Two\",\n            \"priority\" : 1,\n            \"type\" : 1,\n            \"assignee\" : 1\n        },\n        \"TDP-3\" : {\n            \"key\" : \"TDP-3\",\n            \"state\" : 2,\n            \"summary\" : \"Three\",\n            \"priority\" : 2,\n            \"type\" : 2\n        },\n        \"TDP-4\" : {\n            \"key\" : \"TDP-4\",\n            \"state\" : 3,\n            \"summary\" : \"Four\",\n            \"priority\" : 3,\n            \"type\" : 3\n        },\n        \"TDP-5\" : {\n            \"key\" : \"TDP-5\",\n            \"state\" : 0,\n            \"summary\" : \"Five\",\n            \"priority\" : 0,\n            \"type\" : 0,\n            \"assignee\" : 0\n        },\n        \"TDP-6\" : {\n            \"key\" : \"TDP-6\",\n            \"state\" : 1,\n            \"summary\" : \"Six\",\n            \"priority\" : 1,\n            \"type\" : 1,\n            \"assignee\" : 1\n        },\n        \"TDP-7\" : {\n            \"key\" : \"TDP-7\",\n            \"state\" : 2,\n            \"summary\" : \"Seven\",\n            \"priority\" : 2,\n            \"type\" : 2\n        }\n    }";
                // 'Non-Owner' only board ///////////
                TestBoardData.EXPECTED_NON_OWNER_ONLY_BOARD = [
                    [],
                    ["TBG-1", "TBG-3"],
                    ["TBG-2", "TBG-4"],
                    []
                ];
                TestBoardData.NON_OWNER_ONLY_BOARD_PROJECTS = "\n    {\n        \"owner\" : \"TDP\",\n        \"main\" : {\n            \"TDP\" : {\n                \"states\" : [\n                    \"TDP-A\",\n                    \"TDP-B\",\n                    \"TDP-C\",\n                    \"TDP-D\"\n                ],\n                \"colour\" : \"#4667CA\",\n                \"issues\" : [\n                    [],\n                    [],\n                    [],\n                    []\n                ]\n            },\n            \"TBG\" : {\n                \"states\" : [\n                    \"TBG-X\",\n                    \"TBG-Y\"\n                ],\n                \"colour\" : \"#CA6746\",\n                \"state-links\" : {\n                    \"TDP-A\" : null,\n                    \"TDP-B\" : \"TBG-X\",\n                    \"TDP-C\" : \"TBG-Y\",\n                    \"TDP-D\" : null\n                },\n                \"issues\" : [\n                    [],\n                    [\n                        \"TBG-1\",\n                        \"TBG-3\"\n                    ],\n                    [\n                        \"TBG-2\",\n                        \"TBG-4\"\n                    ],\n                    []\n                ]\n            }\n        }\n    }";
                TestBoardData.NON_OWNER_ONLY_BOARD_ISSUES = "\n    {\n        \"TDP-1\" : {\n            \"key\" : \"TDP-1\",\n            \"state\" : 0,\n            \"summary\" : \"One\",\n            \"priority\" : 0,\n            \"type\" : 0,\n            \"assignee\" : 0\n        },\n        \"TDP-2\" : {\n            \"key\" : \"TDP-2\",\n            \"state\" : 1,\n            \"summary\" : \"Two\",\n            \"priority\" : 1,\n            \"type\" : 1,\n            \"assignee\" : 1\n        },\n        \"TDP-3\" : {\n            \"key\" : \"TDP-3\",\n            \"state\" : 2,\n            \"summary\" : \"Three\",\n            \"priority\" : 2,\n            \"type\" : 2\n        },\n        \"TDP-4\" : {\n            \"key\" : \"TDP-4\",\n            \"state\" : 3,\n            \"summary\" : \"Four\",\n            \"priority\" : 3,\n            \"type\" : 3\n        },\n        \"TDP-5\" : {\n            \"key\" : \"TDP-5\",\n            \"state\" : 0,\n            \"summary\" : \"Five\",\n            \"priority\" : 0,\n            \"type\" : 0,\n            \"assignee\" : 0\n        },\n        \"TDP-6\" : {\n            \"key\" : \"TDP-6\",\n            \"state\" : 1,\n            \"summary\" : \"Six\",\n            \"priority\" : 1,\n            \"type\" : 1,\n            \"assignee\" : 1\n        },\n        \"TDP-7\" : {\n            \"key\" : \"TDP-7\",\n            \"state\" : 2,\n            \"summary\" : \"Seven\",\n            \"priority\" : 2,\n            \"type\" : 2\n        },\n        \"TBG-1\" : {\n            \"key\" : \"TBG-1\",\n            \"state\" : 0,\n            \"summary\" : \"One\",\n            \"priority\" : 0,\n            \"type\" : 0,\n            \"assignee\" : 0\n        },\n        \"TBG-2\" : {\n            \"key\" : \"TBG-2\",\n            \"state\" : 1,\n            \"summary\" : \"Two\",\n            \"priority\" : 1,\n            \"type\" : 1,\n            \"assignee\" : 1\n        },\n        \"TBG-3\" : {\n            \"key\" : \"TBG-3\",\n            \"state\" : 0,\n            \"summary\" : \"Three\",\n            \"priority\" : 2,\n            \"type\" : 2\n        },\n        \"TBG-4\" : {\n            \"key\" : \"TBG-4\",\n            \"state\" : 1,\n            \"summary\" : \"Four\",\n            \"priority\" : 3,\n            \"type\" : 3\n        }\n    }";
                return TestBoardData;
            })();
            exports_1("TestBoardData", TestBoardData);
        }
    }
});
//# sourceMappingURL=testData.js.map
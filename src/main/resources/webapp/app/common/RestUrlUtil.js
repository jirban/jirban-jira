System.register([], function(exports_1) {
    var RestUrlUtil;
    return {
        setters:[],
        execute: function() {
            /**
             * Util to calculate the rest urls, depending on if we are running in dev mode (i.e. not deployed as a servlet),
             * or deployed within Jira
             */
            RestUrlUtil = (function () {
                function RestUrlUtil() {
                }
                RestUrlUtil.caclulateRestUrl = function (path) {
                    var location = window.location;
                    var index = location.href.indexOf("/download/resources/");
                    if (index > 0) {
                        var url = location.href.substr(0, index);
                        url = url + "/plugins/servlet/jirban/" + path;
                        return url;
                    }
                    else if (RestUrlUtil.isLocalDebug(location)) {
                        //For the local debugging of the UI, which does not seem to like loading json without a .json suffix
                        return path + ".json";
                    }
                    return path;
                };
                RestUrlUtil.calculateJiraUrl = function () {
                    var location = window.location;
                    console.log("-----> " + location.href);
                    var index = location.href.indexOf("/download/resources/");
                    if (index > 0) {
                        return location.href.substr(0, index);
                    }
                    else if (RestUrlUtil.isLocalDebug(location)) {
                        //Return the locally running Jira instance
                        return "http://localhost:2990/jira";
                    }
                    console.error("Could not determine jir url " + location.href);
                    return "";
                };
                RestUrlUtil.isLocalDebug = function (location) {
                    return location.hostname === "localhost" && location.port === "3000";
                };
                return RestUrlUtil;
            })();
            exports_1("RestUrlUtil", RestUrlUtil);
        }
    }
});
//# sourceMappingURL=RestUrlUtil.js.map
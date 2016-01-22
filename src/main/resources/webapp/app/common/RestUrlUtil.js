System.register([], function(exports_1) {
    var RestUrlUtil;
    return {
        setters:[],
        execute: function() {
            /**
             * Utils to calculate the rest urls, depending on if we are running in dev mode (i.e. not deployed as a servlet),
             * or deployed within Jira
             */
            RestUrlUtil = (function () {
                function RestUrlUtil() {
                }
                RestUrlUtil.caclulateUrl = function (path) {
                    var location = window.location;
                    console.log("href " + location.href);
                    var index = location.href.indexOf("/download/resources/");
                    console.log("Index: " + index);
                    if (index > 0) {
                        var url = location.href.substr(0, index);
                        console.log("Base url " + url);
                        url = url + "/plugins/servlet/jirban/" + path;
                        console.log("Real url: " + url);
                        return url;
                    }
                    console.log("Returning original");
                    return path;
                };
                return RestUrlUtil;
            })();
            exports_1("RestUrlUtil", RestUrlUtil);
        }
    }
});
//# sourceMappingURL=RestUrlUtil.js.map
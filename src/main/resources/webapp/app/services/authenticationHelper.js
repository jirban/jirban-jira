System.register([], function(exports_1) {
    var local, tokenName;
    function setToken(token) {
        localStorage.setItem(tokenName, token);
    }
    exports_1("setToken", setToken);
    function hasToken() {
        return !!getToken();
    }
    exports_1("hasToken", hasToken);
    function getToken() {
        if (local) {
            return "abcd";
        }
        return localStorage.getItem(tokenName);
    }
    exports_1("getToken", getToken);
    function clearToken() {
        console.log("Clearing token");
        localStorage.removeItem(tokenName);
    }
    exports_1("clearToken", clearToken);
    return {
        setters:[],
        execute: function() {
            /**
             * Set this to 'true' to be able to work on the UI with the data in the /rest folder,
             * 'false' to work deployed in wildfly
             */
            local = false;
            tokenName = "jirban_token";
        }
    }
});
//# sourceMappingURL=authenticationHelper.js.map
// Avoid TS error "cannot find name escape"
declare var escape;


/**
 * Set this to 'true' to be able to work on the UI with the data in the /rest folder,
 * 'false' to work deployed in wildfly
 */
const local : boolean = false;

const tokenName:string = "jirban_token";

export function setToken(token : string) {
    localStorage.setItem(tokenName, token);
}

export function hasToken() {
    return !!getToken();
}

export function getToken() {
    if (local) {
        return "abcd";
    }
    return localStorage.getItem(tokenName);
}

export function clearToken() {
    console.log("Clearing token");
    localStorage.removeItem(tokenName);
}



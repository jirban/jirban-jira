/**
 * Utils to calculate the rest urls, depending on if we are running in dev mode (i.e. not deployed as a servlet),
 * or deployed within Jira
 */
export class RestUrlUtil {

    static caclulateUrl(path:string) : string {
        let location:Location = window.location;
        console.log("href " + location.href)

        let index:number = location.href.indexOf("/download/resources/");
        console.log("Index: " + index);
        if (index > 0) {
            let url:string = location.href.substr(0, index);
            url = url + "/plugins/servlet/jirban/" + path;
            return url;
        }

        return path;
    }
}
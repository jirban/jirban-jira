import {Component, Host, View} from 'angular2/core';
import {Headers, Http, Response} from 'angular2/http';
import {Router} from 'angular2/router';
import {clearToken, getToken, hasToken} from '../../services/authenticationHelper';

@Component({
    selector: 'logout-form'
})
@View({
    template: '<p>This will never be shown</p>'
})
export class LogoutComponent {
    private http:Http;
    private router:Router;

    constructor(http:Http, router:Router) {
        this.http = http;
        this.router = router;
        console.log("Logging out");

        if (hasToken()) {
            console.log("Rest call to log out");
            let headers = new Headers();
            headers.append("Authorization", getToken());
            this.http.post(
                'rest/logout', '', {
                    headers: headers
            }).map(res => res).subscribe();

        }
        clearToken();
        //Go back to the about page
        this.router.navigateByUrl('');
    }

}
import {Component, Host, View} from 'angular2/core';
import {FORM_DIRECTIVES, ControlGroup, FormBuilder, Validators} from 'angular2/common';
import {Headers, Http, Response} from 'angular2/http';
import {Router} from 'angular2/router';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import {clearToken,setToken} from '../../services/authenticationHelper';

@Component({
    selector: 'login-form',
    providers: [FormBuilder]
})
@View({
    templateUrl: 'app/components/login/login.html',
    directives: [FORM_DIRECTIVES]
})
export class LoginComponent {
    private http:Http;
    private router:Router;
    private form : any;
    private error : boolean;

    constructor(http:Http, router:Router, fb:FormBuilder) {
        this.http = http;
        this.router = router;
        this.form = fb.group({
            "username": ['', Validators.required],
            "password": ['', Validators.required]
        });
        clearToken();
    }

    private onSubmit() {
        let headers = new Headers();
        headers.append('Content-Type', 'application/x-www-form-urlencoded');

        this.http.post(
            'rest/login', JSON.stringify(this.form.value), {
                headers: headers
            })
            .map(res => (<Response>res).json())
            .subscribe(
                data => {
                    console.log('Login: Got data' + JSON.stringify(data));
                    let token = data["auth-token"];
                    if (!!token) {
                        setToken(token);
                        this.router.navigateByUrl('/boards');
                    }
                }
                ,
                err => {
                    //TODO No idea how to get error code to differentiate between auth and other errors
                    this.error = true;
                    console.error(err)
                },
                () => console.log('Login: auth finished')
            );
    }

    private clearError() {
        this.error = null;
    }
}
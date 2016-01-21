import {Directive, Attribute, ElementRef, DynamicComponentLoader} from 'angular2/core';
import {Router, RouterOutlet, ComponentInstruction} from 'angular2/router';
import {hasToken} from '../services/authenticationHelper';

@Directive({
    selector: 'router-outlet'
})
export class LoggedInRouterOutlet extends RouterOutlet {
    publicRoutes:any;
    private parentRouter:Router;

    constructor(_elementRef:ElementRef, _loader:DynamicComponentLoader,
                _parentRouter:Router, @Attribute('name') nameAttr:string) {
        super(_elementRef, _loader, _parentRouter, nameAttr);

        this.parentRouter = _parentRouter;
        this.publicRoutes = {
            'login': true,
            '': true, //The about page has an empty url
            'logout': true
        };
    }

    activate(instruction: ComponentInstruction) {
        //let url = this.parentRouter.lastNavigationAttempt;
        let url = instruction.urlPath;
        if (!this.publicRoutes[url] && !hasToken()) {
            // todo: redirect to Login, may be there a better way?
            console.log('RouterOutlet: Redirecting to login (' + url + ')');
            this.parentRouter.navigateByUrl('/login');
        }
        console.log('RouterOutlet: Going to requested url: ' + url);
        return super.activate(instruction);
    }
}
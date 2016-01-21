package org.jirban.jira.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;

public class AuthFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    //private final UserManager userManager;

//    @ComponentImport
//    private final com.atlassian.sal.api.user.UserManager salUserManager;

    public AuthFilter() {
        //this.userManager = userManager;
        //this.salUserManager = salUserManager;
        //Jira doesn't seem to like me attempting to inject both of these
        //userManager = ComponentAccessor.getUserManager();


        System.out.println("---> Jira " + ComponentAccessor.getUserManager());

    }

    public void init(FilterConfig filterConfig)throws ServletException{
    }

    public void destroy(){
    }


    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)throws IOException,ServletException{
        final HttpServletRequest req = (HttpServletRequest)request;
        final User user = getUserByKey(req.getRemoteUser());
        System.out.println("-----> Auth Filter");

        //continue the request
        chain.doFilter(request,response);
    }

    private User getUserByKey(String remoteUser) {
        if (remoteUser == null) {
            return null;
        }
//        ApplicationUser user = userManager.getUserByKey(remoteUser);
//        if (user != null) {
//            return user.getDirectoryUser();
//        }
        return null;
    }


}
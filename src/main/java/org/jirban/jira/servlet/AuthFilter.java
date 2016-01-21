package org.jirban.jira.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jirban.jira.api.JiraFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.user.UserProfile;

@Named("jirbanAuthFilter")
public class AuthFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final JiraFacade jiraFacade;

    @Inject
    public AuthFilter(JiraFacade jiraFacade) {
        this.jiraFacade = jiraFacade;
        System.out.println("---> Jira " + jiraFacade);

    }

    public void init(FilterConfig filterConfig)throws ServletException{
    }

    public void destroy(){
    }


    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)throws IOException,ServletException{
        final HttpServletRequest req = (HttpServletRequest)request;
        final User user = getUserByKey(req);
        System.out.println("-----> Auth Filter " + user);

        //continue the request
        if (user != null) {
            Utils.setRemoteUser(req, user);
            chain.doFilter(request, response);
        } else {
            Utils.sendErrorJson((HttpServletResponse)response, HttpServletResponse.SC_UNAUTHORIZED, null);
        }
    }

    private User getUserByKey(HttpServletRequest request) {
        UserProfile userProfile = jiraFacade.getUserManagerSal().getRemoteUser(request);
        if (userProfile == null) {
            return null;
        }
        ApplicationUser user = jiraFacade.getUserManagerJira().getUserByKey(userProfile.getUserKey().getStringValue());
        if (user != null) {
            return user.getDirectoryUser();
        }
        return null;
    }


}
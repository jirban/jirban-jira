package org.jirban.jira.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jirban.jira.api.JiraFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.user.ApplicationUser;

public class RestServlet extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(RestServlet.class);

    private final JiraFacade jiraFacade;

    public RestServlet(JiraFacade jiraFacade) {
        this.jiraFacade = jiraFacade;
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String remoteUser = req.getRemoteUser();
        System.out.println("Remote user " + remoteUser);

        ApplicationUser applicationUser = jiraFacade.getUserByKey(remoteUser);
        System.out.println("Application user " + applicationUser);


        resp.setContentType("text/html");
        resp.getWriter().write("<html><body>Hello World " + jiraFacade.getName() +
                "</body></html>");
    }

}
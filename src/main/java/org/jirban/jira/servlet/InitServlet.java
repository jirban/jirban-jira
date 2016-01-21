package org.jirban.jira.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;

public class InitServlet extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(InitServlet.class);

    /*@ComponentImport
    private final TemplateRenderer templateRenderer;*/

    //@Inject
    public InitServlet(){/*final TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;*/
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ApplicationProperties applicationProperties = ComponentAccessor.getApplicationProperties();
        String baseUrl = applicationProperties.getString(APKeys.JIRA_BASEURL);
        System.out.println("Base url: " + baseUrl);

        //webResourceManager.requireResource("jirban-jira-resources.jirban-jira.webapp");
        resp.setContentType("text/html");
        resp.getWriter().write("<html><body>Init Servlet " +
                "</body></html>");
    }

}
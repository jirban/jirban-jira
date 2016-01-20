package org.jirban.jira.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jirban.jira.api.MyPluginComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestServlet extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(RestServlet.class);

    private final MyPluginComponent myPluginComponent;

    public RestServlet(MyPluginComponent myPluginComponent) {
        this.myPluginComponent = myPluginComponent;
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        resp.setContentType("text/html");
        resp.getWriter().write("<html><body>Hello World</body></html>");
    }

}
package org.jirban.jira.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jirban.jira.api.JiraFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;

@Named("jirbanRestServlet")
public class RestServlet extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(RestServlet.class);

    private final JiraFacade jiraFacade;

    @Inject
    public RestServlet(JiraFacade jiraFacade) {
        this.jiraFacade = jiraFacade;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = Utils.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        System.out.println("Rest servlet GET " + pathInfo + "(" + user + ")");
        if (pathInfo.equals("/boards.json")) {
            System.out.println("Getting boards");
            boolean full = req.getParameter("full") != null;
            String json = jiraFacade.getBoardsJson(full);
            Utils.sendResponseJson(resp, json);
            return;
        }
        else if (pathInfo.equals("/issues.json")) {
            final String boardId = req.getParameter("board");
            String json = jiraFacade.getBoardJson(Integer.valueOf(boardId));
            Utils.sendResponseJson(resp, json);
            return;
        } else {
            Utils.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = Utils.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        System.out.println("Rest servlet DELETE " + pathInfo + "(" + user + ")");
        if (pathInfo.equals("/board")) {
            int id = -1;
            if (req.getParameter("id") != null) {
                id = Integer.valueOf(req.getParameter("id"));
            }
            jiraFacade.deleteBoard(id);
            String json = jiraFacade.getBoardsJson(true);
            Utils.sendResponseJson(resp, json);
            return;
        } else {
            Utils.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
        }    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = Utils.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        System.out.println("Rest servlet POST " + pathInfo + "(" + user + ")");
        if (pathInfo.equals("/save-board")) {
            int id = -1;
            if (req.getParameter("id") != null) {
                id = Integer.valueOf(req.getParameter("id"));
            }
            jiraFacade.saveBoard(id, Utils.getRequestBody(req));
            String json = jiraFacade.getBoardsJson(true);
            Utils.sendResponseJson(resp, json);
            return;
        } else {
            Utils.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
        }


    }
}
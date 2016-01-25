package org.jirban.jira.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.api.JiraFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.search.SearchException;

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
        User user = Util.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        System.out.println("Rest servlet GET " + pathInfo + "(" + user + ")");
        PathAndId pathAndId = PathAndId.parse("GET", pathInfo);
        if (pathAndId.isPath("boards")) {
            pathAndId.validateId(false);
            System.out.println("Getting boards");
            boolean full = req.getParameter("full") != null;
            String json = jiraFacade.getBoardsJson(full);
            Util.sendResponseJson(resp, json);
            return;
        }
        else if (pathInfo.equals("/issues.json")) {
            final String boardId = req.getParameter("board");
            String json = null;
            try {
                json = jiraFacade.getBoardJson(user, Integer.valueOf(boardId));
            } catch (SearchException e) {
                //TODO figure out if a permission violation becomes a search exception
                Util.sendErrorJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            Util.sendResponseJson(resp, json);
            return;
        } else {
            Util.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = Util.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        System.out.println("Rest servlet DELETE " + pathInfo + "(" + user + ")");
        PathAndId pathAndId = PathAndId.parse("DELETE", pathInfo);
        if (pathAndId.isPath("boards")) {
            pathAndId.validateId(true);
            jiraFacade.deleteBoard(pathAndId.id);
            String json = jiraFacade.getBoardsJson(true);
            Util.sendResponseJson(resp, json);
            return;
        } else {
            Util.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = Util.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        System.out.println("Rest servlet POST " + pathInfo + "(" + user + ")");
        PathAndId pathAndId = PathAndId.parse("POST", pathInfo);
        if (pathAndId.isPath("boards")) {
            pathAndId.validateId(false);
            final ModelNode config = Util.getRequestBodyNode(req);
            jiraFacade.saveBoard(-1, Util.getDeployedUrl(req), config);
            String json = jiraFacade.getBoardsJson(true);
            Util.sendResponseJson(resp, json);
            return;
        } else {
            Util.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = Util.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        System.out.println("Rest servlet PUT " + pathInfo + "(" + user + ")");
        PathAndId pathAndId = PathAndId.parse("PUT", pathInfo);
        if (pathAndId.isPath("boards")) {
            pathAndId.validateId(true);
            final ModelNode config = Util.getRequestBodyNode(req);
            jiraFacade.saveBoard(pathAndId.id, Util.getDeployedUrl(req), config);
            String json = jiraFacade.getBoardsJson(true);
            Util.sendResponseJson(resp, json);
            return;
        } else {
            Util.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private static class PathAndId {
        private final String method;
        private final String pathInfo;
        private final String path;
        private final Integer id;

        private PathAndId(String method, String pathInfo, String path, Integer id) {
            this.method = method;
            this.pathInfo = pathInfo;
            this.path = path;
            this.id = id;
        }

        boolean isPath(String path) {
            return this.path.equals(path);
        }

        void validateId(boolean id) {
            if (id && this.id == null) {
                throw new InvalidPathFormatException(method, pathInfo, " does not have an id");
            }
            if (!id && this.id != null) {
                throw new InvalidPathFormatException(method, pathInfo, " should not have an id");
            }
        }

        static PathAndId parse(String method, String pathInfo) {
            if (pathInfo.length() <= 1) {
                throw new InvalidPathFormatException(method, pathInfo, " doesn't contain anything useful");
            }
            int index = pathInfo.lastIndexOf("/");
            if (index == 0) {
                return new PathAndId(method, pathInfo, pathInfo.substring(1), null);
            }

            if (pathInfo.indexOf("/", 1) != index || index == pathInfo.length() - 1) {
                throw new InvalidPathFormatException(method, pathInfo, " has a bad format");
            }

            return new PathAndId(method, pathInfo, pathInfo.substring(1, index), Integer.valueOf(pathInfo.substring(index + 1)));
        }
    }

    private static class InvalidPathFormatException extends RuntimeException {
        public InvalidPathFormatException(String method, String pathInfo, String message) {
            super(method + " " + pathInfo + " " + message);
        }
    }
}
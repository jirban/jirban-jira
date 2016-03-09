package org.jirban.jira.servlet;

import static org.jirban.jira.impl.Constants.BOARDS;
import static org.jirban.jira.impl.Constants.ISSUES;
import static org.jirban.jira.impl.Constants.UPDATES;
import static org.jirban.jira.impl.Constants.VERSION;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanLogger;
import org.jirban.jira.JirbanPermissionException;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.api.JiraFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;

@Named("jirbanRestServlet")
public class RestServlet extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(RestServlet.class);

    /**
     * If we change anything in the payloads etc. we should bump this so that the client can take action.
     * The corresponding location on the client is in app.ts
     */
    private static final int API_VERSION = 1;

    private final JiraFacade jiraFacade;

    @Inject
    public RestServlet(JiraFacade jiraFacade) {
        this.jiraFacade = jiraFacade;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ApplicationUser user = Util.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        JirbanLogger.LOGGER.debug("Rest servlet GET {} - {}", pathInfo, user);

        try {
            PathAndId pathAndId = PathAndId.parse("GET", pathInfo);
            if (pathAndId.isPath(VERSION)) {
                pathAndId.validateId(false);
                ModelNode versionNode = new ModelNode();
                versionNode.get(VERSION).set(API_VERSION);
                Util.sendResponseJson(resp, versionNode.toJSONString(true));
                return;
            } else if (pathAndId.isPath(BOARDS)) {
                pathAndId.validateId(false);
                boolean full = req.getParameter("full") != null;
                final String json;
                if (full) {
                    json = jiraFacade.getBoardConfigurations(user);
                } else {
                    json = jiraFacade.getBoardsForDisplay(user);
                }
                Util.sendResponseJson(resp, json);
                return;
            } else if (pathAndId.isPath(ISSUES)) {
                pathAndId.validateId(true);
                PathAndId next = pathAndId.getNext();
                if (next == null) {
                    try {
                        String json = jiraFacade.getBoardJson(user, pathAndId.getId());
                        Util.sendResponseJson(resp, json);
                    } catch (SearchException e) {
                        //TODO figure out if a permission violation becomes a search exception
                        Util.sendErrorJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                    return;
                } else {
                    next.validateId(true);
                    if (next.isPath(UPDATES)) {
                        try {
                            String json = jiraFacade.getChangesJson(user, pathAndId.getId(), next.getId());
                            Util.sendResponseJson(resp, json);
                        } catch (SearchException e) {
                            //TODO figure out if a permission violation becomes a search exception
                            Util.sendErrorJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        }
                        return;
                    }
                }
            } else {
                Util.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (PathAndId.InvalidPathFormatException | JirbanValidationException e) {
            Util.sendErrorJson(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (JirbanPermissionException e) {
            Util.sendErrorJson(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ApplicationUser user = Util.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        JirbanLogger.LOGGER.debug("Rest servlet DELETE {} - {}", pathInfo, user);

        try {
            PathAndId pathAndId = PathAndId.parse("DELETE", pathInfo);
            if (pathAndId.isPath(BOARDS)) {
                pathAndId.validateId(true);
                jiraFacade.deleteBoardConfiguration(user, pathAndId.getId());
                String json = jiraFacade.getBoardConfigurations(user);
                Util.sendResponseJson(resp, json);
                return;
            } else {
                Util.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (PathAndId.InvalidPathFormatException | JirbanValidationException e) {
            Util.sendErrorJson(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (JirbanPermissionException e) {
            Util.sendErrorJson(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ApplicationUser user = Util.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        JirbanLogger.LOGGER.debug("Rest servlet POST {} - {}", pathInfo, user);

        try {
            PathAndId pathAndId = PathAndId.parse("POST", pathInfo);
            if (pathAndId.isPath(BOARDS)) {
                pathAndId.validateId(false);
                final ModelNode config = Util.getRequestBodyNode(req);
                jiraFacade.saveBoardConfiguration(user, -1, Util.getDeployedUrl(req), config);
                String json = jiraFacade.getBoardConfigurations(user);
                Util.sendResponseJson(resp, json);
                return;
            } else {
                Util.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (PathAndId.InvalidPathFormatException | JirbanValidationException e) {
            Util.sendErrorJson(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (JirbanPermissionException e) {
            Util.sendErrorJson(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ApplicationUser user = Util.getRemoteUser(req);
        String pathInfo = req.getPathInfo();
        JirbanLogger.LOGGER.debug("Rest servlet PUT {} - {}", pathInfo, user);

        try {
            PathAndId pathAndId = PathAndId.parse("PUT", pathInfo);
            if (pathAndId.isPath(BOARDS)) {
                pathAndId.validateId(true);
                final ModelNode config = Util.getRequestBodyNode(req);
                jiraFacade.saveBoardConfiguration(user, pathAndId.getId(), Util.getDeployedUrl(req), config);
                String json = jiraFacade.getBoardConfigurations(user);
                Util.sendResponseJson(resp, json);
                return;
            } else {
                Util.sendErrorJson(resp, HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (PathAndId.InvalidPathFormatException | JirbanValidationException e) {
            Util.sendErrorJson(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (JirbanPermissionException e) {
            Util.sendErrorJson(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }
}
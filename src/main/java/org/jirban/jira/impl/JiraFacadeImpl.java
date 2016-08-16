package org.jirban.jira.impl;

import java.io.InputStream;
import java.util.jar.Manifest;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanLogger;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.api.JiraFacade;
import org.jirban.jira.api.UserAccessManager;
import org.jirban.jira.impl.config.BoardConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;

@Named ("jirbanJiraFacade")
public class JiraFacadeImpl implements JiraFacade, InitializingBean, DisposableBean {
    private final BoardConfigurationManager boardConfigurationManager;

    private final BoardManager boardManager;

    private final UserAccessManager userAccessManager;

    private static final String jirbanVersion;

    static {
        InputStream stream = JiraFacadeImpl.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
        Manifest manifest = null;
        String version;
        try {
            if (stream != null) {
                manifest = new Manifest(stream);
            }
            version = manifest.getMainAttributes().getValue("Bundle-Version");
        } catch (Exception e) {
            // ignored
            version = "Error";
        }
        jirbanVersion = version;
    }

    @Inject
    public JiraFacadeImpl(final BoardConfigurationManager boardConfigurationManager,
                          final BoardManager boardManager,
                          final UserAccessManager userAccessManager) {
        this.boardConfigurationManager = boardConfigurationManager;
        this.boardManager = boardManager;
        this.userAccessManager = userAccessManager;
    }

    @Override
    public String getBoardConfigurations(ApplicationUser user) {
        return boardConfigurationManager.getBoardsJson(user, true);
    }

    @Override
    public String getBoardJsonForConfig(ApplicationUser user, int boardId) {
        return boardConfigurationManager.getBoardJsonConfig(user, boardId);
    }

    @Override
    public void saveBoardConfiguration(ApplicationUser user, int id, String jiraUrl, ModelNode config) {
        BoardConfig boardConfig = boardConfigurationManager.saveBoard(user, id, config);
        if (id >= 0) {
            //We are modifying a board's configuration. Delete the board config and board data to force a refresh.
            boardManager.deleteBoard(user, boardConfig.getCode());
        }
    }

    @Override
    public void deleteBoardConfiguration(ApplicationUser user, int id) {
        final String code = boardConfigurationManager.deleteBoard(user, id);
        boardManager.deleteBoard(user, code);
    }

    @Override
    public String getBoardJson(ApplicationUser user, boolean backlog, String code) throws SearchException {
        try {
            return boardManager.getBoardJson(user, backlog, code);
        } catch (Exception e) {
            //Last parameter is the exception (it does not match a {} entry)
            JirbanLogger.LOGGER.debug("BoardManagerImpl.handleEvent - Error loading board {}", code, e);
            if (e instanceof SearchException || e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBoardsForDisplay(ApplicationUser user) {
        return boardConfigurationManager.getBoardsJson(user, false);
    }

    @Override
    public String getChangesJson(ApplicationUser user, boolean backlog, String code, int viewId) throws SearchException {
        return boardManager.getChangesJson(user, backlog, code, viewId);
    }

    @Override
    public void saveCustomFieldId(ApplicationUser user, ModelNode idNode) {
        boardConfigurationManager.saveRankCustomFieldId(user, idNode);
    }

    @Override
    public String getStateHelpTexts(ApplicationUser user, String boardCode) {
        return boardConfigurationManager.getStateHelpTextsJson(user, boardCode);
    }

    @Override
    public String getJirbanVersion() {
        return jirbanVersion;
    }


    @Override
    public void logUserAccess(ApplicationUser user, String boardCode) {
        userAccessManager.logUserAccess(user, boardCode);
    }

    @Override
    public String getUserAccessJson(ApplicationUser user) {
        return userAccessManager.getUserAccessJson(user);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

}
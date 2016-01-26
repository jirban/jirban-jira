package org.jirban.jira.impl;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.api.JiraFacade;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.user.UserService;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;

@Named ("jirbanJiraFacade")
public class JiraFacadeImpl implements JiraFacade, InitializingBean, DisposableBean {
    @ComponentImport
    private final ApplicationProperties applicationProperties;

    @ComponentImport
    private final IssueService issueService;

    @ComponentImport
    private final SearchService searchService;

    @ComponentImport
    private final UserService userService;

    private final BoardConfigurationManager boardConfigurationManager;

    private final BoardManager boardManager;

    @Inject
    public JiraFacadeImpl(final ApplicationProperties applicationProperties,
                          final IssueService issueService,
                          final SearchService searchService,
                          final UserService userService,
                          final BoardConfigurationManager boardConfigurationManager,
                          final BoardManager boardManager) {
        this.applicationProperties = applicationProperties;
        this.issueService = issueService;
        this.searchService = searchService;
        this.userService = userService;
        this.boardConfigurationManager = boardConfigurationManager;
        this.boardManager = boardManager;
    }

    @Override
    public String getBoardConfigurations(ApplicationUser user) {
        return boardConfigurationManager.getBoardsJson(user, true);
    }

    @Override
    public void saveBoardConfiguration(ApplicationUser user, int id, String jiraUrl, ModelNode config) {
        boardConfigurationManager.saveBoard(user, id, config);
        if (id >= 0) {
            //We are modifying a board's configuration. Delete the board config and board data to force a refresh.
            boardManager.deleteBoard(user,id);
        }
    }

    @Override
    public void deleteBoardConfiguration(ApplicationUser user, int id) {
        boardConfigurationManager.deleteBoard(user, id);
        boardManager.deleteBoard(user, id);
    }

    @Override
    public String getBoardJson(ApplicationUser user, int id) throws SearchException {
        return boardManager.getBoardJson(user, id);
    }

    @Override
    public String getBoardsForDisplay(ApplicationUser user) {
        return boardConfigurationManager.getBoardsJson(user, false);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

}
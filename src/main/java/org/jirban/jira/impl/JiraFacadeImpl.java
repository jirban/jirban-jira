package org.jirban.jira.impl;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.api.JiraFacade;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.user.UserService;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.web.bean.PagerFilter;
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
    public String getBoardsJson(boolean full) {
        return boardConfigurationManager.getBoardsJson(full);
    }

    @Override
    public void saveBoard(int id, String jiraUrl, ModelNode config) {
        boardConfigurationManager.saveBoard(id, jiraUrl, config);
    }

    @Override
    public void deleteBoard(int id) {
        boardConfigurationManager.deleteBoard(id);
    }

    @Override
    public String getBoardJson(User user, int id) throws SearchException {
        return boardManager.getBoardJson(user, id);
    }

    public void populateIssueTable(User user, String boardCode) throws SearchException {
        JqlClauseBuilder jqlBuilder = JqlQueryBuilder.newClauseBuilder();
        jqlBuilder.project("TUTORIAL").buildQuery();
        SearchResults searchResults =
                searchService.search(user, jqlBuilder.buildQuery(), PagerFilter.getUnlimitedFilter());
        System.out.println(searchResults.getIssues());
    }

    public String getName() {
        if(null != applicationProperties)
        {
            return "myComponent:" + applicationProperties.getDisplayName() + new SimpleDateFormat("HH:mm:ss").format(new Date());
        }
        
        return "myComponent";
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

}
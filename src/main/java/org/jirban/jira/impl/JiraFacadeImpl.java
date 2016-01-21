package org.jirban.jira.impl;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.jirban.jira.api.JiraFacade;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.user.UserService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;

@Named ("jirbanJiraFacade")
public class JiraFacadeImpl implements JiraFacade
{
    @ComponentImport
    private final ApplicationProperties applicationProperties;

    @ComponentImport
    private final IssueService issueService;

    @ComponentImport
    private final SearchService searchService;

    @ComponentImport
    private final UserService userService;

    @ComponentImport
    private final UserManager userManagerJira;

    @ComponentImport
    com.atlassian.sal.api.user.UserManager userManagerSal;

    @Inject
    public JiraFacadeImpl(final ApplicationProperties applicationProperties,
                          final IssueService issueService,
                          final SearchService searchService,
                          final UserService userService,
                          final com.atlassian.sal.api.user.UserManager userManagerSal)
    {
        this.applicationProperties = applicationProperties;
        this.issueService = issueService;
        this.searchService = searchService;
        this.userService = userService;
        this.userManagerJira = ComponentAccessor.getUserManager();
        this.userManagerSal = userManagerSal;
    }

    public User getUserByKey(String remoteUser) {
        if (remoteUser == null) {
            return null;
        }
        ApplicationUser user = userManagerJira.getUserByKey(remoteUser);
        if (user != null) {
            return user.getDirectoryUser();
        }
        return null;
    }

    public void populateIssueTable(User user, String boardCode) throws SearchException {
        JqlClauseBuilder jqlBuilder = JqlQueryBuilder.newClauseBuilder();
        jqlBuilder.project("TUTORIAL").buildQuery();
        SearchResults searchResults =
                searchService.search(user, jqlBuilder.buildQuery(), PagerFilter.getUnlimitedFilter());
        System.out.println(searchResults.getIssues());
    }

    public String getName()
    {
        if(null != applicationProperties)
        {
            return "myComponent:" + applicationProperties.getDisplayName() + new SimpleDateFormat("HH:mm:ss").format(new Date());
        }
        
        return "myComponent";
    }

    public UserManager getUserManagerJira() {
        return userManagerJira;
    }

    public com.atlassian.sal.api.user.UserManager getUserManagerSal() {
        return userManagerSal;
    }
}
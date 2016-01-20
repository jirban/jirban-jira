package org.jirban.jira.impl;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.jirban.jira.api.JiraFacade;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.user.UserService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;

@ExportAsService ({JiraFacade.class})
@Named ("myPluginComponent")
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
                          final UserManager userManagerJira,
                          final com.atlassian.sal.api.user.UserManager userManagerSal)
    {
        this.applicationProperties = applicationProperties;
        this.issueService = issueService;
        this.searchService = searchService;
        this.userService = userService;
        this.userManagerJira = userManagerJira;
        this.userManagerSal = userManagerSal;
    }

    public ApplicationUser getLoggedInUser(HttpServletRequest request) {
        return null;
        //return userManagerJira.getUserByKey(userManagerSal.getRemoteUsername(request));
    }

//    public void populateIssueTable(ApplicationUser user, String boardCode) {
//        searchService
//    }

    public String getName()
    {
        if(null != applicationProperties)
        {
            return "myComponent:" + applicationProperties.getDisplayName() + new SimpleDateFormat("HH:mm:ss").format(new Date());
        }
        
        return "myComponent";
    }
}
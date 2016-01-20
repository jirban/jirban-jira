package org.jirban.jira.impl;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.jirban.jira.api.JiraFacade;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.user.UserService;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;

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

    @Inject
    public JiraFacadeImpl(final ApplicationProperties applicationProperties,
                          final IssueService issueService,
                          final SearchService searchService,
                          final UserService userService)
    {
        this.applicationProperties = applicationProperties;
        this.issueService = issueService;
        this.searchService = searchService;
        this.userService = userService;
    }

    public String getName()
    {
        if(null != applicationProperties)
        {
            return "myComponent:" + applicationProperties.getDisplayName() + new SimpleDateFormat("HH:mm:ss").format(new Date());
        }
        
        return "myComponent";
    }
}
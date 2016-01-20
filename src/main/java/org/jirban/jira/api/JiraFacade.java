package org.jirban.jira.api;

import com.atlassian.jira.user.ApplicationUser;

public interface JiraFacade
{
    String getName();

    ApplicationUser getUserByKey(String remoteUser);
}
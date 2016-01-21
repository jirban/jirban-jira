package org.jirban.jira.api;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.util.UserManager;

public interface JiraFacade
{
    String getName();

    User getUserByKey(String remoteUser);

    void populateIssueTable(User user, String boardCode) throws SearchException;

    UserManager getUserManagerJira();

    com.atlassian.sal.api.user.UserManager getUserManagerSal();
}
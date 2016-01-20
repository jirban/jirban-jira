package org.jirban.jira.api;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.search.SearchException;

public interface JiraFacade
{
    String getName();

    User getUserByKey(String remoteUser);

    void populateIssueTable(User user, String boardCode) throws SearchException;
}
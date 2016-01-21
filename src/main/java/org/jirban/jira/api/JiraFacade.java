package org.jirban.jira.api;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.search.SearchException;

public interface JiraFacade
{
    String getName();

    void populateIssueTable(User user, String boardCode) throws SearchException;
}
package org.jirban.jira.api;

import org.jboss.dmr.ModelNode;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;

public interface JiraFacade
{
    String getName();

    void populateIssueTable(User user, String boardCode) throws SearchException;

    String getBoardsJson(boolean full);

    void saveBoard(int id, String jiraUrl, ModelNode config);

    void deleteBoard(int id);

    String getBoardJson(ApplicationUser user, int id) throws SearchException;
}
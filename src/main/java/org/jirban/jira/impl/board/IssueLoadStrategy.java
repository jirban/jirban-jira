package org.jirban.jira.impl.board;

/**
 * Strategy for how to load things like custom fields
 *
 * @author Kabir Khan
 */
interface IssueLoadStrategy {

    /**
     * Called for each issue
     * @param issue the Jira issue
     * @param builder the builder used to load the issue
     */
    void handle(com.atlassian.jira.issue.Issue issue, Issue.Builder builder);

    /**
     * Called when all issues have been loaded
     */
    void finish();
}

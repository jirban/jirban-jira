package org.jirban.jira.api;

/**
 * When an issue is reranked this util figures out the following issue in the ranking order
 * to determine the insertion point. It is implemented as a bean to be able to mock it for unit tests.
 *
 * @author Kabir Khan
 */
public interface NextRankedIssueUtil {

    /**
     * Finds the next issue after the {@code issueKey} in the current ranking
     *
     * @param issueKey the issue key to determine the rank of
     * @return the issue key appearing after {@code issueKey} or {@code null} if this is the last issue
     */
    String findNextRankedIssue(String issueKey);
}

package org.jirban.jira.impl;

import javax.inject.Named;

import org.jirban.jira.api.NextRankedIssueUtil;

/**
 * @author Kabir Khan
 */
@Named("nextRankedIssueUtilImpl")
public class NextRankedIssueUtilImpl implements NextRankedIssueUtil {

    @Override
    public String findNextRankedIssue(String issueKey) {
        //TODO
        return null;
    }
}

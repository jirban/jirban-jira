/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package ut.org.jirban.jira.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.user.util.UserManager;

/**
 * @author Kabir Khan
 */
public class IssueRegistry {
    private final CrowdUserBridge userBridge;
    private final Map<String, List<Issue>> issuesByProject = new HashMap<>();

    public IssueRegistry(UserManager userManager) {
        this.userBridge = new CrowdUserBridge(userManager);
    }

    public IssueRegistry addIssue(String projectCode, String issueType, String priority, String summary,
                                         String state, String assignee) {
        List<Issue> issues = issuesByProject.computeIfAbsent(projectCode, x -> new ArrayList<>());
        String issueKey = projectCode + "-" + (issues.size() + 1);
        issues.add(new MockIssue(issueKey, MockIssueType.create(issueType), MockPriority.create(priority), summary,
                userBridge.getUserByKey(assignee), MockStatus.create(state)));
        return this;
    }

    List<Issue> getIssueList(String project, String searchStatus) {
        List<Issue> issues = issuesByProject.get(project);
        if (issues == null) {
            return Collections.emptyList();
        }

        List<Issue> ret = new ArrayList<>();
        for (Issue issue : issues) {
            if (searchStatus != null) {
                if (!issue.getStatusId().equals(searchStatus)) {
                    continue;
                }
            }
            ret.add(issue);
        }
        return ret;
    }

    private class IssueDetail {
        final Issue issue;

        final IssueType issueType;
        final Priority priority;
        final Status state;

        @SuppressWarnings("deprecation")
        public IssueDetail(String key, String issueType, String priority, String summary,
                           String state, String assignee) {
            //Do the nested mocks first
            this.issueType = MockIssueType.create(issueType);
            this.priority = MockPriority.create(priority);
            this.state = MockStatus.create(state);

            this.issue = new MockIssue(key, this.issueType, this.priority, summary, userBridge.getUserByKey(assignee), this.state);
        }


    }
}

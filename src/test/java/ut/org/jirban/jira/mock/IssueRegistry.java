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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jirban.jira.api.NextRankedIssueUtil;
import org.junit.Assert;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.user.util.UserManager;

/**
 * @author Kabir Khan
 */
public class IssueRegistry implements NextRankedIssueUtil {
    private final CrowdUserBridge userBridge;
    private final Map<String, Map<String, MockIssue>> issuesByProject = new HashMap<>();

    public IssueRegistry(UserManager userManager) {
        this.userBridge = new CrowdUserBridge(userManager);
    }

    public IssueRegistry addIssue(String projectCode, String issueType, String priority, String summary,
                                  String assignee, String[] components, String state) {
        Map<String, MockIssue> issues = issuesByProject.computeIfAbsent(projectCode, x -> new LinkedHashMap<>());
        String issueKey = projectCode + "-" + (issues.size() + 1);
        issues.put(issueKey, new MockIssue(issueKey, MockIssueType.create(issueType), MockPriority.create(priority), summary,
                userBridge.getUserByKey(assignee), MockProjectComponent.createProjectComponents(components), MockStatus.create(state)));
        return this;
    }


    public void updateIssue(String issueKey, String issueTypeName, String priorityName,
                            String summary, String assignee, String[] components, String state) {
        Map<String, MockIssue> issues = issuesByProject.get(getProjectCode(issueKey));
        Assert.assertNotNull(issues);
        Issue issue = issues.get(issueKey);
        Assert.assertNotNull(issue);

        IssueType issueType = issueTypeName == null ? issue.getIssueTypeObject() : MockIssueType.create(issueTypeName);
        Priority priority = priorityName == null ? issue.getPriorityObject() : MockPriority.create(priorityName);
        String summ = summary == null ? issue.getSummary() : summary;
        User assigneeUser = assignee == null ? issue.getAssignee() : userBridge.getUserByKey(assignee);
        Set<ProjectComponent> comps;
        if (components != null) {
            comps = MockProjectComponent.createProjectComponents(components);
        } else {
            comps = issue.getComponentObjects() == null ? null : new HashSet<>(issue.getComponentObjects());
        }
        Status status = state == null ? issue.getStatusObject() : MockStatus.create(state);

        MockIssue newIssue = new MockIssue(issueKey, issueType, priority, summ,
                assigneeUser, comps, status);
        issues.put(issueKey, newIssue);
    }

    public void setCustomField(String issueKey, Long customFieldId, Object value) {
        String projectCode = issueKey.substring(0, issueKey.indexOf("-"));
        Map<String, MockIssue> issues = issuesByProject.get(projectCode);
        Assert.assertNotNull(issues);
        MockIssue issue = issues.get(issueKey);
        Assert.assertNotNull(issue);

        issue.setCustomField(customFieldId, value);
    }


    List<Issue> getIssueList(String searchIssueKey, String project, String searchStatus, Collection<String> doneStatesFilter) {
        if (searchIssueKey != null) {
            String projectCode = searchIssueKey.substring(0, searchIssueKey.indexOf("-"));
            return Collections.singletonList(getIssue(searchIssueKey));
        }
        Map<String, MockIssue> issues = issuesByProject.get(project);
        if (issues == null) {
            return Collections.emptyList();
        }

        List<Issue> ret = new ArrayList<>();
        for (Issue issue : issues.values()) {
            if (searchStatus != null && !issue.getStatusId().equals(searchStatus)) {
                continue;
            }
            if (doneStatesFilter != null && doneStatesFilter.contains(issue.getStatusId())) {
                continue;
            }
            ret.add(issue);
        }
        return ret;
    }

    public Issue getIssue(String issueKey) {
        Map<String, MockIssue> issues = issuesByProject.get(getProjectCode(issueKey));
        return issues.get(issueKey);
    }

    public void rerankIssue(String issueKey, String beforeIssueKey) {
        String projectCode = getProjectCode(issueKey);
        Map<String, MockIssue> issues = issuesByProject.get(projectCode);
        MockIssue issue = issues.get(issueKey);
        List<String> issueList = new ArrayList<>(issues.keySet());
        issueList.remove(issueKey);
        if (beforeIssueKey == null) {
            issueList.add(issueKey);
        } else {
            int index = issueList.indexOf(beforeIssueKey);
            issueList.add(index, issueKey);
        }

        Map<String, MockIssue> newIssues = new LinkedHashMap<>();
        for (String key : issueList) {
            newIssues.put(key, issues.get(key));
        }

        issuesByProject.put(projectCode, newIssues);
    }

    @Override
    public String findNextRankedIssue(String issueKey) {
        int index = issueKey.indexOf("-");
        String projectCode = issueKey.substring(0, index);
        Map<String, MockIssue> issues = issuesByProject.get(projectCode);
        boolean found = false;
        for (MockIssue issue : issues.values()) {
            if (found) {
                return issue.getKey();
            }
            if (issue.getKey().equals(issueKey)) {
                found = true;
            }
        }
        return null;
    }

    public void deleteIssue(String issueKey) {
        Map<String, MockIssue> issues = issuesByProject.get(getProjectCode(issueKey));
        issues.remove(issueKey);
    }

    private String getProjectCode(String issueKey) {
        int index = issueKey.indexOf("-");
        return issueKey.substring(0, index);
    }
}

/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.junit.Assert;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;

/**
 * @author Kabir Khan
 */
public class IssueRegistry implements NextRankedIssueUtil {
    private final UserManager userManager;
    private final Map<String, Map<String, MockIssue>> issuesByProject = new HashMap<>();

    public IssueRegistry(UserManager userManager) {
        this.userManager = userManager;
    }

    public CreateIssueBuilder issueBuilder(
            String projectCode, String issueType, String priority, String summary, String state) {
        return new CreateIssueBuilder(projectCode, issueType, priority, summary, state);
    }

    public void updateIssue(String issueKey, String issueTypeName, String priorityName,
                            String summary, String assignee, Set<ProjectComponent> components, Set<Label> labels, Set<Version> fixVersions,
                            String state) {
        Map<String, MockIssue> issues = issuesByProject.get(getProjectCode(issueKey));
        Assert.assertNotNull(issues);
        Issue issue = issues.get(issueKey);
        Assert.assertNotNull(issue);

        IssueType issueType = issueTypeName == null ? issue.getIssueTypeObject() : MockIssueType.create(issueTypeName);
        Priority priority = priorityName == null ? issue.getPriorityObject() : MockPriority.create(priorityName);
        String summ = summary == null ? issue.getSummary() : summary;
        ApplicationUser assigneeUser = assignee == null ? issue.getAssignee() : userManager.getUserByKey(assignee);
        Set<ProjectComponent> comps;
        if (components != null) {
            comps = components;
        } else {
            comps = issue.getComponentObjects() == null ? null : new HashSet<>(issue.getComponentObjects());
        }
        Set<Label> labelz;
        if (labels != null) {
            labelz = labels;
        } else {
            labelz = issue.getLabels() == null ? null : new HashSet<>(issue.getLabels());
        }
        Set<Version> fixVersionz;
        if (fixVersions != null) {
            fixVersionz = fixVersions;
        } else {
            fixVersionz = issue.getFixVersions() == null ? null : new HashSet<>(issue.getFixVersions());
        }
        Status status = state == null ? issue.getStatusObject() : MockStatus.create(state);


        MockIssue newIssue = new MockIssue(issueKey, issueType, priority, summ,
                assigneeUser, comps, labelz, fixVersionz, status);
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

    public void setParallelTaskField(String issueKey, Long upstreamId, String optionKey) {
        setCustomField(issueKey, upstreamId, optionKey);
    }

    List<Issue> getIssueList(String searchIssueKey, String project, String searchStatus, Collection<String> doneStatesFilter) {
        if (searchIssueKey != null) {
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
    public String findNextRankedIssue(BoardProjectConfig projectConfig, ApplicationUser boardOwner, String issueKey) {
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

    public class CreateIssueBuilder {
        private final String projectCode;
        private final IssueType issueType;
        private final Priority priority;
        private final String summary;
        private final Status state;
        private ApplicationUser assignee;
        private Set<ProjectComponent> components;
        private Set<Label> labels;
        private Set<Version> fixVersions;

        private CreateIssueBuilder(String projectCode, String issueType, String priority, String summary, String state) {
            this(projectCode, MockIssueType.create(issueType), MockPriority.create(priority), summary, MockStatus.create(state));
        }


        private CreateIssueBuilder(String projectCode, IssueType issueType, Priority priority, String summary, Status state) {
            this.projectCode = projectCode;
            this.issueType = issueType;
            this.priority = priority;
            this.summary = summary;
            this.state = state;
        }

        public CreateIssueBuilder assignee(String assignee) {
            this.assignee = userManager.getUserByKey(assignee);
            Assert.assertNotNull(this.assignee);
            return this;
        }

        public CreateIssueBuilder assignee(ApplicationUser assignee) {
            this.assignee = assignee;
            return this;
        }

        public CreateIssueBuilder components(String...components) {
            this.components = MockProjectComponent.createProjectComponents(components);
            return this;
        }

        public CreateIssueBuilder components(Set<ProjectComponent> components) {
            this.components = components;
            return this;
        }

        public CreateIssueBuilder labels(String...labels) {
            this.labels = MockLabel.createLabels(labels);
            return this;
        }

        public CreateIssueBuilder labels(Set<Label> labels) {
            this.labels = labels;
            return this;
        }

        public CreateIssueBuilder fixVersions(String...fixVersions) {
            this.fixVersions = MockVersion.createVersions(fixVersions);
            return this;
        }

        public CreateIssueBuilder fixVersions(Set<Version> fixVersions) {
            this.fixVersions = fixVersions;
            return this;
        }

        public Issue buildAndRegister() {
            Map<String, MockIssue> issues = issuesByProject.computeIfAbsent(projectCode, x -> new LinkedHashMap<>());
            String issueKey = projectCode + "-" + (issues.size() + 1);
            MockIssue issue =
                    new MockIssue(issueKey, issueType, priority, summary, assignee, components, labels, fixVersions, state);
            issues.put(issueKey, issue);
            return issue;
        }

    }
}

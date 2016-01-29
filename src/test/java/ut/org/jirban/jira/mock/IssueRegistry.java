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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.ofbiz.core.entity.GenericValue;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.renderer.IssueRenderContext;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.util.UserManager;

/**
 * @author Kabir Khan
 */
public class IssueRegistry {
    private final CrowdUserBridge userBridge;
    private final Map<String, List<IssueDetail>> issuesByProject = new HashMap<>();

    public IssueRegistry(UserManager userManager) {
        this.userBridge = new CrowdUserBridge(userManager);
    }

    public IssueRegistry addIssue(String projectCode, String issueType, String priority, String summary,
                                         String state, String assignee) {
        List<IssueDetail> issues = issuesByProject.computeIfAbsent(projectCode, x -> new ArrayList<>());
        String issueKey = projectCode + "-" + (issues.size() + 1);
        issues.add(new IssueDetail(issueKey, issueType, priority, summary, state, assignee));
        return this;
    }

    List<Issue> getIssueList(String project) {
        List<IssueDetail> issues = issuesByProject.get(project);
        if (issues == null) {
            return Collections.emptyList();
        }

        List<Issue> ret = new ArrayList<>();
        issues.forEach(id -> ret.add(id.issue));
        return ret;
    }

    private class IssueDetail {
        final Issue issue;

        final IssueType issueType = mock(IssueType.class);
        final Priority priority = mock(Priority.class);
        final Status state = mock(Status.class);

        @SuppressWarnings("deprecation")
        public IssueDetail(String key, String issueType, String priority, String summary,
                           String state, String assignee) {
            //Do the nested mocks first
            when(this.issueType.toString()).thenReturn(issueType);
            when(this.issueType.getName()).thenReturn(issueType);
            when(this.priority.toString()).thenReturn(priority);
            when(this.priority.getName()).thenReturn(priority);
            when(this.state.toString()).thenReturn(state);
            when(this.state.getName()).thenReturn(state);

            //Mockito seems to get a bit confused, so implement this properly
            this.issue = new Issue() {
                @Override
                public Long getId() {
                    return null;
                }

                @Override
                public GenericValue getProject() {
                    return null;
                }

                @Override
                public Project getProjectObject() {
                    return null;
                }

                @Override
                public Long getProjectId() {
                    return null;
                }

                @Override
                public GenericValue getIssueType() {
                    return null;
                }

                @Override
                public IssueType getIssueTypeObject() {
                    return IssueDetail.this.issueType;
                }

                @Override
                public String getIssueTypeId() {
                    return null;
                }

                @Override
                public String getSummary() {
                    return summary;
                }

                @Override
                public User getAssigneeUser() {
                    return userBridge.getUserByKey(assignee);
                }

                @Override
                public User getAssignee() {
                    return userBridge.getUserByKey(assignee);
                }

                @Override
                public String getAssigneeId() {
                    return null;
                }

                @Override
                public Collection<GenericValue> getComponents() {
                    return null;
                }

                @Override
                public Collection<ProjectComponent> getComponentObjects() {
                    return null;
                }

                @Override
                public User getReporterUser() {
                    return null;
                }

                @Override
                public User getReporter() {
                    return null;
                }

                @Override
                public String getReporterId() {
                    return null;
                }

                @Override
                public User getCreator() {
                    return null;
                }

                @Override
                public String getCreatorId() {
                    return null;
                }

                @Override
                public String getDescription() {
                    return null;
                }

                @Override
                public String getEnvironment() {
                    return null;
                }

                @Override
                public Collection<Version> getAffectedVersions() {
                    return null;
                }

                @Override
                public Collection<Version> getFixVersions() {
                    return null;
                }

                @Override
                public Timestamp getDueDate() {
                    return null;
                }

                @Override
                public GenericValue getSecurityLevel() {
                    return null;
                }

                @Override
                public Long getSecurityLevelId() {
                    return null;
                }

                @Nullable
                @Override
                public GenericValue getPriority() {
                    return null;
                }

                @Nullable
                @Override
                public Priority getPriorityObject() {
                    return IssueDetail.this.priority;
                }

                @Override
                public String getResolutionId() {
                    return null;
                }

                @Override
                public GenericValue getResolution() {
                    return null;
                }

                @Override
                public Resolution getResolutionObject() {
                    return null;
                }

                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public Long getNumber() {
                    return null;
                }

                @Override
                public Long getVotes() {
                    return null;
                }

                @Override
                public Long getWatches() {
                    return null;
                }

                @Override
                public Timestamp getCreated() {
                    return null;
                }

                @Override
                public Timestamp getUpdated() {
                    return null;
                }

                @Override
                public Timestamp getResolutionDate() {
                    return null;
                }

                @Override
                public Long getWorkflowId() {
                    return null;
                }

                @Override
                public Object getCustomFieldValue(CustomField customField) {
                    return null;
                }

                @Override
                public GenericValue getStatus() {
                    return null;
                }

                @Override
                public String getStatusId() {
                    return null;
                }

                @Override
                public Status getStatusObject() {
                    return IssueDetail.this.state;
                }

                @Override
                public Long getOriginalEstimate() {
                    return null;
                }

                @Override
                public Long getEstimate() {
                    return null;
                }

                @Override
                public Long getTimeSpent() {
                    return null;
                }

                @Override
                public Object getExternalFieldValue(String s) {
                    return null;
                }

                @Override
                public boolean isSubTask() {
                    return false;
                }

                @Override
                public Long getParentId() {
                    return null;
                }

                @Override
                public boolean isCreated() {
                    return false;
                }

                @Override
                public Issue getParentObject() {
                    return null;
                }

                @Override
                public GenericValue getParent() {
                    return null;
                }

                @Override
                public Collection<GenericValue> getSubTasks() {
                    return null;
                }

                @Override
                public Collection<Issue> getSubTaskObjects() {
                    return null;
                }

                @Override
                public boolean isEditable() {
                    return false;
                }

                @Override
                public IssueRenderContext getIssueRenderContext() {
                    return null;
                }

                @Override
                public Collection<Attachment> getAttachments() {
                    return null;
                }

                @Override
                public Set<Label> getLabels() {
                    return null;
                }

                @Override
                public GenericValue getGenericValue() {
                    return null;
                }

                @Override
                public String getString(String s) {
                    return null;
                }

                @Override
                public Timestamp getTimestamp(String s) {
                    return null;
                }

                @Override
                public Long getLong(String s) {
                    return null;
                }

                @Override
                public void store() {

                }
            };
        }


    }
}

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

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.ofbiz.core.entity.GenericValue;

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
import com.atlassian.jira.user.ApplicationUser;

/**
 * Mockito seems to get confused with deep mocks, so hardcode these value objects
 *
 * @author Kabir Khan
 */
public class MockIssue implements Issue {

    private final String key;
    private final IssueType issueType;
    private final Priority priority;
    private final String summary;
    private final ApplicationUser assignee;
    private final Set<ProjectComponent> components;
    private final Set<Label> labels;
    private final Set<Version> fixVersions;
    private final Status state;

    private final Map<Long, Object> customFields = new HashMap<>();

    public MockIssue(String key, IssueType issueType, Priority priority, String summary, ApplicationUser assignee,
                     Set<ProjectComponent> components, Set<Label> labels, Set<Version> fixVersions, Status state) {
        this.key = key;
        this.issueType = issueType;
        this.priority = priority;
        this.summary = summary;
        this.assignee = assignee;
        this.components = components;
        this.labels = labels;
        this.fixVersions = fixVersions;
        this.state = state;
    }

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
    public IssueType getIssueTypeObject() {
        return issueType;
    }

    @Override
    public String getIssueTypeId() {
        return issueType.getId();
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public ApplicationUser getAssigneeUser() {
        return assignee;
    }

    @Override
    public ApplicationUser getAssignee() {
        return assignee;
    }

    @Override
    public String getAssigneeId() {
        return assignee.getName();
    }

    @Override
    public Collection<ProjectComponent> getComponentObjects() {
        return components;
    }

    @Override
    public ApplicationUser getReporterUser() {
        return null;
    }

    @Override
    public ApplicationUser getReporter() {
        return null;
    }

    @Override
    public String getReporterId() {
        return null;
    }

    @Override
    public ApplicationUser getCreator() {
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
        return fixVersions;
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
    public Priority getPriorityObject() {
        return priority;
    }

    @Override
    public String getResolutionId() {
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
        return customFields.get(customField.getIdAsLong());
    }

    @Override
    public String getStatusId() {
        return state.getId();
    }

    @Override
    public Status getStatusObject() {
        return state;
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
        return labels;
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

    @Override
    public IssueType getIssueType() {
        return issueType;
    }

    @Override
    public Collection<ProjectComponent> getComponents() {
        return components;
    }

    @Nullable
    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    public Resolution getResolution() {
        return null;
    }

    @Override
    public Status getStatus() {
        return state;
    }

    void setCustomField(Long customFieldId, Object value) {
        if (value == null) {
            customFields.remove(customFieldId);
        } else {
            customFields.put(customFieldId, value);
        }
    }
}

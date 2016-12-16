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
package org.jirban.jira.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public class JirbanIssueEvent {
    private final Type type;
    private final String issueKey;
    private final String projectCode;
    private final Detail detail;

    private JirbanIssueEvent(Type type, String issueKey, String projectCode, Detail detail) {
        this.type = type;
        this.issueKey = issueKey;
        this.projectCode = projectCode;
        this.detail = detail;
    }

    public Type getType() {
        return type;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public String getProjectCode() {
        return projectCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("JirbanIssueEvent{type="
                + type + ";key=" + issueKey + ";project=" + projectCode);
        if (detail != null) {
            sb.append("Detail{");
            sb.append("issueType=" + detail.issueType);
            sb.append(";priority=" + detail.priority);
            sb.append(";summary=" + detail.summary);
            sb.append(";assignee=" + (detail.assignee != null ? detail.assignee.getName() : "null"));
            sb.append(";state=" + detail.state);
            sb.append(";reorder=" + detail.reranked);
            sb.append("}}");
        } else {
            sb.append("}");
        }
        return sb.toString();
    }

    /**
     * If {@link #getType()} is {@link Type#DELETE}, {@code null} is returned. Otherwise a {@code Detail} object is
     * returned. For a {@link Type#DELETE} the {@code Detail} object is populated with all the fields set on the issue
     * when it was created. For a {@link Type#DELETE} the {@code Detail} object is populated with the fields that were
     * actually changed.
     *
     * @return the details
     */
    public Detail getDetails() {
        return detail;
    }

    public static JirbanIssueEvent createDeleteEvent(String issueKey, String projectCode) {
        return new JirbanIssueEvent(Type.DELETE, issueKey, projectCode, null);
    }

    public static JirbanIssueEvent createCreateEvent(String issueKey, String projectCode, String issueType, String priority,
                                                     String summary, ApplicationUser assignee, Collection<ProjectComponent> components,
                                                     Collection<Label> labels, Collection<Version> fixVersions,
                                                     String state, Map<Long, String> customFieldValues) {
        Detail detail = new Detail(issueType, priority, summary, assignee, components, labels, fixVersions,
                null, state, true, customFieldValues);
        return new JirbanIssueEvent(Type.CREATE, issueKey, projectCode, detail);
    }

    public static JirbanIssueEvent createUpdateEvent(String issueKey, String projectCode, String issueType, String priority,
                                                     String summary, ApplicationUser assignee, Collection<ProjectComponent> components,
                                                     Collection<Label> labels, Collection<Version> fixVersions,
                                                     String currentState, String state, boolean reranked,
                                                     Map<Long, String> customFieldValues) {
        Detail detail = new Detail(issueType, priority, summary, assignee, components, labels, fixVersions,
                currentState, state, reranked, customFieldValues);
        return new JirbanIssueEvent(Type.UPDATE, issueKey, projectCode, detail);
    }

    public boolean isRecalculateState() {
        if (type == Type.DELETE) {
            return false;
        } else if (type == Type.CREATE) {
            return true;
        } else if (type == Type.UPDATE) {
            return detail.isReranked();
        }
        return false;
    }

    public boolean isRerankOnly() {
        if (type == Type.UPDATE) {
            if (detail.isReranked()) {
                return detail.getComponents() == null && detail.getIssueType() == null && detail.getAssignee() == null &&
                        detail.getPriority() == null && detail.getState() == null && detail.getSummary() == null;

            }
        }
        return false;
    }

    public static class Detail {
        private final String issueType;
        private final String priority;
        private final String summary;
        private final ApplicationUser assignee;
        private final Collection<ProjectComponent> components;
        private final Collection<Label> labels;
        private final Collection<Version> fixVersions;
        private final String oldState;
        private final String state;
        private final boolean reranked;
        private final Map<Long, String> customFieldValues;

        private Detail(String issueType, String priority, String summary, ApplicationUser assignee,
                       Collection<ProjectComponent> components, Collection<Label> labels, Collection<Version> fixVersions,
                       String oldState, String state, boolean reranked, Map<Long, String> customFieldValues) {
            this.summary = summary;
            this.assignee = assignee;
            this.components = components;
            this.labels = labels;
            this.fixVersions = fixVersions;
            this.issueType = issueType;
            this.priority = priority;
            this.oldState = oldState;
            this.state = state;
            this.reranked = reranked;
            this.customFieldValues = customFieldValues != null ? customFieldValues : Collections.emptyMap();
        }

        public String getIssueType() {
            return issueType;
        }

        public String getPriority() {
            return priority;
        }

        public String getSummary() {
            return summary;
        }

        public ApplicationUser getAssignee() {
            return assignee;
        }

        public Collection<ProjectComponent> getComponents() {
            return components;
        }

        public Collection<Label> getLabels() {
            return labels;
        }

        public Collection<Version> getFixVersions() {
            return fixVersions;
        }

        public String getOldState() {
            return oldState;
        }

        public String getState() {
            return state;
        }

        public boolean isReranked() {
            return reranked;
        }

        public Map<Long, String> getCustomFieldValues() {
            return customFieldValues;
        }
    }

    public enum Type {
        /** The issue was created */
        CREATE,
        /** The issue was updated */
        UPDATE,
        /** The issue was deleted */
        DELETE
    }

    /**
     * Used during an update event to indicate that the assignee was set to unassigned
     */
    public static final ApplicationUser UNASSIGNED = new ApplicationUser() {
        public long getDirectoryId() {
            return 0;
        }

        public boolean isActive() {
            return false;
        }

        public String getEmailAddress() {
            return "";
        }

        public String getDisplayName() {
            return Constants.UNASSIGNED;
        }

        public String getName() {
            return Constants.UNASSIGNED;
        }

        @Override
        public String getKey() {
            return Constants.UNASSIGNED;
        }

        @Override
        public String getUsername() {
            return Constants.UNASSIGNED;
        }

        @Override
        public User getDirectoryUser() {
            return null;
        }

        @Override
        public Long getId() {
            return -1L;
        }
    };
}

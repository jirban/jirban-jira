/*
 *
 *  JBoss, Home of Professional Open Source
 *  Copyright 2016, Red Hat, Inc., and individual contributors as indicated
 *  by the @authors tag.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.jirban.jira.impl.board;

import static org.jirban.jira.impl.Constants.ASSIGNEE;
import static org.jirban.jira.impl.Constants.CUSTOM;
import static org.jirban.jira.impl.Constants.KEY;
import static org.jirban.jira.impl.Constants.LINKED_ISSUES;
import static org.jirban.jira.impl.Constants.PRIORITY;
import static org.jirban.jira.impl.Constants.STATE;
import static org.jirban.jira.impl.Constants.SUMMARY;
import static org.jirban.jira.impl.Constants.TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.Constants;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.LinkedProjectConfig;
import org.jirban.jira.impl.config.ProjectConfig;

import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;

/**
 * The data for an issue on the board
 *
 * @author Kabir Khan
 */
public abstract class Issue {

    private final ProjectConfig project;
    private final String key;
    private final String state;
    private final Integer stateIndex;
    private final String summary;

    Issue(ProjectConfig project, String key, String state, Integer stateIndex, String summary) {
        this.project = project;
        this.key = key;
        this.state = state;
        this.stateIndex = stateIndex;
        this.summary = summary;
    }

    public String getKey() {
        return key;
    }

    public String getState() {
        return state;
    }

    public String getSummary() {
        return summary;
    }

    public String getProjectCode() {
        return project.getCode();
    }

    Integer getStateIndex() {
        return stateIndex;
    }

    boolean hasLinkedIssues() {
        return false;
    }

    Iterable<LinkedIssue> getLinkedIssues() {
        return () -> Collections.<LinkedIssue>emptySet().iterator();
    }

    ModelNode getModelNodeForFullRefresh(Board board) {
        ModelNode issueNode = getBaseModelNode();
        return issueNode;
    }

    private ModelNode getBaseModelNode() {
        ModelNode issueNode = new ModelNode();
        issueNode.get(KEY).set(key);
        issueNode.get(STATE).set(project.getStateIndex(state));
        issueNode.get(SUMMARY).set(summary);
        return issueNode;
    }

    /**
     * Returns a builder for the board issues during a full load of the board. Linked issues are handled internally.
     *
     * @param project the builder for the project containing the issues
     * @return the builder
     */
    static Builder builder(BoardProject.Accessor project) {
        return new Builder(project);
    }

    /**
     * Creates a new issue. If the issue is for a state, priority or issue type not configured,
     * {@code null} will be returned, having updated the 'missing' maps in Board.
     *
     * @param project the project the issue belongs to
     * @param issueKey the issue's key
     * @param state the issue's state id
     * @param summary the issue summary
     * @param issueType the issue's type
     * @param priority the priority
     * @param assignee the assignee
     * @param components the component
     * @param customFieldValues
     * @return the issue
     */
    static Issue createForCreateEvent(BoardProject.Accessor project, String issueKey, String state,
                                      String summary, String issueType, String priority, Assignee assignee,
                                      Set<Component> components, Map<Long, String> customFieldValues) {
        Builder builder = new Builder(project, issueKey);
        builder.setState(state);
        builder.setSummary(summary);
        builder.setIssueType(issueType);
        builder.setPriority(priority);
        builder.setAssignee(assignee);
        builder.setComponents(components);
        builder.setCustomFieldValues(customFieldValues);

        //TODO linked issues
        return builder.build();
    }

    /**
     * Creates a new issue based on an {@code existing} one. The data is then updated with the results of
     * {@code issueType}, {@code priority}, {@code summary}, {@code issueAssignee}, {@code rankOrStateChanged}
     * {@code state} if they are different from the current issue detail. Note that the update event might be raised
     * for fields we are not interested in, in which case we don't care about the change, and return {@code null}.If
     * the issue is for a state, priority or issue type not configured, {@code null} will be returned, having
     * updated the 'missing' maps in Board.
     *
     * @param project the project the issue belongs to
     * @param existing the issue to update
     * @param issueType the new issue type
     * @param priority the new issue priority
     * @param summary the new issue summary
     * @param issueAssignee the new issue assignee
     * @param issueComponents the new issue components
     *@param state the state of the issue  @return the new issue
     */
    static Issue copyForUpdateEvent(BoardProject.Accessor project, Issue existing, String issueType, String priority,
                                    String summary, Assignee issueAssignee, Set<Component> issueComponents, String state) {
        if (existing instanceof BoardIssue == false) {
            return null;
        }
        return copyForUpdateEvent(project, (BoardIssue)existing, issueType, priority, summary, issueAssignee, issueComponents, state);
    }

    private static Issue copyForUpdateEvent(BoardProject.Accessor project, BoardIssue existing, String issueType, String priority,
                                    String summary, Assignee issueAssignee, Set<Component> issueComponents, String state) {
        Builder builder = new Builder(project, existing);
        boolean changed = false;
        if (issueType != null) {
            builder.setIssueType(issueType);
            changed = true;
        }
        if (priority != null) {
            builder.setPriority(priority);
            changed = true;
        }
        if (summary != null) {
            builder.setSummary(summary);
            changed = true;
        }
        if (issueAssignee != null) {
            //A non-null assignee means it was updated, either to an assignee or unassigned.
            if (issueAssignee == Assignee.UNASSIGNED) {
                builder.setAssignee(null);
                changed = true;
            } else {
                builder.setAssignee(issueAssignee);
                changed = true;
            }
        }
        if (issueComponents != null) {
            //A non-null component means it was updated.
            if (issueComponents.size() == 0) {
                builder.setComponents(null);
                changed = true;
            } else {
                builder.setComponents(issueComponents);
                changed = true;
            }
        }
        if (state != null) {
            changed = true;
            builder.setState(state);
        }
        if (changed) {
            return builder.build();
        }
        return null;
    }

    abstract BoardChangeRegistry.IssueChange convertToCreateIssueChange(BoardChangeRegistry registry, BoardConfig boardConfig);

    private static class BoardIssue extends Issue {
        private final Assignee assignee;
        private final Set<Component> components;
        /** The index of the issue type in the owning board config */
        private final Integer issueTypeIndex;
        /** The index of the priority in the owning board config */
        private final Integer priorityIndex;
        private final List<LinkedIssue> linkedIssues;
        private final Map<String, CustomFieldValue> customFields;

        public BoardIssue(BoardProjectConfig project, String key, String state, Integer stateIndex, String summary,
                          Integer issueTypeIndex, Integer priorityIndex, Assignee assignee,
                          Set<Component> components, List<LinkedIssue> linkedIssues,
                          Map<String, CustomFieldValue> customFields) {
            super(project, key, state, stateIndex, summary);
            this.issueTypeIndex = issueTypeIndex;
            this.priorityIndex = priorityIndex;
            this.assignee = assignee;
            this.components = components;
            this.linkedIssues = linkedIssues;
            this.customFields = customFields;
        }

        boolean hasLinkedIssues() {
            return linkedIssues.size() > 0;
        }

        Iterable<LinkedIssue> getLinkedIssues() {
            return linkedIssues::iterator;
        }

        @Override
        ModelNode getModelNodeForFullRefresh(Board board) {
            BoardProject boardProject = board.getBoardProject(getProjectCode());
            ModelNode issueNode = super.getModelNodeForFullRefresh(board);
            issueNode.get(PRIORITY).set(priorityIndex);
            issueNode.get(TYPE).set(issueTypeIndex);
            if (assignee != null) {
                //This map will always be populated
                issueNode.get(ASSIGNEE).set(boardProject.getAssigneeIndex(assignee));
            }
            if (components != null) {
                for (Component component : components) {
                    //This map will always be populated
                    issueNode.get(Constants.COMPONENTS).add(boardProject.getComponentIndex(component));
                }
            }
            if (customFields.size() > 0) {
                ModelNode custom = issueNode.get(CUSTOM);
                for (CustomFieldValue customFieldValue : customFields.values()) {
                    custom.get(customFieldValue.getCustomFieldName()).set(boardProject.getCustomFieldValueIndex(customFieldValue));
                }
            }
            if (hasLinkedIssues()) {
                ModelNode linkedIssuesNode = issueNode.get(LINKED_ISSUES);
                for (Issue linkedIssue : linkedIssues) {
                    ModelNode linkedIssueNode = linkedIssue.getModelNodeForFullRefresh(board);
                    linkedIssuesNode.add(linkedIssueNode);
                }
            }
            return issueNode;
        }

        @Override
        BoardChangeRegistry.IssueChange convertToCreateIssueChange(BoardChangeRegistry registry, BoardConfig boardConfig) {
            String issueType = boardConfig.getIssueTypeName(issueTypeIndex);
            String priority = boardConfig.getPriorityName(priorityIndex);
            return registry.createCreateIssueChange(this, assignee, issueType, priority, components);
        }
    }

    private static class LinkedIssue extends Issue {
        public LinkedIssue(LinkedProjectConfig project, String key, String state, Integer stateIndex, String summary) {
            super(project, key, state, stateIndex, summary);
        }

        @Override
        BoardChangeRegistry.IssueChange convertToCreateIssueChange(BoardChangeRegistry registry, BoardConfig boardConfig) {
            throw new IllegalStateException("Not for linked issues");
        }
    }

    /**
     * The builder for the board issues
     */
    static class Builder {
        private final BoardProject.Accessor project;

        String issueKey;
        String summary;
        Assignee assignee;
        Set<Component> components;
        Integer issueTypeIndex;
        Integer priorityIndex;
        String state;
        Integer stateIndex;
        Set<LinkedIssue> linkedIssues;
        Map<String, CustomFieldValue> customFieldValues;

        private Builder(BoardProject.Accessor project) {
            this.project = project;
            this.issueKey = null;
        }

        private Builder(BoardProject.Accessor project, String issueKey) {
            this.project = project;
            this.issueKey = issueKey;
        }

        private Builder(BoardProject.Accessor project, BoardIssue existing) {
            this.project = project;
            this.issueKey = existing.getKey();
            this.summary = existing.getSummary();
            this.assignee = existing.assignee;
            this.components = existing.components;
            this.issueTypeIndex = existing.issueTypeIndex;
            this.priorityIndex = existing.priorityIndex;
            this.state = existing.getState();
            this.stateIndex = existing.getStateIndex();
            if (existing.linkedIssues.size() > 0) {
                Set<LinkedIssue> linkedIssues = createLinkedIssueSet();
                linkedIssues.addAll(existing.linkedIssues);
                this.linkedIssues = Collections.unmodifiableSet(linkedIssues);
            } else {
                this.linkedIssues = Collections.emptySet();
            }
        }

        void load(com.atlassian.jira.issue.Issue issue) {
            issueKey = issue.getKey();
            summary = issue.getSummary();
            assignee = project.getAssignee(issue.getAssignee());
            components = project.getComponents(issue.getComponentObjects());
            setIssueType(issue.getIssueTypeObject().getName());
            setPriority(issue.getPriorityObject().getName());
            setState(issue.getStatusObject().getName());

            customFieldValues =
                    CustomFieldValue.loadCustomFields(project, issue);

            final IssueLinkManager issueLinkManager = project.getIssueLinkManager();
            addLinkedIssues(issueLinkManager.getOutwardLinks(issue.getId()), true);
            addLinkedIssues(issueLinkManager.getInwardLinks(issue.getId()), false);
        }

        private Builder setIssueKey(String issueKey) {
            this.issueKey = issueKey;
            return this;
        }

        private Builder setSummary(String summary) {
            this.summary = summary;
            return this;
        }

        private Builder setAssignee(Assignee assignee) {
            this.assignee = assignee == Assignee.UNASSIGNED ? null : assignee;
            return this;
        }

        private Builder setComponents(Set<Component> components) {
            if (components == null || components.size() == 0) {
                this.components = null;
            } else {
                this.components = components;
            }
            return this;
        }

        private Builder setIssueType(String issueTypeName) {
            this.issueTypeIndex = project.getIssueTypeIndexRecordingMissing(issueKey, issueTypeName);
            return this;
        }

        private Builder setPriority(String priorityName) {
            this.priorityIndex = project.getPriorityIndexRecordingMissing(issueKey, priorityName);
            return this;
        }

        private Builder setState(String stateName) {
            state = stateName;
            stateIndex = project.getStateIndexRecordingMissing(issueKey, state);
            return this;
        }

        private void addLinkedIssues(List<IssueLink> links, boolean outbound) {
            if (links == null) {
                return;
            }
            if (links.size() == 0) {
                return;
            }
            for (IssueLink link : links) {
                com.atlassian.jira.issue.Issue linkedIssue = outbound ? link.getDestinationObject() : link.getSourceObject();
                String linkedProjectKey = linkedIssue.getProjectObject().getKey();
                BoardProject.LinkedProjectContext linkedProjectContext = project.getLinkedProjectContext(linkedProjectKey);
                if (linkedProjectContext == null) {
                    //This was not set up as one of the linked projects we are interested in
                    continue;
                }
                String stateName = linkedIssue.getStatusObject().getName();
                Integer stateIndex = linkedProjectContext.getStateIndexRecordingMissing(linkedProjectContext.getCode(), linkedIssue.getKey(), stateName);
                if (stateIndex != null) {
                    if (linkedIssues == null) {
                        linkedIssues = createLinkedIssueSet();
                    }
                    linkedIssues.add(new LinkedIssue(linkedProjectContext.getConfig(), linkedIssue.getKey(),
                            stateName, stateIndex, linkedIssue.getSummary()));
                }
            }
        }

        private TreeSet<LinkedIssue> createLinkedIssueSet() {
            return new TreeSet<>(new Comparator<LinkedIssue>() {
                @Override
                public int compare(LinkedIssue o1, LinkedIssue o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });
        }

        Issue build() {
            if (issueTypeIndex != null && priorityIndex != null && stateIndex != null) {
                List<LinkedIssue> linkedList = linkedIssues == null ?
                        Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(linkedIssues));
                return new BoardIssue(
                        project.getConfig(), issueKey, state, stateIndex, summary,
                        issueTypeIndex, priorityIndex, assignee, components, linkedList,
                        customFieldValues == null ? Collections.emptyMap() : Collections.unmodifiableMap(customFieldValues));
            }
            return null;
        }

        public Builder setCustomFieldValues(Map<Long, String> customFieldValues) {
            if (customFieldValues != null) {
                this.customFieldValues = CustomFieldValue.loadCustomFields(project, customFieldValues);
            }
            return this;
        }
    }
 }

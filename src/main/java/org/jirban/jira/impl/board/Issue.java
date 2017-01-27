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

package org.jirban.jira.impl.board;

import static org.jirban.jira.impl.Constants.ASSIGNEE;
import static org.jirban.jira.impl.Constants.CUSTOM;
import static org.jirban.jira.impl.Constants.KEY;
import static org.jirban.jira.impl.Constants.LINKED_ISSUES;
import static org.jirban.jira.impl.Constants.PARALLEL_TASKS;
import static org.jirban.jira.impl.Constants.PRIORITY;
import static org.jirban.jira.impl.Constants.STATE;
import static org.jirban.jira.impl.Constants.SUMMARY;
import static org.jirban.jira.impl.Constants.TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.Constants;
import org.jirban.jira.impl.board.MultiSelectNameOnlyValue.Component;
import org.jirban.jira.impl.board.MultiSelectNameOnlyValue.FixVersion;
import org.jirban.jira.impl.board.MultiSelectNameOnlyValue.Label;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.LinkedProjectConfig;
import org.jirban.jira.impl.config.ParallelTaskConfig;
import org.jirban.jira.impl.config.ProjectConfig;

import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.util.Consumer;

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
    static Builder builder(BoardProject.Accessor project, IssueLoadStrategy issueLoadStrategy) {
        return new Builder(project, issueLoadStrategy);
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
     * @param components the components
     * @param labels the labels
     * @param fixVersions the fix versions
     * @param customFieldValues the custom field values
     * @param parallelTaskValues the parallel task values
     * @return the issue
     */
    static Issue createForCreateEvent(BoardProject.Accessor project, String issueKey, String state,
                                      String summary, String issueType, String priority, Assignee assignee,
                                      Set<Component> components, Set<Label> labels, Set<FixVersion> fixVersions,
                                      Map<String, CustomFieldValue> customFieldValues,
                                      Map<Integer, Integer> parallelTaskValues) {
        Builder builder = new Builder(project, issueKey);
        builder.setState(state);
        builder.setSummary(summary);
        builder.setIssueType(issueType);
        builder.setPriority(priority);
        builder.setAssignee(assignee);
        builder.setComponents(components);
        builder.setLabels(labels);
        builder.setFixVersions(fixVersions);
        builder.setCustomFieldValues(customFieldValues);
        builder.setParallelTaskFieldValues(parallelTaskValues);

        //TODO parallel task values

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
     * @param project the project the issue belongs to
     * @param existing the issue to update
     * @param issueType the new issue type
     * @param priority the new issue priority
     * @param summary the new issue summary
     * @param issueAssignee the new issue assignee
     * @param issueComponents the new issue components
     * @param labels the new issue labels
     * @param fixVersions the new issue fix versions
     * @param state the state of the issue  @return the new issue
     * @param customFieldValues the custom field values
     * @param parallelTaskValues the parallel task values
     */
    static Issue copyForUpdateEvent(BoardProject.Accessor project, Issue existing, String issueType, String priority,
                                    String summary, Assignee issueAssignee, Set<Component> issueComponents,
                                    Set<Label> labels, Set<FixVersion> fixVersions,
                                    String state, Map<String, CustomFieldValue> customFieldValues,
                                    Map<Integer, Integer> parallelTaskValues) {
        if (existing instanceof BoardIssue == false) {
            return null;
        }
        return copyForUpdateEvent(project, (BoardIssue)existing, issueType, priority,
                summary, issueAssignee, issueComponents, labels, fixVersions, state,
                customFieldValues, parallelTaskValues);
    }

    private static Issue copyForUpdateEvent(BoardProject.Accessor project, BoardIssue existing, String issueType, String priority,
                                            String summary, Assignee issueAssignee, Set<Component> issueComponents,
                                            Set<Label> labels, Set<FixVersion> fixVersions,
                                            String state, Map<String, CustomFieldValue> customFieldValues,
                                            Map<Integer, Integer> parallelTaskValues) {
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
        if (labels != null) {
            //A non-null labels means it was updated
            if (labels.size() == 0) {
                builder.setLabels(null);
                changed = true;
            } else {
                builder.setLabels(labels);
                changed = true;
            }
        }
        if (fixVersions != null) {
            //A non-null labels means it was updated
            if (fixVersions.size() == 0) {
                builder.setFixVersions(null);
                changed = true;
            } else {
                builder.setFixVersions(fixVersions);
                changed = true;
            }
        }
        if (state != null) {
            changed = true;
            builder.setState(state);
        }
        if (customFieldValues.size() > 0) {
            changed = true;
            builder.setCustomFieldValues(customFieldValues);
        }
        if (parallelTaskValues.size() > 0) {
            changed = true;
            builder.setParallelTaskFieldValues(parallelTaskValues);
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
        private final Set<Label> labels;
        private final Set<FixVersion> fixVersions;
        /** The index of the issue type in the owning board config */
        private final Integer issueTypeIndex;
        /** The index of the priority in the owning board config */
        private final Integer priorityIndex;
        private final List<LinkedIssue> linkedIssues;
        private final Map<String, CustomFieldValue> customFieldValues;
        private final List<Integer> parallelTaskFieldValues;

        public BoardIssue(BoardProjectConfig project, String key, String state, Integer stateIndex, String summary,
                          Integer issueTypeIndex, Integer priorityIndex, Assignee assignee,
                          Set<Component> components, Set<Label> labels, Set<FixVersion> fixVersions,
                          List<LinkedIssue> linkedIssues,
                          Map<String, CustomFieldValue> customFieldValues, List<Integer> parallelTaskFieldValues) {
            super(project, key, state, stateIndex, summary);
            this.issueTypeIndex = issueTypeIndex;
            this.priorityIndex = priorityIndex;
            this.assignee = assignee;
            this.components = components;
            this.labels = labels;
            this.fixVersions = fixVersions;
            this.linkedIssues = linkedIssues;
            this.customFieldValues = customFieldValues;
            this.parallelTaskFieldValues = parallelTaskFieldValues;
        }

        boolean hasLinkedIssues() {
            return linkedIssues.size() > 0;
        }

        Iterable<LinkedIssue> getLinkedIssues() {
            return linkedIssues::iterator;
        }

        @Override
        ModelNode getModelNodeForFullRefresh(Board board) {
            final BoardProject boardProject = board.getBoardProject(getProjectCode());
            final ModelNode issueNode = super.getModelNodeForFullRefresh(board);
            issueNode.get(PRIORITY).set(priorityIndex);
            issueNode.get(TYPE).set(issueTypeIndex);
            if (assignee != null) {
                //This map will always be populated
                issueNode.get(ASSIGNEE).set(boardProject.getAssigneeIndex(assignee));
            }
            if (components != null) {
                components.forEach(component -> issueNode.get(Constants.COMPONENTS).add(boardProject.getComponentIndex(component)));
            }
            if (labels != null) {
                labels.forEach(label -> issueNode.get(Constants.LABELS).add(boardProject.getLabelIndex(label)));
            }
            if (fixVersions != null) {
                fixVersions.forEach(fixVersion -> issueNode.get(Constants.FIX_VERSIONS).add(boardProject.getFixVersionIndex(fixVersion)));
            }
            if (customFieldValues.size() > 0) {
                final ModelNode custom = issueNode.get(CUSTOM);
                customFieldValues.values().forEach(
                        customFieldValue -> custom.get(customFieldValue.getCustomFieldName()).set(boardProject.getCustomFieldValueIndex(customFieldValue)));
            }
            if (parallelTaskFieldValues != null) {
                final ModelNode parallel = issueNode.get(PARALLEL_TASKS).setEmptyList();
                parallelTaskFieldValues.forEach(value -> parallel.add(value));
            }

            if (hasLinkedIssues()) {
                final ModelNode linkedIssuesNode = issueNode.get(LINKED_ISSUES);
                linkedIssues.forEach(linkedIssue -> linkedIssuesNode.add(linkedIssue.getModelNodeForFullRefresh(board)));
            }

            return issueNode;
        }

        @Override
        BoardChangeRegistry.IssueChange convertToCreateIssueChange(BoardChangeRegistry registry, BoardConfig boardConfig) {
            String issueType = boardConfig.getIssueTypeName(issueTypeIndex);
            String priority = boardConfig.getPriorityName(priorityIndex);
            return registry.createCreateIssueChange(this, assignee, issueType, priority, components, labels, fixVersions);
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

        private final IssueLoadStrategy issueLoadStrategy;
        private String issueKey;
        private String summary;
        private Assignee assignee;
        private Set<Component> components;
        private Set<Label> labels;
        private Set<FixVersion> fixVersions;
        private Integer issueTypeIndex;
        private Integer priorityIndex;
        private String state;
        private Integer stateIndex;
        private Set<LinkedIssue> linkedIssues;
        //Will only be set for an update
        private Map<String, CustomFieldValue> originalCustomFieldValues;
        private Map<String, CustomFieldValue> customFieldValues;

        //Will only be set for an update
        private List<Integer> originalParallelTaskValues;
        private Integer[] parallelTaskValues;

        private Builder(BoardProject.Accessor project, IssueLoadStrategy issueLoadStrategy) {
            this.project = project;
            this.issueKey = null;
            this.issueLoadStrategy = issueLoadStrategy == null ? new LazyLoadStrategy(project) : issueLoadStrategy;
        }

        private Builder(BoardProject.Accessor project, String issueKey) {
            //Used when handling a create event for an issue
            this.project = project;
            this.issueKey = issueKey;
            this.issueLoadStrategy = new LazyLoadStrategy(project);
        }

        private Builder(BoardProject.Accessor project, BoardIssue existing) {
            //Used when handling an update event for an issue
            this.project = project;
            this.issueKey = existing.getKey();
            this.issueLoadStrategy = new LazyLoadStrategy(project);
            this.summary = existing.getSummary();
            this.assignee = existing.assignee;
            this.components = existing.components;
            this.labels = existing.labels;
            this.fixVersions = existing.fixVersions;
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
            this.originalCustomFieldValues = existing.customFieldValues;
            this.originalParallelTaskValues = existing.parallelTaskFieldValues;
        }

        void load(com.atlassian.jira.issue.Issue issue) {
            issueKey = issue.getKey();
            summary = issue.getSummary();
            assignee = project.getAssignee(issue.getAssignee());
            components = project.getComponents(issue.getComponentObjects());
            labels = project.getLabels(issue.getLabels());
            fixVersions = project.getFixVersions(issue.getFixVersions());
            setIssueType(issue.getIssueTypeObject().getName());
            setPriority(issue.getPriorityObject().getName());
            setState(issue.getStatusObject().getName());

            //Load the custom fields
            issueLoadStrategy.handle(issue, this);

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
            return setMultiSelectNameOnlyValue(components, set -> this.components = set);
        }

        private Builder setLabels(Set<Label> labels) {
            return setMultiSelectNameOnlyValue(labels, set -> this.labels = set);
        }

        private Builder setFixVersions(Set<FixVersion> fixVersions) {
            return setMultiSelectNameOnlyValue(fixVersions, set -> this.fixVersions = set);
        }

        private <T extends MultiSelectNameOnlyValue> Builder setMultiSelectNameOnlyValue(
                Set<T> values, Consumer<Set<T>> consumer) {
            if (values == null || values.size() == 0) {
                consumer.consume(null);
            } else {
                consumer.consume(values);
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
            issueLoadStrategy.finish();
            if (issueTypeIndex != null && priorityIndex != null && stateIndex != null) {
                List<LinkedIssue> linkedList = linkedIssues == null ?
                        Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(linkedIssues));

                //Map<String, CustomFieldValue> builtCustomFieldValues
                return new BoardIssue(
                        project.getConfig(), issueKey, state, stateIndex, summary,
                        issueTypeIndex, priorityIndex, assignee, components,
                        labels, fixVersions,
                        linkedList,
                        mergeCustomFieldValues(), mergeParallelTaskFieldValues());
            }
            return null;
        }

        private Map<String, CustomFieldValue> mergeCustomFieldValues() {
            if (originalCustomFieldValues == null) {
                //We are creating a new issue
                return customFieldValues == null ? Collections.emptyMap() : Collections.unmodifiableMap(customFieldValues);
            } else {
                if (customFieldValues == null) {
                    return originalCustomFieldValues;
                }
                Map<String, CustomFieldValue> merged = new HashMap<>(originalCustomFieldValues);
                customFieldValues.entrySet().forEach(entry -> {
                    if (entry.getValue() == null) {
                        merged.remove(entry.getKey());
                    } else {
                        merged.put(entry.getKey(), entry.getValue());
                    }});

                return Collections.unmodifiableMap(merged);
            }
        }

        private List<Integer> mergeParallelTaskFieldValues() {
            if (originalParallelTaskValues == null) {
                //We are creating a new issue
                initialiseParallelTaskValues();
                if (parallelTaskValues == null) {
                    return null;
                }
                List<Integer> values = new ArrayList<>();
                for (int i = 0 ; i < parallelTaskValues.length ; i++) {
                    Integer val = parallelTaskValues[i];
                    if (val == null) {
                        val = 0;
                    }
                    values.add(val);
                }
                return Collections.unmodifiableList(values);
            } else {
                if (parallelTaskValues == null) {
                    return originalParallelTaskValues;
                }
                List<Integer> merged = new ArrayList<>();
                for (int i = 0 ; i < originalParallelTaskValues.size() ; i++) {
                    Integer newVal = parallelTaskValues[i];
                    if (newVal != null) {
                        merged.add(newVal);
                    } else {
                        merged.add(originalParallelTaskValues.get(i));
                    }
                }
                return Collections.unmodifiableList(merged);
            }
        }

        Builder setCustomFieldValues(Map<String, CustomFieldValue> customFieldValues) {
            if (customFieldValues != null) {
                this.customFieldValues = customFieldValues;
            }
            return this;
        }

        Builder addCustomFieldValue(CustomFieldValue value) {
            if (customFieldValues == null) {
                customFieldValues = new HashMap<>();
            }
            customFieldValues.put(value.getKey(), value);
            return this;
        }

        String getIssueKey() {
            return issueKey;
        }

        public Builder setParallelTaskFieldValue(int taskFieldIndex, int optionIndex) {
            initialiseParallelTaskValues();
            parallelTaskValues[taskFieldIndex] = optionIndex;
            return this;
        }

        private Builder setParallelTaskFieldValues(Map<Integer, Integer> parallelTaskValues) {
            parallelTaskValues.entrySet().forEach(value -> setParallelTaskFieldValue(value.getKey(), value.getValue()));
            return this;
        }

        private void initialiseParallelTaskValues() {
            if (parallelTaskValues == null) {
                ParallelTaskConfig parallelTaskConfig = project.getConfig().getParallelTaskConfig();
                if (parallelTaskConfig != null) {
                    parallelTaskValues = new Integer[parallelTaskConfig.getConfigs().size()];
                }
            }
        }

    }

    /**
     * <p>Loads up things like custom fields using the entities. This is done lazily for each issue, and is
     * fine when we are handling events to create or update entities.</p>
     * <p>This is not suitable for loading the full board, since the lazy loading results in an extra sql
     * query behind the scenes for every single custom field, for every single issue. When loading the full board
     * we instead do a bulk load to avoid this performance overhead.</p>
     * <p>For unit tests we currently use the lazy loading mechanism to load the custom fields, this is mainly
     * to avoid having to set up the mocks at present.</p>
     */
    private static class LazyLoadStrategy implements IssueLoadStrategy {
        private final BoardProject.Accessor project;

        private LazyLoadStrategy(BoardProject.Accessor project) {
            this.project = project;
        }



        @Override
        public void handle(com.atlassian.jira.issue.Issue issue, Builder builder) {
            builder.setCustomFieldValues(CustomFieldValue.loadCustomFieldValues(project, issue));
            CustomFieldValue.loadParallelTaskValues(project, issue, builder);
        }

        @Override
        public void finish() {

        }
    }
 }

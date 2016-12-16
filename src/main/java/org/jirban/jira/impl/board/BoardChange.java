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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jirban.jira.impl.JirbanIssueEvent;
import org.jirban.jira.impl.board.MultiSelectNameOnlyValue.Component;
import org.jirban.jira.impl.board.MultiSelectNameOnlyValue.FixVersion;
import org.jirban.jira.impl.board.MultiSelectNameOnlyValue.Label;

/**
 * Contains the details of a change to the board, which the clients will apply when polling for changes since their
 * current view id
 *
 * @author Kabir Khan
 */
public class BoardChange {
    //The time of the change
    private final long time = System.currentTimeMillis();

    //The view id following the change
    private final int view;

    //The event, containing the issue id etc.
    private final JirbanIssueEvent event;

    //The new assignee, if the change brings in an assignee not currently on the board
    private final Assignee newAssignee;

    //The new components, labels and fixVersions if the change brings in some not currently on the board
    private final Set<Component> newComponents;
    private final Set<Label> newLabels;
    private final Set<FixVersion> newFixVersions;

    //If the blacklist was modified. We are mainly interested in if a new unmapped state/priority/type pops up, and
    //its associated issue. Also, if an issue is deleted, it should be removed from the blacklist.
    private final String addedBlacklistState;
    private final String addedBlacklistPriority;
    private final String addedBlacklistIssueType;
    private final String addedBlacklistIssue;
    private final String deletedBlacklistIssue;


    //Whether or not the issue state is a backlog state (may be null in case the change doesn't do anything to an issue, just the
    private final Boolean backlogState;
    private final Map<String, CustomFieldValue> customFieldValues;
    private final Map<String, CustomFieldValue> newCustomFieldValues;
    private final Map<Integer, Integer> parallelTaskValues;
    private final Boolean fromBacklogState;


    private BoardChange(int view, JirbanIssueEvent event, Assignee newAssignee,
                        Set<Component> newComponents, Set<Label> newLabels,
                        Set<FixVersion> newFixVersions, String addedBlacklistState,
                        String addedBlacklistPriority, String addedBlacklistIssueType,
                        String addedBlacklistIssue, String deletedBlacklistIssue,
                        Boolean fromBacklogState, Boolean backlogState,
                        Map<String, CustomFieldValue> customFieldValues,
                        Map<String, CustomFieldValue> newCustomFieldValues,
                        Map<Integer, Integer> parallelTaskValues) {
        this.view = view;
        this.event = event;
        this.newAssignee = newAssignee;
        this.newComponents = newComponents;
        this.newLabels = newLabels;
        this.newFixVersions = newFixVersions;
        this.addedBlacklistState = addedBlacklistState;
        this.addedBlacklistPriority = addedBlacklistPriority;
        this.addedBlacklistIssueType = addedBlacklistIssueType;
        this.addedBlacklistIssue = addedBlacklistIssue;
        this.deletedBlacklistIssue = deletedBlacklistIssue;
        this.fromBacklogState = fromBacklogState;
        this.backlogState = backlogState;
        this.customFieldValues = customFieldValues;
        this.newCustomFieldValues = newCustomFieldValues;
        this.parallelTaskValues = parallelTaskValues;
    }

    long getTime() {
        return time;
    }

    int getView() {
        return view;
    }

    JirbanIssueEvent getEvent() {
        return event;
    }

    Assignee getNewAssignee() {
        return newAssignee;
    }

    Set<Component> getNewComponents() {
        return newComponents;
    }

    Set<Label> getNewLabels() {
        return newLabels;
    }

    Set<FixVersion> getNewFixVersions() {
        return newFixVersions;
    }

    String getAddedBlacklistState() {
        return addedBlacklistState;
    }

    String getAddedBlacklistPriority() {
        return addedBlacklistPriority;
    }

    String getAddedBlacklistIssueType() {
        return addedBlacklistIssueType;
    }

    String getAddedBlacklistIssue() {
        return addedBlacklistIssue;
    }

    String getDeletedBlacklistIssue() {
        return deletedBlacklistIssue;
    }

    boolean isBlacklistEvent() {
        return addedBlacklistIssue != null || deletedBlacklistIssue != null;
    }

    public Boolean getBacklogState() {
        return backlogState;
    }

    public Boolean getFromBacklogState() {
        return fromBacklogState;
    }

    public Map<String, CustomFieldValue> getCustomFieldValues() {
        return customFieldValues;
    }

    public Map<String, CustomFieldValue> getNewCustomFieldValues() {
        return newCustomFieldValues;
    }

    public Map<Integer, Integer> getParallelTaskValues() {
        return parallelTaskValues;
    }

    public static class Builder {
        private final BoardChangeRegistry registry;
        private final int view;
        private final JirbanIssueEvent event;

        //The new assignee if one was brought in
        private Assignee newAssignee;

        //The new components, labels and fixVersions if any were brought in
        private Set<Component> newComponents;
        private Set<Label> newLabels;
        private Set<FixVersion> newFixVersions;

        private Map<String, CustomFieldValue> newCustomFieldValues;

        //If the blacklist was changed
        private String addedBlacklistState;
        private String addedBlacklistPriority;
        private String addedBlacklistIssueType;
        private String addedBlacklistIssue;
        private String deletedBlacklistIssue;

        //If the state was recalculated
        private Boolean fromBacklogState;
        private Boolean backlogState;
        private Map<String, CustomFieldValue> customFieldValues;
        private Map<Integer, Integer> parallelTaskValues;

        Builder(BoardChangeRegistry registry, int view, JirbanIssueEvent event) {
            this.registry = registry;
            this.view = view;
            this.event = event;
        }

        public Builder addNewAssignee(Assignee newAssignee) {
            this.newAssignee = newAssignee;
            return this;
        }

        public Builder addBlacklist(String addedState, String addedIssueType, String addedPriority, String addedIssue) {
            addedBlacklistState = addedState;
            addedBlacklistIssueType = addedIssueType;
            addedBlacklistPriority = addedPriority;
            addedBlacklistIssue = addedIssue;
            return this;
        }

        public Builder deleteBlacklist(String deletedIssue) {
            deletedBlacklistIssue = deletedIssue;
            return this;
        }

        public Builder addNewComponents(Set<Component> newComponents) {
            this.newComponents = Collections.unmodifiableSet(newComponents);
            return this;
        }

        public Builder addNewLabels(Set<Label> newLabels) {
            this.newLabels = Collections.unmodifiableSet(newLabels);
            return this;
        }

        public Builder addNewFixVersions(Set<FixVersion> newFixVersions) {
            this.newFixVersions = Collections.unmodifiableSet(newFixVersions);
            return this;
        }

        public Builder setFromBacklogState(boolean fromBacklogState) {
            this.fromBacklogState = fromBacklogState;
            return this;
        }

        public Builder setBacklogState(boolean backlogState) {
            this.backlogState = backlogState;
            return this;
        }

        public Builder addCustomFieldValues(
                Map<String, SortedCustomFieldValues> originalCustomFieldValues,
                Map<String, CustomFieldValue> customFieldValues) {

            Map<String, CustomFieldValue> newValues = new HashMap<>();
            if (customFieldValues.size() > 0) {
                for (Map.Entry<String, CustomFieldValue> change : customFieldValues.entrySet()) {
                    SortedCustomFieldValues existingForField = originalCustomFieldValues.get(change.getKey());
                    CustomFieldValue value = change.getValue();
                    if (existingForField == null ||
                            (value != null && existingForField.getCustomFieldValue(value.getKey()) == null)) {
                        newValues.put(change.getKey(), change.getValue());
                    }
                }
                this.customFieldValues = Collections.unmodifiableMap(customFieldValues);
            }
            if (newValues.size() > 0) {
                this.newCustomFieldValues = Collections.unmodifiableMap(newValues);
            }
            return this;
        }

        public Builder setParallelTaskValues(Map<Integer, Integer> parallelTaskValues) {
            this.parallelTaskValues = Collections.unmodifiableMap(parallelTaskValues);
            return this;
        }


        public void buildAndRegister() {
            BoardChange change = new BoardChange(
                    view, event, newAssignee, newComponents, newLabels, newFixVersions, addedBlacklistState,
                    addedBlacklistPriority, addedBlacklistIssueType, addedBlacklistIssue, deletedBlacklistIssue,
                    fromBacklogState, backlogState, customFieldValues, newCustomFieldValues,
                    parallelTaskValues);
            registry.registerChange(change);
        }

    }
}

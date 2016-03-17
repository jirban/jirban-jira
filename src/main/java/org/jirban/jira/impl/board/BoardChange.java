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
package org.jirban.jira.impl.board;

import java.util.Set;

import org.jirban.jira.impl.JirbanIssueEvent;

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

    //The new components, if the change brings in some not currently on the board
    private final Set<Component> newComponents;

    //If the blacklist was modified. We are mainly interested in if a new unmapped state/priority/type pops up, and
    //its associated issue. Also, if an issue is deleted, it should be removed from the blacklist.
    private final String addedBlacklistState;
    private final String addedBlacklistPriority;
    private final String addedBlacklistIssueType;
    private final String addedBlacklistIssue;
    private final String deletedBlacklistIssue;

    //If the state was changed
    private final String changedState;

    //Whether or not the issue state is a backlog state (may be null in case the change doesn't do anything to an issue, just the
    private final Boolean backlogState;
    private final Boolean fromBacklogState;


    private BoardChange(int view, JirbanIssueEvent event, Assignee newAssignee, Set<Component> newComponents, String addedBlacklistState,
                        String addedBlacklistPriority, String addedBlacklistIssueType,
                        String addedBlacklistIssue, String deletedBlacklistIssue, String changedState,
                        Boolean fromBacklogState, Boolean backlogState) {
        this.view = view;
        this.event = event;
        this.newAssignee = newAssignee;
        this.newComponents = newComponents;
        this.addedBlacklistState = addedBlacklistState;
        this.addedBlacklistPriority = addedBlacklistPriority;
        this.addedBlacklistIssueType = addedBlacklistIssueType;
        this.addedBlacklistIssue = addedBlacklistIssue;
        this.deletedBlacklistIssue = deletedBlacklistIssue;
        this.changedState = changedState;
        this.fromBacklogState = fromBacklogState;
        this.backlogState = backlogState;
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

    String getChangedState() {
        return changedState;
    }

    public Boolean getBacklogState() {
        return backlogState;
    }

    public Boolean getFromBacklogState() {
        return fromBacklogState;
    }


    public static class Builder {
        private final BoardChangeRegistry registry;
        private final int view;
        private final JirbanIssueEvent event;

        //The new assignee if one was brought in
        private Assignee newAssignee;

        //The new components if any were brought in
        private Set<Component> newComponents;

        //If the blacklist was changed
        private String addedBlacklistState;
        private String addedBlacklistPriority;
        private String addedBlacklistIssueType;
        private String addedBlacklistIssue;
        private String deletedBlacklistIssue;

        //If the state was recalculated
        private String changedState;
        private Boolean fromBacklogState;
        private Boolean backlogState;

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

        public Builder addStateRecalculation(String state) {
            changedState = state;
            return this;
        }

        public Builder addNewComponents(Set<Component> newComponents) {
            this.newComponents = newComponents;
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

        public void buildAndRegister() {
            BoardChange change = new BoardChange(view, event, newAssignee, newComponents, addedBlacklistState, addedBlacklistPriority,
                    addedBlacklistIssueType, addedBlacklistIssue, deletedBlacklistIssue, changedState, fromBacklogState, backlogState);
            registry.registerChange(change);
        }
    }
}

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
package org.jirban.jira.impl;

import java.util.List;

import org.jirban.jira.impl.board.Assignee;

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

    //If the blacklist was modified. We are mainly interested in if a new unmapped state/priority/type pops up, and
    //its associated issue. Also, if an issue is deleted, it should be removed from the blacklist.
    private final String addedBlacklistState;
    private final String addedBlacklistPriority;
    private final String addedBlacklistIssueType;
    private final String addedBlacklistIssue;
    private final String deletedBlacklistIssue;

    //If the state was changed
    private final String changedState;
    private final List<String> changedStateIssues;


    private BoardChange(int view, JirbanIssueEvent event, Assignee newAssignee, String addedBlacklistState,
                        String addedBlacklistPriority, String addedBlacklistIssueType,
                        String addedBlacklistIssue, String deletedBlacklistIssue, String changedState, List<String> changedStateIssues) {
        this.view = view;
        this.event = event;
        this.newAssignee = newAssignee;
        this.addedBlacklistState = addedBlacklistState;
        this.addedBlacklistPriority = addedBlacklistPriority;
        this.addedBlacklistIssueType = addedBlacklistIssueType;
        this.addedBlacklistIssue = addedBlacklistIssue;
        this.deletedBlacklistIssue = deletedBlacklistIssue;
        this.changedState = changedState;
        this.changedStateIssues = changedStateIssues;
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

    List<String> getChangedStateIssues() {
        return changedStateIssues;
    }

    public static class Builder {
        private final BoardChangeRegistry registry;
        private final int view;
        private final JirbanIssueEvent event;

        //The new assignee if one was brought in
        private Assignee newAssignee;

        //If the blacklist was changed
        private String addedBlacklistState;
        private String addedBlacklistPriority;
        private String addedBlacklistIssueType;
        private String addedBlacklistIssue;
        private String deletedBlacklistIssue;

        //If the state was recalculated
        private String changedState;
        private List<String> changedStateIssues;

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

        public void buildAndRegister() {
            BoardChange change = new BoardChange(view, event, newAssignee, addedBlacklistState, addedBlacklistPriority,
                    addedBlacklistIssueType, addedBlacklistIssue, deletedBlacklistIssue, changedState, changedStateIssues);
            registry.registerChange(change);
        }

        public void addStateRecalculation(String state, List<String> changedStateIssues) {
            changedState = state;
            this.changedStateIssues = changedStateIssues;
        }
    }
}

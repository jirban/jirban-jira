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

import static org.jirban.jira.impl.Constants.ASSIGNEE;
import static org.jirban.jira.impl.Constants.ASSIGNEES;
import static org.jirban.jira.impl.Constants.BLACKLIST;
import static org.jirban.jira.impl.Constants.CHANGES;
import static org.jirban.jira.impl.Constants.CLEAR_COMPONENTS;
import static org.jirban.jira.impl.Constants.COMPONENTS;
import static org.jirban.jira.impl.Constants.ISSUES;
import static org.jirban.jira.impl.Constants.ISSUE_TYPES;
import static org.jirban.jira.impl.Constants.KEY;
import static org.jirban.jira.impl.Constants.NEW;
import static org.jirban.jira.impl.Constants.PRIORITIES;
import static org.jirban.jira.impl.Constants.PRIORITY;
import static org.jirban.jira.impl.Constants.REMOVED_ISSUES;
import static org.jirban.jira.impl.Constants.STATE;
import static org.jirban.jira.impl.Constants.STATES;
import static org.jirban.jira.impl.Constants.SUMMARY;
import static org.jirban.jira.impl.Constants.TYPE;
import static org.jirban.jira.impl.Constants.UNASSIGNED;
import static org.jirban.jira.impl.Constants.VIEW;
import static org.jirban.jira.impl.JirbanIssueEvent.Type.CREATE;
import static org.jirban.jira.impl.JirbanIssueEvent.Type.DELETE;
import static org.jirban.jira.impl.JirbanIssueEvent.Type.UPDATE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.JirbanIssueEvent.Type;
import org.jirban.jira.impl.board.Assignee;
import org.jirban.jira.impl.board.Board;
import org.jirban.jira.impl.board.BoardProject;
import org.jirban.jira.impl.board.Component;

import com.atlassian.jira.project.Project;

/**
 * Since Jira 6.4.x does not support web sockets, this maintains a queue of changes. A client connects and gets the full
 * board which has a current {@code view} sequence number. The client then polls every so often, and passes in its known
 * {@code view} sequence number. This class then returns the json of the delta between the client's sequence number and
 * the current {@code view} sequence number.
 *
 * @author Kabir Khan
 */
public class BoardChangeRegistry {

    //Look for items to clean up every 15 seconds
    private static final int CLEANUP_TICK_MS = 15000;

    //Delete items older than one minute
    private static final int CLEANUP_AGE_SECONDS = 60000;

    private volatile Board board;

    //The time for the next cleanup
    private volatile long nextCleanup;

    private volatile List<BoardChange> changes = new ArrayList<>();

    private volatile int startView;
    private volatile int endView;

    BoardChangeRegistry(Board board) {
        this.board = board;
        this.startView = board.getCurrentView();
        this.endView = startView;
        incrementNextCleanup();
    }

    public BoardChange.Builder addChange(int view, JirbanIssueEvent event) {
        return new BoardChange.Builder(this, view, event);
    }

    //This gets called by the board change builder
    void registerChange(BoardChange boardChange) {
        //Do the cleanup here if needed, but....
        getChangesListCleaningUpIfNeeded();
        synchronized (this) {
            //....make sure we work on the instance variable when adding
            changes.add(boardChange);
            endView = boardChange.getView();
        }
    }

    //This gets called by the board manager after the board has been built
    void setBoard(Board board) {
        this.board = board;
    }


    ModelNode getChangesSince(int sinceView) throws FullRefreshNeededException {
        //Get a snapshot of the changes
        if (sinceView > endView) {
            //Our board was probably reset since we last connected, so we need to send a full refresh instead
            throw new FullRefreshNeededException();
        }
        if (sinceView < startView) {
            //The client has taken too long to ask for changes
            throw new FullRefreshNeededException();
        }

        final Board board = this.board;
        final List<BoardChange> changes = getChangesListCleaningUpIfNeeded();
        final ChangeSetCollector collector = new ChangeSetCollector(board.getCurrentView());
        for (BoardChange change : changes) {
            if (change.getView() <= sinceView) {
                continue;
            }
            collector.addChange(change);
            if (change.getView() > board.getCurrentView()) {
                break;
            }
        }

        return collector.serialize(board);
    }

    private List<BoardChange> getChangesListCleaningUpIfNeeded() {
        final long current = System.currentTimeMillis();
        if (current < nextCleanup) {
            return changes;
        }
        final long expiryTime = current - CLEANUP_AGE_SECONDS;
        synchronized (this) {
            if (current < nextCleanup) {
                return changes;
            }

            int firstView = 0;
            if (changes.size() > 0 && changes.get(0).getTime() < expiryTime) {
                final List<BoardChange> changesCopy = new ArrayList<>();
                for (BoardChange change : changes) {
                    if (change.getTime() < expiryTime) {
                        continue;
                    }
                    if (firstView == 0) {
                        firstView = change.getView();
                    }
                    changesCopy.add(change);
                    endView = change.getView();
                }
                startView = firstView;
                changes = new CopyOnWriteArrayList<>(changesCopy);
            }
            incrementNextCleanup();
        }
        return changes;
    }

    private void incrementNextCleanup() {
        nextCleanup = System.currentTimeMillis() + CLEANUP_TICK_MS;
    }

    private static class ChangeSetCollector {
        private int view;
        private final Map<String, IssueChange> issueChanges = new HashMap<>();
        private final BlacklistChange blacklistChange = new BlacklistChange();
        private final Set<String> issuesWithStateChanges = new HashSet<>();

        public ChangeSetCollector(int endView) {
            this.view = endView;
        }

        void addChange(BoardChange boardChange) {
            final String issueKey = boardChange.getEvent().getIssueKey();

            if (!boardChange.isBlacklistEvent()) {
                IssueChange issueChange = issueChanges.get(issueKey);
                if (issueChange == null) {
                    issueChange = IssueChange.create(boardChange);
                    issueChanges.put(issueKey, issueChange);
                } else {
                    issueChange.merge(boardChange);
                    if (issueChange.type == null) {
                        issueChanges.remove(issueChange.issueKey);
                    }
                }
                if (issueChange.changedState != null) {
                    issuesWithStateChanges.add(issueKey);
                } else {
                    issuesWithStateChanges.remove(issueKey);
                }
            } else {
                blacklistChange.populate(boardChange);
            }

            if (boardChange.getView() > view) {
                view = boardChange.getView();
            }
        }

        private Map<String, Set<String>> processStateChanges() {
            //Several issues might undergo the same state changes, and be moved out,
            //make sure the last one wins
            Map<String, Set<String>> stateChangesByProject = new HashMap<>();
            Map<String, Map<String, Integer>> stateChangeViewsByProject = new HashMap<>();

            for (String issueKey : issuesWithStateChanges) {
                IssueChange change = issueChanges.get(issueKey);
                if (change != null) {
                    String changedState = change.changedState;

                    Set<String> stateChangesByState = stateChangesByProject.computeIfAbsent(change.projectCode, pc -> new HashSet<>());
                    Map<String, Integer> viewsByState = stateChangeViewsByProject.computeIfAbsent(change.projectCode, pc -> new HashMap<>());

                    Integer maxView = viewsByState.get(changedState);
                    if (maxView == null || maxView < change.view) {
                        viewsByState.put(changedState, change.view);
                        stateChangesByState.add(changedState);
                    }
                }
            }
            return stateChangesByProject;
        }

        ModelNode serialize(Board board) {
            ModelNode output = new ModelNode();
            ModelNode changes = output.get(CHANGES);
            changes.get(VIEW).set(view);

            Set<IssueChange> newIssues = new HashSet<>();
            Set<IssueChange> updatedIssues = new HashSet<>();
            Set<IssueChange> deletedIssues = new HashSet<>();
            Map<String, Assignee> newAssignees = new HashMap<>();
            Map<String, Component> newComponents = new HashMap<>();
            sortIssues(newIssues, updatedIssues, deletedIssues, newAssignees, newComponents);

            ModelNode issues = new ModelNode();
            serializeIssues(issues, newIssues, updatedIssues, deletedIssues);
            if (issues.isDefined()) {
                changes.get(ISSUES).set(issues);
            }

            serializeAssignees(changes, newAssignees);
            serializeComponents(changes, newComponents);
            serializeStateChanges(board, changes, processStateChanges());
            serializeBlacklist(changes);
            return output;
        }

        private void serializeIssues(ModelNode parent, Set<IssueChange> newIssues, Set<IssueChange> updatedIssues, Set<IssueChange> deletedIssues) {
            serializeIssues(parent, NEW, newIssues);
            serializeIssues(parent, Constants.UPDATE, updatedIssues);
            serializeIssues(parent, Constants.DELETE, deletedIssues);
        }

        private void serializeIssues(ModelNode parent, String key, Set<IssueChange> issues) {
            if (issues.size() == 0) {
                return;
            }
            ModelNode issuesNode = parent.get(key);
            for (IssueChange change : issues) {
                issuesNode.add(change.serialize());
            }
        }

        private void serializeAssignees(ModelNode parent, Map<String, Assignee> newAssignees) {
            if (newAssignees.size() > 0) {
                ModelNode assignees = parent.get(ASSIGNEES);
                for (Assignee assignee : newAssignees.values()) {
                    assignee.serialize(assignees);
                }
            }
        }

        private void serializeComponents(ModelNode parent, Map<String, Component> newComponents) {
            if (newComponents.size() > 0) {
                ModelNode components = parent.get(COMPONENTS);
                for (Component component : newComponents.values()) {
                    component.serialize(components);
                }
            }
        }

        private void serializeStateChanges(Board board, ModelNode parent, Map<String, Set<String>> stateChangesByProject) {
            if (stateChangesByProject.size() > 0) {
                for (Map.Entry<String, Set<String>> projectEntry : stateChangesByProject.entrySet()) {
                    final String projectCode = projectEntry.getKey();
                    final Set<String> changesForProject = projectEntry.getValue();

                    BoardProject project = board.getBoardProject(projectCode);
                    for (String state : changesForProject) {
                        final ModelNode stateChangesNode = parent.get(Constants.STATES, projectCode, state);
                        for (String issueKey : project.getIssuesForOwnState(state)) {
                            stateChangesNode.add(issueKey);
                        }
                    }
                }
            }
        }

        private void sortIssues(Set<IssueChange> newIssues, Set<IssueChange> updatedIssues,
                                Set<IssueChange> deletedIssues, Map<String, Assignee> newAssignees,
                                Map<String, Component> newComponents) {
            for (IssueChange change : issueChanges.values()) {
                Assignee newAssignee = null;
                Map<String, Component> newComponentsForIssue = null;
                if (change.type == CREATE) {
                    newIssues.add(change);
                    newAssignee = change.newAssignee;
                    newComponentsForIssue = change.newComponents;
                } else if (change.type == UPDATE) {
                    updatedIssues.add(change);
                    newAssignee = change.newAssignee;
                    newComponentsForIssue = change.newComponents;
                } else if (change.type == DELETE) {
                    deletedIssues.add(change);
                }

                if (newAssignee != null) {
                    newAssignees.put(newAssignee.getKey(), newAssignee);
                }
                if (newComponentsForIssue != null) {
                    newComponents.putAll(newComponentsForIssue);
                }
            }
        }

        private void serializeBlacklist(ModelNode changes) {
            ModelNode blacklistNode = blacklistChange.serialize();
            if (blacklistNode.isDefined()) {
                changes.get(BLACKLIST).set(blacklistNode);
            }
        }
    }

    //Will all be called in one thread by ChangeSetCollector, so
    private static class IssueChange {
        private final String projectCode;
        private final String issueKey;
        private int view;
        private Type type;

        private String issueType;
        private String priority;
        private String summary;
        private String assignee;
        private boolean unassigned;
        private Set<String> components;
        private boolean clearedComponents;
        private String state;

        private Assignee newAssignee;
        private Map<String, Component> newComponents;

        //We are interested in the latest state for the issue if it was moved or re-ranked
        //This information will then be used by the board change collector to figure out which states
        //should be shipped to the client. If several issues are moved to the same state, the one with the highest
        //view should be sent to the client.
        private String changedState;

        public IssueChange(String projectCode, String issueKey) {
            this.projectCode = projectCode;
            this.issueKey = issueKey;
        }

        static IssueChange create(BoardChange boardChange) {
            JirbanIssueEvent event = boardChange.getEvent();
            IssueChange change = new IssueChange(event.getProjectCode(), event.getIssueKey());
            change.merge(boardChange);

            return change;
        }

        void merge(BoardChange boardChange) {
            JirbanIssueEvent event = boardChange.getEvent();
            view = boardChange.getView();
            mergeType(event);
            if (type == null) {
                //If the issue was both updated and deleted we return null
                return;
            }
            switch (type) {
                case CREATE:
                case UPDATE:
                    mergeFields(event, boardChange.getNewAssignee(), boardChange.getNewComponents());
                    if (boardChange.getChangedState() != null) {
                        changedState = boardChange.getChangedState();
                    }
                    break;
                case DELETE:
                    //No need to do anything, we will not serialize this issue's details
                    //Clear the state change details
                    changedState = null;
                    break;
                default:
            }


        }

        void mergeFields(JirbanIssueEvent event, Assignee evtNewAssignee, Set<Component> evtNewComponents) {
            final JirbanIssueEvent.Detail detail = event.getDetails();
            if (detail.getIssueType() != null) {
                issueType = detail.getIssueType();
            }
            if (detail.getPriority() != null) {
                priority = detail.getPriority();
            }
            if (detail.getSummary() != null) {
                summary = detail.getSummary();
            }
            if (detail.getAssignee() != null) {
                if (detail.getAssignee() == JirbanIssueEvent.UNASSIGNED) {
                    assignee = null;
                    unassigned = true;
                    this.newAssignee = null;
                } else {
                    assignee = detail.getAssignee().getName();
                    unassigned = false;
                    if (this.newAssignee != null && !this.newAssignee.getKey().equals(detail.getAssignee().getName())) {
                        this.newAssignee = null;
                    }
                    if (evtNewAssignee != null) {
                        newAssignee = evtNewAssignee;
                    }
                }
            }
            if (detail.getComponents() != null) {
                if (detail.getComponents() == JirbanIssueEvent.NO_COMPONENT) {
                    components = null;
                    clearedComponents = true;
                    newComponents = null;
                } else {
                    components = new HashSet<>(detail.getComponents().size());
                    detail.getComponents().forEach(comp -> components.add(comp.getName()));
                    clearedComponents = false;
                    if (evtNewComponents != null) {
                        newComponents = newComponents == null ? new HashMap<>() : newComponents;
                        for (Component newComponent : evtNewComponents) {
                            newComponents.put(newComponent.getName(), newComponent);
                        }
                    }
                    if (newComponents != null) {
                        Set<String> removes = new HashSet<>();
                        for (Component newComponent : newComponents.values()) {
                            if (!components.contains(newComponent.getName())) {
                                removes.add(newComponent.getName());
                            }
                        }
                        for (String remove : removes) {
                            newComponents.remove(remove);
                        }
                    }
                }
            }
            if (detail.getState() != null) {
                state = detail.getState();
            }

        }

        void mergeType(JirbanIssueEvent event) {
            Type evtType = event.getType();
            if (type == null) {
                type = evtType;
                return;
            }
            switch (type) {
                case CREATE:
                    //We were created as part of this change-set, so keep CREATE unless we were deleted
                    if (evtType == DELETE) {
                        //We are deleting something created in this change set, so set null as a signal to remove it
                        type = null;
                    }
                    break;
                case UPDATE:
                    type = evtType;
                    break;
                case DELETE:
                    //No more changes should happen here
                    break;
            }
        }

        public ModelNode serialize() {
            ModelNode output = new ModelNode();
            switch (type) {
                case CREATE:
                    output.get(KEY).set(issueKey);
                    output.get(TYPE).set(issueType);
                    output.get(PRIORITY).set(priority);
                    output.get(SUMMARY).set(summary);
                    if (assignee != null) {
                        output.get(ASSIGNEE).set(assignee);
                    }
                    if (components != null) {
                        components.forEach(component -> output.get(COMPONENTS).add(component));
                    }
                    output.get(STATE).set(state);
                    break;
                case UPDATE:
                    output.get(KEY).set(issueKey);
                    if (issueType != null) {
                        output.get(TYPE).set(issueType);
                    }
                    if (priority != null) {
                        output.get(PRIORITY).set(priority);
                    }
                    if (summary != null) {
                        output.get(SUMMARY).set(summary);
                    }
                    if (assignee != null) {
                        output.get(ASSIGNEE).set(assignee);
                    }
                    if (components != null) {
                        components.forEach(component -> output.get(COMPONENTS).add(component));
                    }
                    if (state != null) {
                        output.get(STATE).set(state);
                    }
                    if (unassigned) {
                        output.get(UNASSIGNED).set(true);
                    }
                    if (clearedComponents) {
                        output.get(CLEAR_COMPONENTS).set(true);
                    }
                    break;
                case DELETE:
                    //No more data needed
                    output.set(issueKey);
                    break;
            }
            return output;
        }
    }

    private static class BlacklistChange {
        Set<String> states;
        Set<String> issueTypes;
        Set<String> priorities;
        Set<String> issues;
        Set<String> removedIssues;

        private BlacklistChange() {
        }

        void populate(BoardChange change) {
            if (change.getAddedBlacklistState() != null) {
                if (states == null) {
                    states = new HashSet<>();
                }
                states.add(change.getAddedBlacklistState());
            }
            if (change.getAddedBlacklistIssueType() != null) {
                if (issueTypes == null) {
                    issueTypes = new HashSet<>();
                }
                issueTypes.add(change.getAddedBlacklistIssueType());
            }
            if (change.getAddedBlacklistPriority() != null) {
                if (priorities == null) {
                    priorities = new HashSet<>();
                }
                priorities.add(change.getAddedBlacklistPriority());
            }
            if (change.getAddedBlacklistIssue() != null) {
                if (issues == null) {
                    issues = new HashSet<>();
                }
                issues.add(change.getAddedBlacklistIssue());
            }
            if (change.getDeletedBlacklistIssue() != null) {
                if (issues != null) {
                    issues.remove(change.getDeletedBlacklistIssue());
                }
                //It was not added to this change set, so add it to the deleted issues set
                if (removedIssues == null) {
                    removedIssues = new HashSet<>();
                }
                removedIssues.add(change.getDeletedBlacklistIssue());
            }
        }

        ModelNode serialize() {
            ModelNode modelNode = new ModelNode();
            if (states != null) {
                states.forEach(state -> modelNode.get(STATES).add(state));
            }
            if (issueTypes != null) {
                issueTypes.forEach(type -> modelNode.get(ISSUE_TYPES).add(type));
            }
            if (priorities != null) {
                priorities.forEach(priority -> modelNode.get(PRIORITIES).add(priority));
            }
            if (issues != null && issues.size() > 0) { //Check size here as well since removes can happen in populate()
                issues.forEach(issue -> modelNode.get(ISSUES).add(issue));
            }
            if (removedIssues != null) {
                removedIssues.forEach(issue -> modelNode.get(REMOVED_ISSUES).add(issue));
            }

            return modelNode;
        }
    }

    private static class StateChange {
        final String state;
        final List<String> issues;

        public StateChange(String state, List<String> issues) {
            this.state = state;
            this.issues = issues;
        }
    }

    public class FullRefreshNeededException extends Exception {

    }
}

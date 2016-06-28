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

import static org.jirban.jira.impl.Constants.ASSIGNEES;
import static org.jirban.jira.impl.Constants.COMPONENTS;
import static org.jirban.jira.impl.Constants.CUSTOM;
import static org.jirban.jira.impl.Constants.ISSUES;
import static org.jirban.jira.impl.Constants.MAIN;
import static org.jirban.jira.impl.Constants.PROJECTS;
import static org.jirban.jira.impl.Constants.VIEW;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanLogger;
import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.JirbanIssueEvent;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.CustomFieldConfig;
import org.jirban.jira.impl.config.LinkedProjectConfig;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;

/**
 * The data for a board.
 *
 * @author Kabir Khan
 */
public class Board {
    //This is incremented every time a change is made to the board
    final int currentView;

    private final BoardConfig boardConfig;

    //Map of assignees sorted by their display name
    private final Map<String, Assignee> sortedAssignees;
    private final Map<String, Integer> assigneeIndices;
    private final Map<String, Component> sortedComponents;
    private final Map<String, Integer> componentIndices;
    private final Map<String, Issue> allIssues;
    private final Map<String, BoardProject> projects;
    private final Map<String, SortedCustomFieldValues> sortedCustomFieldValues;

    private final Blacklist blacklist;

    private Board(Board old, BoardConfig boardConfig,
                    Map<String, Assignee> sortedAssignees,
                    Map<String, Component> sortedComponents,
                    Map<String, Issue> allIssues,
                    Map<String, BoardProject> projects,
                    Map<String, SortedCustomFieldValues> sortedCustomFieldValues,
                    Blacklist blacklist) {
        this.currentView = old == null ? 0 : old.currentView + 1;
        this.boardConfig = boardConfig;

        this.sortedAssignees = sortedAssignees;
        this.assigneeIndices = Collections.unmodifiableMap(getIndices(sortedAssignees));

        this.sortedComponents = sortedComponents;
        this.componentIndices = Collections.unmodifiableMap(getIndices(sortedComponents));

        this.allIssues = allIssues;
        this.projects = projects;
        this.sortedCustomFieldValues = sortedCustomFieldValues;
        this.blacklist = blacklist;
    }

    static Map<String, Integer> getIndices(Map<String, ?> map) {
        Map<String, Integer> indices = new HashMap<>();
        int i = 0;
        for (String key : map.keySet()) {
            indices.put(key, i++);
        }
        return indices;
    }

    public static Builder builder(JiraInjectables jiraInjectables, BoardConfig boardConfig, ApplicationUser boardOwner) {
        return new Builder(jiraInjectables, boardConfig, boardOwner);
    }

    public Board handleEvent(JiraInjectables jiraInjectables, ApplicationUser boardOwner, JirbanIssueEvent event,
                             BoardChangeRegistry changeRegistry) throws SearchException {
        Updater boardUpdater = new Updater(jiraInjectables, this, boardOwner, changeRegistry);
        return boardUpdater.handleEvent(event);
    }

    public ModelNode serialize(JiraInjectables jiraInjectables, boolean backlog, ApplicationUser user) {
        ModelNode outputNode = new ModelNode();
        //Sort the assignees by name
        outputNode.get(VIEW).set(currentView);

        ModelNode assigneesNode = outputNode.get(ASSIGNEES);
        assigneesNode.setEmptyList();
        for (Assignee assignee : sortedAssignees.values()) {
            assignee.serialize(assigneesNode);
        }

        if (sortedComponents.size() > 0) {
            ModelNode componentsNode = outputNode.get(COMPONENTS);
            componentsNode.setEmptyList();
            for (Component component : sortedComponents.values()) {
                component.serialize(componentsNode);
            }
        }
        if (sortedCustomFieldValues.size() > 0) {
            ModelNode customNode = outputNode.get(CUSTOM);
            for (SortedCustomFieldValues values : sortedCustomFieldValues.values()) {
                values.serialize(customNode);
            }
        }

        boardConfig.serializeModelNodeForBoard(outputNode);

        ModelNode allIssues = outputNode.get(ISSUES);
        this.allIssues.forEach((code, issue) -> {
            boolean relevant = true;
            if (!backlog) {
                relevant = !getBoardProject(issue.getProjectCode()).isBacklogState(issue.getState());
            }
            if (relevant) {
                allIssues.get(code).set(issue.getModelNodeForFullRefresh(this));
            }
        });

        ModelNode mainProjectsParent = outputNode.get(PROJECTS, MAIN);

        for (Map.Entry<String, BoardProject> projectEntry : projects.entrySet()) {
            final String projectCode = projectEntry.getKey();
            ModelNode project = mainProjectsParent.get(projectCode);
            projectEntry.getValue().serialize(jiraInjectables, project, user, backlog);
        }

        blacklist.serialize(outputNode);

        return outputNode;
    }

    public Issue getIssue(String key) {
        return allIssues.get(key);
    }

    public BoardConfig getConfig() {
        return boardConfig;
    }

    public int getProjectCode() {
        return boardConfig.getId();
    }

    public BoardProject getBoardProject(String code) {
        return projects.get(code);
    }

    public int getAssigneeIndex(Assignee assignee) {
        return assigneeIndices.get(assignee.getKey());
    }

    public int getComponentIndex(Component component) {
        return componentIndices.get(component.getName());
    }

    public int getCustomFieldIndex(CustomFieldValue customFieldValue) {
        return sortedCustomFieldValues.get(customFieldValue.getCustomFieldName()).getCustomFieldIndex(customFieldValue);
    }

    private void updateBoardInProjects() {
        for (BoardProject project : projects.values()) {
            project.setBoard(this);
        }
    }

    private static Assignee createAssignee(JiraInjectables jiraInjectables, ApplicationUser boardOwner, User assigneeUser) {
        ApplicationUser assigneeAppUser = ApplicationUsers.from(assigneeUser);
        URI avatarUrl = jiraInjectables.getAvatarService().getAvatarURL(boardOwner, assigneeAppUser, Avatar.Size.NORMAL);
        Assignee assignee = Assignee.create(assigneeUser, avatarUrl.toString());
        return assignee;
    }

    public int getCurrentView() {
        return currentView;
    }

    BoardChangeRegistry.IssueChange createCreateIssueChange(BoardChangeRegistry registry, String issueKey) {
        Issue issue = allIssues.get(issueKey);
        return issue.convertToCreateIssueChange(registry, getConfig());
    }

    static abstract class Accessor {
        protected final JiraInjectables jiraInjectables;
        protected final BoardConfig boardConfig;
        protected final ApplicationUser boardOwner;


        Accessor(JiraInjectables jiraInjectables, BoardConfig boardConfig, ApplicationUser boardOwner) {
            this.jiraInjectables = jiraInjectables;
            this.boardConfig = boardConfig;
            this.boardOwner = boardOwner;
        }

        Integer getIssueTypeIndexRecordingMissing(String issueKey, String issueTypeName) {
            final Integer issueTypeIndex = boardConfig.getIssueTypeIndex(issueTypeName);
            if (issueTypeIndex == null) {
                getBlacklist().addMissingIssueType(issueKey, issueTypeName);
            }
            return issueTypeIndex;
        }

        Integer getPriorityIndexRecordingMissing(String issueKey, String priorityName) {
            final Integer priorityIndex = boardConfig.getPriorityIndex(priorityName);
            if (priorityIndex == null) {
                getBlacklist().addMissingPriority(issueKey, priorityName);
            }
            return priorityIndex;
        }

        void addMissingState(String issueKey, String stateName) {
            getBlacklist().addMissingState(issueKey, stateName);
        }

        IssueLinkManager getIssueLinkManager() {
            return jiraInjectables.getIssueLinkManager();
        }


        public BoardProject.LinkedProjectContext getLinkedProjectContext(String linkedProjectCode) {
            LinkedProjectConfig projectCfg = boardConfig.getLinkedProjectConfig(linkedProjectCode);
            if (projectCfg == null) {
                return null;
            }
            return BoardProject.linkedProjectContext(this, projectCfg);
        }

        abstract Accessor addIssue(Issue issue);
        abstract Assignee getAssignee(User assigneeUser);
        abstract Issue getIssue(String issueKey);
        abstract Blacklist.Accessor getBlacklist();

        abstract CustomFieldValue getCustomField(CustomFieldConfig customField, Object fieldValue);

        abstract Set<Component> getComponents(Collection<ProjectComponent> componentObjects);

        public List<String> getStateNames() {
            return boardConfig.getStateNames();
        }

        public BoardConfig getConfig() {
            return boardConfig;
        }
    }

    private static Map<String, Assignee> sortAssignees(Map<String, Assignee> assignees) {
        List<Assignee> assigneeNames = new ArrayList<>(assignees.values());
        Collections.sort(assigneeNames, Comparator.comparing(Assignee::getKey, String.CASE_INSENSITIVE_ORDER));
        LinkedHashMap<String, Assignee> result = new LinkedHashMap<>();
        for (Assignee assignee : assigneeNames) {
            result.put(assignee.getKey(), assignee);
        }
        return result;
    }

    private static Map<String, Component> sortComponents(Map<String, Component> components) {
        List<Component> componentNames = new ArrayList<>(components.values());
        Collections.sort(componentNames, Comparator.comparing(Component::getName, String.CASE_INSENSITIVE_ORDER));
        LinkedHashMap<String, Component> result = new LinkedHashMap<>();
        for (Component component : componentNames) {
            result.put(component.getName(), component);
        }
        return result;
    }

    /**
     * Used to create a new board
     */
    public static class Builder extends Accessor {
        private final Map<String, Assignee> assignees = new HashMap<>();
        private final Map<String, Component> components = new HashMap<>();
        private final Map<String, Issue> allIssues = new HashMap<>();
        private final Map<String, BoardProject.Builder> projects = new HashMap<>();
        private final Blacklist.Builder blacklist = new Blacklist.Builder();
        private final Map<Long, SortedCustomFieldValues.Builder> customFieldBuilders = new HashMap();

        public Builder(JiraInjectables jiraInjectables,
                       BoardConfig boardConfig, ApplicationUser boardOwner) {
            super(jiraInjectables, boardConfig, boardOwner);
        }

        public Builder load() throws SearchException {
            for (BoardProjectConfig boardProjectConfig : boardConfig.getBoardProjects()) {
                BoardProjectConfig project = boardConfig.getBoardProject(boardProjectConfig.getCode());
                BoardProject.Builder projectBuilder = BoardProject.builder(jiraInjectables, this, project, boardOwner);
                projectBuilder.load();
                projects.put(projectBuilder.getCode(), projectBuilder);
            }
            return this;
        }

        @Override
        public Accessor addIssue(Issue issue) {
            allIssues.put(issue.getKey(), issue);
            return this;
        }

        @Override
        Assignee getAssignee(User assigneeUser) {
            if (assigneeUser == null) {
                //Unassigned issue
                return null;
            }
            Assignee assignee = assignees.get(assigneeUser.getName());
            if (assignee != null) {
                return assignee;
            }
            assignee = createAssignee(jiraInjectables, boardOwner, assigneeUser);
            assignees.put(assigneeUser.getName(), assignee);
            return assignee;
        }

        @Override
        Set<Component> getComponents(Collection<ProjectComponent> componentObjects) {
            if (componentObjects == null || componentObjects.size() == 0) {
                //No component for issue
                return null;
            }
            Set<Component> ret = new HashSet<>(componentObjects.size());
            for (ProjectComponent projectComponent : componentObjects) {
                Component component = components.get(projectComponent.getName());
                if (component == null) {
                    component = new Component(projectComponent.getName());
                    components.put(component.getName(), component);
                }
                ret.add(component);
            }
            return ret;
        }

        @Override
        CustomFieldValue getCustomField(CustomFieldConfig customFieldConfig, Object fieldValue) {
            final SortedCustomFieldValues.Builder customFieldBuilder =
                    customFieldBuilders.computeIfAbsent(customFieldConfig.getId(), id -> new SortedCustomFieldValues.Builder(customFieldConfig));
            return customFieldBuilder.getCustomField(fieldValue);
        }

        @Override
        Issue getIssue(String issueKey) {
            //Should not get called for this code path
            throw new IllegalStateException();
        }

        @Override
        Blacklist.Accessor getBlacklist() {
            return blacklist;
        }

        public Board build() {
            Map<String, BoardProject> projects = new LinkedHashMap<>();

            BoardProject.Builder ownerProject = this.projects.remove(boardConfig.getOwnerProjectCode());
            projects.put(boardConfig.getOwnerProjectCode(), ownerProject.build());

            this.projects.forEach((name, projectBuilder) -> {
                if (boardConfig.getBoardProject(name) != null) {
                    projects.put(name, projectBuilder.build());
                }
            });

            Map<String, SortedCustomFieldValues> sortedCustomFieldValues = new HashMap<>();
            for (SortedCustomFieldValues.Builder scfBuilder : this.customFieldBuilders.values()) {
                SortedCustomFieldValues fieldValues = scfBuilder.build();
                sortedCustomFieldValues.put(fieldValues.getFieldName(), fieldValues);
            }

            Board board = new Board(
                    null, boardConfig,
                    Collections.unmodifiableMap(sortAssignees(assignees)),
                    Collections.unmodifiableMap(sortComponents(components)),
                    Collections.unmodifiableMap(allIssues),
                    Collections.unmodifiableMap(projects),
                    Collections.unmodifiableMap(sortedCustomFieldValues),
                    blacklist.build());

            for (BoardProject project : projects.values()) {
                project.setBoard(board);
            }
            return board;
        }
    }

    /**
     * Used to update an already existing/loaded board
     */
    static class Updater extends Accessor {
        private final Board board;
        private final BoardChangeRegistry changeRegistry;
        private final Blacklist.Updater blacklist;

        //Will only be populated if a new assignee is brought in
        private Map<String, Assignee> assigneesCopy;
        //Will only be populated if new components are brought in
        private Map<String, Component> componentsCopy;
        Map<String, Issue> allIssuesCopy;

        private Assignee newAssignee;
        private Set<Component> newComponents;
        private final Map<Long, SortedCustomFieldValues.Builder> customFieldBuilders = new HashMap();

        Updater(JiraInjectables jiraInjectables, Board board, ApplicationUser boardOwner, BoardChangeRegistry changeRegistry) {
            super(jiraInjectables, board.getConfig(), boardOwner);
            this.board = board;
            this.changeRegistry = changeRegistry;
            this.blacklist = new Blacklist.Updater(board.blacklist);

        }

        Board handleEvent(JirbanIssueEvent event) throws SearchException {
            switch (event.getType()) {
                case DELETE:
                    return handleDeleteEvent(event);
                case CREATE:
                    return handleCreateOrUpdateIssue(event, true);
                case UPDATE:
                    return handleCreateOrUpdateIssue(event, false);
                default:
                    throw new IllegalArgumentException("Unknown event type " + event.getType());
            }
        }

        private Board handleDeleteEvent(JirbanIssueEvent event) throws SearchException {
            JirbanLogger.LOGGER.debug("Board.Updater.handleDeleteEvent - Handling delete event for {}", event.getIssueKey());
            final BoardProject project = board.projects.get(event.getProjectCode());
            if (project == null) {
                throw new IllegalArgumentException("Can't find project " + event.getProjectCode() +
                        " in board " + board.boardConfig.getId());
            }

            final Map<String, BoardProject> projectsCopy;
            final Map<String, Issue> allIssuesCopy;
            if (board.blacklist.isBlacklisted(event.getIssueKey())) {
                JirbanLogger.LOGGER.debug("Board.Updater.handleDeleteEvent - Handling delete event for blacklisted issue {}", event.getIssueKey());
                //For a delete of an issue that has been blacklisted we simply remove the issue from the blacklist.
                //It is not part of any of the issue tables so just use the old projects
                projectsCopy = board.projects;
                allIssuesCopy = board.allIssues;

                //We still need to update the board somewhat though to include the new blacklist (we only remove the
                // issue and not the bad state/issue-type/priority)
                blacklist.deleteIssue(event.getIssueKey());
            } else {
                JirbanLogger.LOGGER.debug("Board.Updater.handleDeleteEvent - Handling delete event for issue {}", event.getIssueKey());
                final Issue issue = board.allIssues.get(event.getIssueKey());
                if (issue == null) {
                    throw new IllegalArgumentException("Can't find issue to delete " + event.getIssueKey() +
                            " in board " + board.boardConfig.getId());
                }
                final BoardProject projectCopy = project.copyAndDeleteIssue(issue);
                projectsCopy = copyAndPut(board.projects, event.getProjectCode(), projectCopy, HashMap::new);

                Map<String, Issue> allIssues = new HashMap<>(board.allIssues);
                allIssues.remove(issue.getKey());
                allIssuesCopy = Collections.unmodifiableMap(allIssues);
            }

            Board boardCopy = new Board(board, board.boardConfig,
                    board.sortedAssignees,
                    board.sortedComponents,
                    Collections.unmodifiableMap(allIssuesCopy),
                    projectsCopy,
                    Collections.emptyMap(),//TODO
                    blacklist.build());
            boardCopy.updateBoardInProjects();

            //Register the event
            BoardChange.Builder changeBuilder = changeRegistry.addChange(boardCopy.currentView, event);
            if (blacklist.isUpdated()) {
                changeBuilder.deleteBlacklist(blacklist.getDeletedIssue());
            }
            changeBuilder.buildAndRegister();
            return boardCopy;
        }

        Board handleCreateOrUpdateIssue(JirbanIssueEvent event, boolean create) throws SearchException {

            JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - Handling create or update event for {}; create: {}", event.getIssueKey(), create);
            if (!create && board.blacklist.isBlacklisted(event.getIssueKey())) {
                //For an update of an issue that has been blacklisted we will not be able to figure out the state
                //So just return the original board
                JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - update event for blacklisted {} - returning original board", event.getIssueKey());
                return board;
            }

            final BoardProject project = board.projects.get(event.getProjectCode());
            if (project == null) {
                throw new IllegalArgumentException("Can't find project " + event.getProjectCode()
                        + " in board " + board.boardConfig.getId());
            }
            JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - using board project {}", project.getCode());

            JirbanIssueEvent.Detail evtDetail = event.getDetails();

            boolean moveFromDone = false;
            if (!create) {
                JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - updating issue {}", event.getIssueKey());
                if (event.getDetails().getState() != null) {

                    //This was a move, work out if we are moving to a done state or to an old state
                    boolean newDone = project.isDoneState(event.getDetails().getState());
                    boolean oldDone = project.isDoneState(event.getDetails().getOldState());

                    JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - possible state change from {} to {}. oldDone: {}; newDone: {}",
                            event.getDetails().getOldState(), event.getDetails().getState(), oldDone, newDone);

                    if (newDone && oldDone) {
                        //The whole move happened within the 'done' states, so ignore this update
                        JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - whole move happened in done states - return original");
                        return board;
                    }
                    if (newDone && !oldDone) {
                        //We are moving from a non-done to a 'done' state. Delete this issue from our cache
                        JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - Moving from non-done to done state, delete issue");
                        return handleDeleteEvent(JirbanIssueEvent.createDeleteEvent(event.getIssueKey(), event.getProjectCode()));
                    }
                    moveFromDone = oldDone && !newDone;
                } else if (project.isDoneState(event.getDetails().getOldState())) {
                    JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue- ignoring done issue {}, state: {}. Return original",
                            event.getIssueKey(), event.getDetails().getOldState());
                    //This was not a move, so if the 'old state' (which is the current one) is a done state
                    //we should return since we ignore these 'done' issues
                    return board;
                }
            }

            JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - moveFromDone: {}", moveFromDone);

            //Might bring in a new assignee and/or component, need to add those first
            //Will populate assigneeCopy and newAssignee if we need to add the assignee
            final Assignee issueAssignee = getOrCreateIssueAssignee(evtDetail);
            final Set<Component> issueComponents = getOrCreateIssueComponents(evtDetail);

            final BoardProject.Updater projectUpdater = project.updater(jiraInjectables, this, boardOwner);
            final Issue existingIssue;
            final Issue newIssue;
            if (create) {
                JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue- create issue {}", event.getIssueKey());
                existingIssue = null;
                newIssue = projectUpdater.createIssue(event.getIssueKey(), evtDetail.getIssueType(),
                        evtDetail.getPriority(), evtDetail.getSummary(), issueAssignee, issueComponents, evtDetail.getState());
            } else {
                existingIssue = board.allIssues.get(event.getIssueKey());
                if (existingIssue == null) {
                    if (moveFromDone) {
                        JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue- Moving from done {}", event.getIssueKey());

                        //We are doing a state change from one of the 'done' states for which we do not cache issues,
                        //into a cached state. Load it up and add it to the board
                        newIssue = projectUpdater.loadSingleIssue(event.getIssueKey());
                        if (newIssue == null) {
                            throw new IllegalArgumentException("Can't load issue that was updated from a 'done' state: " + event.getIssueKey() + " in board " + board.boardConfig.getId());
                        }
                    } else {
                        throw new IllegalArgumentException("Can't find issue to update " + event.getIssueKey() + " in board " + board.boardConfig.getId());
                    }
                } else {
                    JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue- Updating issue {}", event.getIssueKey());
                    newIssue = projectUpdater.updateIssue(existingIssue, evtDetail.getIssueType(),
                            evtDetail.getPriority(), evtDetail.getSummary(), issueAssignee,
                            issueComponents,
                            evtDetail.isRankOrStateChanged(), evtDetail.getState());
                }
            }


            //This will replace the old issue
            allIssuesCopy = newIssue != null ?
                    copyAndPut(board.allIssues, event.getIssueKey(), newIssue, HashMap::new) :
                    board.allIssues;

            JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - newIssue {}; updatedBlacklist {}; changedRankOrState {}",
                    newIssue, blacklist.isUpdated(), evtDetail.isRankOrStateChanged());

            if (newIssue != null || blacklist.isUpdated() || evtDetail.isRankOrStateChanged()) {
                //The project's issue tables will be updated if needed
                JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - Copying project {}", project.getCode());
                final BoardProject projectCopy = projectUpdater.build();
                final Map<String, BoardProject> projectsCopy = new HashMap<>(board.projects);
                projectsCopy.put(event.getProjectCode(), projectCopy);

                final Board boardCopy = new Board(board, board.boardConfig,
                        assigneesCopy == null ? board.sortedAssignees : Collections.unmodifiableMap(sortAssignees(assigneesCopy)),
                        componentsCopy == null ? board.sortedComponents : Collections.unmodifiableMap(sortComponents(componentsCopy)),
                        allIssuesCopy,
                        Collections.unmodifiableMap(projectsCopy),
                        Collections.emptyMap(), //TODO
                        blacklist.build());

                //Register the event
                boardCopy.updateBoardInProjects();

                if (moveFromDone) {
                    //We are making an issue visible again by moving it from a done state to a non-done state
                    //In this case invalidate. While it could be done, the change set becomes quite hard to keep
                    //track of if we think of moving to a done state as a delete, and moving out of a done state to
                    //a non-done state as a (re)create for e.g. the following scenarios:
                    //  non-done state -> done state (delete) == we have this now, and it becomes a delete
                    //  done state -> non-done state == a create
                    //  non-done state -> done state (delete) -> non-done state (recreate) == a noop (if no data changed), or an update
                    //  done state -> non-done state (create) -> done state == a noop
                    //
                    //To simplify things make moving an issue from a 'done' state to a normal state force a full board
                    //refresh for the clients. Issues being moved to 'done' is the norm. Issues being moved out from
                    //'done' to a prior state is not.
                    changeRegistry.forceRefresh();
                } else {
                    BoardChange.Builder changeBuilder = changeRegistry.addChange(boardCopy.currentView, event);

                    if (newAssignee != null) {
                        changeBuilder.addNewAssignee(newAssignee);
                    }
                    if (newComponents != null) {
                        changeBuilder.addNewComponents(newComponents);
                    }
                    if (blacklist.isUpdated()) {
                        changeBuilder.addBlacklist(blacklist.getAddedState(), blacklist.getAddedIssueType(),
                                blacklist.getAddedPriority(), blacklist.getAddedIssue());
                    }
                    //Propagate the new state somehow
                    if (evtDetail.isRankOrStateChanged()) {
                        Issue issue = boardCopy.getIssue(event.getIssueKey());
                        if (issue != null) {
                            final String state = issue.getState();
                            changeBuilder.addStateRecalculation(state);
                        }
                    }

                    if (existingIssue != null) {
                        changeBuilder.setFromBacklogState(project.isBacklogState(existingIssue.getState()));
                    }
                    if (newIssue != null) {
                        changeBuilder.setBacklogState(project.isBacklogState(newIssue.getState()));
                    }

                    JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - Registering change");
                    changeBuilder.buildAndRegister();
                }

                return boardCopy;
            }
            JirbanLogger.LOGGER.debug("Board.Updater.handleCreateOrUpdateIssue - Returning null board");
            return null;
        }

        @Override
        Assignee getAssignee(User assigneeUser) {
            return getOrCreateIssueAssignee(assigneeUser);
        }

        @Override
        Set<Component> getComponents(Collection<ProjectComponent> componentObjects) {
            return getOrCreateIssueComponents(componentObjects);
        }

        @Override
        CustomFieldValue getCustomField(CustomFieldConfig customField, Object fieldValue) {
            return null;
        }

        @Override
        Accessor addIssue(Issue issue) {
            //This should not happen for this code path
            throw new IllegalStateException();
        }

        @Override
        Issue getIssue(String issueKey) {
            return allIssuesCopy.get(issueKey);
        }

        @Override
        Blacklist.Accessor getBlacklist() {
            return blacklist;
        }

        private Assignee getOrCreateIssueAssignee(JirbanIssueEvent.Detail evtDetail) {
            return getOrCreateIssueAssignee(evtDetail.getAssignee());
        }

        private Assignee getOrCreateIssueAssignee(User evtAssignee) {
            if (evtAssignee == null) {
                return null;
            } else if (evtAssignee == JirbanIssueEvent.UNASSIGNED) {
                return Assignee.UNASSIGNED;
            } else {
                Assignee assignee = board.sortedAssignees.get(evtAssignee.getName());
                if (assignee == null) {
                    assignee = Board.createAssignee(jiraInjectables, boardOwner, evtAssignee);
                    newAssignee = assignee;
                    assigneesCopy = copyAndPut(board.sortedAssignees, evtAssignee.getName(), assignee, HashMap::new);
                }
                return assignee;
            }
        }

        private Set<Component> getOrCreateIssueComponents(JirbanIssueEvent.Detail evtDetail) {
            return getOrCreateIssueComponents(evtDetail.getComponents());
        }

        private Set<Component> getOrCreateIssueComponents(Collection<ProjectComponent> evtComponents) {
            if (evtComponents == null) {
                return null;
            } else if (evtComponents == JirbanIssueEvent.NO_COMPONENT) {
                return Collections.emptySet();
            } else {
                Set<Component> components = new HashSet<>();
                for (ProjectComponent evtComponent : evtComponents) {
                    Map<String, Component> componentMap = componentsCopy == null ? board.sortedComponents : componentsCopy;

                    Component component = componentMap.get(evtComponent.getName());
                    if (component == null) {
                        component = new Component(evtComponent.getName());
                        if (componentsCopy == null) {
                            componentsCopy = new HashMap<>(board.sortedComponents);
                        }
                        componentsCopy.put(component.getName(), component);
                        if (newComponents == null) {
                            newComponents = new HashSet<>();
                        }
                        newComponents.add(component);
                    }
                    components.add(component);
                }
                return components;
            }
        }

        private <K, V> Map<K, V> copyAndPut(Map<K, V> map, K key, V value, Supplier<Map<K, V>> supplier) {
            JirbanLogger.LOGGER.debug("Board.Updater.copyAndPut - Overwrite {}", key);
            Map<K, V> copy = supplier.get();
            copy.putAll(map);
            copy.put(key, value);
            return Collections.unmodifiableMap(copy);
        }
    }
}

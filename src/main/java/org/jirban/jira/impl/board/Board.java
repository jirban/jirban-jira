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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.JirbanIssueEvent;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.LinkedProjectConfig;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.user.util.UserManager;

/**
 * The data for a board.
 *
 * @author Kabir Khan
 */
public class Board {
    //This is incremented every time a change is made to the board
    final int currentView;

    private final BoardConfig boardConfig;

    private final Map<String, Assignee> assignees;
    private final Map<String, Integer> assigneeIndices;
    private final Map<String, Issue> allIssues;
    private final Map<String, BoardProject> projects;

    private final Map<String, List<String>> missingIssueTypes;
    private final Map<String, List<String>> missingPriorities;
    private final Map<String, List<String>> missingStates;

    private Board(int currentView, BoardConfig boardConfig,
                 Map<String, Assignee> assignees,
                  Map<String, Integer> assigneeIndices,
                 Map<String, Issue> allIssues,
                 Map<String, BoardProject> projects,
                 Map<String, List<String>> missingIssueTypes,
                 Map<String, List<String>> missingPriorities,
                 Map<String, List<String>> missingStates) {
        this.currentView = currentView;
        this.boardConfig = boardConfig;
        this.assignees = assignees;
        this.assigneeIndices = assigneeIndices;
        this.allIssues = allIssues;
        this.projects = projects;
        this.missingIssueTypes = missingIssueTypes;
        this.missingPriorities = missingPriorities;
        this.missingStates = missingStates;
    }

    public static Builder builder(SearchService searchService, AvatarService avatarService,
                                  IssueLinkManager issueLinkManager, UserManager userManager, BoardConfig boardConfig, ApplicationUser boardOwner) {
        return new Builder(searchService, avatarService, issueLinkManager, userManager,boardConfig, boardOwner);
    }

    public Board handleEvent(SearchService searchService, AvatarService avatarService,
                             IssueLinkManager issueLinkManager, ApplicationUser boardOwner, JirbanIssueEvent event) throws SearchException {
        Updater boardUpdater = new Updater(searchService, avatarService, issueLinkManager, this, boardOwner);
        return boardUpdater.handleEvent(event);
    }

    public ModelNode serialize() {
        ModelNode outputNode = new ModelNode();
        //Sort the assignees by name
        outputNode.get("viewId").set(currentView);
        ModelNode assigneesNode = outputNode.get("assignees");
        assigneesNode.setEmptyList();
        List<Assignee> assigneeNames = new ArrayList<>(assignees.values());
        Collections.sort(assigneeNames, (a1, a2) -> a1.getDisplayName().compareTo(a2.getDisplayName()));
        for (Assignee assignee : assigneeNames) {
            assignees.get(assignee.getKey()).serialize(assigneesNode);
        }

        boardConfig.serializeModelNodeForBoard(outputNode);

        ModelNode allIssues = outputNode.get("issues");
        this.allIssues.forEach((code, issue) -> {
            allIssues.get(code).set(issue.getModelNodeForFullRefresh(this));
        });

        ModelNode mainProjectsParent = outputNode.get("projects", "main");

        for (Map.Entry<String, BoardProject> projectEntry : projects.entrySet()) {
            final String projectCode = projectEntry.getKey();
            ModelNode project = mainProjectsParent.get(projectCode);
            projectEntry.getValue().serialize(project);
        }

        serializeMissing(outputNode, "priorities", missingPriorities);
        serializeMissing(outputNode, "issue-types", missingIssueTypes);
        serializeMissing(outputNode, "states", missingStates);

        return outputNode;
    }

    private void serializeMissing(ModelNode root, String key, Map<String, List<String>> missing) {
        if (missing.size() == 0) {
            return;
        }
        ModelNode missingNode = root.get("missing", key);
        for (Map.Entry<String, List<String>> entry : missing.entrySet()) {
            ModelNode issues = missingNode.get(entry.getKey(), "issues");
            for (String issue : entry.getValue()) {
                issues.add(issue);
            }
        }
    }

    public boolean isDataSame(Board that) {
        //I don't want to do a standard equals() since I am not comparing all the data
        if (that == null) return false;

        if (assignees.size() != that.assignees.size()) {
            return false;
        }
        for (Map.Entry<String, Assignee> entry : assignees.entrySet()) {
            if (!entry.getValue().isDataSame(that.assignees.get(entry.getKey()))) {
                return false;
            }
        }

        if (projects.size() != that.projects.size()) {
            return false;
        }
        for (Map.Entry<String, BoardProject> entry : projects.entrySet()) {
            if (!entry.getValue().isDataSame(that.projects.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
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

    private void updateBoardInProjects() {
        for (BoardProject project : projects.values()) {
            project.setBoard(this);
        }
    }

    private static Assignee createAssignee(AvatarService avatarService, ApplicationUser boardOwner, User assigneeUser) {
        ApplicationUser assigneeAppUser = ApplicationUsers.from(assigneeUser);
        URI avatarUrl = avatarService.getAvatarURL(boardOwner, assigneeAppUser, Avatar.Size.NORMAL);
        Assignee assignee = Assignee.create(assigneeUser, avatarUrl.toString());
        return assignee;
    }

    static abstract class Accessor {
        protected final BoardConfig boardConfig;
        protected final IssueLinkManager issueLinkManager;
        protected final ApplicationUser boardOwner;

        protected final Map<String, List<String>> missingIssueTypes = new TreeMap<>();
        protected final Map<String, List<String>> missingPriorities = new TreeMap<>();
        protected final Map<String, List<String>> missingStates = new TreeMap<>();


        public Accessor(IssueLinkManager issueLinkManager, BoardConfig boardConfig, ApplicationUser boardOwner) {
            this.issueLinkManager = issueLinkManager;
            this.boardConfig = boardConfig;
            this.boardOwner = boardOwner;
        }

        Set<String> getOwnerStateNames() {
            return boardConfig.getOwnerStateNames();
        }

        Integer getIssueTypeIndexRecordingMissing(String issueKey, String issueTypeName) {
            final Integer issueTypeIndex = boardConfig.getIssueTypeIndex(issueTypeName);
            if (issueTypeIndex == null) {
                addMissing(missingIssueTypes, issueTypeName, issueKey);
            }
            return issueTypeIndex;
        }

        Integer getPriorityIndexRecordingMissing(String issueKey, String priorityName) {
            final Integer priorityIndex = boardConfig.getPriorityIndex(priorityName);
            if (priorityIndex == null) {
                addMissing(missingPriorities, priorityName, issueKey);
            }
            return priorityIndex;
        }

        void addMissingState(String issueKey, String stateName) {
            addMissing(missingStates, stateName, issueKey);
        }

        BoardProjectConfig getOwningProject() {
            return boardConfig.getOwningProject();
        }

        IssueLinkManager getIssueLinkManager() {
            return issueLinkManager;
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

        private void addMissing(Map<String, List<String>> missingMap, String mapKey, String issueKey) {
            List<String> missingIssues = missingMap.computeIfAbsent(mapKey, l -> new ArrayList<String>());
            missingIssues.add(issueKey);
        }
    }

    private static Map<String, Integer> getAssigneeIndices(Map<String, Assignee> assignees) {
        Map<String, Integer> assigneeIndices = new HashMap<>();
        for (String assigneeKey : assignees.keySet()) {
            assigneeIndices.put(assigneeKey, assigneeIndices.size());
        }
        return assigneeIndices;
    }

    /**
     * Used to create a new board
     */
    public static class Builder extends Accessor {
        private final SearchService searchService;
        private final AvatarService avatarService;

        private final Map<String, Assignee> assignees = new TreeMap<>();
        private final Map<String, Issue> allIssues = new HashMap<>();
        private final Map<String, BoardProject.Builder> projects = new HashMap<>();

        public Builder(SearchService searchService, AvatarService avatarService, IssueLinkManager issueLinkManager,
                       UserManager userManager, BoardConfig boardConfig, ApplicationUser boardOwner) {
            super(issueLinkManager, boardConfig, boardOwner);
            this.searchService = searchService;
            this.avatarService = avatarService;
        }

        public Builder load() throws SearchException {
            for (BoardProjectConfig boardProjectConfig : boardConfig.getBoardProjects()) {
                BoardProjectConfig project = boardConfig.getBoardProject(boardProjectConfig.getCode());
                BoardProject.Builder projectBuilder = BoardProject.builder(searchService, this, project, boardOwner);
                projectBuilder.load();
                projects.put(projectBuilder.getCode(), projectBuilder);
            }
            return this;
        }

        public Accessor addIssue(Issue issue) {
            allIssues.put(issue.getKey(), issue);
            return this;
        }

        Assignee getAssignee(User assigneeUser) {
            if (assigneeUser == null) {
                //Unassigned issue
                return null;
            }
            Assignee assignee = assignees.get(assigneeUser.getName());
            if (assignee != null) {
                return assignee;
            }
            assignee = createAssignee(avatarService, boardOwner, assigneeUser);
            assignees.put(assigneeUser.getName(), assignee);
            return assignee;
        }

        @Override
        Issue getIssue(String issueKey) {
            //Should not get called for this code path
            throw new IllegalStateException();
        }

        public Board build() {
            Map<String, BoardProject> projects = new LinkedHashMap<>();

            BoardProject.Builder ownerProject = this.projects.remove(boardConfig.getOwnerProjectCode());
            projects.put(boardConfig.getOwnerProjectCode(), ownerProject.build(true));

            this.projects.forEach((name, projectBuilder) -> {
                if (boardConfig.getBoardProject(name) != null) {
                    projects.put(name, projectBuilder.build(false));
                }
            });

            Board board = new Board(0, boardConfig, Collections.unmodifiableMap(assignees), Collections.unmodifiableMap(getAssigneeIndices(assignees)),
                    Collections.unmodifiableMap(allIssues), Collections.unmodifiableMap(projects),
                    Collections.unmodifiableMap(missingIssueTypes), Collections.unmodifiableMap(missingPriorities),
                    Collections.unmodifiableMap(missingStates));
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
        private final AvatarService avatarService;
        private final SearchService searchService;
        //Will only be populated if a new assignee is brought in
        private Map<String, Assignee> assigneesCopy;
        Map<String, Issue> allIssuesCopy;

        Updater(SearchService searchService, AvatarService avatarService, IssueLinkManager issueLinkManager,
                Board board, ApplicationUser boardOwner) {
            super(issueLinkManager, board.getConfig(), boardOwner);
            this.board = board;
            this.searchService = searchService;
            this.avatarService = avatarService;
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
            final BoardProject project = board.projects.get(event.getProjectCode());
            if (project == null) {
                throw new IllegalArgumentException("Can't find project " + event.getProjectCode() +
                        " in board " + board.boardConfig.getId());
            }
            final Issue issue = board.allIssues.get(event.getIssueKey());
            if (issue == null) {
                throw new IllegalArgumentException("Can't find issue to delete " + event.getIssueKey() +
                        " in board " + board.boardConfig.getId());
            }
            final BoardProject projectCopy = project.copyAndDeleteIssue(issue);

            final Map<String, BoardProject> projectsCopy = new HashMap<>(board.projects);
            projectsCopy.put(event.getProjectCode(), projectCopy);

            final Map<String, Issue> allIssuesCopy = new HashMap<>(board.allIssues);
            allIssuesCopy.remove(issue.getKey());

            //TODO delete the issue from the missing

            //TODO put the event in some wrapper with the view id on the 'update registry'

            Board boardCopy = new Board(board.currentView + 1, board.boardConfig,
                    board.assignees, board.assigneeIndices,
                    Collections.unmodifiableMap(allIssuesCopy),
                    Collections.unmodifiableMap(projectsCopy),
                    board.missingIssueTypes,
                    board.missingPriorities,
                    board.missingStates);
            boardCopy.updateBoardInProjects();
            return boardCopy;
        }

        Board handleCreateOrUpdateIssue(JirbanIssueEvent event, boolean create) throws SearchException {
            final BoardProject project = board.projects.get(event.getProjectCode());
            if (project == null) {
                throw new IllegalArgumentException("Can't find project " + event.getProjectCode()
                        + " in board " + board.boardConfig.getId());
            }

            JirbanIssueEvent.Detail evtDetail = event.getDetails();

            //Might bring in a new assignee, need to add that first
            final Assignee newAssignee = createAssigneeIfNeeded(evtDetail);
            final Assignee issueAssignee = getIssueAssignee(newAssignee, evtDetail);
            final BoardProject.Updater projectUpdater = project.updater(searchService, this, boardOwner);
            final Issue newIssue;
            final Issue existing;
            if (create) {
                existing = null;
                newIssue = projectUpdater.createIssue(event.getIssueKey(), evtDetail.getIssueType(),
                        evtDetail.getPriority(), evtDetail.getSummary(), issueAssignee, evtDetail.getState());
            } else {
                existing = board.allIssues.get(event.getIssueKey());
                if (existing == null) {
                    throw new IllegalArgumentException("Can't find issue to update " + event.getIssueKey() + " in board " + board.boardConfig.getId());
                }
                newIssue = projectUpdater.updateIssue(existing, evtDetail.getIssueType(),
                        evtDetail.getPriority(), evtDetail.getSummary(), issueAssignee,
                        evtDetail.isRankOrStateChanged(), evtDetail.getState());
            }
            //This will replace the old issue
            allIssuesCopy = copyAndPut(board.allIssues, event.getIssueKey(), newIssue, HashMap::new);

            //The project's issue tables will be updated if needed
            BoardProject projectCopy = projectUpdater.update();
            final Map<String, BoardProject> projectsCopy = new HashMap<>(board.projects);
            projectsCopy.put(event.getProjectCode(), projectCopy);


            //TODO put the event in some wrapper with the view id on the 'update registry'
            //Include the assignee if it was new
            //Include the project state table if recalculated

            //TODO recalculate the missingXXXs maps (if an update we will need to remove the old ones)

            Board boardCopy = new Board(board.currentView + 1, board.boardConfig,
                    assigneesCopy == null ? board.assignees : Collections.unmodifiableMap(assigneesCopy),
                    assigneesCopy == null ? board.assigneeIndices : Collections.unmodifiableMap(getAssigneeIndices(assigneesCopy)),
                    Collections.unmodifiableMap(allIssuesCopy),
                    Collections.unmodifiableMap(projectsCopy),
                    board.missingIssueTypes,
                    board.missingPriorities,
                    board.missingStates);
            boardCopy.updateBoardInProjects();
            return boardCopy;
        }

        Assignee getAssignee(User assigneeUser) {
            //This should not happen for this code path
            throw new IllegalStateException();
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

        private Assignee createAssigneeIfNeeded(JirbanIssueEvent.Detail evtDetail) {
            //Might bring in a new assignee, need to add that first
            final User eventAssignee = evtDetail.getAssignee();
            final Map<String, Assignee> assigneesCopy;
            Assignee newAssignee = null;
            if (eventAssignee != null && !board.assignees.containsKey(eventAssignee.getName())) {
                newAssignee = Board.createAssignee(avatarService, boardOwner, evtDetail.getAssignee());
                this.assigneesCopy = Collections.unmodifiableMap(
                        copyAndPut(board.assignees, eventAssignee.getName(), newAssignee, TreeMap::new));
            }
            return newAssignee;
        }

        private Assignee getIssueAssignee(Assignee newAssignee, JirbanIssueEvent.Detail evtDetail) {
            if (newAssignee != null) {
                return newAssignee;
            } else if (evtDetail.getAssignee() == null) {
                return evtDetail.isUnassigned() ? Assignee.UNASSIGNED : null;
            } else {
                return board.assignees.get(evtDetail.getAssignee().getName());
            }
        }
        private <K, V> Map<K, V> copyAndPut(Map<K, V> map, K key, V value, Supplier<Map<K, V>> supplier) {
            Map<K, V> copy = supplier.get();
            copy.putAll(map);
            copy.put(key, value);
            return copy;
        }
    }
}

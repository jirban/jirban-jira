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

import static org.jirban.jira.impl.Constants.ISSUES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.LinkedProjectConfig;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.order.SortOrder;

/**
 * The data for a board project, i.e. a project whose issues should appear as cards on the board.
 *
 * @author Kabir Khan
 */
class BoardProject {

    private volatile Board board;
    private final BoardProjectConfig projectConfig;
    //TODO linkedhashset for the issues list?
    private final List<List<String>> issueKeysByState;

    private BoardProject(BoardProjectConfig projectConfig, List<List<String>> issueKeysByState) {
        this.projectConfig = projectConfig;
        this.issueKeysByState = issueKeysByState;
    }

    void setBoard(Board board) {
        this.board = board;
    }

    boolean isDataSame(BoardProject boardProject) {
        return false;
    }

    int getAssigneeIndex(Assignee assignee) {
        return board.getAssigneeIndex(assignee);
    }

    int getComponentIndex(Component component) {
        return board.getComponentIndex(component);
    }

    void serialize(ModelNode parent) {
        ModelNode projectIssues = parent.get(ISSUES);
        for (List<String> issuesForState : issueKeysByState) {
            ModelNode issuesForStateNode = new ModelNode();
            issuesForStateNode.setEmptyList();
            for (String key: issuesForState) {
                issuesForStateNode.add(key);
            }
            projectIssues.add(issuesForStateNode);
        }
    }

    List<String> getIssuesForOwnState(String ownState) {
        int stateIndex = projectConfig.mapOwnStateOntoBoardStateIndex(ownState);
        return issueKeysByState.get(stateIndex);
    }

    boolean isOwner() {
        return board.getConfig().getOwnerProjectCode().equals(projectConfig.getCode());
    }

    static Builder builder(SearchService searchService, Board.Builder builder, BoardProjectConfig projectConfig,
                           ApplicationUser boardOwner) {
        return new Builder(searchService, builder, projectConfig, boardOwner);
    }

    static LinkedProjectContext linkedProjectContext(Board.Accessor board, LinkedProjectConfig linkedProjectConfig) {
        return new LinkedProjectContext(board, linkedProjectConfig);
    }

    public BoardProject copyAndDeleteIssue(Issue deleteIssue) throws SearchException {
        Updater updater = new Updater(null, null, this, null);
        updater.deleteIssue(deleteIssue);
        return updater.build();
    }

    public Updater updater(SearchService searchService, Board.Updater boardUpdater,
                           ApplicationUser boardOwner) {
        return new Updater(searchService, boardUpdater, this, boardOwner);
    }

    static class Accessor {
        protected final SearchService searchService;
        protected final Board.Accessor board;
        protected final BoardProjectConfig projectConfig;
        protected final ApplicationUser boardOwner;

        public Accessor(SearchService searchService, Board.Accessor board, BoardProjectConfig projectConfig, ApplicationUser boardOwner) {
            this.searchService = searchService;
            this.board = board;
            this.projectConfig = projectConfig;
            this.boardOwner = boardOwner;
        }

        BoardProjectConfig getConfig() {
            return projectConfig;
        }

        Integer getPriorityIndexRecordingMissing(String issueKey, String priorityName) {
            return board.getPriorityIndexRecordingMissing(issueKey, priorityName);
        }

        Integer getIssueTypeIndexRecordingMissing(String issueKey, String issueTypeName) {
            return board.getIssueTypeIndexRecordingMissing(issueKey, issueTypeName);
        }

        Integer getStateIndexRecordingMissing(String issueKey, String stateName) {
            final Integer index = projectConfig.getStateIndex(stateName);
            if (index == null) {
                board.addMissingState(issueKey, stateName);
            } else {
                if (!projectConfig.isOwner()) {
                    Integer ownerStateIndex = null;
                    String ownerState = projectConfig.mapOwnStateOntoBoardState(stateName);
                    if (ownerState != null) {
                        ownerStateIndex = board.getOwningProject().getStateIndex(ownerState);
                    }
                    if (ownerStateIndex == null) {
                        //This was not mapped to a valid owner state so report the problem
                        board.addMissingState(issueKey, ownerState != null ? ownerState : stateName);
                        return null;
                    }
                    //Do not return the owner state index here although all was fine. The calculation of the columns
                    //depends on everything using their own state index.
                }
            }
            return index;
        }

        IssueLinkManager getIssueLinkManager() {
            return board.getIssueLinkManager();
        }

        Assignee getAssignee(User assigneeUser) {
            return board.getAssignee(assigneeUser);
        }

        public String getCode() {
            return projectConfig.getCode();
        }

        public LinkedProjectContext getLinkedProjectContext(String linkedProjectCode) {
            return board.getLinkedProjectContext(linkedProjectCode);
        }

        public Set<Component> getComponents(Collection<ProjectComponent> componentObjects) {
            return board.getComponents(componentObjects);
        }
    }

    /**
     * Used to load a project when creating a new board
     */
    static class Builder extends Accessor {
        private final Map<String, List<String>> issueKeysByState = new HashMap<>();


        private Builder(SearchService searchService, Board.Accessor board, BoardProjectConfig projectConfig,
                        ApplicationUser boardOwner) {
            super(searchService, board, projectConfig, boardOwner);
        }

        Builder addIssue(String state, Issue issue) {
            final List<String> issueKeys = issueKeysByState.computeIfAbsent(state, l -> new ArrayList<>());
            issueKeys.add(issue.getKey());
            board.addIssue(issue);
            return this;
        }

        void load() throws SearchException {
            JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
            queryBuilder.where().project(projectConfig.getCode());
            if (projectConfig.getQueryFilter() != null) {
                queryBuilder.where().addCondition(projectConfig.getQueryFilter());
            }
            queryBuilder.orderBy().addSortForFieldName("Rank", SortOrder.ASC, true);

            SearchResults searchResults =
                    searchService.search(boardOwner.getDirectoryUser(), queryBuilder.buildQuery(), PagerFilter.getUnlimitedFilter());

            for (com.atlassian.jira.issue.Issue jiraIssue : searchResults.getIssues()) {
                Issue.Builder issueBuilder = Issue.builder(this);
                issueBuilder.load(jiraIssue);
                Issue issue = issueBuilder.build();
                if (issue != null) {
                    addIssue(issue.getState(), issue);
                }
            }
        }

        BoardProject build(boolean owner) {
            final List<List<String>> resultIssues = new ArrayList<>();
            for (String state : board.getOwnerStateNames()) {
                List<String> issues = issueKeysByState.get(owner ? state : projectConfig.mapBoardStateOntoOwnState(state));
                if (issues == null) {
                    issues = new ArrayList<>();
                }
                resultIssues.add(Collections.synchronizedList(issues));
            }

            return new BoardProject(projectConfig, Collections.unmodifiableList(resultIssues));
        }
    }

    /**
     * Used to update an existing board
     */
    static class Updater extends Accessor {
        private final BoardProject project;
        private Issue existing;
        private Issue newIssue;
        private boolean updatedState;


        Updater(SearchService searchService, Board.Accessor board, BoardProject project,
                       ApplicationUser boardOwner) {
            super(searchService, board, project.projectConfig, boardOwner);
            this.project = project;
        }

        Issue createIssue(String issueKey, String issueType, String priority, String summary,
                          Assignee assignee, Set<Component> issueComponents, String state) {
            newIssue = Issue.createForCreateEvent(this, issueKey, state, summary, issueType, priority, assignee, issueComponents);
            updatedState = newIssue != null;
            return newIssue;
        }

        Issue updateIssue(Issue existing, String issueType, String priority, String summary,
                          Assignee issueAssignee, Set<Component> issueComponents, boolean rankOrStateChanged, String state) {
            this.existing = existing;
            newIssue = existing.copyForUpdateEvent(this, existing, issueType, priority, summary, issueAssignee, issueComponents, state);
            if (newIssue == null && rankOrStateChanged) {
                newIssue = existing;
            }
            this.updatedState = rankOrStateChanged && newIssue != null;
            return newIssue;
        }

        void deleteIssue(Issue issue) {
            this.existing = issue;

        }

        BoardProject build() throws SearchException {

            final int deleteIndex = getStateIndexToDeleteFrom();
            final List<String> deletedStateIssues = deleteIndex >= 0 ?
                    deleteFromState(deleteIndex, existing.getKey()) : null;

            final List<String> toStateIssues;
            final int toIndex;
            if (!updatedState || newIssue == null) {
                toIndex = -1;
                toStateIssues = null;
            } else {
                toIndex = project.projectConfig.mapOwnStateOntoBoardStateIndex(newIssue.getState());
                toStateIssues = updateState();
            }

            final List<List<String>> issuesKeysByStateCopy = new ArrayList<>();

            for (int i = 0; i < project.issueKeysByState.size() ; i++) {
                if (deleteIndex == i) {
                    issuesKeysByStateCopy.add(Collections.unmodifiableList(deletedStateIssues));
                } else if (toIndex == i) {
                    issuesKeysByStateCopy.add(Collections.unmodifiableList(toStateIssues));
                } else {
                    issuesKeysByStateCopy.add(project.issueKeysByState.get(i));
                }
            }
            return new BoardProject(projectConfig, Collections.unmodifiableList(issuesKeysByStateCopy));
        }

        private List<String> deleteFromState(int stateIndex, String issueKey) {
            List<String> issueKeys = project.issueKeysByState.get(stateIndex);
            List<String> copy = new ArrayList<>();
            for (String key : issueKeys) {
                if (!key.equals(issueKey)) {
                    copy.add(key);
                }
            }
            return copy;
        }

        private int getStateIndexToDeleteFrom() {
            if (existing != null) {
                if (newIssue == null) {
                    //It was a plain delete
                    return project.projectConfig.mapOwnStateOntoBoardStateIndex(existing.getState());
                } else if (!existing.getState().equals(newIssue.getState())) {
                    //It was a move which affected states
                    return project.projectConfig.mapOwnStateOntoBoardStateIndex(existing.getState());
                }
            }
            return -1;
        }

        private List<String> updateState() throws SearchException {
            JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
            queryBuilder.where().project(projectConfig.getCode()).and().status(newIssue.getState());
            if (projectConfig.getQueryFilter() != null) {
                queryBuilder.where().addCondition(projectConfig.getQueryFilter());
            }
            //TODO if it is possible to narrow this down to only get the product keys that would be better than loading everything
            queryBuilder.orderBy().addSortForFieldName("Rank", SortOrder.ASC, true);

            SearchResults searchResults =
                    searchService.search(boardOwner.getDirectoryUser(), queryBuilder.buildQuery(), PagerFilter.getUnlimitedFilter());

            List<String> issues = new ArrayList<>();
            for (com.atlassian.jira.issue.Issue jiraIssue : searchResults.getIssues()) {
                String issueKey = jiraIssue.getKey();
                Issue issue = board.getIssue(issueKey);
                if (issue == null) {
                    System.out.println("Could not find issue " + issue);
                }
                issues.add(issue.getKey());
            }
            return Collections.unmodifiableList(issues);
        }
    }

    static class LinkedProjectContext {

        private final Board.Accessor board;
        private final LinkedProjectConfig linkedProjectConfig;

        LinkedProjectContext(Board.Accessor board, LinkedProjectConfig linkedProjectConfig) {
            this.board = board;
            this.linkedProjectConfig = linkedProjectConfig;
        }

        public LinkedProjectConfig getConfig() {
            return linkedProjectConfig;
        }

        Integer getStateIndexRecordingMissing(String projectCode, String issueKey, String stateName) {
            final Integer index = linkedProjectConfig.getStateIndex(stateName);
            if (index == null) {
                board.addMissingState(issueKey, stateName);
            }
            return index;
        }

        public String getCode() {
            return linkedProjectConfig.getCode();
        }
    }
}

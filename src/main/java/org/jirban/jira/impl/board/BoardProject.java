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
import static org.jirban.jira.impl.Constants.RANK;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanLogger;
import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.CustomFieldConfig;
import org.jirban.jira.impl.config.LinkedProjectConfig;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.clause.Clause;
import com.atlassian.query.order.SortOrder;

/**
 * The data for a board project, i.e. a project whose issues should appear as cards on the board.
 *
 * @author Kabir Khan
 */
public class BoardProject {

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

    int getAssigneeIndex(Assignee assignee) {
        return board.getAssigneeIndex(assignee);
    }

    int getComponentIndex(Component component) {
        return board.getComponentIndex(component);
    }

    public int getCustomFieldValueIndex(CustomFieldValue customFieldValue) {
        return board.getCustomFieldIndex(customFieldValue);
    }


    void serialize(JiraInjectables jiraInjectables, ModelNode parent, ApplicationUser user, boolean backlog) {
        parent.get(RANK).set(hasRankPermission(user, jiraInjectables.getProjectManager(), jiraInjectables.getPermissionManager()));
        ModelNode projectIssues = parent.get(ISSUES);

        for (int i = 0 ; i < issueKeysByState.size() ; i++) {

            ModelNode issuesForStateNode = new ModelNode();
            issuesForStateNode.setEmptyList();

            if (backlog || !board.getConfig().isBacklogState(i)) {
                List<String> issuesForState = issueKeysByState.get(i);
                for (String key: issuesForState) {
                    issuesForStateNode.add(key);
                }
            }

            projectIssues.add(issuesForStateNode);
        }
    }

    public List<String> getIssuesForOwnState(String ownState) {
        int stateIndex = projectConfig.mapOwnStateOntoBoardStateIndex(ownState);
        return issueKeysByState.get(stateIndex);
    }

    boolean isOwner() {
        return board.getConfig().getOwnerProjectCode().equals(projectConfig.getCode());
    }

    static Builder builder(JiraInjectables jiraInjectables, Board.Builder builder, BoardProjectConfig projectConfig,
                           ApplicationUser boardOwner) {
        return new Builder(jiraInjectables, builder, projectConfig, boardOwner);
    }

    static LinkedProjectContext linkedProjectContext(Board.Accessor board, LinkedProjectConfig linkedProjectConfig) {
        return new LinkedProjectContext(board, linkedProjectConfig);
    }

    public BoardProject copyAndDeleteIssue(Issue deleteIssue) throws SearchException {
        Updater updater = new Updater(null, null, this, null);
        updater.deleteIssue(deleteIssue);
        return updater.build();
    }

    public Updater updater(JiraInjectables jiraInjectables, Board.Updater boardUpdater,
                           ApplicationUser boardOwner) {
        return new Updater(jiraInjectables, boardUpdater, this, boardOwner);
    }

    public boolean isBacklogState(String state) {
        return projectConfig.isBacklogState(state);
    }

    public boolean isDoneState(String state) {
        return projectConfig.isDoneState(state);
    }

    public String getCode() {
        return projectConfig.getCode();
    }


    private boolean hasRankPermission(ApplicationUser user, ProjectManager projectManager, PermissionManager permissionManager) {
        Project project = projectManager.getProjectByCurrentKey(projectConfig.getCode());
        if (!permissionManager.hasPermission(ProjectPermissions.SCHEDULE_ISSUES, project, user)) {
            return false;
        }
        return true;
    }

    static class Accessor {
        protected final JiraInjectables jiraInjectables;
        protected final Board.Accessor board;
        protected final BoardProjectConfig projectConfig;
        protected final ApplicationUser boardOwner;

        public Accessor(JiraInjectables jiraInjectables, Board.Accessor board, BoardProjectConfig projectConfig, ApplicationUser boardOwner) {
            this.jiraInjectables = jiraInjectables;
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
//                if (!projectConfig.isOwner()) {
//                    Integer ownerStateIndex = null;
//                    String ownerState = projectConfig.mapOwnStateOntoBoardState(stateName);
//                    if (ownerState != null) {
//                        ownerStateIndex = board.getOwningProject().getStateIndex(ownerState);
//                    }
//                    if (ownerStateIndex == null) {
//                        //This was not mapped to a valid owner state so report the problem
//                        board.addMissingState(issueKey, ownerState != null ? ownerState : stateName);
//                        return null;
//                    }
//                    //Do not return the owner state index here although all was fine. The calculation of the columns
//                    //depends on everything using their own state index.
//                }
            }
            return index;
        }

        IssueLinkManager getIssueLinkManager() {
            return jiraInjectables.getIssueLinkManager();
        }

        Assignee getAssignee(User assigneeUser) {
            return board.getAssignee(assigneeUser);
        }

        CustomFieldManager getCustomFieldManager() {
            return jiraInjectables.getCustomFieldManager();
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

        public Board.Accessor getBoard() {
            return board;
        }

        public CustomFieldValue getCustomFieldValue(CustomFieldConfig customField, Object fieldValue) {
            return board.getCustomFieldValue(customField, fieldValue);
        }

        public CustomFieldValue getCustomFieldValue(CustomFieldConfig customField, String key) {
            return board.getCustomFieldValue(customField, key);
        }

        public JiraInjectables getJiraInjectables() {
            return jiraInjectables;
        }
    }

    /**
     * Used to load a project when creating a new board
     */
    static class Builder extends Accessor {
        private final Map<String, List<String>> issueKeysByState = new HashMap<>();


        private Builder(JiraInjectables jiraInjectables, Board.Accessor board, BoardProjectConfig projectConfig,
                        ApplicationUser boardOwner) {
            super(jiraInjectables, board, projectConfig, boardOwner);
        }

        Builder addIssue(String state, Issue issue) {
            final List<String> issueKeys = issueKeysByState.computeIfAbsent(state, l -> new ArrayList<>());
            issueKeys.add(issue.getKey());
            board.addIssue(issue);
            return this;
        }

        void load() throws SearchException {
            final JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
            queryBuilder.where().project(projectConfig.getCode());
            if (projectConfig.getOwnDoneStateNames().size() > 0) {
                queryBuilder.where().and().not().addStringCondition("status", projectConfig.getOwnDoneStateNames());
            }
            queryBuilder.orderBy().addSortForFieldName("Rank", SortOrder.ASC, true);
            Query query = queryBuilder.buildQuery();

            final SearchService searchService = jiraInjectables.getSearchService();
            if (projectConfig.getQueryFilter() != null) {
                final SearchService.ParseResult parseResult = searchService.parseQuery(
                        boardOwner.getDirectoryUser(),
                        "(" + projectConfig.getQueryFilter() + ")");
                if (!parseResult.isValid()) {
                    throw new RuntimeException("The query-filter for " + projectConfig.getCode() + ": '" + projectConfig.getQueryFilter() + "' could not be parsed");
                }
                final JqlQueryBuilder queryWithFilterBuilder = JqlQueryBuilder.newBuilder(query);
                final Clause clause =  JqlQueryBuilder.newClauseBuilder(parseResult.getQuery()).buildClause();
                query = queryWithFilterBuilder.where().and().addClause(clause).buildQuery();
            }

            SearchResults searchResults =
                        searchService.search(boardOwner.getDirectoryUser(), query, PagerFilter.getUnlimitedFilter());

            for (com.atlassian.jira.issue.Issue jiraIssue : searchResults.getIssues()) {
                Issue.Builder issueBuilder = Issue.builder(this);
                issueBuilder.load(jiraIssue);
                Issue issue = issueBuilder.build();
                if (issue != null) {
                    addIssue(issue.getState(), issue);
                }
            }
        }

        BoardProject build() {
            final List<List<String>> resultIssues = new ArrayList<>();
            for (String state : board.getStateNames()) {
                List<String> issues = issueKeysByState.get(projectConfig.mapBoardStateOntoOwnState(state));
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


        Updater(JiraInjectables jiraInjectables, Board.Accessor board, BoardProject project,
                       ApplicationUser boardOwner) {
            super(jiraInjectables, board, project.projectConfig, boardOwner);
            JirbanLogger.LOGGER.debug("BoardProject.Updater - init {}", project.projectConfig.getCode());
            this.project = project;
        }

        Issue createIssue(String issueKey, String issueType, String priority, String summary,
                          Assignee assignee, Set<Component> issueComponents, String state,
                          Map<Long, String> customFieldValues) {
            JirbanLogger.LOGGER.debug("BoardProject.Updater.createIssue - {}", issueKey);
            newIssue = Issue.createForCreateEvent(
                    this, issueKey, state, summary, issueType, priority, assignee, issueComponents, customFieldValues);
            JirbanLogger.LOGGER.debug("BoardProject.Updater.createIssue - created {}", newIssue);
            updatedState = newIssue != null;
            return newIssue;
        }

        Issue updateIssue(Issue existing, String issueType, String priority, String summary,
                          Assignee issueAssignee, Set<Component> issueComponents, boolean rankOrStateChanged, String state) {
            JirbanLogger.LOGGER.debug("BoardProject.Updater.updateIssue - {}, rankOrStateChanged: {}", existing.getKey(), rankOrStateChanged);
            this.existing = existing;
            newIssue = existing.copyForUpdateEvent(this, existing, issueType, priority, summary, issueAssignee, issueComponents, state);
            if (newIssue == null && rankOrStateChanged) {
                newIssue = existing;
            }
            JirbanLogger.LOGGER.debug("BoardProject.Updater - updated issue {} to {}", existing, newIssue);
            this.updatedState = rankOrStateChanged && newIssue != null;
            return newIssue;
        }

        void deleteIssue(Issue issue) {
            this.existing = issue;

        }


        Issue loadSingleIssue(String issueKey) throws SearchException {
            JirbanLogger.LOGGER.debug("BoardProject.Updater.loadSingleIssue - {}", issueKey);
            JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
            queryBuilder.where().issue(issueKey);

            final SearchService searchService = jiraInjectables.getSearchService();

            SearchResults searchResults =
                    searchService.search(boardOwner.getDirectoryUser(), queryBuilder.buildQuery(), PagerFilter.getUnlimitedFilter());

            List<com.atlassian.jira.issue.Issue> issues = searchResults.getIssues();
            if (issues.size() == 0) {
                JirbanLogger.LOGGER.debug("BoardProject.Updater.loadSingleIssue - no issue found");
                return null;
            }
            Issue.Builder issueBuilder = Issue.builder(this);
            issueBuilder.load(issues.get(0));
            newIssue = issueBuilder.build();
            JirbanLogger.LOGGER.debug("BoardProject.Updater.loadSingleIssue - found {}", newIssue);
            this.updatedState = true;
            return newIssue;
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
                if (project.projectConfig.isUnorderedState(toIndex)) {
                    //Copy the old issues, and add this new one on the end
                    toStateIssues = new ArrayList<>(project.issueKeysByState.get(toIndex));
                    toStateIssues.add(newIssue.getKey());
                } else {
                    toStateIssues = rankIssuesForState();
                }
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

        private List<String> rankIssuesForState() throws SearchException {
            JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
            queryBuilder.where().project(projectConfig.getCode()).and().status(newIssue.getState());
            if (projectConfig.getQueryFilter() != null) {
                queryBuilder.where().addCondition(projectConfig.getQueryFilter());
            }
            //TODO if it is possible to narrow this down to only get the product keys that would be better than loading everything
            queryBuilder.orderBy().addSortForFieldName("Rank", SortOrder.ASC, true);

            final SearchService searchService = jiraInjectables.getSearchService();

            SearchResults searchResults =
                    searchService.search(boardOwner.getDirectoryUser(), queryBuilder.buildQuery(), PagerFilter.getUnlimitedFilter());

            List<String> issues = new ArrayList<>();
            for (com.atlassian.jira.issue.Issue jiraIssue : searchResults.getIssues()) {
                String issueKey = jiraIssue.getKey();
                Issue issue = board.getIssue(issueKey);
                if (issue == null) {
                    JirbanLogger.LOGGER.warn("BoardProject.Updater.rankIssuesForState - No existing issue found for {}", issueKey);
                    continue;
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

    static class SingleLoadedIssueWrapper {
        private final com.atlassian.jira.issue.Issue jiraIssue;
        private final Issue issue;

        public SingleLoadedIssueWrapper(com.atlassian.jira.issue.Issue jiraIssue, Issue issue) {
            this.jiraIssue = jiraIssue;
            this.issue = issue;
        }

        Issue getIssue() {
            return issue;
        }
    }
}

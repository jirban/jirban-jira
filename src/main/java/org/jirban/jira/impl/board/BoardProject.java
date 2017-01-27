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

import static org.jirban.jira.impl.Constants.PARALLEL_TASKS;
import static org.jirban.jira.impl.Constants.RANK;
import static org.jirban.jira.impl.Constants.RANKED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanLogger;
import org.jirban.jira.api.NextRankedIssueUtil;
import org.jirban.jira.api.ProjectParallelTaskOptionsLoader;
import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.board.MultiSelectNameOnlyValue.Component;
import org.jirban.jira.impl.board.MultiSelectNameOnlyValue.FixVersion;
import org.jirban.jira.impl.board.MultiSelectNameOnlyValue.Label;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.CustomFieldConfig;
import org.jirban.jira.impl.config.LinkedProjectConfig;

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
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.Consumer;
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
    private final List<String> rankedIssueKeys;
    private final Map<String, SortedParallelTaskFieldOptions> parallelTaskValues;

    private BoardProject(BoardProjectConfig projectConfig, List<String> rankedIssueKeys, Map<String, SortedParallelTaskFieldOptions> parallelTaskValues) {
        this.projectConfig = projectConfig;
        this.rankedIssueKeys = rankedIssueKeys;
        this.parallelTaskValues = parallelTaskValues;
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

    int getLabelIndex(Label label) {
        return board.getLabelIndex(label);
    }

    int getFixVersionIndex(FixVersion fixVersion) {
        return board.getFixVersionIndex(fixVersion);
    }

    public int getCustomFieldValueIndex(CustomFieldValue customFieldValue) {
        return board.getCustomFieldIndex(customFieldValue);
    }

    public List<String> getRankedIssueKeys() {
        return rankedIssueKeys;
    }

    void serialize(JiraInjectables jiraInjectables, Board board, ModelNode parent, ApplicationUser user, boolean backlog) {
        //Whether the user can rank issues or not
        parent.get(RANK).set(hasRankPermission(user, jiraInjectables.getProjectManager(), jiraInjectables.getPermissionManager()));

        ModelNode ranked = new ModelNode();
        ranked.setEmptyList();
        for (String key : rankedIssueKeys) {
            if (backlog) {
                ranked.add(key);
            } else if (!board.isBacklogIssue(projectConfig, key)) {
                ranked.add(key);
            }
        }
        parent.get(RANKED).set(ranked);

        if (parallelTaskValues.size() > 0) {
            ModelNode parallelTasks = parent.get(PARALLEL_TASKS).setEmptyList();
            getParallelTaskValues().values().forEach(
                    sortedCustomFieldValues -> sortedCustomFieldValues.serialize(parallelTasks));
        }

    }

    boolean isOwner() {
        return board.getConfig().getOwnerProjectCode().equals(projectConfig.getCode());
    }

    static Builder builder(JiraInjectables jiraInjectables, ProjectParallelTaskOptionsLoader projectParallelTaskOptionsLoader, Board.Builder builder, BoardProjectConfig projectConfig,
                           ApplicationUser boardOwner) {
        Map<String, SortedParallelTaskFieldOptions> parallelTaskValues = projectParallelTaskOptionsLoader.loadValues(jiraInjectables, builder.getConfig(), projectConfig);
        parallelTaskValues = Collections.unmodifiableMap(parallelTaskValues);
        return new Builder(jiraInjectables, builder, projectConfig, boardOwner, parallelTaskValues);
    }

    static LinkedProjectContext linkedProjectContext(Board.Accessor board, LinkedProjectConfig linkedProjectConfig) {
        return new LinkedProjectContext(board, linkedProjectConfig);
    }

    public BoardProject copyAndDeleteIssue(Issue deleteIssue) throws SearchException {
        Updater updater = new Updater(null, null, null, this, null);
        updater.deleteIssue(deleteIssue);
        return updater.build();
    }

    public Updater updater(JiraInjectables jiraInjectables, NextRankedIssueUtil nextRankedIssueUtil, Board.Updater boardUpdater,
                           ApplicationUser boardOwner) {
        return new Updater(jiraInjectables, nextRankedIssueUtil, boardUpdater, this, boardOwner);
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

    public Map<String, SortedParallelTaskFieldOptions> getParallelTaskValues() {
        return parallelTaskValues;
    }

    private boolean hasRankPermission(ApplicationUser user, ProjectManager projectManager, PermissionManager permissionManager) {
        Project project = projectManager.getProjectByCurrentKey(projectConfig.getCode());
        if (!permissionManager.hasPermission(ProjectPermissions.SCHEDULE_ISSUES, project, user)) {
            return false;
        }
        return true;
    }

    public static Query initialiseQuery(BoardProjectConfig projectConfig, ApplicationUser boardOwner,
                                        SearchService searchService, Consumer<JqlQueryBuilder> queryAddition) {
        JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        queryBuilder.where().project(projectConfig.getCode());
        if (projectConfig.getOwnDoneStateNames().size() > 0) {
            queryBuilder.where().and().not().addStringCondition("status", projectConfig.getOwnDoneStateNames());
        }
        queryBuilder.orderBy().addSortForFieldName("Rank", SortOrder.ASC, true);
        if (projectConfig.getQueryFilter() != null) {
            final SearchService.ParseResult parseResult = searchService.parseQuery(
                    boardOwner,
                    "(" + projectConfig.getQueryFilter() + ")");
            if (!parseResult.isValid()) {
                throw new RuntimeException("The query-filter for " + projectConfig.getCode() + ": '" + projectConfig.getQueryFilter() + "' could not be parsed");
            }
            queryBuilder = JqlQueryBuilder.newBuilder(queryBuilder.buildQuery());
            final Clause clause =  JqlQueryBuilder.newClauseBuilder(parseResult.getQuery()).buildClause();
            queryBuilder.where().and().addClause(clause);
        }

        if (queryAddition != null) {
            queryAddition.consume(queryBuilder);
        }

        return queryBuilder.buildQuery();
    }

    public static abstract class Accessor {
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

        Assignee getAssignee(ApplicationUser assigneeUser) {
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

        public Set<Label> getLabels(Set<com.atlassian.jira.issue.label.Label> labels) {
            return board.getLabels(labels);
        }

        public Set<FixVersion> getFixVersions(Collection<Version> fixVersions) {
            return board.getFixVersions(fixVersions);
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

        abstract Map<String, SortedParallelTaskFieldOptions> getParallelTaskValues();

    }

    /**
     * Used to load a project when creating a new board
     */
    public static class Builder extends Accessor {
        private final List<String> rankedIssueKeys = new ArrayList<>();
        private final Map<String, List<String>> issueKeysByState = new HashMap<>();
        private final Map<String, SortedParallelTaskFieldOptions> parallelTaskValues;


        private Builder(JiraInjectables jiraInjectables, Board.Accessor board, BoardProjectConfig projectConfig,
                        ApplicationUser boardOwner, Map<String, SortedParallelTaskFieldOptions> parallelTaskValues) {
            super(jiraInjectables, board, projectConfig, boardOwner);
            this.parallelTaskValues = parallelTaskValues;
        }

        Builder addIssue(String state, Issue issue) {
            final List<String> issueKeys = issueKeysByState.computeIfAbsent(state, l -> new ArrayList<>());
            issueKeys.add(issue.getKey());
            board.addIssue(issue);
            return this;
        }

        public Map<String, SortedParallelTaskFieldOptions> getParallelTaskValues() {
            return parallelTaskValues;
        }

        void load() throws SearchException {
            final SearchService searchService = jiraInjectables.getSearchService();
            final Query query = initialiseQuery(projectConfig, boardOwner, searchService, null);

            SearchResults searchResults =
                        searchService.search(boardOwner, query, PagerFilter.getUnlimitedFilter());

            final BulkIssueLoadStrategy issueLoadStrategy = BulkIssueLoadStrategy.create(this);
            List<Issue.Builder> issueBuilders = new ArrayList<>();
            for (com.atlassian.jira.issue.Issue jiraIssue : searchResults.getIssues()) {
                Issue.Builder issueBuilder = Issue.builder(this, issueLoadStrategy);
                issueBuilder.load(jiraIssue);
                issueBuilders.add(issueBuilder);
                if (!board.getBlacklist().isBlackListed(jiraIssue.getKey())) {
                    rankedIssueKeys.add(jiraIssue.getKey());
                }
            }
            issueBuilders.forEach(issueBuilder -> {
                Issue issue = issueBuilder.build();
                if (issue != null) {
                    addIssue(issue.getState(), issue);
                }});
        }

        BoardProject build() {
            return new BoardProject(
                    projectConfig,
                    Collections.unmodifiableList(rankedIssueKeys),
                    Collections.unmodifiableMap(parallelTaskValues));
        }

        public void addBulkLoadedCustomFieldValue(CustomFieldConfig customFieldConfig, CustomFieldValue value) {
            board.addBulkLoadedCustomFieldValue(customFieldConfig, value);
        }
    }

    /**
     * Used to update an existing board
     */
    static class Updater extends Accessor {
        private final BoardProject project;
        private final NextRankedIssueUtil nextRankedIssueUtil;
        private Issue newIssue;
        private List<String> rankedIssueKeys;


        Updater(JiraInjectables jiraInjectables, NextRankedIssueUtil nextRankedIssueUtil, Board.Accessor board, BoardProject project,
                       ApplicationUser boardOwner) {
            super(jiraInjectables, board, project.projectConfig, boardOwner);
            this.nextRankedIssueUtil = nextRankedIssueUtil;
            JirbanLogger.LOGGER.debug("BoardProject.Updater - init {}", project.projectConfig.getCode());
            this.project = project;
        }

        Issue createIssue(String issueKey, String issueType, String priority, String summary,
                          Assignee assignee, Set<Component> issueComponents,
                          Set<Label> labels, Set<FixVersion> fixVersions, String state,
                          Map<String, CustomFieldValue> customFieldValues, Map<Integer, Integer> parallelTaskValues) throws SearchException {
            JirbanLogger.LOGGER.debug("BoardProject.Updater.createIssue - {}", issueKey);
            newIssue = Issue.createForCreateEvent(
                    this, issueKey, state, summary, issueType, priority,
                    assignee, issueComponents, labels, fixVersions, customFieldValues, parallelTaskValues);
            JirbanLogger.LOGGER.debug("BoardProject.Updater.createIssue - created {}", newIssue);

            if (newIssue != null) {
                rankedIssueKeys = rankIssues(issueKey);
            }
            return newIssue;
        }

        Issue updateIssue(Issue existing, String issueType, String priority, String summary,
                          Assignee issueAssignee, Set<Component> issueComponents,
                          Set<Label> labels, Set<FixVersion> fixVersions, boolean reranked,
                          String state, Map<String, CustomFieldValue> customFieldValues,
                          Map<Integer, Integer> parallelTaskValues) throws SearchException {
            JirbanLogger.LOGGER.debug("BoardProject.Updater.updateIssue - {}, rankOrStateChanged: {}", existing.getKey(), reranked);
            newIssue = existing.copyForUpdateEvent(this, existing, issueType, priority,
                    summary, issueAssignee, issueComponents, labels, fixVersions,
                    state, customFieldValues, parallelTaskValues);
            JirbanLogger.LOGGER.debug("BoardProject.Updater - updated issue {} to {}. Reranked: {}", existing, newIssue, reranked);
            if (reranked) {
                rankedIssueKeys = rankIssues(existing.getKey());
            }
            return newIssue;
        }

        void deleteIssue(Issue issue) {
            rankedIssueKeys = new ArrayList<>(project.rankedIssueKeys);
            rankedIssueKeys.remove(issue.getKey());
        }

        public Map<String, SortedParallelTaskFieldOptions> getParallelTaskValues() {
            return project.getParallelTaskValues();
        }


        List<String> rankIssues(String issueKey) throws SearchException {
            String nextIssueKey = nextRankedIssueUtil.findNextRankedIssue(this.projectConfig, boardOwner, issueKey);
            //If the next issue is blacklisted, keep searching until we find the next valid one
            while (nextIssueKey != null && board.getBlacklist().isBlackListed(nextIssueKey)) {
                nextIssueKey = nextRankedIssueUtil.findNextRankedIssue(this.projectConfig, boardOwner, nextIssueKey);
            }
            final List<String> newRankedKeys = new ArrayList<>();
            if (nextIssueKey == null) {
                //Add it at the end
                project.rankedIssueKeys.forEach(key -> {
                    if (!key.equals(issueKey)) {
                        //Don't copy the one we are moving to the end
                        newRankedKeys.add(key);
                    }
                });
                newRankedKeys.add(issueKey);
            } else {
                final String nextKey = nextIssueKey;
                //Remove it from the middle and add it at the end
                project.rankedIssueKeys.forEach(key -> {
                    if (key.equals(nextKey)) {
                        newRankedKeys.add(issueKey);
                    }
                    if (!key.equals(issueKey)) {
                        newRankedKeys.add(key);
                    }
                });
            }
            return new ArrayList<>(newRankedKeys);
        }

        Issue loadSingleIssue(String issueKey) throws SearchException {
            JirbanLogger.LOGGER.debug("BoardProject.Updater.loadSingleIssue - {}", issueKey);
            JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
            queryBuilder.where().issue(issueKey);

            final SearchService searchService = jiraInjectables.getSearchService();

            SearchResults searchResults =
                    searchService.search(boardOwner, queryBuilder.buildQuery(), PagerFilter.getUnlimitedFilter());

            List<com.atlassian.jira.issue.Issue> issues = searchResults.getIssues();
            if (issues.size() == 0) {
                JirbanLogger.LOGGER.debug("BoardProject.Updater.loadSingleIssue - no issue found");
                return null;
            }
            Issue.Builder issueBuilder = Issue.builder(this, null);
            issueBuilder.load(issues.get(0));
            newIssue = issueBuilder.build();
            JirbanLogger.LOGGER.debug("BoardProject.Updater.loadSingleIssue - found {}", newIssue);
            rankedIssueKeys = rankIssues(issueKey);
            return newIssue;
        }

        BoardProject build() throws SearchException {

            //Update the ranked issue list if a rerank was done
            List<String> rankedIssueKeys =
                    this.rankedIssueKeys != null ?
                            Collections.unmodifiableList(this.rankedIssueKeys) : project.rankedIssueKeys;

            return new BoardProject(projectConfig, rankedIssueKeys, project.parallelTaskValues);
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

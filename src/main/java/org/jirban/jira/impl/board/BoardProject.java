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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.LinkedProjectConfig;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.priority.Priority;
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
    private final List<List<Issue>> issuesByState;

    private BoardProject(BoardProjectConfig projectConfig, List<List<Issue>> issuesByState) {
        this.projectConfig = projectConfig;
        this.issuesByState = issuesByState;
    }

    void setBoard(Board board) {
        this.board = board;
    }

    boolean isDataSame(BoardProject boardProject) {
        return false;
    }

    int getStateIndex(String state) {
        return 0;
    }

    int getAssigneeIndex(Assignee assignee) {
        return board.getAssigneeIndex(assignee);
    }

    void serialize(ModelNode parent) {
        ModelNode projectIssues = parent.get("issues");
        for (List<Issue> issuesForState : issuesByState) {
            ModelNode issuesForStateNode = new ModelNode();
            issuesForStateNode.setEmptyList();
            for (Issue issue : issuesForState) {
                issuesForStateNode.add(issue.getKey());
            }
            projectIssues.add(issuesForStateNode);
        }
    }

    static Builder builder(SearchService searchService, Board.Builder builder, BoardProjectConfig projectConfig, ApplicationUser boardOwner) {
        return new Builder(searchService, builder, projectConfig, boardOwner);
    }

    static LinkedProjectContext linkedProjectContext(Board.Builder boardBuilder, LinkedProjectConfig linkedProjectConfig) {
        return new LinkedProjectContext(boardBuilder, linkedProjectConfig);
    }

    public BoardProject copyAndDeleteIssue(Issue deleteIssue) {
        List<List<Issue>> issuesByStateCopy = new ArrayList<>();
        int stateIndex = getStateIndex(deleteIssue.getState());
        for (int i = 0 ; i < issuesByState.size() ; i++) {
            if (stateIndex == i) {
                //delete the issue
                List<Issue> issues = issuesByState.get(i);
                List<Issue> issuesCopy = new ArrayList<>(i);
                for (Issue curr : issues) {
                    if (deleteIssue.getKey().equals(curr.getKey())) {
                        continue;
                    }
                    issuesCopy.add(curr);
                }
            } else {
                issuesByStateCopy.add(issuesByState.get(i));
            }
        }
        return null;
    }

    static class Builder {
        private final SearchService searchService;
        private final Board.Builder boardBuilder;
        private final BoardProjectConfig projectConfig;
        private final ApplicationUser boardOwner;
        private final Map<String, List<Issue>> issuesByState = new HashMap<>();


        private Builder(SearchService searchService, Board.Builder boardBuilder, BoardProjectConfig projectConfig, ApplicationUser boardOwner) {
            this.searchService = searchService;
            this.boardBuilder = boardBuilder;
            this.projectConfig = projectConfig;
            this.boardOwner = boardOwner;
        }

        Builder addIssue(String state, Issue issue) {
            final List<Issue> issues = issuesByState.computeIfAbsent(state, l -> new ArrayList<>());
            issues.add(issue);
            boardBuilder.addIssue(issue);
            return this;
        }

        BoardProject build(BoardConfig boardConfig, boolean owner) {
            final List<List<Issue>> resultIssues = new ArrayList<>();
            for (String state : boardBuilder.getOwnerStateNames()) {
                List<Issue> issues = issuesByState.get(owner ? state : projectConfig.mapBoardStateOntoOwnState(state));
                if (issues == null) {
                    issues = new ArrayList<>();
                }
                resultIssues.add(Collections.synchronizedList(issues));
            }

            return new BoardProject(projectConfig, Collections.unmodifiableList(resultIssues));
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

        BoardProjectConfig getConfig() {
            return projectConfig;
        }

        Integer getPriorityIndexRecordingMissing(String issueKey, Priority priorityObject) {
            return boardBuilder.getPriorityIndexRecordingMissing(issueKey, priorityObject);
        }

        Integer getIssueTypeIndexRecordingMissing(String issueKey, IssueType issueTypeObject) {
            return boardBuilder.getIssueTypeIndexRecordingMissing(issueKey, issueTypeObject);
        }

        Integer getStateIndexRecordingMissing(String projectCode, String issueKey, String stateName) {
            final Integer index = projectConfig.getStateIndex(stateName);
            if (index == null) {
                boardBuilder.addMissingState(issueKey, stateName);
            } else {
                if (!projectConfig.isOwner()) {
                    Integer ownerStateIndex = null;
                    String ownerState = projectConfig.mapOwnStateOntoBoardState(stateName);
                    if (ownerState != null) {
                        ownerStateIndex = boardBuilder.getOwningProject().getStateIndex(ownerState);
                    }
                    if (ownerStateIndex == null) {
                        //This was not mapped to a valid owner state so report the problem
                        boardBuilder.addMissingState(issueKey, ownerState != null ? ownerState : stateName);
                        return null;
                    }
                    //Do not return the owner state index here although all was fine. The calculation of the columns
                    //depends on everything using their own state index.
                }
            }
            return index;
        }

        Assignee getAssignee(User assigneeUser) {
            return boardBuilder.getAssignee(assigneeUser);
        }

        String getCode() {
            return projectConfig.getCode();
        }

        public IssueLinkManager getIssueLinkManager() {
            return boardBuilder.getIssueLinkManager();
        }

        public LinkedProjectContext getLinkedProjectBuilder(String linkedProjectCode) {
            return boardBuilder.getLinkedProjectBuilder(linkedProjectCode);
        }
    }

    static class LinkedProjectContext {

        private final Board.Builder boardBuilder;
        private final LinkedProjectConfig linkedProjectConfig;

        LinkedProjectContext(Board.Builder boardBuilder, LinkedProjectConfig linkedProjectConfig) {
            this.boardBuilder = boardBuilder;
            this.linkedProjectConfig = linkedProjectConfig;
        }

        public LinkedProjectConfig getConfig() {
            return linkedProjectConfig;
        }

        Integer getStateIndexRecordingMissing(String projectCode, String issueKey, String stateName) {
            final Integer index = linkedProjectConfig.getStateIndex(stateName);
            if (index == null) {
                boardBuilder.addMissingState(issueKey, stateName);
            }
            return index;
        }

        public String getCode() {
            return linkedProjectConfig.getCode();
        }
    }
}

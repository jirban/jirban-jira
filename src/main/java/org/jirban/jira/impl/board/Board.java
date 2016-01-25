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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.search.SearchException;

/**
 * The data for a board.
 *
 * @author Kabir Khan
 */
public class Board {
    //This is incremented every time a change is made to the board
    volatile int currentView = 0;

    private final BoardConfig boardConfig;

    private final Map<String, Assignee> assignees;
    private final Map<String, Issue> allIssues;
    private final Map<String, BoardProject> projects;

    private final Map<String, List<String>> missingIssueTypes;
    private final Map<String, List<String>> missingPriorities;
    private final Map<String, List<String>> missingStates;

    public Board(BoardConfig boardConfig,
                 Map<String, Assignee> assignees,
                 Map<String, Issue> allIssues,
                 Map<String, BoardProject> projects,
                 Map<String, List<String>> missingIssueTypes,
                 Map<String, List<String>> missingPriorities,
                 Map<String, List<String>> missingStates) {
        this.boardConfig = boardConfig;
        this.assignees = assignees;
        this.allIssues = allIssues;
        this.projects = projects;
        this.missingIssueTypes = missingIssueTypes;
        this.missingPriorities = missingPriorities;
        this.missingStates = missingStates;
    }

    public static Builder builder(SearchService searchService, User user, BoardConfig boardConfig) {
        return new Builder(searchService, user, boardConfig);
    }

    public ModelNode serialize() {
        ModelNode outputNode = new ModelNode();
        outputNode.get("jira-url").set(boardConfig.getJiraUrl());
        //Sort the assignees by name
        ModelNode assigneesNode = outputNode.get("assignees");
        assigneesNode.setEmptyList();
        List<Assignee> assigneeNames = new ArrayList<>(assignees.values());
        Collections.sort(assigneeNames, (a1, a2) -> a1.getDisplayName().compareTo(a2.getDisplayName()));
        int assigneeIndex = 0;
        for (Assignee assignee : assigneeNames) {
            assignees.get(assignee.getKey()).serialize(assigneeIndex++, assigneesNode);
        }

        boardConfig.serializeModelNode(outputNode);

        ModelNode allIssues = outputNode.get("issues");
        this.allIssues.forEach((code, issue) -> {
            if (issue.isValid()) {
                allIssues.get(code).set(issue.toModelNode(this));
            }
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

    public static class Builder {
        private final User user;
        private final BoardConfig boardConfig;
        private final SearchService searchService;

        private final Map<String, Assignee> assignees = new TreeMap<>();
        private final Map<String, Issue> allIssues = new HashMap<>();
        private final Map<String, BoardProject.Builder> projects = new HashMap<>();

        private final Map<String, List<String>> missingIssueTypes = new TreeMap<>();
        private final Map<String, List<String>> missingPriorities = new TreeMap<>();
        private final Map<String, List<String>> missingStates = new TreeMap<>();

        public Builder(SearchService searchService, User user, BoardConfig boardConfig) {
            this.searchService = searchService;
            this.user = user;
            this.boardConfig = boardConfig;
        }

        public Builder load() throws SearchException {
            for (BoardProjectConfig boardProjectConfig : boardConfig.getBoardProjects()) {
                BoardProjectConfig project = boardConfig.getBoardProject(boardProjectConfig.getCode());
                BoardProject.Builder projectBuilder = BoardProject.builder(searchService, user, this, project);
                projectBuilder.load();
                projects.put(projectBuilder.getCode(), projectBuilder);
            }
            return this;
        }

        Builder addIssue(Issue issue) {
            allIssues.put(issue.getKey(), issue);
            return this;
        }

        public Board build() {
            Map<String, BoardProject> projects = new LinkedHashMap<>();

            BoardProject.Builder ownerProject = this.projects.remove(boardConfig.getOwnerProjectCode());
            projects.put(boardConfig.getOwnerProjectCode(), ownerProject.build(boardConfig, true));

            this.projects.forEach((name, projectBuilder) -> {
                if (boardConfig.getBoardProject(name) != null) {
                    projects.put(name, projectBuilder.build(boardConfig, false));
                }
            });

            return new Board(boardConfig, Collections.unmodifiableMap(assignees),
                    Collections.unmodifiableMap(allIssues), Collections.unmodifiableMap(projects),
                    Collections.unmodifiableMap(missingIssueTypes), Collections.unmodifiableMap(missingPriorities),
                    Collections.unmodifiableMap(missingStates));
        }

        public Assignee getAssignee(User user) {
            if (user != null) {
                //Unassigned issue
                return null;
            }
            Assignee assignee = assignees.get(user.getName());
            if (assignee != null) {
                return assignee;
            }
            assignee = Assignee.create(user);
            assignees.put(user.getName(), assignee);
            return assignee;
        }

        public Integer getIssueTypeIndex(String issueKey, IssueType issueType) {
            final Integer issueTypeIndex = boardConfig.getIssueTypeIndex(issueType.getName());
            if (issueTypeIndex == null) {
                addMissing(missingIssueTypes, issueType.getName(), issueKey);
            }
            return issueTypeIndex;
        }

        public Integer getPriorityIndex(String issueKey, Priority priority) {
            final Integer priorityIndex = boardConfig.getPriorityIndex(priority.getName());
            if (priority == null) {
                addMissing(missingPriorities, priority.getName(), issueKey);
            }
            return priorityIndex;
        }

        private void addMissingState(String issueKey, String stateName) {
            addMissing(missingStates, stateName, issueKey);
        }

        private void addMissing(Map<String, List<String>> missingMap, String mapKey, String issueKey) {
            List<String> missingIssues = missingMap.computeIfAbsent(mapKey, l -> new ArrayList<String>());
            missingIssues.add(issueKey);
        }

        public List<BoardProject.Builder> getMainProjects() {
            List<BoardProject.Builder> builders = new ArrayList<>();
            for (BoardProjectConfig projectConfig : boardConfig.getBoardProjects()) {
                builders.add(projects.computeIfAbsent(projectConfig.getCode(), m -> BoardProject.builder(searchService, user, this, projectConfig)));
            }
            return builders;
        }

        private BoardProject.Builder getProject(String projectCode) {
            BoardProjectConfig mainCfg = boardConfig.getBoardProject(projectCode);
            //TODO
            return null;
            //BoardProjectConfig projectCfg =  mainCfg != null ? mainCfg : boardConfig.getLinkedProject(projectCode);
            //return projects.computeIfAbsent(projectCode, m -> BoardProject.builder(this, projectCfg));
        }

        public BoardProject.Builder getLinkedProjectFromIssueKey(String key) {
            return null;
//            LinkedProjectConfig projectCfg = boardConfig.getLinkedProjectFromIssueKey(key);
//            if (projectCfg == null) {
//                return null;
//            }
//            return projects.computeIfAbsent(projectCfg.getCode(), m -> BoardProject.builder(this, projectCfg));
        }

        public BoardProjectConfig getOwningProject() {
            return boardConfig.getOwningProject();
        }

        public Set<String> getOwnerStateNames() {
            return boardConfig.getOwnerStateNames();
        }
    }

}
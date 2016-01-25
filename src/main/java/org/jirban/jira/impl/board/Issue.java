/*
 *
 *  JBoss, Home of Professional Open Source
 *  Copyright 2016, Red Hat, Inc., and individual contributors as indicated
 *  by the @authors tag.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.jirban.jira.impl.board;

import java.util.Collections;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.config.BoardProjectConfig;

import com.atlassian.jira.issue.status.Status;

/**
 * The data for an issue on the board
 *
 * @author Kabir Khan
 */
public abstract class Issue {

    protected final BoardProjectConfig project;
    protected final String key;
    protected final String state;
    protected final String summary;

    Issue(BoardProjectConfig project, String key, String state, String summary) {
        this.project = project;
        this.key = key;
        this.state = state;
        this.summary = summary;
    }

//    static Issue createIssue(final BoardProject.Builder project, final ModelNode modelNode) {
//        String key = modelNode.get(IssueFields.KEY).asString();
//        return createIssue(project, key, modelNode, true);
//    }
//
//    private static Issue createIssue(final BoardProject.Builder project, final String issueKey, final ModelNode modelNode, boolean master) {
//        final ModelNode fields = modelNode.get(IssueFields.FIELDS);
//        final String status = fields.get(IssueFields.STATUS, "name").asString();
//        final String summary = fields.get(IssueFields.SUMMARY).asString();
//        return null;
//
//        final Assignee assignee = project.getProjectGroup().getAssignee(fields.get(IssueFields.ASSIGNEE));
//        final Integer issueType = project.getProjectGroup().getIssueTypeIndex(issueKey, fields.get(IssueFields.ISSUE_TYPE));
//        final Integer priority = project.getProjectGroup().getPriorityIndex(issueKey, fields.get(IssueFields.PRIORITY));
//        final Integer stateIndex = project.getStateIndex(issueKey, status);
//
//        Map<String, List<Issue>> links = null;
//        if (master) {
//            ModelNode issueLinks = fields.get(IssueFields.LINKS);
//            for (ModelNode issueLink : issueLinks.asList()) {
//                //TODO understand better whether this will always be one or the other, or...????
//                ModelNode linkNode = issueLink.get("outwardIssue");
//                if (!linkNode.isDefined()) {
//                    linkNode = issueLink.get("inwardIssue");
//                }
//
//                String linkedKey = linkNode.get(IssueFields.KEY).asString();
//                ProjectGroupData.ProjectData.Builder linkedProject = project.getProjectGroup().getLinkedProjectFromIssueKey(linkedKey);
//                if (linkedProject == null) {
//                    continue;
//                }
//                Issue linkedIssue = createIssue(linkedProject, linkedKey, linkNode, false);
//                if (linkedIssue != null) {
//                    if (links == null) {
//                        links = new TreeMap<>();
//                    }
//                    List<Issue> projectLinks = links.computeIfAbsent(project.getCode(), p -> new ArrayList<>());
//                    projectLinks.add(linkedIssue);
//                }
//            }
//        }
//        List<Issue> linkedIssues = null;
//        if (links != null) {
//            linkedIssues = new ArrayList<>();
//            for (List<Issue> list : links.values()) {
//                for (Issue issue : list) {
//                    linkedIssues.add(issue);
//                }
//            }
//        }
//        return new Issue(project.getConfig(), issueKey, status, stateIndex, summary, assignee, issueType, priority, linkedIssues);
//
//    }


    public String getKey() {
        return key;
    }

    public String getState() {
        return state;
    }

    public String getSummary() {
        return summary;
    }

    boolean hasLinkedIssues() {
        return false;
    }

    Iterable<Issue> getLinkedIssues() {
        return () -> Collections.<Issue>emptySet().iterator();
    }

    public ModelNode toModelNode(Board board) {
        ModelNode issueNode = getBaseModelNode();
        return issueNode;
    }

    private ModelNode getBaseModelNode() {
        ModelNode issueNode = new ModelNode();
        issueNode.get("key").set(key);
        issueNode.get("state").set(project.getStateIndex(state));
        issueNode.get("summary").set(summary);
        return issueNode;
    }

    public abstract boolean isValid();

    public boolean isDataSame(Issue that) {
        if (that == null) {
            return false;
        }
        //I don't want to do a standard equals() since I am not comparing all the data
        if (!key.equals(that.key)) return false;
        if (!state.equals(that.state)) return false;
        if (!summary.equals(that.summary)) return false;

        return true;
    }

    public String getProjectCode() {
        return project.getCode();
    }

    /**
     * Returns a builder for the board issues. Linked issues are handled internally
     *
     * @param project the builder for the project containing the issues
     * @return the builder
     */
    static Builder builder(BoardProject.Builder project) {
        return new Builder(project);
    }

    private static class BoardIssue extends Issue {
        private final Assignee assignee;
        /** The index of the issue type in the owning board config */
        private final Integer issueTypeIndex;
        /** The index of the priority in the owning board config */
        private final Integer priorityIndex;
        private final List<Issue> linkedIssues;

        public BoardIssue(BoardProjectConfig project, String key, String state, String summary, Integer issueTypeIndex, Integer priorityIndex, Assignee assignee, List<Issue> linkedIssues) {
            super(project, key, state, summary);
            this.issueTypeIndex = issueTypeIndex;
            this.priorityIndex = priorityIndex;
            this.assignee = assignee;
            this.linkedIssues = linkedIssues == null ? Collections.emptyList() : Collections.unmodifiableList(linkedIssues);
        }

        boolean hasLinkedIssues() {
            return linkedIssues.size() > 0;
        }

        Iterable<Issue> getLinkedIssues() {
            return () -> linkedIssues.iterator();
        }

        @Override
        public boolean isDataSame(Issue that) {
            boolean same = super.isDataSame(that);
            if (!same) {
                return false;
            }
            if (that instanceof BoardIssue == false) {
                return false;
            }
            BoardIssue thatBi = (BoardIssue)that;
            if (!assignee.isDataSame(thatBi.assignee)) return false;
            if (!priorityIndex.equals(thatBi.priorityIndex)) return false;
            if (!issueTypeIndex.equals(thatBi.issueTypeIndex)) return false;
            if (linkedIssues.size() != thatBi.linkedIssues.size()) {
                for (int i = 0 ; i < linkedIssues.size() ; i++) {
                    if (!linkedIssues.get(i).equals(thatBi.linkedIssues.get(i))) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public ModelNode toModelNode(Board board) {
            BoardProject boardProject = board.getBoardProject(project.getCode());
            ModelNode issueNode = super.toModelNode(board);
            issueNode.get("priority").set(priorityIndex);
            issueNode.get("type").set(issueTypeIndex);
            if (assignee != null) {
                issueNode.get("assignee").set(boardProject.getAssigneeIndex(assignee));
            }
            if (hasLinkedIssues()) {
                ModelNode linkedIssuesNode = issueNode.get("linked-issues");
                for (Issue linkedIssue : linkedIssues) {
                    if (linkedIssue.isValid()) {
                        ModelNode linkedIssueNode = linkedIssue.toModelNode(board);
                        linkedIssuesNode.add(linkedIssueNode);
                    }
                }
            }
            return issueNode;
        }

        @Override
        public boolean isValid() {
            //return (project.isLinked() && stateIndex != null)
            return issueTypeIndex != null && priorityIndex != null && project.getStateIndex(state) != null;
        }
    }

    private static class LinkedIssue extends Issue {
        public LinkedIssue(BoardProjectConfig project, String key, String state, String summary) {
            super(project, key, state, summary);
        }

        @Override
        public boolean isValid() {
            return project.getStateIndex(state) != null;
        }
    }

    /**
     * The builder for the board issues
     */
    static class Builder {
        private final BoardProject.Builder project;

        String issueKey ;
        Status status;
        String summary;
        Assignee assignee;
        Integer issueTypeIndex;
        Integer priorityIndex;
        String state;

        private Builder(BoardProject.Builder project) {
            this.project = project;
        }

        void load(com.atlassian.jira.issue.Issue issue) {
            issueKey = issue.getKey();
            status = issue.getStatusObject();
            summary = issue.getSummary();
            assignee = project.getAssignee(issue.getAssignee());
            issueTypeIndex = project.getIssueTypeIndex(issueKey, issue.getIssueTypeObject());
            priorityIndex = project.getPriorityIndex(issueKey, issue.getPriorityObject());
            state = issue.getStatusObject().getName();

            //TODO linked objects


        }

        Issue build() {
            return new BoardIssue(project.getConfig(), issueKey, state, summary, issueTypeIndex, priorityIndex, assignee, null);
        }
    }
 }

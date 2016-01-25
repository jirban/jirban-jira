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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.LinkedProjectConfig;
import org.jirban.jira.impl.config.ProjectConfig;

import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.status.Status;

/**
 * The data for an issue on the board
 *
 * @author Kabir Khan
 */
public abstract class Issue {

    protected final ProjectConfig project;
    protected final String key;
    protected final String state;
    protected final Integer stateIndex;
    protected final String summary;

    Issue(ProjectConfig project, String key, String state, Integer stateIndex, String summary) {
        this.project = project;
        this.key = key;
        this.state = state;
        this.stateIndex = stateIndex;
        this.summary = summary;
    }

    String getKey() {
        return key;
    }

    String getState() {
        return state;
    }

    String getSummary() {
        return summary;
    }

    boolean hasLinkedIssues() {
        return false;
    }

    Iterable<LinkedIssue> getLinkedIssues() {
        return () -> Collections.<LinkedIssue>emptySet().iterator();
    }

    ModelNode toModelNode(Board board) {
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

    boolean isDataSame(Issue that) {
        if (that == null) {
            return false;
        }
        //I don't want to do a standard equals() since I am not comparing all the data
        if (!key.equals(that.key)) return false;
        if (!state.equals(that.state)) return false;
        if (!summary.equals(that.summary)) return false;

        return true;
    }

    String getProjectCode() {
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
        private final List<LinkedIssue> linkedIssues;

        public BoardIssue(BoardProjectConfig project, String key, String state, Integer stateIndex, String summary, Integer issueTypeIndex, Integer priorityIndex, Assignee assignee, List<LinkedIssue> linkedIssues) {
            super(project, key, state, stateIndex, summary);
            this.issueTypeIndex = issueTypeIndex;
            this.priorityIndex = priorityIndex;
            this.assignee = assignee;
            this.linkedIssues = linkedIssues == null ? Collections.emptyList() : Collections.unmodifiableList(linkedIssues);
        }

        boolean hasLinkedIssues() {
            return linkedIssues.size() > 0;
        }

        Iterable<LinkedIssue> getLinkedIssues() {
            return linkedIssues::iterator;
        }

        @Override
        boolean isDataSame(Issue that) {
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
        ModelNode toModelNode(Board board) {
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
                    ModelNode linkedIssueNode = linkedIssue.toModelNode(board);
                    linkedIssuesNode.add(linkedIssueNode);
                }
            }
            return issueNode;
        }
    }

    private static class LinkedIssue extends Issue {
        public LinkedIssue(LinkedProjectConfig project, String key, String state, Integer stateIndex, String summary) {
            super(project, key, state, stateIndex, summary);
        }

    }

    /**
     * The builder for the board issues
     */
    static class Builder {
        private final BoardProject.Builder project;

        String issueKey;
        Status status;
        String summary;
        Assignee assignee;
        Integer issueTypeIndex;
        Integer priorityIndex;
        String state;
        Integer stateIndex;
        Set<LinkedIssue> linkedIssues;

        private Builder(BoardProject.Builder project) {
            this.project = project;
        }

        void load(com.atlassian.jira.issue.Issue issue) {
            issueKey = issue.getKey();
            status = issue.getStatusObject();
            summary = issue.getSummary();
            assignee = project.getAssignee(issue.getAssignee());
            issueTypeIndex = project.getIssueTypeIndexRecordingMissing(issueKey, issue.getIssueTypeObject());
            priorityIndex = project.getPriorityIndexRecordingMissing(issueKey, issue.getPriorityObject());
            state = issue.getStatusObject().getName();
            stateIndex = project.getStateIndexRecordingMissing(project.getCode(), issueKey, state);

            //TODO linked objects
            final IssueLinkManager issueLinkManager = project.getIssueLinkManager();
            addLinkedIssues(issueLinkManager.getOutwardLinks(issue.getId()), true);
            addLinkedIssues(issueLinkManager.getInwardLinks(issue.getId()), false);
        }

        private void addLinkedIssues(List<IssueLink> links, boolean outbound) {
            if (links == null) {
                return;
            }
            if (links.size() == 0) {
                return;
            }
            for (IssueLink link : links) {
                com.atlassian.jira.issue.Issue linkedIssue = outbound ? link.getDestinationObject() : link.getSourceObject();
                String linkedProjectKey = linkedIssue.getProjectObject().getKey();
                BoardProject.LinkedProjectContext linkedProjectContext = project.getLinkedProjectBuilder(linkedProjectKey);
                if (linkedProjectContext == null) {
                    //This was not set up as one of the linked projects we are interested in
                    continue;
                }
                String stateName = linkedIssue.getStatusObject().getName();
                Integer stateIndex = linkedProjectContext.getStateIndexRecordingMissing(linkedProjectContext.getCode(), linkedIssue.getKey(), stateName);
                if (stateIndex !=null) {
                    if (linkedIssues == null) {
                        linkedIssues = new TreeSet<>(new Comparator<LinkedIssue>() {
                            @Override
                            public int compare(LinkedIssue o1, LinkedIssue o2) {
                                return o1.getKey().compareTo(o2.getKey());
                            }
                        });
                    }
                    linkedIssues.add(new LinkedIssue(linkedProjectContext.getConfig(), linkedIssue.getKey(),
                            stateName, stateIndex, linkedIssue.getSummary()));
                }
            }
        }

        Issue build() {
            if (issueTypeIndex != null && priorityIndex != null && stateIndex != null) {
                return new BoardIssue(project.getConfig(), issueKey, state, stateIndex, summary, issueTypeIndex, priorityIndex, assignee, null);
            }
            return null;
        }

    }
 }

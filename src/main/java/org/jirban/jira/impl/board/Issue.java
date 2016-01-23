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

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.status.Status;

/**
 * The data for an issue on the board
 *
 * @author Kabir Khan
 */
public class Issue {

    private final BoardProjectConfig project;
    private final String key;
    private final String state;
    private final Integer stateIndex;
    private final String summary;
    private final Assignee assignee;
    private final Integer issueType;
    private final Integer priority;
    private final List<Issue> linkedIssues;

    Issue(BoardProjectConfig project, String key, String state, Integer stateIndex, String summary, Assignee assignee, Integer issueType, Integer priority, List<Issue> linkedIssues) {
        this.project = project;
        this.key = key;
        this.state = state;
        this.stateIndex = stateIndex;
        this.summary = summary;
        this.assignee = assignee;
        this.issueType = issueType;
        this.priority = priority;
        this.linkedIssues = linkedIssues != null ?
                Collections.unmodifiableList(linkedIssues) : Collections.emptyList();
    }

    static Issue createIssue(final BoardProject.Builder project, final ModelNode modelNode) {
        String key = modelNode.get(IssueFields.KEY).asString();
        return createIssue(project, key, modelNode, true);
    }

    private static Issue createIssue(final BoardProject.Builder project, final String issueKey, final ModelNode modelNode, boolean master) {
        final ModelNode fields = modelNode.get(IssueFields.FIELDS);
        final String status = fields.get(IssueFields.STATUS, "name").asString();
        final String summary = fields.get(IssueFields.SUMMARY).asString();
        return null;

//        final Assignee assignee = project.getProjectGroup().getAssignee(fields.get(IssueFields.ASSIGNEE));
//        final Integer issueType = project.getProjectGroup().getIssueType(issueKey, fields.get(IssueFields.ISSUE_TYPE));
//        final Integer priority = project.getProjectGroup().getPriority(issueKey, fields.get(IssueFields.PRIORITY));
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

    }


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
        return linkedIssues.size() > 0;
    }

    Iterable<Issue> getLinkedIssues() {
        return () -> linkedIssues.iterator();
    }

    public ModelNode toModelNode() {
        ModelNode issueNode = new ModelNode();
        issueNode.get("key").set(key);
        issueNode.get("state").set(stateIndex);
        issueNode.get("summary").set(summary);
        if (assignee != null) {
            issueNode.get("assignee").set(assignee.getIndex());
        }
        if (priority != null) {
            issueNode.get("priority").set(priority);
        }
        if (issueType != null) {
            issueNode.get("type").set(issueType);
        }

        if (hasLinkedIssues()) {
            ModelNode linkedIssuesNode = issueNode.get("linked-issues");
            for (Issue linkedIssue : linkedIssues) {
                if (linkedIssue.isValid()) {
                    ModelNode linkedIssueNode = linkedIssue.toModelNode();
                    linkedIssuesNode.add(linkedIssueNode);
                }
            }
        }
        return issueNode;
    }

    public boolean isValid() {
        return false;
        //TODO
//        return (project.isLinked() && stateIndex != null)
//                || (issueType != null && priority != null && stateIndex != null);
    }

    public boolean isDataSame(Issue that) {
        if (that == null) {
            return false;
        }
        //I don't want to do a standard equals() since I am not comparing all the data
        if (!key.equals(that.key)) return false;
        if (!state.equals(that.state)) return false;
        if (stateIndex != null ? !stateIndex.equals(that.stateIndex) : that.stateIndex != null) return false;
        if (!summary.equals(that.summary)) return false;
        if (assignee != null ? !assignee.isDataSame(that.assignee) : that.assignee != null) return false;
        if (issueType != null ? !issueType.equals(that.issueType) : that.issueType != null) return false;
        if (priority != null ? !priority.equals(that.priority) : that.priority != null) return false;

        if (linkedIssues.size() != that.linkedIssues.size()) {
            for (int i = 0 ; i < linkedIssues.size() ; i++) {
                if (!linkedIssues.get(i).equals(that.linkedIssues.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getProjectCode() {
        return project.getCode();
    }


    public Issue copyToState(String toBoardState) {
        //TODO
        return null;
//        //The state and the index should be from the issue's project
//        final String toState = project.isOwner() ? toBoardState : project.getStateFromOwnerState(toBoardState);
//        final Integer toStateIndex = project.getStateIndex(toState);
//        return new Issue(project, key, toState, toStateIndex, summary,
//                assignee, issueType, priority, linkedIssues);
    }

    public Integer getStateIndex() {
        return stateIndex;
    }

    static Builder builder(BoardProjectConfig projectConfig) {
        return new Builder(projectConfig);
    }

    static class Builder {
        private final BoardProjectConfig projectConfig;

        private Builder(BoardProjectConfig projectConfig) {
            this.projectConfig = projectConfig;
        }

        void load(com.atlassian.jira.issue.Issue issue) {
            String issueKey = issue.getKey();
            Status status = issue.getStatusObject();
            String summary = issue.getSummary();
            User assignee = issue.getAssignee();
            IssueType issueType = issue.getIssueTypeObject();
            Priority priority = issue.getPriorityObject();

            System.out.println("- - - - Issue " + issueKey);
            System.out.println("Summary " + summary);

            System.out.println("Status " + status);
            System.out.println(status.getName());
            System.out.println(status.getId());

            System.out.println("Assignee " + assignee);
            System.out.println(assignee.getName());
            System.out.println(assignee.getDisplayName());
            System.out.println(assignee.getEmailAddress());
            //TODO avatar

            System.out.println("IssueType " + issueType);
            System.out.println(issueType.getName());
            System.out.println(issueType.getIconUrl());

            System.out.println("Priority " + priority);
            System.out.println(priority.getName());
            System.out.println(priority.getIconUrl());

//            issue.

            //TODO linked objects
        }
    }
}

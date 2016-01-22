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

package org.jirban.jira.impl.config;

import static org.jirban.jira.impl.config.Util.getRequiredChild;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * The set of projects to be displayed on a board. The 'owner' is the project which contains the
 * states. The 'board' projects include the 'owner' and the other projects whose issues should be displayed as cards
 * on the Kanban board. The non-'owner' projects map their states onto the 'owner's states. The 'linked' projects are
 * 'upstream' projects, which we are interested in linking to from the 'main' projects' boards.
 *
 * @author Kabir Khan
 */
public class BoardConfig {

    private final int id;
    private final String name;
    private final String owningProjectName;
    private final String jiraUrl;
    private final int rankCustomFieldId;
    private final Map<String, BoardProjectConfig> boardProjects;
    private final Map<String, LinkedProjectConfig> linkedProjects;
    private final Map<String, NameAndUrl> priorities;
    private final Map<String, Integer> priorityIndex;
    private final Map<String, NameAndUrl> issueTypes;
    private final Map<String, Integer> issueTypeIndex;

    private BoardConfig(int id, String name, String owningProjectName, String jiraUrl,
                        int rankCustomFieldId,
                        Map<String, BoardProjectConfig> boardProjects, Map<String, LinkedProjectConfig> linkedProjects,
                        Map<String, NameAndUrl> priorities, Map<String, NameAndUrl> issueTypes) {

        this.id = id;
        this.name = name;
        this.owningProjectName = owningProjectName;
        this.jiraUrl = jiraUrl;
        this.rankCustomFieldId = rankCustomFieldId;
        this.boardProjects = boardProjects;
        this.linkedProjects = linkedProjects;
        this.priorities = priorities;
        this.priorityIndex = getIndexMap(priorities);
        this.issueTypes = issueTypes;
        this.issueTypeIndex = getIndexMap(issueTypes);
    }

    public static BoardConfig load(int id, String configJson) {
        ModelNode boardNode = ModelNode.fromJSONString(configJson);
        String projectGroupName = getRequiredChild(boardNode, "Group", null, "name").asString();
        int refreshInterval = getRequiredChild(boardNode, "Group", null, "refresh-interval").asInt();
        String owningProjectName = getRequiredChild(boardNode, "Group", projectGroupName, "owning-project").asString();
        String jiraUrl = getRequiredChild(boardNode, "Group", projectGroupName, "jira-url").asString();
        int rankCustomFieldId = getRequiredChild(boardNode, "Group", projectGroupName, "rank-custom-field-id").asInt();

        ModelNode projects = getRequiredChild(boardNode, "Group", projectGroupName, "projects");
        ModelNode mainProject = projects.remove(owningProjectName);
        if (!mainProject.isDefined()) {
            throw new IllegalStateException("Project group '" + projectGroupName + "' specifies '" + mainProject + "' as its main project but it does not exist");
        }
        Map<String, BoardProjectConfig> mainProjects = new LinkedHashMap<>();
        OwnerBoardProjectConfig mainProjectConfig = OwnerBoardProjectConfig.load(owningProjectName, mainProject);
        mainProjects.put(owningProjectName, mainProjectConfig);

        for (String projectName : projects.keys()) {
            ModelNode project = projects.get(projectName);
            //TODO add these other top level projects, mapping their states to the main project states
            mainProjects.put(projectName, NonOwnerBoardProjectConfig.load(projectName, project));
        }

        ModelNode linked = boardNode.get("linked-projects");
        Map<String, LinkedProjectConfig> linkedProjects = new LinkedHashMap<>();
        if (linked.isDefined()) {
            for (String projectName : linked.keys()) {
                ModelNode project = linked.get(projectName);
                linkedProjects.put(projectName, LinkedProjectConfig.load(projectName, project));
            }
        }
        return new BoardConfig(id, projectGroupName, owningProjectName, jiraUrl,
                rankCustomFieldId,
                Collections.unmodifiableMap(mainProjects),
                Collections.unmodifiableMap(linkedProjects),
                Collections.unmodifiableMap(loadNameAndUrlPairs(boardNode.get("priorities"))),
                Collections.unmodifiableMap(loadNameAndUrlPairs(boardNode.get("issue-types")))
        );
    }

    private static Map<String, NameAndUrl> loadNameAndUrlPairs(ModelNode parent) {
        Map<String, NameAndUrl> pairs = new LinkedHashMap<>();
        if (parent.isDefined()) {
            for (Property property : parent.asPropertyList()) {
                pairs.put(property.getName(), new NameAndUrl(property.getName(), property.getValue().asString()));
            }
        }
        return pairs;
    }

    private Map<String, Integer> getIndexMap(Map<String, NameAndUrl> original) {
        Map<String, Integer> result = new HashMap<>();
        for (String key : original.keySet()) {
            result.put(key, result.size());
        }
        return Collections.unmodifiableMap(result);
    }

    public String getName() {
        return name;
    }

    public Collection<BoardProjectConfig> getBoardProjects() {
        return boardProjects.values();
    }

    public BoardProjectConfig getOwnerProject(String projectCode) {
        return boardProjects.get(projectCode);
    }

    public LinkedProjectConfig getLinkedProject(String projectCode) {
        return linkedProjects.get(projectCode);
    }

    public LinkedProjectConfig getLinkedProjectFromIssueKey(String issueKey) {
        return linkedProjects.get(getProjectCodeFromIssueKey(issueKey));
    }

    private String getProjectCodeFromIssueKey(String issueKey) {
        return issueKey.substring(0, issueKey.lastIndexOf("-"));
    }

    public void serializeModelNode(ModelNode parent) {
        ModelNode prioritiesNode = parent.get("priorities");
        for (NameAndUrl priority : priorities.values()) {
            priority.serialize(prioritiesNode);
        }

        ModelNode issueTypesNode = parent.get("issue-types");
        for (NameAndUrl issueType : issueTypes.values()) {
            issueType.serialize(issueTypesNode);
        }

        final ModelNode projects = parent.get("projects");
        projects.get("owner").set(owningProjectName);

        final ModelNode main = projects.get("main");
        for (BoardProjectConfig project : boardProjects.values()) {
            project.serializeModelNode(this, main);
        }
        final ModelNode linked = projects.get("linked");
        for (LinkedProjectConfig project : linkedProjects.values()) {
            project.serializeModelNode(this, linked);
        }
    }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public Set<String> getOwnerStateNames() {
        return boardProjects.get(owningProjectName).getStateNames();
    }

    public String getOwningProjectName() {
        return owningProjectName;
    }

    public BoardProjectConfig getOwningProject() {
        return boardProjects.get(owningProjectName);
    }

    public Integer getIssueTypeIndex(String name) {
        return issueTypeIndex.get(name);
    }

    public Integer getPriorityIndex(String name) {
        return priorityIndex.get(name);
    }

    public int getId() {
        return id;
    }

    public int getRankCustomFieldId() {
        return rankCustomFieldId;
    }


}

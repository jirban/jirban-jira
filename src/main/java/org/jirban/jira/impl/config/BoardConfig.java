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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;

import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.PriorityManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;

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
    private final String owningUserKey;
    private final String ownerProjectCode;
    private final int rankCustomFieldId;
    private final Map<String, BoardProjectConfig> boardProjects;
    private final Map<String, LinkedProjectConfig> linkedProjects;
    private final Map<String, NameAndUrl> priorities;
    private final Map<String, Integer> priorityIndex;
    private final Map<String, NameAndUrl> issueTypes;
    private final Map<String, Integer> issueTypeIndex;

    private BoardConfig(int id, String name, String owningUserKey, String ownerProjectCode,
                        int rankCustomFieldId,
                        Map<String, BoardProjectConfig> boardProjects, Map<String, LinkedProjectConfig> linkedProjects,
                        Map<String, NameAndUrl> priorities, Map<String, NameAndUrl> issueTypes) {

        this.id = id;
        this.name = name;
        this.owningUserKey = owningUserKey;
        this.ownerProjectCode = ownerProjectCode;
        this.rankCustomFieldId = rankCustomFieldId;
        this.boardProjects = boardProjects;
        this.linkedProjects = linkedProjects;
        this.priorities = priorities;
        this.priorityIndex = getIndexMap(priorities);
        this.issueTypes = issueTypes;
        this.issueTypeIndex = getIndexMap(issueTypes);
    }

    public static BoardConfig load(IssueTypeManager issueTypeManager, PriorityManager priorityManager, int id,
                                   String owningUserKey, String configJson) {
        ModelNode boardNode = ModelNode.fromJSONString(configJson);
        return load(issueTypeManager, priorityManager, id, owningUserKey, boardNode);
    }

    public static ModelNode validateAndSerialize(IssueTypeManager issueTypeManager, PriorityManager priorityManager,
                                                 int id, String owningUserKey, ModelNode boardNode) {
        BoardConfig boardConfig = load(issueTypeManager, priorityManager, id, owningUserKey, boardNode);
        return boardConfig.serializeModelNodeForConfig();
    }

    private static BoardConfig load(IssueTypeManager issueTypeManager, PriorityManager priorityManager,
                                    int id, String owningUserKey, ModelNode boardNode) {
        String projectGroupName = getRequiredChild(boardNode, "Group", null, "name").asString();
        String owningProjectName = getRequiredChild(boardNode, "Group", projectGroupName, "owning-project").asString();
        int rankCustomFieldId = getRequiredChild(boardNode, "Group", projectGroupName, "rank-custom-field-id").asInt();

        ModelNode projects = getRequiredChild(boardNode, "Group", projectGroupName, "projects");
        ModelNode mainProject = projects.remove(owningProjectName);
        if (mainProject == null || !mainProject.isDefined()) {
            throw new IllegalStateException("Project group '" + projectGroupName + "' specifies '" + owningProjectName + "' as its main project but it does not exist");
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

        BoardConfig boardConfig = new BoardConfig(id, projectGroupName, owningUserKey, owningProjectName,
                rankCustomFieldId,
                Collections.unmodifiableMap(mainProjects),
                Collections.unmodifiableMap(linkedProjects),
                Collections.unmodifiableMap(loadPriorities(priorityManager, boardNode.get("priorities").asList())),
                Collections.unmodifiableMap(loadIssueTypes(issueTypeManager, boardNode.get("issue-types").asList())));
        for (BoardProjectConfig cfg : mainProjects.values()) {
            cfg.setBoardConfig(boardConfig);
        }
        return boardConfig;
    }

    private static Map<String, NameAndUrl> loadIssueTypes(IssueTypeManager issueTypeManager, List<ModelNode> typeNodes) {
        final Collection<IssueType> allTypes = issueTypeManager.getIssueTypes();
        Map<String, IssueType> types = new HashMap<>();
        for (IssueType type : allTypes) {
            types.put(type.getName(), type);
        }
        Map<String, NameAndUrl> issueTypes = new LinkedHashMap<>();
        for (ModelNode typeNode : typeNodes) {
            IssueType type = types.get(typeNode.asString());
            if (type == null) {
                throw new JirbanValidationException(typeNode.asString() + " is not a known issue type in this Jira instance");
            }
            issueTypes.put(type.getName(), new NameAndUrl(type.getName(), type.getIconUrl()));
        }
        return issueTypes;
    }

    private static Map<String, NameAndUrl> loadPriorities(PriorityManager priorityManager, List<ModelNode> priorityNodes) {
        final Collection<Priority> allPriorities = priorityManager.getPriorities();
        Map<String, Priority> priorities = new HashMap<>();
        for (Priority priority : allPriorities) {
            priorities.put(priority.getName(), priority);
        }
        Map<String, NameAndUrl> priorityMap = new LinkedHashMap<>();
        for (ModelNode priorityNode : priorityNodes) {
            Priority priority = priorities.get(priorityNode.asString());
            if (priority == null) {
                throw new JirbanValidationException(priorityNode.asString() + " is not a known priority name in this Jira instance");
            }
            priorityMap.put(priority.getName(), new NameAndUrl(priority.getName(), priority.getIconUrl()));
        }
        return priorityMap;
    }

    private Map<String, Integer> getIndexMap(Map<String, NameAndUrl> original) {
        Map<String, Integer> result = new HashMap<>();
        for (String key : original.keySet()) {
            result.put(key, result.size());
        }
        return Collections.unmodifiableMap(result);
    }

    public String getOwningUserKey() {
        return owningUserKey;
    }

    public String getName() {
        return name;
    }

    public Collection<BoardProjectConfig> getBoardProjects() {
        return boardProjects.values();
    }

    public BoardProjectConfig getOwnerProject() {
        return boardProjects.get(ownerProjectCode);
    }

    public BoardProjectConfig getBoardProject(String projectCode) {
        return boardProjects.get(projectCode);
    }

    public LinkedProjectConfig getLinkedProject(String projectCode) {
        return linkedProjects.get(projectCode);
    }

    public LinkedProjectConfig getLinkedProjectConfig(String linkedProjectCode) {
        return linkedProjects.get(linkedProjectCode);
    }

    public void serializeModelNodeForBoard(ModelNode boardNode) {
        ModelNode prioritiesNode = boardNode.get("priorities");
        for (NameAndUrl priority : priorities.values()) {
            priority.serialize(prioritiesNode);
        }

        ModelNode issueTypesNode = boardNode.get("issue-types");
        for (NameAndUrl issueType : issueTypes.values()) {
            issueType.serialize(issueTypesNode);
        }

        final ModelNode projects = boardNode.get("projects");
        projects.get("owner").set(ownerProjectCode);

        final ModelNode main = projects.get("main");
        for (BoardProjectConfig project : boardProjects.values()) {
            project.serializeModelNodeForBoard(this, main);
        }
        final ModelNode linked = projects.get("linked");
        linked.setEmptyObject();
        for (LinkedProjectConfig project : linkedProjects.values()) {
            project.serializeModelNodeForBoard(this, linked);
        }
    }

    private ModelNode serializeModelNodeForConfig() {
        ModelNode boardNode = new ModelNode();
        boardNode.get("name").set(name);
        boardNode.get("owning-project").set(ownerProjectCode);
        boardNode.get("rank-custom-field-id").set(rankCustomFieldId);

        ModelNode prioritiesNode = boardNode.get("priorities");
        for (NameAndUrl priority : priorities.values()) {
            prioritiesNode.add(priority.getName());
        }

        ModelNode issueTypesNode = boardNode.get("issue-types");
        for (NameAndUrl issueType : issueTypes.values()) {
            issueTypesNode.add(issueType.getName());
        }

        final ModelNode projectsNode = boardNode.get("projects");
        for (BoardProjectConfig project : boardProjects.values()) {
            projectsNode.get(project.getCode()).set(project.serializeModelNodeForConfig());
        }

        final ModelNode linkedProjectsNode = boardNode.get("linked-projects");
        linkedProjectsNode.setEmptyObject();
        for (LinkedProjectConfig project : linkedProjects.values()) {
            linkedProjectsNode.get(project.getCode()).set(project.serializeModelNodeForConfig());
        }
        return boardNode;
    }

    public Set<String> getOwnerStateNames() {
        return boardProjects.get(ownerProjectCode).getStateNames();
    }

    public String getOwnerProjectCode() {
        return ownerProjectCode;
    }

    public BoardProjectConfig getOwningProject() {
        return boardProjects.get(ownerProjectCode);
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

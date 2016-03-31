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

import static org.jirban.jira.impl.Constants.CODE;
import static org.jirban.jira.impl.Constants.ISSUE_TYPES;
import static org.jirban.jira.impl.Constants.LINKED;
import static org.jirban.jira.impl.Constants.LINKED_PROJECTS;
import static org.jirban.jira.impl.Constants.MAIN;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.OWNER;
import static org.jirban.jira.impl.Constants.OWNING_PROJECT;
import static org.jirban.jira.impl.Constants.PRIORITIES;
import static org.jirban.jira.impl.Constants.PROJECTS;
import static org.jirban.jira.impl.Constants.RANK_CUSTOM_FIELD_ID;
import static org.jirban.jira.impl.Constants.STATES;
import static org.jirban.jira.impl.config.Util.getRequiredChild;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.impl.Constants;

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
    private final String code;
    private final String name;
    private final String owningUserKey;
    private final String ownerProjectCode;
    /** The 'Rank' custom field as output by  */
    private final int rankCustomFieldId;
    private final BoardStates boardStates;
    private final Map<String, BoardProjectConfig> boardProjects;
    private final Map<String, LinkedProjectConfig> linkedProjects;
    private final Map<String, NameAndUrl> priorities;
    private final Map<String, Integer> priorityIndex;
    private final List<String> priorityNames;
    private final Map<String, NameAndUrl> issueTypes;
    private final Map<String, Integer> issueTypeIndex;
    private final List<String> issueTypeNames;

    private BoardConfig(int id, String code, String name, String owningUserKey, String ownerProjectCode,
                        int rankCustomFieldId,
                        BoardStates boardStates,
                        Map<String, BoardProjectConfig> boardProjects, Map<String, LinkedProjectConfig> linkedProjects,
                        Map<String, NameAndUrl> priorities, Map<String, NameAndUrl> issueTypes) {

        this.id = id;
        this.code = code;
        this.name = name;
        this.owningUserKey = owningUserKey;
        this.ownerProjectCode = ownerProjectCode;
        this.rankCustomFieldId = rankCustomFieldId;
        this.boardStates = boardStates;
        this.boardProjects = boardProjects;
        this.linkedProjects = linkedProjects;

        this.priorities = priorities;
        Map<String, Integer> priorityIndex = new HashMap<>();
        List<String> priorityNames = new ArrayList<>();
        getIndexMap(priorities, priorityIndex, priorityNames);
        this.priorityIndex = Collections.unmodifiableMap(priorityIndex);
        this.priorityNames = Collections.unmodifiableList(priorityNames);

        this.issueTypes = issueTypes;
        Map<String, Integer> issueTypeIndex = new HashMap<>();
        List<String> issueTypeNames = new ArrayList<>();
        getIndexMap(issueTypes, issueTypeIndex, issueTypeNames);
        this.issueTypeIndex = Collections.unmodifiableMap(issueTypeIndex);
        this.issueTypeNames = Collections.unmodifiableList(issueTypeNames);
    }

    public static BoardConfig load(IssueTypeManager issueTypeManager, PriorityManager priorityManager, int id,
                                   String owningUserKey, String configJson, int rankCustomFieldId) {
        ModelNode boardNode = ModelNode.fromJSONString(configJson);
        return load(issueTypeManager, priorityManager, id, owningUserKey, boardNode, rankCustomFieldId);
    }

    public static ModelNode validateAndSerialize(IssueTypeManager issueTypeManager, PriorityManager priorityManager,
                                                 int id, String owningUserKey, ModelNode boardNode, int rankCustomFieldId) {
        BoardConfig boardConfig = load(issueTypeManager, priorityManager, id, owningUserKey, boardNode, rankCustomFieldId);
        return boardConfig.serializeModelNodeForConfig();
    }

    public static BoardConfig load(IssueTypeManager issueTypeManager, PriorityManager priorityManager,
                                    int id, String owningUserKey, ModelNode boardNode, int rankCustomFieldId) {
        String code = getRequiredChild(boardNode, "Group", null, CODE).asString();
        String boardName = getRequiredChild(boardNode, "Group", null, NAME).asString();
        String owningProjectName = getRequiredChild(boardNode, "Group", boardName, OWNING_PROJECT).asString();

        final BoardStates boardStates = BoardStates.loadBoardStates(boardNode.get(STATES));

        ModelNode projects = getRequiredChild(boardNode, "Group", boardName, PROJECTS);
        ModelNode mainProject = projects.remove(owningProjectName);
        if (mainProject == null || !mainProject.isDefined()) {
            throw new IllegalStateException("Project group '" + boardName + "' specifies '" + owningProjectName + "' as its main project but it does not exist");
        }
        Map<String, BoardProjectConfig> mainProjects = new LinkedHashMap<>();
        BoardProjectConfig mainProjectConfig = BoardProjectConfig.load(boardStates, owningProjectName, mainProject);
        mainProjects.put(owningProjectName, mainProjectConfig);

        for (String projectName : projects.keys()) {
            ModelNode project = projects.get(projectName);
            mainProjects.put(projectName, BoardProjectConfig.load(boardStates, projectName, project));
        }

        ModelNode linked = boardNode.get(LINKED_PROJECTS);
        Map<String, LinkedProjectConfig> linkedProjects = new LinkedHashMap<>();
        if (linked.isDefined()) {
            for (String projectName : linked.keys()) {
                ModelNode project = linked.get(projectName);
                linkedProjects.put(projectName, LinkedProjectConfig.load(projectName, project));
            }
        }

        BoardConfig boardConfig = new BoardConfig(id, code, boardName, owningUserKey, owningProjectName,
                rankCustomFieldId,
                boardStates,
                Collections.unmodifiableMap(mainProjects),
                Collections.unmodifiableMap(linkedProjects),
                Collections.unmodifiableMap(loadPriorities(priorityManager, boardNode.get(PRIORITIES).asList())),
                Collections.unmodifiableMap(loadIssueTypes(issueTypeManager, boardNode.get(ISSUE_TYPES).asList())));
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

    private void getIndexMap(Map<String, NameAndUrl> original, Map<String, Integer> index, List<String> list) {
        for (String key : original.keySet()) {
            index.put(key, index.size());
            list.add(key);
        }
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

    public BoardProjectConfig getBoardProject(String projectCode) {
        return boardProjects.get(projectCode);
    }

    public LinkedProjectConfig getLinkedProjectConfig(String linkedProjectCode) {
        return linkedProjects.get(linkedProjectCode);
    }

    /**
     * Used to serialize the board for the view board view
     *
     * @param boardNode The node to serialize the board to
     */
    public void serializeModelNodeForBoard(ModelNode boardNode) {
        boardNode.get(Constants.RANK_CUSTOM_FIELD_ID).set(rankCustomFieldId);

        boardStates.toModelNodeForBoard(boardNode);

        ModelNode prioritiesNode = boardNode.get(PRIORITIES);
        for (NameAndUrl priority : priorities.values()) {
            priority.serialize(prioritiesNode);
        }

        ModelNode issueTypesNode = boardNode.get(ISSUE_TYPES);
        for (NameAndUrl issueType : issueTypes.values()) {
            issueType.serialize(issueTypesNode);
        }

        final ModelNode projects = boardNode.get(PROJECTS);
        projects.get(OWNER).set(ownerProjectCode);

        final ModelNode main = projects.get(MAIN);
        for (BoardProjectConfig project : boardProjects.values()) {
            project.serializeModelNodeForBoard(this, main);
        }
        final ModelNode linked = projects.get(LINKED);
        linked.setEmptyObject();
        for (LinkedProjectConfig project : linkedProjects.values()) {
            project.serializeModelNodeForBoard(this, linked);
        }
    }

    /**
     * Used to serialize the board for the view/edit board config view
     */
    public ModelNode serializeModelNodeForConfig() {
        ModelNode boardNode = new ModelNode();
        boardNode.get(NAME).set(name);
        boardNode.get(CODE).set(code);
        boardNode.get(OWNING_PROJECT).set(ownerProjectCode);

        boardStates.toModelNodeForConfig(boardNode);

        ModelNode prioritiesNode = boardNode.get(PRIORITIES);
        for (NameAndUrl priority : priorities.values()) {
            prioritiesNode.add(priority.getName());
        }

        ModelNode issueTypesNode = boardNode.get(ISSUE_TYPES);
        for (NameAndUrl issueType : issueTypes.values()) {
            issueTypesNode.add(issueType.getName());
        }

        final ModelNode projectsNode = boardNode.get(PROJECTS);
        for (BoardProjectConfig project : boardProjects.values()) {
            projectsNode.get(project.getCode()).set(project.serializeModelNodeForConfig());
        }

        final ModelNode linkedProjectsNode = boardNode.get(LINKED_PROJECTS);
        linkedProjectsNode.setEmptyObject();
        for (LinkedProjectConfig project : linkedProjects.values()) {
            linkedProjectsNode.get(project.getCode()).set(project.serializeModelNodeForConfig());
        }
        return boardNode;
    }

    public String getOwnerProjectCode() {
        return ownerProjectCode;
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

    public String getCode() {
        return code;
    }

    public List<String> getStateNames() {
        return boardStates.getStateNames();
    }

    public boolean isBacklogState(int stateIndex) {
        return boardStates.isBacklogState(stateIndex);
    }

    public boolean isUnorderedState(int boardStateIndex) {
        return boardStates.isUnorderedState(boardStateIndex);
    }

    public boolean isDoneState(int boardStateIndex) {
        return boardStates.isDoneState(boardStateIndex);
    }

    public String getIssueTypeName(int issueTypeIndex) {
        return issueTypeNames.get(issueTypeIndex);
    }

    public String getPriorityName(int priorityIndex) {
        return priorityNames.get(priorityIndex);
    }
}

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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Kabir Khan
 */
public class ProjectConfig {
    private final ProjectType projectType;
    private final String code;
    private final String queryFilter;
    private final String colour;
    private final Map<String, Integer> states;
    private final Map<String, String> ownerStateMapping;
    /** Maps the owner states onto our states */
    private final Map<String, String> ownerStateReverseMapping;

    private ProjectConfig(ProjectType projectType, String code, String queryFilter, String colour, Map<String, Integer> states, Map<String, String> ownerStateMapping) {
        this.projectType = projectType;
        this.code = code;
        this.queryFilter = queryFilter;
        this.colour = colour;
        this.states = states;
        this.ownerStateMapping = ownerStateMapping;

        if (ownerStateMapping != null) {
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<String, String> entry : ownerStateMapping.entrySet()) {
                map.put(entry.getValue(), entry.getKey());
            }
            this.ownerStateReverseMapping = Collections.unmodifiableMap(map);
        } else {
            this.ownerStateReverseMapping = null;
        }
    }

    /**
     * Creates the project that 'owns' the board. This means that
     * <ol>
     *     <li>The project's issues are shown as cards on the board</li>
     *     <li>The project's states become the board columns</li>
     * </ol>
     * @param projectName the name of the project
     * @param project the configuration
     * @return
     */
    static ProjectConfig loadOwnerProject(String projectName, ModelNode project) {
        String colour = getRequiredChild(project, "Project", projectName, "colour").asString();
        List<ModelNode> statesList = getRequiredChild(project, "Project", projectName, "states").asList();
        if (project.hasDefined("state-links")) {
            throw new IllegalStateException("The main project should not have state-links, only a states array");
        }
        Map<String, Integer> statesMap = getStringIntegerMap(statesList);
        return new ProjectConfig(ProjectType.OWNER, projectName, getQueryFilter(project), colour, statesMap, null);
    }

    static ProjectConfig loadNonOwnerBoardProject(String projectName, ModelNode project) {
        String colour = getRequiredChild(project, "Project", projectName, "colour").asString();
        ModelNode statesLinks = getRequiredChild(project, "Project", projectName, "state-links");
        if (project.hasDefined("states")) {
            throw new IllegalStateException("The non-main projects should not have states, only a state-links entry mapping its states to those of the main project");
        }

        Map<String, Integer> statesMap = new LinkedHashMap<>();
        Map<String, String> ownerStateMappings = new HashMap<>();
        int i = 0;
        for (Property prop : statesLinks.asPropertyList()) {
            statesMap.put(prop.getName(), i++);
            ownerStateMappings.put(prop.getName(), prop.getValue().asString());
        }
        return new ProjectConfig(ProjectType.TOP_LEVEL, projectName, getQueryFilter(project), colour, Collections.unmodifiableMap(statesMap), Collections.unmodifiableMap(ownerStateMappings));
    }

    static ProjectConfig loadLinkedProject(String projectName, ModelNode project) {
        List<ModelNode> statesList = getRequiredChild(project, "Project", projectName, "states").asList();
        Map<String, Integer> statesMap = getStringIntegerMap(statesList);
        return new ProjectConfig(ProjectType.LINKED, projectName, null, null, Collections.unmodifiableMap(statesMap), null);
    }

    private static Map<String, Integer> getStringIntegerMap(final List<ModelNode> statesList) {
        Map<String, Integer> statesMap = new LinkedHashMap<>();
        for (int i = 0; i < statesList.size(); i++) {
            statesMap.put(statesList.get(i).asString(), i);
        }
        return statesMap;
    }

    private static String getQueryFilter(ModelNode project) {
        if (!project.hasDefined("query-filter")) {
            return null;
        }
        String filter = project.get("query-filter").asString().trim();
        if (filter.length() == 0) {
            return null;
        }
        return filter;
    }

    public String getCode() {
        return code;
    }

    public Set<String> getStateNames() {
        return states.keySet();
    }

    public Integer getStateIndex(String stateName) {
        return states.get(stateName);
    }

    public int getMaxStateIndex() {
        return states.size() - 1;
    }

    void serializeModelNode(BoardConfig groupConfig, ModelNode parent) {
        ModelNode projectNode = parent.get(code);
        if (projectType != ProjectType.LINKED) {
            projectNode.get("colour").set(colour);
        }
        ModelNode statesNode = projectNode.get("states");
        this.states.keySet().forEach(statesNode::add);

        if (projectType == ProjectType.TOP_LEVEL) {
            ModelNode stateLinksNode = projectNode.get("state-links");
            for (String state : groupConfig.getOwningProject().getStateNames()) {
                String myState = getStateFromOwnerState(state);
                stateLinksNode.get(state).set(myState == null ? new ModelNode() : new ModelNode(myState));
            }
        }
    }

    public String getStateFromOwnerState(String ownerState) {
        if (projectType != ProjectType.TOP_LEVEL) {
            throw new IllegalStateException();
        }
        return ownerStateReverseMapping.get(ownerState);
    }

    public String getOwnerState(String state) {
        if (projectType != ProjectType.TOP_LEVEL) {
            throw new IllegalStateException();
        }
        return ownerStateMapping.get(state);
    }

    public boolean isLinked() {
        return projectType == ProjectType.LINKED;
    }

    public boolean isOwner() {
        return projectType == ProjectType.OWNER;
    }

    public String getQueryFilter() {
        return queryFilter;
    }

    private enum ProjectType {
        OWNER,
        TOP_LEVEL,
        LINKED
    }

}

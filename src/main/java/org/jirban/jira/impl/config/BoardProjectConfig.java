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
package org.jirban.jira.impl.config;

import static org.jirban.jira.impl.Constants.COLOUR;
import static org.jirban.jira.impl.Constants.CUSTOM;
import static org.jirban.jira.impl.Constants.PARALLEL_TASKS;
import static org.jirban.jira.impl.Constants.STATE_LINKS;
import static org.jirban.jira.impl.config.Util.getRequiredChild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jirban.jira.JirbanLogger;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.impl.Constants;

/** Abstract base class for project configurations of projects whose issues should appear as cards on the board.
 * @author Kabir Khan
 */
public class BoardProjectConfig extends ProjectConfig {
    private final BoardStates boardStates;
    private final String queryFilter;
    private final String colour;
    private final Map<String, String> ownToBoardStates;
    /** Maps the owner states onto our states */
    private final Map<String, String> boardToOwnStates;

    private final Set<String> ownDoneStateNames;

    private final List<String> customFieldNames;
    private final ParallelTaskConfig parallelTaskConfig;

    private BoardProjectConfig(final BoardStates boardStates,
                               final String code, final String queryFilter,
                               final String colour, final Map<String, Integer> states,
                               final Map<String, String> ownToBoardStates,
                               final Map<String, String> boardToOwnStates,
                               final List<String> customFieldNames,
                               final ParallelTaskConfig parallelTaskConfig) {
        super(code, states);
        this.boardStates = boardStates;
        this.queryFilter = queryFilter;
        this.colour = colour;
        this.boardToOwnStates = boardToOwnStates;
        this.ownToBoardStates = ownToBoardStates;
        this.parallelTaskConfig = parallelTaskConfig;

        Set<String> ownDoneStateNames = new HashSet<>();
        for (String boardDoneState : boardStates.getDoneStates()) {
            String ownDoneState = boardToOwnStates.get(boardDoneState);
            if (ownDoneState != null) {
                ownDoneStateNames.add(ownDoneState);
            }
        }
        this.ownDoneStateNames = Collections.unmodifiableSet(ownDoneStateNames);
        this.customFieldNames = customFieldNames;
    }

    static BoardProjectConfig load(final BoardStates boardStates, final String projectCode, ModelNode project,
                                   CustomFieldRegistry<CustomFieldConfig> customFieldConfigs, ParallelTaskConfig parallelTaskConfig) {
        String colour = getRequiredChild(project, "Project", projectCode, COLOUR).asString();
        ModelNode statesLinks = getRequiredChild(project, "Project", projectCode, STATE_LINKS);

        Map<String, String> ownToBoardStates = new LinkedHashMap<>();
        Map<String, String> boardToOwnStates = new HashMap<>();
        for (Property prop : statesLinks.asPropertyList()) {
            final String ownState = prop.getName();
            final String boardState = prop.getValue().asString();
            ownToBoardStates.put(ownState, boardState);
            boardToOwnStates.put(boardState, ownState);
        }

        int i = 0;
        Map<String, Integer> states = new LinkedHashMap<>();
        for (String boardState : boardStates.getStateNames()) {
            final String ownState = boardToOwnStates.get(boardState);
            if (ownState != null) {
                states.put(ownState, i++);
            }
        }


        final List<String> customFieldNames;
        if (!project.hasDefined(CUSTOM)) {
            customFieldNames = Collections.emptyList();
        } else {
            customFieldNames = new ArrayList<>();
            final ModelNode customFieldNode = project.get(CUSTOM);
            if (customFieldNode.getType() != ModelType.LIST) {
                throw new JirbanValidationException("The \"custom\" element of project \"" + projectCode + "\" must be an array of strings");
            }
            for (ModelNode field : customFieldNode.asList()) {
                final String fieldName = field.asString();
                if (customFieldConfigs.getForJirbanName(fieldName) == null) {
                    throw new JirbanValidationException("The \"custom\" element of project \"" + projectCode + "\" contains \"" + fieldName + "\", which does not exist in the board's \"custom\" section.");
                }
                customFieldNames.add(fieldName);
            }
        }

        final ParallelTaskConfig projectParallelTaskConfig;
        if (project.hasDefined(PARALLEL_TASKS)) {
            ModelNode parallelTasks = project.get(PARALLEL_TASKS);
            if (parallelTasks.getType() != ModelType.LIST) {
                throw new JirbanValidationException("The \"parallel-tasks\" element of project \"" + projectCode + "\" must be an array");
            }
            Map<String, ParallelTaskCustomFieldConfig> fieldConfigs = new LinkedHashMap<>();
            for (ModelNode parallelTask : parallelTasks.asList()) {
                ParallelTaskCustomFieldConfig fieldConfig = parallelTaskConfig.getConfigs().getForJirbanName(parallelTask.asString());
                if (fieldConfig == null) {
                    throw new JirbanValidationException("The \"parallel-tasks\" element of project \"" + projectCode + "\" " +
                            "references a parallel task '" + parallelTask.asString() + "' which does not exist in the global parallel-tasks fields list");
                }
                fieldConfigs.put(fieldConfig.getName(), fieldConfig);
            }
            projectParallelTaskConfig = fieldConfigs.size() > 0 ? new ParallelTaskConfig(fieldConfigs) : null;
        } else {
            projectParallelTaskConfig = null;
        }

        return new BoardProjectConfig(boardStates, projectCode, loadQueryFilter(project), colour,
                Collections.unmodifiableMap(states),
                Collections.unmodifiableMap(ownToBoardStates),
                Collections.unmodifiableMap(boardToOwnStates),
                Collections.unmodifiableList(customFieldNames),
                projectParallelTaskConfig);
    }


    static String loadQueryFilter(ModelNode project) {
        if (!project.hasDefined(Constants.QUERY_FILTER)) {
            return null;
        }
        String filter = project.get(Constants.QUERY_FILTER).asString().trim();
        if (filter.length() == 0) {
            return null;
        }
        return filter;
    }

    public String getQueryFilter() {
        return queryFilter;
    }

    public String getColour() {
        return colour;
    }

    public Integer mapOwnStateOntoBoardStateIndex(String state) {
        String boardState = mapOwnStateOntoBoardState(state);
        return boardStates.getStateIndex(boardState);

    }
    public String mapBoardStateOntoOwnState(String boardState) {
        return boardToOwnStates.get(boardState);
    }

    public String mapOwnStateOntoBoardState(String state) {
        return ownToBoardStates.get(state);
    }

    @Override
    ModelNode serializeModelNodeForBoard(BoardConfig boardConfig, ModelNode parent) {
        ModelNode projectNode = super.serializeModelNodeForBoard(boardConfig, parent);
        ModelNode stateLinksNode = projectNode.get(STATE_LINKS);
        for (String state : boardStates.getStateNames()) {
            String myState = mapBoardStateOntoOwnState(state);
            stateLinksNode.get(state).set(myState == null ? new ModelNode() : new ModelNode(myState));
        }
        projectNode.get(Constants.COLOUR).set(colour);
        return projectNode;
    }

    ModelNode serializeModelNodeForConfig() {
        final ModelNode projectNode = new ModelNode();
        projectNode.get(Constants.QUERY_FILTER).set(queryFilter == null ? new ModelNode() : new ModelNode(queryFilter));
        projectNode.get(Constants.COLOUR).set(colour);

        if (customFieldNames.size() > 0) {
            final ModelNode customFieldsNode = projectNode.get(CUSTOM);
            for (String customFieldName : customFieldNames) {
                customFieldsNode.add(customFieldName);
            }
        }

        if (parallelTaskConfig != null) {
            ModelNode parallelTasksNode = projectNode.get(PARALLEL_TASKS).setEmptyList();
            for (ParallelTaskCustomFieldConfig parallelTaskCustomFieldConfig : parallelTaskConfig.getConfigs().values()) {
                parallelTasksNode.add(parallelTaskCustomFieldConfig.getName());
            }
        }

        final ModelNode stateLinksNode = projectNode.get(STATE_LINKS);
        stateLinksNode.setEmptyObject();
        for (Map.Entry<String, String> entry : ownToBoardStates.entrySet()) {
            stateLinksNode.get(entry.getKey()).set(entry.getValue());
        }
        return projectNode;
    }

    public boolean isBacklogState(String ownState) {
        return isBacklogState(mapOwnStateOntoBoardStateIndex(ownState));
    }

    public boolean isDoneState(String ownState) {
        Integer boardStateIndex = mapOwnStateOntoBoardStateIndex(ownState);
        return boardStateIndex == null ? false : isDoneState(boardStateIndex);
    }

    private boolean isBacklogState(int boardStateIndex) {
        return boardStates.isBacklogState(boardStateIndex);
    }

    public boolean isDoneState(int boardStateIndex) {
        return boardStates.isDoneState(boardStateIndex);
    }

    public Set<String> getOwnDoneStateNames() {
        return ownDoneStateNames;
    }

    public List<String> getCustomFieldNames() {
        JirbanLogger.LOGGER.trace("Custom fields for project {} are {}", getCode(), customFieldNames);
        return customFieldNames;
    }

    public ParallelTaskConfig getParallelTaskConfig() {
        return parallelTaskConfig;
    }

}

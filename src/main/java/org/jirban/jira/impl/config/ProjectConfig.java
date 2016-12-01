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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.Constants;

/**
 * Abstract base class for all kinds of project configurations.
 *
 * @author Kabir Khan
 */
public abstract class ProjectConfig {
    protected final String code;
    protected final List<String> statesList;
    protected final Map<String, Integer> states;

    public ProjectConfig(final String code, final Map<String, Integer> states) {
        this.code = code;
        this.states = states;

        List<String> statesList = new ArrayList<>(states.size());
        states.keySet().forEach(s -> statesList.add(s));
        this.statesList = Collections.unmodifiableList(statesList);
    }

    public String getCode() {
        return code;
    }

    public Map<String, Integer> getStates() {
        return states;
    }

    public Set<String> getStateNames() {
        return states.keySet();
    }

    public Integer getStateIndex(String stateName) {
        return states.get(stateName);
    }

    public String getStateName(int index) {
        return statesList.get(index);
    }

    private ModelNode getModelNodeForCode(ModelNode parent) {
        ModelNode projectNode = parent.get(code);
        return projectNode;
    }

    ModelNode serializeModelNodeForBoard(BoardConfig boardConfig, ModelNode parent) {
        ModelNode projectNode = getModelNodeForCode(parent);
        ModelNode states = projectNode.get(Constants.STATES).setEmptyList();
        for (String state : this.states.keySet()) {
            states.add(state);
        }
        return projectNode;
    }

}

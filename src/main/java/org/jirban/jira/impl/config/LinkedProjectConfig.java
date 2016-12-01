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

import static org.jirban.jira.impl.config.Util.getRequiredChild;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.Constants;

/**
 * Project which does not appear on the board as a card, but is linked to from the cards.
 *
 * @author Kabir Khan
 */
public class LinkedProjectConfig extends ProjectConfig {

    public LinkedProjectConfig(final String code, final Map<String, Integer> states) {
        super(code, states);
    }

    static LinkedProjectConfig load(final String projectCode, final ModelNode project) {
        List<ModelNode> statesList = getRequiredChild(project, "Project", projectCode, Constants.STATES).asList();
        Map<String, Integer> statesMap = getStringIntegerMap(statesList);
        return new LinkedProjectConfig(projectCode, Collections.unmodifiableMap(statesMap));
    }

    private static Map<String, Integer> getStringIntegerMap(final List<ModelNode> statesList) {
        Map<String, Integer> statesMap = new LinkedHashMap<>();
        for (int i = 0; i < statesList.size(); i++) {
            statesMap.put(statesList.get(i).asString(), i);
        }
        return statesMap;
    }

    ModelNode serializeModelNodeForConfig() {
        final ModelNode projectNode = new ModelNode();
        final ModelNode statesNode = projectNode.get(Constants.STATES);
        statesNode.setEmptyList();
        for (String state : states.keySet()) {
            statesNode.add(state);
        }
        return projectNode;
    }
}

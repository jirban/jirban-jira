/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jirban.jira.impl.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;

/**
 * @author Kabir Khan
 */
public class BoardStates {
    private final Map<String, Integer> stateIndices;
    private final List<String> states;

    public BoardStates(Map<String, Integer> stateIndices, List<String> states) {
        this.stateIndices = stateIndices;
        this.states = states;
    }

    static BoardStates loadBoardStates(ModelNode statesNode) {
        if (!statesNode.isDefined()) {
            throw new JirbanValidationException("A board must have some states associated with it");
        }
        List<String> states = new ArrayList<>();
        Map<String, Integer> stateIndices = new LinkedHashMap<>();
        try {
            int i = 0;
            for (ModelNode stateNode : statesNode.asList()) {
                states.add(stateNode.asString());
                stateIndices.put(stateNode.asString(), i++);
            }
        } catch (IllegalStateException e) {
            throw new JirbanValidationException("A board must have some states associated with it, and it should be an array strings");
        }

        return new BoardStates(
                Collections.unmodifiableMap(stateIndices),
                Collections.unmodifiableList(states));
    }

    ModelNode toModelNode() {
        ModelNode ret = new ModelNode();
        ret.setEmptyList();
        states.forEach(ret::add);
        return ret;
    }

    public Integer getStateIndex(String boardState) {
        return stateIndices.get(boardState);
    }

    public List<String> getStateNames() {
        return states;
    }
}

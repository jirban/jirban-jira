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

import static org.jirban.jira.impl.Constants.BACKLOG;
import static org.jirban.jira.impl.Constants.HEADER;
import static org.jirban.jira.impl.Constants.HEADERS;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.STATES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;

/**
 * @author Kabir Khan
 */
public class BoardStates {
    private final Map<String, Integer> stateIndices;
    private final List<String> states;
    private final Map<String, String> stateHeaders;
    private final Set<String> backlogStates;

    public BoardStates(Map<String, Integer> stateIndices, List<String> states, Map<String, String> stateHeaders,
                       Set<String> backlogStates) {
        this.stateIndices = stateIndices;
        this.states = states;
        this.stateHeaders = stateHeaders;
        this.backlogStates = backlogStates;
    }

    static BoardStates loadBoardStates(ModelNode statesNode) {
        if (!statesNode.isDefined()) {
            throw new JirbanValidationException("A board must have some states associated with it");
        }
        final Set<String> seenHeaders = new HashSet<>();
        final List<String> states = new ArrayList<>();
        final Map<String, String> stateHeaders = new HashMap<>();
        final Map<String, Integer> stateIndices = new LinkedHashMap<>();
        final Set<String> backlogStates = new HashSet<>();
        try {
            String lastHeader = null;
            int i = 0;
            int lastBacklog = -1;
            for (ModelNode stateNode : statesNode.asList()) {
                if (!stateNode.hasDefined(NAME)) {
                    throw new JirbanValidationException("A state must have a name");
                }
                final String stateName = stateNode.get(NAME).asString();
                states.add(stateName);
                stateIndices.put(stateName, i);

                boolean backlog = stateNode.hasDefined(BACKLOG) && stateNode.get(BACKLOG).asBoolean();
                if (backlog) {
                    if (lastBacklog < i - 1) {
                        throw new JirbanValidationException("The backlog states can only be the first states without any gaps");
                    }
                    backlogStates.add(stateName);
                    lastBacklog = i;
                }

                if (stateNode.hasDefined(HEADER)) {
                    if (backlog) {
                        throw new JirbanValidationException("A backlog state can not have a header");
                    }
                    final ModelNode headerNode = stateNode.get(HEADER);
                    String header = headerNode.asString();
                    if (!header.equals(lastHeader) && seenHeaders.contains(header)) {
                        throw new JirbanValidationException("A state header must be used on neighbouring states. " +
                            "There can't be any gaps as in '" + header + "' used for '" + stateName + "'.");
                    }
                    stateHeaders.put(stateName, header);
                    seenHeaders.add(header);
                    lastHeader = headerNode.asString();
                } else {
                    lastHeader = null;
                }

                i++;
            }
        } catch (IllegalStateException e) {
            throw new JirbanValidationException("A board must have some states associated with it, and it should be an array strings");
        }

        return new BoardStates(
                Collections.unmodifiableMap(stateIndices),
                Collections.unmodifiableList(states),
                Collections.unmodifiableMap(stateHeaders), backlogStates);
    }

    ModelNode toModelNodeForConfig(ModelNode parent) {
        final ModelNode states = new ModelNode();
        states.setEmptyList();

        for (String state : this.states) {
            final ModelNode stateNode = new ModelNode();
            stateNode.get(NAME).set(state);

            final String header = stateHeaders.get(state);
            if (header != null) {
                stateNode.get(HEADER).set(header);
            }
            if (backlogStates.contains(state)) {
                stateNode.get(BACKLOG).set(true);
            }

            states.add(stateNode);
        }

        parent.get(STATES).set(states);
        return states;
    }

    ModelNode toModelNodeForBoard(ModelNode parent) {
        final ModelNode states = new ModelNode();
        states.setEmptyList();

        Set<String> headers = new LinkedHashSet<>();
        final ModelNode headersNode = new ModelNode();
        for (String state : this.states) {
            final ModelNode stateNode = new ModelNode();
            stateNode.get(NAME).set(state);

            final String header = stateHeaders.get(state);
            if (header != null) {
                if (!headers.contains(header)) {
                    headers.add(header);
                    headersNode.add(header);
                }
                stateNode.get(HEADER).set(headers.size() - 1);
            }
            states.add(stateNode);
        }

        parent.get(STATES).set(states);

        if (headers.size() > 0) {
            parent.get(HEADERS).set(headersNode);
        }
        parent.get(BACKLOG).set(backlogStates.size());
        return states;
    }

    public Integer getStateIndex(String boardState) {
        return stateIndices.get(boardState);
    }

    public List<String> getStateNames() {
        return states;
    }
}

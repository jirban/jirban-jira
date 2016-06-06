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
import static org.jirban.jira.impl.Constants.DONE;
import static org.jirban.jira.impl.Constants.HEADER;
import static org.jirban.jira.impl.Constants.HEADERS;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.STATES;
import static org.jirban.jira.impl.Constants.UNORDERED;

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
    private final Set<String> doneStates;
    private final Set<String> unorderedStates;

    private BoardStates(Map<String, Integer> stateIndices, List<String> states, Map<String, String> stateHeaders,
                        Set<String> backlogStates, Set<String> doneStates, Set<String> unorderedStates) {
        this.stateIndices = stateIndices;
        this.states = states;
        this.stateHeaders = stateHeaders;
        this.backlogStates = backlogStates;
        this.doneStates = doneStates;
        this.unorderedStates = unorderedStates;
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
        final Set<String> doneStates = new HashSet<>();
        final Set<String> unorderedStates = new HashSet<>();
        try {
            String lastHeader = null;
            int i = 0;
            int lastBacklog = -1;
            int lastDone = -1;
            for (ModelNode stateNode : statesNode.asList()) {
                if (!stateNode.hasDefined(NAME)) {
                    throw new JirbanValidationException("A state must have a name");
                }
                final String stateName = stateNode.get(NAME).asString();
                if (states.contains(stateName)) {
                    throw new JirbanValidationException("State names for a project must be unique");
                }
                states.add(stateName);
                stateIndices.put(stateName, i);

                boolean backlog = stateNode.hasDefined(BACKLOG) && stateNode.get(BACKLOG).asBoolean();
                boolean unordered = stateNode.hasDefined(UNORDERED) && stateNode.get(UNORDERED).asBoolean();
                boolean done = stateNode.hasDefined(DONE) && stateNode.get(DONE).asBoolean();
                String headerName = stateNode.hasDefined(HEADER) ? stateNode.get(HEADER).asString() : null;

                if ( (backlog ? 1:0)+ (done ? 1:0) + (headerName != null ? 1:0) > 1) {
                    throw new JirbanValidationException("A state can use at the most one of [backlog, done, header]");
                }

                if ( (backlog ? 1:0)+ (done ? 1:0) + (unordered ? 1:0) > 1) {
                    throw new JirbanValidationException("A state can use at the most one of [backlog, done, unordered]");
                }

                if (backlog) {
                    if (lastBacklog < i - 1) {
                        throw new JirbanValidationException("The backlog states can only be the first states without any gaps");
                    }
                    backlogStates.add(stateName);
                    lastBacklog = i;
                }

                if (unordered) {
                    unorderedStates.add(stateName);
                }

                if (done) {
                    if (lastDone > -1 && lastDone < i - 1) {
                        throw new JirbanValidationException("The done states should be consecutive.");
                    }
                    doneStates.add(stateName);
                    lastDone = i;
                }

                if (headerName != null) {
                    if (!headerName.equals(lastHeader) && seenHeaders.contains(headerName)) {
                        throw new JirbanValidationException("A state header must be used on neighbouring states. " +
                            "There can't be any gaps as in '" + headerName + "' used for '" + stateName + "'.");
                    }
                    stateHeaders.put(stateName, headerName);
                    seenHeaders.add(headerName);
                    lastHeader = headerName;
                } else {
                    lastHeader = null;
                }

                i++;
            }
            if (lastDone > -1 && lastDone < i - 1) {
                throw new JirbanValidationException("The done states should be at the end, they cannot be in the middle of the list of states");
            }
        } catch (IllegalStateException e) {
            throw new JirbanValidationException("A board must have some states associated with it, and it should be an array strings");
        }

        return new BoardStates(
                Collections.unmodifiableMap(stateIndices),
                Collections.unmodifiableList(states),
                Collections.unmodifiableMap(stateHeaders),
                Collections.unmodifiableSet(backlogStates),
                Collections.unmodifiableSet(doneStates),
                Collections.unmodifiableSet(unorderedStates));
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
            if (doneStates.contains(state)) {
                stateNode.get(DONE).set(true);
            }
            if (unorderedStates.contains(state)) {
                stateNode.get(UNORDERED).set(true);
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
        final ModelNode unorderedStates = new ModelNode();

        for (int i = 0 ; i < this.states.size() ; i++) {
            final String state = this.states.get(i);
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
            if (this.unorderedStates.contains(state)) {
                unorderedStates.add(i);
            }
            states.add(stateNode);
        }

        parent.get(STATES).set(states);

        if (headers.size() > 0) {
            parent.get(HEADERS).set(headersNode);
        }
        if (backlogStates.size() > 0) {
            parent.get(BACKLOG).set(backlogStates.size());
        }
        if (doneStates.size() > 0) {
            parent.get(DONE).set(doneStates.size());
        }
        if (unorderedStates.isDefined()) {
            parent.get(UNORDERED).set(unorderedStates);
        }
        return states;
    }

    public Integer getStateIndex(String boardState) {
        return stateIndices.get(boardState);
    }

    public List<String> getStateNames() {
        return states;
    }

    public Set<String> getBacklogStates() {
        return backlogStates;
    }

    public boolean isBacklogState(int boardStateIndex) {
        return boardStateIndex < backlogStates.size();
    }

    public boolean isUnorderedState(int boardStateIndex) {
        String state = states.get(boardStateIndex);
        return unorderedStates.contains(state);
    }

    public boolean isDoneState(int boardStateIndex) {
        String state = states.get(boardStateIndex);
        return doneStates.contains(state);
    }

    public Set<String> getDoneStates() {
        return doneStates;
    }
}

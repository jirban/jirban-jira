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

import static org.jirban.jira.impl.Constants.COLOUR;
import static org.jirban.jira.impl.Constants.STATES;
import static org.jirban.jira.impl.Constants.STATE_LINKS;
import static org.jirban.jira.impl.config.Util.getRequiredChild;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Configuration for a project whose issues should appear as cards on the board.
 * Its states need mapping to the board states.
 *
 * @author Kabir Khan
 */
public class NonOwnerBoardProjectConfig extends BoardProjectConfig {
    private final Map<String, String> ownToBoardStates;
    /** Maps the owner states onto our states */
    private final Map<String, String> boardToOwnStates;

    private NonOwnerBoardProjectConfig(String code, String queryFilter, String colour, Map<String, Integer> states, Map<String, String> ownToBoardStates) {
        super(code, queryFilter, colour, states);
        this.ownToBoardStates = ownToBoardStates;
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, String> entry : ownToBoardStates.entrySet()) {
            map.put(entry.getValue(), entry.getKey());
        }
        this.boardToOwnStates = Collections.unmodifiableMap(map);
    }

    static NonOwnerBoardProjectConfig load(final String projectCode, ModelNode project) {
        String colour = getRequiredChild(project, "Project", projectCode, COLOUR).asString();
        ModelNode statesLinks = getRequiredChild(project, "Project", projectCode, STATE_LINKS);
        if (project.hasDefined(STATES)) {
            throw new IllegalStateException("The non-main projects should not have states, only a state-links entry mapping its states to those of the main project");
        }

        Map<String, Integer> states = new LinkedHashMap<>();
        Map<String, String> ownToBoardStates = new HashMap<>();
        int i = 0;
        for (Property prop : statesLinks.asPropertyList()) {
            states.put(prop.getName(), i++);
            ownToBoardStates.put(prop.getName(), prop.getValue().asString());
        }
        return new NonOwnerBoardProjectConfig(projectCode, loadQueryFilter(project), colour,
                Collections.unmodifiableMap(states), Collections.unmodifiableMap(ownToBoardStates));
    }

    @Override
    public boolean isOwner() {
        return false;
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
        for (String state : boardConfig.getOwningProject().getStateNames()) {
            String myState = mapBoardStateOntoOwnState(state);
            stateLinksNode.get(state).set(myState == null ? new ModelNode() : new ModelNode(myState));
        }
        return projectNode;
    }

    @Override
    ModelNode serializeModelNodeForConfig() {
        final ModelNode projectsNode = super.serializeModelNodeForConfig();
        final ModelNode stateLinksNode = projectsNode.get(STATE_LINKS);
        stateLinksNode.setEmptyObject();
        for (Map.Entry<String, String> entry : ownToBoardStates.entrySet()) {
            stateLinksNode.get(entry.getKey()).set(entry.getValue());
        }
        return projectsNode;
    }
}

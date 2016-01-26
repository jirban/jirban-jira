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

import static org.jirban.jira.impl.config.Util.getRequiredChild;

import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;

/**
 * Configuration for the project that 'owns' the board. Its issues appear as cards on the board, and its states
 * are the board columns.
 *
 * @author Kabir Khan
 */
public class OwnerBoardProjectConfig extends BoardProjectConfig {
    public OwnerBoardProjectConfig(String code, String queryFilter, String colour, Map<String, Integer> states) {
        super(code, queryFilter, colour, states);
    }

    static OwnerBoardProjectConfig load(String projectCode, ModelNode project) {
        String colour = getRequiredChild(project, "Project", projectCode, "colour").asString();
        List<ModelNode> statesList = getRequiredChild(project, "Project", projectCode, "states").asList();
        if (project.hasDefined("state-links")) {
            throw new IllegalStateException("The main project should not have state-links, only a states array");
        }
        Map<String, Integer> statesMap = loadStringIntegerMap(statesList);
        return new OwnerBoardProjectConfig(projectCode, loadQueryFilter(project), colour, statesMap);
    }

    @Override
    public boolean isOwner() {
        return true;
    }

    @Override
    public String mapBoardStateOntoOwnState(String boardState) {
        return boardState;
    }

    @Override
    public String mapOwnStateOntoBoardState(String state) {
        return state;
    }

    @Override
    ModelNode serializeModelNodeForConfig() {
        final ModelNode projectNode = super.serializeModelNodeForConfig();
        final ModelNode statesNode = projectNode.get("states");
        statesNode.setEmptyList();
        for (String state : states.keySet()) {
            statesNode.add(state);
        }
        return projectNode;
    }
}

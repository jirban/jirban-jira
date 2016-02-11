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

import static org.jirban.jira.impl.Constants.STATES;

import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;

/**
 * Abstract base class for all kinds of project configurations.
 *
 * @author Kabir Khan
 */
public abstract class ProjectConfig {
    protected final String code;
    protected final Map<String, Integer> states;

    public ProjectConfig(final String code, final Map<String, Integer> states) {
        this.code = code;
        this.states = states;
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

    public int getMaxStateIndex() {
        return states.size() - 1;
    }

    private ModelNode getModelNodeWithStates(ModelNode parent) {
        ModelNode projectNode = parent.get(code);
        ModelNode statesNode = projectNode.get(STATES);
        this.states.keySet().forEach(statesNode::add);
        return projectNode;
    }

    ModelNode serializeModelNodeForBoard(BoardConfig boardConfig, ModelNode parent) {
        return getModelNodeWithStates(parent);
    }

}

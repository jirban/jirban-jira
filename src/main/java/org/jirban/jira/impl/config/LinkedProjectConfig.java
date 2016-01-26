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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;

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
        List<ModelNode> statesList = getRequiredChild(project, "Project", projectCode, "states").asList();
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

//    public ModelNode serializeModelNodeForConfig() {
//        s
//    }
}

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
package org.jirban.jira.impl;

import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.api.BoardCfg;
import org.jirban.jira.api.BoardConfigurationManager;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * @author Kabir Khan
 */
@Named
public class BoardConfigurationManagerImpl implements BoardConfigurationManager {

    @ComponentImport
    private final ActiveObjects activeObjects;

    @Inject
    public BoardConfigurationManagerImpl(final ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    @Override
    public String getBoardsJson() {
        Set<BoardCfg> configs = loadBoardConfigs();
        if (configs.size() == 0) {
            return "[]";
        }

        ModelNode output = new ModelNode();
        for (BoardCfg config : configs) {
            ModelNode configNode = new ModelNode();
            configNode.get("id").set(config.getID());
            configNode.get("name").set(config.getName());
            output.add(configNode);
        }
        return output.toJSONString(true);
    }

    private Set<BoardCfg> loadBoardConfigs() {
        return activeObjects.executeInTransaction(new TransactionCallback<Set<BoardCfg>>(){
            @Override
            public Set<BoardCfg> doInTransaction() {
                Set<BoardCfg> configs = new TreeSet<>((o1, o2) -> {
                    return o1.getName().compareTo(o2.getName());
                });
                for (BoardCfg boardCfg : activeObjects.find(BoardCfg.class)) {
                    configs.add(boardCfg);

                }
                return configs;
            }
        });
    }

}

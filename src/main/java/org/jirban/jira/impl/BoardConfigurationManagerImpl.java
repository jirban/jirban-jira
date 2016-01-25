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

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.api.BoardCfg;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.impl.config.BoardConfig;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.PriorityManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;

import net.java.ao.DBParam;

/**
 * @author Kabir Khan
 */
@Named("jirbanBoardConfigurationManager")
public class BoardConfigurationManagerImpl implements BoardConfigurationManager {

    //TODO Perhaps we should not cache this here, if we do we need to delete/update the entry when
    //updating/deleting and notify the Boards using this
    private volatile Map<Integer, BoardConfig> projectGroupConfigs = new ConcurrentHashMap<>();

    @ComponentImport
    private final ActiveObjects activeObjects;

    @ComponentImport
    private final IssueTypeManager issueTypeManager;

    @ComponentImport
    private final PriorityManager priorityManager;

    @Inject
    public BoardConfigurationManagerImpl(final ActiveObjects activeObjects,
                                         final IssueTypeManager issueTypeManager,
                                         final PriorityManager priorityManager) {
        this.activeObjects = activeObjects;
        this.issueTypeManager = issueTypeManager;
        this.priorityManager = priorityManager;
    }

    @Override
    public String getBoardsJson(boolean full) {
        Set<BoardCfg> configs = loadBoardConfigs();
        if (configs.size() == 0) {
            return "[]";
        }

        ModelNode output = new ModelNode();
        for (BoardCfg config : configs) {
            ModelNode configNode = new ModelNode();
            configNode.get("id").set(config.getID());
            configNode.get("name").set(config.getName());
            if (full) {
                ModelNode json = ModelNode.fromJSONString(config.getConfigJson());
                configNode.get("config").set(json);
            }
            output.add(configNode);
        }
        return output.toJSONString(true);
    }

    @Override
    public void saveBoard(final int id, final String json) {
        final ModelNode board = ModelNode.fromJSONString(json);
        final String name = board.get("name").asString();

        //TODO Validate the data

        activeObjects.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                if (id >= 0) {
                    final BoardCfg cfg = activeObjects.get(BoardCfg.class, id);
                    cfg.setName(name);
                    cfg.setConfigJson(json);
                    cfg.save();
                } else {
                    final BoardCfg cfg = activeObjects.create(
                            BoardCfg.class,
                            new DBParam("NAME", name),
                            //Compact the json before saving it
                            new DBParam("CONFIG_JSON", board.toJSONString(true)));
                    cfg.save();
                }
                return null;
            }
        });
    }

    @Override
    public void deleteBoard(int id) {
        activeObjects.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                BoardCfg cfg = activeObjects.get(BoardCfg.class, id);
                activeObjects.delete(cfg);
                return null;
            }
        });
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

    public BoardConfig getBoardConfig(final int id) {
        BoardConfig boardConfig =  projectGroupConfigs.get(id);
        if (boardConfig == null) {
            BoardCfg cfg = activeObjects.executeInTransaction(new TransactionCallback<BoardCfg>(){
                @Override
                public BoardCfg doInTransaction() {
                    return activeObjects.get(BoardCfg.class, id);
                }
            });
            if (cfg != null) {
                boardConfig = BoardConfig.load(issueTypeManager, priorityManager, id, cfg.getConfigJson());
                BoardConfig old = projectGroupConfigs.putIfAbsent(id, boardConfig);
                if (old != null) {
                    boardConfig = old;
                }
            }
        }
        return boardConfig;
    }
}

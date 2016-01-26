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
import org.jirban.jira.JirbanPermissionException;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.api.BoardCfg;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.PriorityManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;

import net.java.ao.DBParam;

/**
 * @author Kabir Khan
 */
@Named("jirbanBoardConfigurationManager")
public class BoardConfigurationManagerImpl implements BoardConfigurationManager {

    private volatile Map<Integer, BoardConfig> projectGroupConfigs = new ConcurrentHashMap<>();

    @ComponentImport
    private final ActiveObjects activeObjects;

    @ComponentImport
    private final IssueTypeManager issueTypeManager;

    @ComponentImport
    private final PriorityManager priorityManager;

    @ComponentImport
    private final PermissionManager permissionManager;

    @ComponentImport
    private final ProjectManager projectManager;

    @Inject
    public BoardConfigurationManagerImpl(final ActiveObjects activeObjects,
                                         final IssueTypeManager issueTypeManager,
                                         final PriorityManager priorityManager,
                                         final PermissionManager permissionManager,
                                         final ProjectManager projectManager) {
        this.activeObjects = activeObjects;
        this.issueTypeManager = issueTypeManager;
        this.priorityManager = priorityManager;
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
    }

    @Override
    public String getBoardsJson(ApplicationUser user, boolean forConfig) {
        Set<BoardCfg> configs = loadBoardConfigs();
        if (configs.size() == 0) {
            return "[]";
        }

        ModelNode output = new ModelNode();
        for (BoardCfg config : configs) {
            ModelNode configNode = new ModelNode();
            configNode.get("id").set(config.getID());
            configNode.get("name").set(config.getName());
            ModelNode configJson = ModelNode.fromJSONString(config.getConfigJson());
            if (forConfig) {
                if (canEditBoard(user, configJson)) {
                    configNode.get("edit").set(true);
                }
                configNode.get("config").set(configJson);
                output.add(configNode);
            } else {
                //Just a wild guess at what is needed to view the boards
                if (canViewBoard(user, configNode)) {
                    output.add(configNode);
                }
            }
        }
        return output.toJSONString(true);
    }

    @Override
    public BoardConfig getBoardConfigForBoardDisplay(ApplicationUser user, final int id) {
        BoardConfig boardConfig =  projectGroupConfigs.get(id);
        if (boardConfig == null) {
            BoardCfg cfg = activeObjects.executeInTransaction(new TransactionCallback<BoardCfg>(){
                @Override
                public BoardCfg doInTransaction() {
                    return activeObjects.get(BoardCfg.class, id);
                }
            });
            if (cfg != null) {
                boardConfig = BoardConfig.load(issueTypeManager, priorityManager, id, cfg.getOwningUser(), cfg.getConfigJson());
                BoardConfig old = projectGroupConfigs.putIfAbsent(id, boardConfig);
                if (old != null) {
                    boardConfig = old;
                }
            }
        }
        if (!canViewBoard(user, boardConfig)) {
            throw new JirbanPermissionException("Insufficient permissions to view board " +
                    boardConfig.getName() + " (" + id + ")");
        }
        return boardConfig;
    }

    @Override
    public void saveBoard(ApplicationUser user, final int id, final ModelNode config) {
        final String name = config.get("name").asString();

        //Validate it, and serialize it so that the order of fields is always the same
        final ModelNode validConfig;
        try {
            validConfig = BoardConfig.validateAndSerialize(issueTypeManager, priorityManager, id, user.getKey(), config);
        } catch (Exception e) {
            throw new JirbanValidationException("Invalid data: " + e.getMessage());
        }

        activeObjects.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                if (!canEditBoard(user, validConfig)) {
                    if (id >= 0) {
                        throw new JirbanPermissionException("Insufficient permissions to edit board '" +
                                validConfig.get("name") + "' (" + id + ")");
                    } else {
                        throw new JirbanPermissionException("Insufficient permissions to create board '" +
                                validConfig.get("name") + "'");
                    }
                }

                if (id >= 0) {
                    final BoardCfg cfg = activeObjects.get(BoardCfg.class, id);
                    cfg.setName(name);
                    cfg.setOwningUserKey(user.getKey());
                    cfg.setConfigJson(validConfig.toJSONString(true));
                    cfg.save();
                } else {
                    final BoardCfg cfg = activeObjects.create(
                            BoardCfg.class,
                            new DBParam("NAME", name),
                            new DBParam("OWNING_USER", user.getKey()),
                            //Compact the json before saving it
                            new DBParam("CONFIG_JSON", validConfig.toJSONString(true)));
                    cfg.save();
                }
                if (id >= 0) {
                    projectGroupConfigs.remove(id);
                }
                return null;
            }
        });
    }

    @Override
    public void deleteBoard(ApplicationUser user, int id) {
        activeObjects.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                BoardCfg cfg = activeObjects.get(BoardCfg.class, id);
                if (cfg == null) {
                    return null;
                }
                final ModelNode boardConfig = ModelNode.fromJSONString(cfg.getConfigJson());
                if (!canEditBoard(user, boardConfig)) {
                    throw new JirbanPermissionException("Insufficient permissions to delete board '" +
                            boardConfig.get("name") + "' (" + id + ")");
                }
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

    //Permission methods
    private boolean canEditBoard(ApplicationUser user, ModelNode boardConfig) {
        return hasPermissionBoard(user, boardConfig, ProjectPermissions.ADMINISTER_PROJECTS);
    }

    private boolean canViewBoard(ApplicationUser user, ModelNode boardConfig) {
        //A wild guess at a reasonable permission needed to view the boards
        return hasPermissionBoard(user, boardConfig, ProjectPermissions.TRANSITION_ISSUES);
    }

    private boolean canViewBoard(ApplicationUser user, BoardConfig boardConfig) {
        //A wild guess at a reasonable permission needed to view the boards
        return hasPermissionBoard(user, boardConfig, ProjectPermissions.TRANSITION_ISSUES);
    }

    private boolean hasPermissionBoard(ApplicationUser user, BoardConfig boardConfig, ProjectPermissionKey... permissions) {
        for (BoardProjectConfig boardProject : boardConfig.getBoardProjects()) {
            if (!hasPermission(user, boardProject.getCode(), permissions)) {
                return false;
            }
        }
        return true;
    }


    private boolean hasPermissionBoard(ApplicationUser user, ModelNode boardConfig, ProjectPermissionKey...permissions) {
        if (!boardConfig.hasDefined("projects")) {
            //The project is empty, start checking once they add something
            return true;
        }
        for (String projectCode : boardConfig.get("projects").keys()) {
            if (!hasPermission(user, projectCode, permissions)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission(ApplicationUser user, String projectCode, ProjectPermissionKey[] permissions) {
        Project project = projectManager.getProjectByCurrentKey(projectCode);
        for (ProjectPermissionKey permission : permissions) {
            if (!permissionManager.hasPermission(permission, project, user)) {
                return false;
            }
        }
        return true;
    }
}

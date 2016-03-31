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

import static org.jirban.jira.impl.Constants.CODE;
import static org.jirban.jira.impl.Constants.CONFIG;
import static org.jirban.jira.impl.Constants.CONFIGS;
import static org.jirban.jira.impl.Constants.RANK_CUSTOM_FIELD;
import static org.jirban.jira.impl.Constants.EDIT;
import static org.jirban.jira.impl.Constants.ID;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.PROJECTS;
import static org.jirban.jira.impl.Constants.RANK_CUSTOM_FIELD_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanPermissionException;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.impl.activeobjects.BoardCfg;
import org.jirban.jira.impl.activeobjects.Setting;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.PriorityManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;

import net.java.ao.DBParam;
import net.java.ao.Query;

/**
 * @author Kabir Khan
 */
@Named("jirbanBoardConfigurationManager")
public class BoardConfigurationManagerImpl implements BoardConfigurationManager {

    private volatile Map<String, BoardConfig> boardConfigs = new ConcurrentHashMap<>();

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

    @ComponentImport
    private final GlobalPermissionManager globalPermissionManager;

    /** The 'Rank' custom field as output by  */
    private volatile int rankCustomFieldId = -1;

    @Inject
    public BoardConfigurationManagerImpl(final ActiveObjects activeObjects,
                                         final IssueTypeManager issueTypeManager,
                                         final PriorityManager priorityManager,
                                         final PermissionManager permissionManager,
                                         final ProjectManager projectManager,
                                         final GlobalPermissionManager globalPermissionManager) {
        this.activeObjects = activeObjects;
        this.issueTypeManager = issueTypeManager;
        this.priorityManager = priorityManager;
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
        this.globalPermissionManager = globalPermissionManager;
    }

    @Override
    public String getBoardsJson(ApplicationUser user, boolean forConfig) {
        Set<BoardCfg> configs = loadBoardConfigs();
        ModelNode configsList = new ModelNode();
        configsList.setEmptyList();
        for (BoardCfg config : configs) {
            ModelNode configNode = new ModelNode();
            configNode.get(ID).set(config.getID());
            configNode.get(CODE).set(config.getCode());
            configNode.get(NAME).set(config.getName());
            ModelNode configJson = ModelNode.fromJSONString(config.getConfigJson());
            if (forConfig) {
                if (canEditBoard(user, configJson)) {
                    configNode.get(EDIT).set(true);
                }
                configNode.get(CONFIG).set(configJson);
                configsList.add(configNode);
            } else {
                //A guess at what is needed to view the boards
                if (canViewBoard(user, configNode)) {
                    configsList.add(configNode);
                }
            }
        }

        //Add a few more fields
        ModelNode config = new ModelNode();
        config.get(CONFIGS).set(configsList);

        if (forConfig) {
            addCustomFieldInfo(user, config.get(RANK_CUSTOM_FIELD));
        }

        return config.toJSONString(true);
    }

    private void addCustomFieldInfo(ApplicationUser user, ModelNode customFieldConfig) {
        int customFieldId = getRankCustomFieldId();
        customFieldConfig.get(ID).set(customFieldId);
        customFieldConfig.get(EDIT).set(canEditCustomField(user));
    }

    @Override
    public BoardConfig getBoardConfigForBoardDisplay(ApplicationUser user, final String code) {
        BoardConfig boardConfig =  boardConfigs.get(code);
        if (boardConfig == null) {
            BoardCfg[] cfgs = activeObjects.executeInTransaction(new TransactionCallback<BoardCfg[]>(){
                @Override
                public BoardCfg[] doInTransaction() {
                    return activeObjects.find(BoardCfg.class, Query.select().where("code = ?", code));
                }
            });

            if (cfgs != null && cfgs.length == 1) {
                BoardCfg cfg = cfgs[0];
                boardConfig = BoardConfig.load(issueTypeManager, priorityManager, cfg.getID(), cfg.getOwningUser(), cfg.getConfigJson(), getRankCustomFieldId());
                BoardConfig old = boardConfigs.putIfAbsent(code, boardConfig);
                if (old != null) {
                    boardConfig = old;
                }
            }
        }
        if (boardConfig != null && !canViewBoard(user, boardConfig)) {
            throw new JirbanPermissionException("Insufficient permissions to view board " +
                    boardConfig.getName() + " (" + code + ")");
        }
        return boardConfig;
    }

    @Override
    public BoardConfig saveBoard(ApplicationUser user, final int id, final ModelNode config) {
        final String code = config.get(CODE).asString();
        final String name = config.get(NAME).asString();

        //Validate it, and serialize it so that the order of fields is always the same
        final BoardConfig boardConfig;
        final ModelNode validConfig;
        try {
            boardConfig = BoardConfig.load(issueTypeManager, priorityManager, id, user.getKey(), config, getRankCustomFieldId());
            validConfig = boardConfig.serializeModelNodeForConfig();
        } catch (Exception e) {
            throw new JirbanValidationException("Invalid data: " + e.getMessage());
        }

        activeObjects.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                if (!canEditBoard(user, validConfig)) {
                    if (id >= 0) {
                        throw new JirbanPermissionException("Insufficient permissions to edit board '" +
                                validConfig.get(NAME) + "' (" + id + ")");
                    } else {
                        throw new JirbanPermissionException("Insufficient permissions to create board '" +
                                validConfig.get(NAME) + "'");
                    }
                }

                if (id >= 0) {
                    final BoardCfg cfg = activeObjects.get(BoardCfg.class, id);
                    cfg.setCode(code);
                    cfg.setName(name);
                    cfg.setOwningUserKey(user.getKey());
                    cfg.setConfigJson(validConfig.toJSONString(true));
                    cfg.save();
                } else {
                    final BoardCfg cfg = activeObjects.create(
                            BoardCfg.class,
                            new DBParam("CODE", code),
                            new DBParam("NAME", name),
                            new DBParam("OWNING_USER", user.getKey()),
                            //Compact the json before saving it
                            new DBParam("CONFIG_JSON", validConfig.toJSONString(true)));
                    cfg.save();
                }
                if (id >= 0) {
                    boardConfigs.remove(code);
                }
                return null;
            }
        });
        return boardConfig;
    }

    @Override
    public String deleteBoard(ApplicationUser user, int id) {
        final String code = activeObjects.executeInTransaction(new TransactionCallback<String>() {
            @Override
            public String doInTransaction() {
                BoardCfg cfg = activeObjects.get(BoardCfg.class, id);
                if (cfg == null) {
                    return null;
                }
                final String code = cfg.getCode();
                final ModelNode boardConfig = ModelNode.fromJSONString(cfg.getConfigJson());
                if (!canEditBoard(user, boardConfig)) {
                    throw new JirbanPermissionException("Insufficient permissions to delete board '" +
                            boardConfig.get(NAME) + "' (" + id + ")");
                }
                activeObjects.delete(cfg);
                return code;
            }
        });
        if (code != null) {
            boardConfigs.remove(code);
        }
        return code;
    }

    @Override
    public List<String> getBoardCodesForProjectCode(String projectCode) {
        //For now just iterate
        List<String> boardCodes = new ArrayList<>();
        for (Map.Entry<String, BoardConfig> entry : boardConfigs.entrySet()) {
            if (entry.getValue().getBoardProject(projectCode) != null) {
                boardCodes.add(entry.getKey());
            }
        }
        return boardCodes;
    }

    @Override
    public void saveCustomFieldId(ApplicationUser user, ModelNode idNode) {
        if (!canEditCustomField(user)) {
            throw new JirbanPermissionException("Only Jira Administrators can edit the custom field id");
        }
        final int id;
        try {
            id = idNode.get(ID).asInt();
        } catch (Exception e) {
            throw new JirbanValidationException("The id needs to be a number");
        }
        String idValue = String.valueOf(id);

        activeObjects.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                Setting[] settings =  activeObjects.find(Setting.class, Query.select().where("name = ?", RANK_CUSTOM_FIELD_ID));

                if (settings.length == 0) {
                    //Insert
                    final Setting setting = activeObjects.create(
                            Setting.class,
                            new DBParam("NAME", RANK_CUSTOM_FIELD_ID),
                            new DBParam("VALUE", idValue));
                    setting.save();
                } else {
                    //update
                    Setting setting = settings[0];
                    setting.setValue(idValue);
                    setting.save();
                }
                rankCustomFieldId = Integer.valueOf(idValue);

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

    private int getRankCustomFieldId() {
        int customFieldId = this.rankCustomFieldId;
        if (customFieldId < 0) {
            Setting[] settings = activeObjects.executeInTransaction(new TransactionCallback<Setting[]>() {
                @Override
                public Setting[] doInTransaction() {
                    return activeObjects.find(Setting.class, Query.select().where("name = ?", RANK_CUSTOM_FIELD_ID));
                }
            });
            if (settings.length == 1) {
                customFieldId = Integer.valueOf(settings[0].getValue());
                this.rankCustomFieldId = customFieldId;
            }
        }
        return customFieldId;
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

    private boolean canEditCustomField(ApplicationUser user) {
        //Only Jira Administrators can tweak the custom field id
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
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
        if (!boardConfig.hasDefined(PROJECTS)) {
            //The project is empty, start checking once they add something
            return true;
        }
        for (String projectCode : boardConfig.get(PROJECTS).keys()) {
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

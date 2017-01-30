/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jirban.jira.impl;

import static org.jirban.jira.impl.Constants.CODE;
import static org.jirban.jira.impl.Constants.CONFIGS;
import static org.jirban.jira.impl.Constants.EDIT;
import static org.jirban.jira.impl.Constants.ID;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.PROJECTS;
import static org.jirban.jira.impl.Constants.RANK_CUSTOM_FIELD;
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
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.transaction.TransactionCallback;

import net.java.ao.DBParam;
import net.java.ao.Query;

/**
 * @author Kabir Khan
 */
@Named("jirbanBoardConfigurationManager")
public class BoardConfigurationManagerImpl implements BoardConfigurationManager {

    private volatile Map<String, BoardConfig> boardConfigs = new ConcurrentHashMap<>();

    private final JiraInjectables jiraInjectables;

    /** The 'Rank' custom field id */
    private volatile long rankCustomFieldId = -1;

    @Inject
    public BoardConfigurationManagerImpl(JiraInjectables jiraInjectables) {
        this.jiraInjectables = jiraInjectables;
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

    @Override
    public String getBoardJsonConfig(ApplicationUser user, int boardId) {
        BoardCfg[] cfgs = jiraInjectables.getActiveObjects().executeInTransaction(new TransactionCallback<BoardCfg[]>(){
            @Override
            public BoardCfg[] doInTransaction() {
                return jiraInjectables.getActiveObjects().find(BoardCfg.class, Query.select().where("id = ?", boardId));
            }
        });
        ModelNode configJson = ModelNode.fromJSONString(cfgs[0].getConfigJson());
        return configJson.toJSONString(true);
    }

    private void addCustomFieldInfo(ApplicationUser user, ModelNode customFieldConfig) {
        long customFieldId = getRankCustomFieldId();
        customFieldConfig.get(ID).set(customFieldId);
        customFieldConfig.get(EDIT).set(canEditCustomField(user));
    }

    @Override
    public BoardConfig getBoardConfigForBoardDisplay(ApplicationUser user, final String code) {
        BoardConfig boardConfig = getBoardConfig(code);
        
        if (boardConfig != null && !canViewBoard(user, boardConfig)) {
            throw new JirbanPermissionException("Insufficient permissions to view board " +
                    boardConfig.getName() + " (" + code + ")");
        }
        return boardConfig;
    }

    @Override
    public BoardConfig getBoardConfig(final String code) {
        BoardConfig boardConfig =  boardConfigs.get(code);
        if (boardConfig == null) {
            final ActiveObjects activeObjects = jiraInjectables.getActiveObjects();
            BoardCfg[] cfgs = activeObjects.executeInTransaction(new TransactionCallback<BoardCfg[]>(){
                @Override
                public BoardCfg[] doInTransaction() {
                    return activeObjects.find(BoardCfg.class, Query.select().where("code = ?", code));
                }
            });

            if (cfgs != null && cfgs.length == 1) {
                BoardCfg cfg = cfgs[0];
                boardConfig = BoardConfig.load(jiraInjectables, cfg.getID(),
                        cfg.getOwningUser(), cfg.getConfigJson(), getRankCustomFieldId());

                BoardConfig old = boardConfigs.putIfAbsent(code, boardConfig);
                if (old != null) {
                    boardConfig = old;
                }
            }
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
            boardConfig = BoardConfig.load(jiraInjectables, id,
                    user.getKey(), config, getRankCustomFieldId());

            validConfig = boardConfig.serializeModelNodeForConfig();
        } catch (Exception e) {
            throw new JirbanValidationException("Invalid data: " + e.getMessage());
        }

        final ActiveObjects activeObjects = jiraInjectables.getActiveObjects();

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
        final ActiveObjects activeObjects = jiraInjectables.getActiveObjects();

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
    public void saveRankCustomFieldId(ApplicationUser user, ModelNode idNode) {
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

        final ActiveObjects activeObjects = jiraInjectables.getActiveObjects();

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

    @Override
    public String getStateHelpTextsJson(ApplicationUser user, String boardCode) {
        BoardConfig cfg = getBoardConfigForBoardDisplay(user, boardCode);
        Map<String, String> helpTexts = cfg.getStateHelpTexts();
        ModelNode output = new ModelNode();
        output.setEmptyObject();
        for (Map.Entry<String, String> entry : helpTexts.entrySet()) {
            output.get(entry.getKey()).set(entry.getValue());
        }
        return output.toJSONString(true);
    }

    private Set<BoardCfg> loadBoardConfigs() {
        final ActiveObjects activeObjects = jiraInjectables.getActiveObjects();

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

    public long getRankCustomFieldId() {
        long customFieldId = this.rankCustomFieldId;
        if (customFieldId < 0) {
            final ActiveObjects activeObjects = jiraInjectables.getActiveObjects();

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
        return isJiraAdministrator(user);
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
        if (isJiraAdministrator(user)) {
            return true;
        }
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
        if (isJiraAdministrator(user)) {
            return true;
        }
        final ProjectManager projectManager = jiraInjectables.getProjectManager();
        final PermissionManager permissionManager = jiraInjectables.getPermissionManager();

        Project project = projectManager.getProjectByCurrentKey(projectCode);
        for (ProjectPermissionKey permission : permissions) {
            if (!permissionManager.hasPermission(permission, project, user)) {
                return false;
            }
        }
        return true;
    }

    private boolean isJiraAdministrator(ApplicationUser user) {
        final GlobalPermissionManager globalPermissionManager = jiraInjectables.getGlobalPermissionManager();

        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }
}

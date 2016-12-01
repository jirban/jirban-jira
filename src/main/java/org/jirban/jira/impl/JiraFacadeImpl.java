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

import java.io.InputStream;
import java.util.jar.Manifest;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanLogger;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.api.JiraFacade;
import org.jirban.jira.api.UserAccessManager;
import org.jirban.jira.impl.config.BoardConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;

@Named ("jirbanJiraFacade")
public class JiraFacadeImpl implements JiraFacade, InitializingBean, DisposableBean {
    private final BoardConfigurationManager boardConfigurationManager;

    private final BoardManager boardManager;

    private final UserAccessManager userAccessManager;

    private static final String jirbanVersion;

    static {
        String version;
        try (InputStream stream = JiraFacadeImpl.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
            Manifest manifest = null;
            if (stream != null) {
                manifest = new Manifest(stream);
            }
            version = manifest.getMainAttributes().getValue("Bundle-Version");
        } catch (Exception e) {
            // ignored
            version = "Error";
        }
        jirbanVersion = version;
    }

    @Inject
    public JiraFacadeImpl(final BoardConfigurationManager boardConfigurationManager,
                          final BoardManager boardManager,
                          final UserAccessManager userAccessManager) {
        this.boardConfigurationManager = boardConfigurationManager;
        this.boardManager = boardManager;
        this.userAccessManager = userAccessManager;
    }

    @Override
    public String getBoardConfigurations(ApplicationUser user) {
        return boardConfigurationManager.getBoardsJson(user, true);
    }

    @Override
    public String getBoardJsonForConfig(ApplicationUser user, int boardId) {
        return boardConfigurationManager.getBoardJsonConfig(user, boardId);
    }

    @Override
    public void saveBoardConfiguration(ApplicationUser user, int id, String jiraUrl, ModelNode config) {
        BoardConfig boardConfig = boardConfigurationManager.saveBoard(user, id, config);
        if (id >= 0) {
            //We are modifying a board's configuration. Delete the board config and board data to force a refresh.
            boardManager.deleteBoard(user, boardConfig.getCode());
        }
    }

    @Override
    public void deleteBoardConfiguration(ApplicationUser user, int id) {
        final String code = boardConfigurationManager.deleteBoard(user, id);
        boardManager.deleteBoard(user, code);
    }

    @Override
    public String getBoardJson(ApplicationUser user, boolean backlog, String code) throws SearchException {
        try {
            return boardManager.getBoardJson(user, backlog, code);
        } catch (Exception e) {
            //Last parameter is the exception (it does not match a {} entry)
            JirbanLogger.LOGGER.debug("BoardManagerImpl.handleEvent - Error loading board {}", code, e);
            if (e instanceof SearchException || e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBoardsForDisplay(ApplicationUser user) {
        return boardConfigurationManager.getBoardsJson(user, false);
    }

    @Override
    public String getChangesJson(ApplicationUser user, boolean backlog, String code, int viewId) throws SearchException {
        return boardManager.getChangesJson(user, backlog, code, viewId);
    }

    @Override
    public void saveCustomFieldId(ApplicationUser user, ModelNode idNode) {
        boardConfigurationManager.saveRankCustomFieldId(user, idNode);
    }

    @Override
    public String getStateHelpTexts(ApplicationUser user, String boardCode) {
        return boardConfigurationManager.getStateHelpTextsJson(user, boardCode);
    }

    @Override
    public String getJirbanVersion() {
        return jirbanVersion;
    }


    @Override
    public void logUserAccess(ApplicationUser user, String boardCode, String userAgent) {
        userAccessManager.logUserAccess(user, boardCode, userAgent);
    }

    @Override
    public String getUserAccessJson(ApplicationUser user) {
        return userAccessManager.getUserAccessJson(user);
    }

    @Override
    public void updateParallelTaskForIssue(ApplicationUser user, String boardCode, String issueKey, int taskIndex, int optionIndex) throws SearchException{
        try {
            boardManager.updateParallelTaskForIssue(user, boardCode, issueKey, taskIndex, optionIndex);
        } catch (Exception e) {
            //Last parameter is the exception (it does not match a {} entry)
            JirbanLogger.LOGGER.debug("BoardManagerImpl.handleEvent - Error updating board {}", boardCode, e);
            if (e instanceof SearchException || e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

}
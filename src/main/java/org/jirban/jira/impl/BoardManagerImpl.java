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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanLogger;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.impl.board.Board;
import org.jirban.jira.impl.board.BoardChangeRegistry;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.CustomFieldConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

/**
 * The interface to the loaded boards
 *
 * @author Kabir Khan
 */
@Named("jirbanBoardManager")
public class BoardManagerImpl implements BoardManager, InitializingBean, DisposableBean {

    private static final int REFRESH_TIMEOUT_SECONDS = 5 * 60;

    //Guarded by this
    private Map<String, Board> boards = new HashMap<>();
    //Guarded by this
    private Map<String, BoardChangeRegistry> boardChangeRegistries = new HashMap<>();

    @ComponentImport
    private final SearchService searchService;

    @ComponentImport
    private final AvatarService avatarService;

    @ComponentImport
    private final IssueLinkManager issueLinkManager;

    @ComponentImport
    private final ProjectManager projectManager;

    @ComponentImport
    private final PermissionManager permissionManager;

    private final UserManager userManager;

    private final BoardConfigurationManager boardConfigurationManager;

    private final ExecutorService boardRefreshExecutor = Executors.newSingleThreadExecutor();

    private final Queue<RefreshEntry> boardRefreshQueue = new LinkedBlockingQueue<>();

    //Guarded by this
    private final Map<String, RefreshEntry> refreshEntries = new HashMap<>();

    @Inject
    public BoardManagerImpl(SearchService searchService, AvatarService avatarService, IssueLinkManager issueLinkManager,
                            ProjectManager projectManager, PermissionManager permissionManager,
                            BoardConfigurationManager boardConfigurationManager) {
        this.searchService = searchService;
        this.avatarService = avatarService;
        this.issueLinkManager = issueLinkManager;
        this.projectManager = projectManager;
        this.permissionManager = permissionManager;
        this.userManager = ComponentAccessor.getUserManager();
        this.boardConfigurationManager = boardConfigurationManager;
    }

    @Override
    public String getBoardJson(ApplicationUser user, boolean backlog, String code) throws SearchException {
        Board board = boards.get(code);
        if (board == null) {
            synchronized (this) {
                board = boards.get(code);
                if (board == null) {

                    //Use the logged in user to check if we are allowed to view the board
                    final BoardConfig boardConfig = boardConfigurationManager.getBoardConfigForBoardDisplay(user, code);
                    /*
                    Use the board owner to load the board data. The board is only loaded once, and shared amongst all
                    users.
                    Since I was not 100% sure which permission to use to determine if a user can view the board in the
                    check done by getBoardConfigForBoardDisplay(), it feels less error-prone to use the user who created
                    the board (who needs the project admin permission) to load this data.
                    This user is only used to load board data; all changes will be done using the logged in user.
                    */

                    final ApplicationUser boardOwner = userManager.getUserByKey(boardConfig.getOwningUserKey());
                    board = Board.builder(searchService, avatarService, issueLinkManager, userManager, boardConfig, boardOwner).load().build();
                    JirbanLogger.LOGGER.debug("Full refresh of board {}; backlog: {}", code, backlog);
                    boards.put(code, board);
                    boardChangeRegistries.put(code, new BoardChangeRegistry(this, board));
                    final RefreshEntry refreshEntry = new RefreshEntry(code, REFRESH_TIMEOUT_SECONDS);
                    boardRefreshQueue.add(refreshEntry);
                    refreshEntries.put(code, refreshEntry);
                }
            }
        }
        return board.serialize(backlog, user, projectManager, permissionManager).toJSONString(true);
    }

    @Override
    public void deleteBoard(ApplicationUser user, String code) {
        deleteBoard(code);
    }

    public void forceRefresh(String code) {
        deleteBoard(code);
    }

    private void deleteBoard(String code) {
        synchronized (this) {
            boards.remove(code);
            BoardChangeRegistry registry = boardChangeRegistries.remove(code);
            if (registry != null) {
                registry.invalidate();
            }
            RefreshEntry refreshEntry = refreshEntries.remove(code);
            if (refreshEntry != null) {
                refreshEntry.invalidate();
            }
        }
    }

    @Override
    public boolean hasBoardsForProjectCode(String projectCode) {
        List<String> boardCodes = boardConfigurationManager.getBoardCodesForProjectCode(projectCode);
        if (boardCodes.size() == 0) {
            return false;
        }
        synchronized (this) {
            for (String boardCode : boardCodes) {
                //There might be a config, but no board. So check if there is a board first.
                if (boards.get(boardCode) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Set<CustomFieldConfig> getCustomFieldsForUpdateEvent(String projectCode, String jiraCustomFieldName) {
        List<String> boardCodes = boardConfigurationManager.getBoardCodesForProjectCode(projectCode);
        if (boardCodes.size() == 0) {
            return null;
        }
        Set<String> activeBoards = new HashSet<>();
        synchronized (this) {
            for (String boardCode : boardCodes) {
                //There might be a config, but no board. So check if there is a board first.
                //There is a slight chance that a new board might pop up so we will miss this update, but it isn't a big
                //deal. It will come in during the next periodic full refresh.
                if (boards.get(boardCode) != null) {
                    activeBoards.add(boardCode);
                }
            }
        }

        Set<CustomFieldConfig> result = null;
        for (String boardCode : boardCodes) {
            BoardConfig boardConfig = null;
            try {
                boardConfig = boardConfigurationManager.getBoardConfig(boardCode);
                if (boardConfig != null) {
                    CustomFieldConfig config = boardConfig.getCustomFieldObjectForJiraName(jiraCustomFieldName);
                    if (config != null) {
                        if (result == null) {
                            result = new HashSet<>();
                        }
                        result.add(config);
                    }
                }
            } catch (JirbanValidationException e) {
                JirbanLogger.LOGGER.error("Error loading board {} {}", boardCode, e.getMessage());
            }
        }
        return result != null ? result : Collections.emptySet();
    }

    @Override
    public Set<CustomFieldConfig> getCustomFieldsForCreateEvent(String projectCode) {
        List<String> boardCodes = boardConfigurationManager.getBoardCodesForProjectCode(projectCode);
        if (boardCodes.size() == 0) {
            return Collections.emptySet();
        }
        Set<String> activeBoards = new HashSet<>();
        synchronized (this) {
            for (String boardCode : boardCodes) {
                //There might be a config, but no board. So check if there is a board first.
                //There is a slight chance that a new board might pop up so we will miss this update, but it isn't a big
                //deal. It will come in during the next periodic full refresh.
                if (boards.get(boardCode) != null) {
                    activeBoards.add(boardCode);
                }
            }
        }
        Set<CustomFieldConfig> result = null;
        for (String boardCode : boardCodes) {
            BoardConfig boardConfig = null;
            try {
                boardConfig = boardConfigurationManager.getBoardConfig(boardCode);
                Set<CustomFieldConfig> configs = boardConfig.getCustomFieldConfigs();
                if (configs.size() > 0) {
                    if (result == null) {
                        result = new HashSet<>();
                    }
                    result.addAll(configs);
                }
            } catch (JirbanValidationException e) {
                JirbanLogger.LOGGER.error("Error loading board {} {}", boardCode, e.getMessage());
            }
        }
        return result != null ? result : Collections.emptySet();
    }

    @Override
    public void handleEvent(JirbanIssueEvent event) {
        //Jira seems to only handle one event at a time, which is good

        List<String> boardCodes = boardConfigurationManager.getBoardCodesForProjectCode(event.getProjectCode());
        for (String boardCode : boardCodes) {
            final Board board;
            final BoardChangeRegistry changeRegistry;
            synchronized (this) {
                board = boards.get(boardCode);
                if (board == null) {
                    continue;
                }
                changeRegistry = boardChangeRegistries.get(boardCode);
            }
            final ApplicationUser boardOwner = userManager.getUserByKey(board.getConfig().getOwningUserKey());
            try {
                JirbanLogger.LOGGER.debug("BoardManagerImpl.handleEvent - Handling event on board {}", board.getConfig().getCode());
                Board newBoard = board.handleEvent(searchService, avatarService, issueLinkManager, boardOwner, event, changeRegistry);
                if (newBoard == null) {
                    //The changes in the issue were not relevant
                    return;
                }
                synchronized (this) {
                    //An event ending up in forceRefresh() might have deleted the board and the change registry
                    //with the intent of forcing the next read to perform a full refresh
                    //We have the new board returned, but check if we need to recreate the registry
                    if (changeRegistry.isValid()) {
                        changeRegistry.setBoard(newBoard);
                        boards.put(boardCode, newBoard);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                //Last parameter is the exception (it does not match a {} entry)
                JirbanLogger.LOGGER.error("BoardManagerImpl.handleEvent - Error handling event {} - {}", event.getIssueKey(), e.getMessage());

                //Last parameter is the exception (it does not match a {} entry)
                JirbanLogger.LOGGER.debug("BoardManagerImpl.handleEvent - Error handling event {}", event.getIssueKey(), e);
            }
        }
    }

    @Override
    public String getChangesJson(ApplicationUser user, boolean backlog, String code, int viewId) throws SearchException {
        //Check we are allowed to view the board
        boardConfigurationManager.getBoardConfigForBoardDisplay(user, code);

        BoardChangeRegistry boardChangeRegistry;
        synchronized (this) {
            boardChangeRegistry = boardChangeRegistries.get(code);
        }

        if (boardChangeRegistry == null) {
            //There is config but no board, so do a full refresh
            return getBoardJson(user, backlog, code);
        }

        final ModelNode changes;
        try {
            changes = boardChangeRegistry.getChangesSince(backlog, viewId);
        } catch (BoardChangeRegistry.FullRefreshNeededException e) {
            return getBoardJson(user, backlog, code);
        }

        return changes.toJSONString(true);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        boardRefreshExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(10000);

                        //Throw out all the 'expired' boards so that they are refreshed again
                        synchronized (BoardManagerImpl.this) {
                            RefreshEntry entry = boardRefreshQueue.peek();
                            while (entry != null && System.currentTimeMillis() > entry.endTime) {
                                //Remove the entry we peeked at
                                entry = boardRefreshQueue.poll();

                                if (entry.isValid()) {
                                    JirbanLogger.LOGGER.debug("Periodic task deleting board " + entry.boardCode);
                                    //Remove the board, an attempt to read it will result in a new instance being fully loaded
                                    //and created
                                    boardChangeRegistries.remove(entry.boardCode);
                                    boards.remove(entry.boardCode);
                                    //When an attempt is made to get the board again, a new entry will be added to the  queue
                                }
                                entry = boardRefreshQueue.peek();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        boardRefreshExecutor.shutdownNow();
        boardRefreshExecutor.awaitTermination(10, TimeUnit.SECONDS);
    }

    private static class RefreshEntry {
        private final String boardCode;
        private final long endTime;
        private volatile boolean valid = true;

        public RefreshEntry(String boardCode, int timeoutSeconds) {
            this.boardCode = boardCode;
            this.endTime = System.currentTimeMillis() + timeoutSeconds *1000;
        }

        void invalidate() {
            valid = false;
        }

        boolean isValid() {
            return valid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RefreshEntry that = (RefreshEntry) o;
            return boardCode == that.boardCode;
        }

        @Override
        public int hashCode() {
            return boardCode.hashCode();
        }
    }
}

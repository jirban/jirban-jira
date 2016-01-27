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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.impl.board.Board;
import org.jirban.jira.impl.config.BoardConfig;

import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

/**
 * The interface to the loaded boards
 *
 * @author Kabir Khan
 */
@Named("jirbanBoardManager")
public class BoardManagerImpl implements BoardManager {

    private Map<Integer, Board> boards = Collections.synchronizedMap(new HashMap<>());

    @ComponentImport
    private final SearchService searchService;

    @ComponentImport
    private final AvatarService avatarService;

    @ComponentImport
    private final IssueLinkManager issueLinkManager;

    private final UserManager userManager;

    private final BoardConfigurationManager boardConfigurationManager;


    @Inject
    public BoardManagerImpl(SearchService searchService, AvatarService avatarService, IssueLinkManager issueLinkManager,
                            BoardConfigurationManager boardConfigurationManager) {
        this.searchService = searchService;
        this.avatarService = avatarService;
        this.issueLinkManager = issueLinkManager;
        //AuthManager is loading a few different user managers, which breaks injection
        this.userManager = ComponentAccessor.getUserManager();
        this.boardConfigurationManager = boardConfigurationManager;

    }

    @Override
    public String getBoardJson(ApplicationUser user, int id) throws SearchException {
        Board board = boards.get(id);
        if (board == null) {
            synchronized (this) {
                board = boards.get(id);
                if (board == null) {
                    //Use the logged in user to check if we are allowed to view the board
                    final BoardConfig boardConfig = boardConfigurationManager.getBoardConfigForBoardDisplay(user, id);
                    /*
                    Use the board owner to load the board data. The board is only loaded once, and shared amongst all
                    users.
                    Since I was not 100% sure which permission to use to determine if a user can view the board in the
                    check done by getBoardConfigForBoardDisplay(), it feels less error-prone to use the user who created
                    the board (who needs the project admin permission) to load this data.
                    This user is only used to load board data; all changes will be done using the logged in user.
                    */

                    final ApplicationUser boardOwner = userManager.getUserByKey(boardConfig.getOwningUserKey());
                    board = Board.builder(searchService, avatarService, issueLinkManager, boardConfig, boardOwner).load().build();
                    boards.put(id, board);
                }
            }
        }
        System.out.println(board.serialize().toJSONString(false));
        return board.serialize().toJSONString(true);
    }

    @Override
    public void deleteBoard(ApplicationUser user, int id) {
        synchronized (this) {
            boards.remove(id);
        }
    }

    @Override
    public boolean hasBoardsForProjectCode(String projectCode) {
        List<Integer> boardIds = boardConfigurationManager.getBoardIdsForProjectCode(projectCode);
        if (boardIds.size() == 0) {
            return false;
        }
        for (Integer boardId : boardIds) {
            if (boards.get(boardId) != null) {
                return true;
            }
        }
        return false;
    }
}

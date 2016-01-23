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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.impl.board.Board;
import org.jirban.jira.impl.config.BoardConfig;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.search.SearchException;
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

    private final BoardConfigurationManager boardConfigurationManager;

    @Inject
    public BoardManagerImpl(SearchService searchService, BoardConfigurationManager boardConfigurationManager) {
        this.searchService = searchService;
        this.boardConfigurationManager = boardConfigurationManager;
    }

    @Override
    public String getBoardJson(User user, int id) throws SearchException {
        Board board = boards.get(id);
        if (board == null) {
            synchronized (this) {
                board = boards.get(id);
                if (board == null) {
                    final BoardConfig boardConfig = boardConfigurationManager.getBoardConfig(id);
                    final Board.Builder boardBuilder = Board.builder(searchService, user, boardConfig).load();
                    boards.put(id, board);
                }
            }
        }
        return "{}";
        //return board.serialize().toJSONString(true);
    }


}

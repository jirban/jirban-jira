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

import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;

import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.user.util.UserManager;

import ut.org.jirban.jira.mock.AvatarServiceBuilder;

/**
 * @author Kabir Khan
 */
public class BoardManagerBuilder {

    private SearchService searchService;
    private AvatarService avatarService = AvatarServiceBuilder.getUserNameUrlMock();
    private IssueLinkManager issueLinkManager;
    private UserManager userManager;
    private BoardConfigurationManager boardConfigurationManager;

    public BoardManagerBuilder() {
    }

    public BoardManagerBuilder setSearchService(SearchService searchService) {
        this.searchService = searchService;
        return this;
    }

    public BoardManagerBuilder setAvatarService(AvatarService avatarService) {
        this.avatarService = avatarService;
        return this;
    }

    public BoardManagerBuilder setIssueLinkManager(IssueLinkManager issueLinkManager) {
        this.issueLinkManager = issueLinkManager;
        return this;
    }

    public BoardManagerBuilder setBoardConfigurationManager(BoardConfigurationManager boardConfigurationManager) {
        this.boardConfigurationManager = boardConfigurationManager;
        return this;
    }

    public BoardManagerBuilder setUserManager(UserManager userManager) {
        this.userManager = userManager;
        return this;
    }

    public BoardManager build() {
        return new BoardManagerImpl(searchService, avatarService, issueLinkManager, boardConfigurationManager);
    }
}

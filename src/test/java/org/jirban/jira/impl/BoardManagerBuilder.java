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
import org.jirban.jira.api.NextRankedIssueUtil;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.user.UserService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.PriorityManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.sal.api.ApplicationProperties;

import ut.org.jirban.jira.mock.AvatarServiceBuilder;
import ut.org.jirban.jira.mock.PermissionManagerBuilder;
import ut.org.jirban.jira.mock.ProjectManagerBuilder;

/**
 * @author Kabir Khan
 */
public class BoardManagerBuilder {

    private SearchService searchService;
    private AvatarService avatarService = AvatarServiceBuilder.getUserNameUrlMock();
    private IssueLinkManager issueLinkManager;
    private UserManager userManager;
    private BoardConfigurationManager boardConfigurationManager;
    private ProjectManager projectManager = ProjectManagerBuilder.getAnyProjectManager();
    private PermissionManager permissionManager = PermissionManagerBuilder.getAllowsAll();
    private NextRankedIssueUtil nextRankedIssueUtil;

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

    public BoardManagerBuilder setProjectManager(ProjectManager projectManager) {
        this.projectManager = projectManager;
        return this;
    }

    public BoardManagerBuilder setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
        return this;
    }

    public BoardManagerBuilder setNextRankedIssueUtil(NextRankedIssueUtil nextRankedIssueUtil) {
        this.nextRankedIssueUtil = nextRankedIssueUtil;
        return this;
    }

    public BoardManager build() {
        //These are not needed for this code path at the moment
        final ActiveObjects activeObjects = null;
        final ApplicationProperties applicationProperties = null;
        final CustomFieldManager customFieldManager = null;
        final GlobalPermissionManager globalPermissionManager = null;
        final IssueService issueService = null;
        final IssueTypeManager issueTypeManager = null;
        final PriorityManager priorityManager = null;
        final UserService userService = null;
        final VersionManager versionManager = null;

        JiraInjectables jiraInjectables = new JiraInjectables(
                activeObjects,
                applicationProperties,
                avatarService,
                customFieldManager,
                globalPermissionManager,
                issueService,
                issueLinkManager,
                issueTypeManager,
                permissionManager,
                projectManager,
                priorityManager,
                searchService,
                userService,
                versionManager);

        return new BoardManagerImpl(jiraInjectables, boardConfigurationManager);
    }
}

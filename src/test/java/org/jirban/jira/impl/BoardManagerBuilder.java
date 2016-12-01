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

import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.api.NextRankedIssueUtil;
import org.jirban.jira.api.ProjectParallelTaskOptionsLoader;
import org.jirban.jira.impl.board.ProjectParallelTaskOptionsLoaderBuilder;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.user.UserService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.PriorityManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchContextFactory;
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
    private ProjectParallelTaskOptionsLoader projectParallelTaskOptionsLoader = new ProjectParallelTaskOptionsLoaderBuilder().build();

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

    public BoardManagerBuilder setProjectParallelTaskOptionsLoader(ProjectParallelTaskOptionsLoader projectParallelTaskOptionsLoader) {
        this.projectParallelTaskOptionsLoader = projectParallelTaskOptionsLoader;
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
        final OptionsManager optionsManager = null;
        final PriorityManager priorityManager = null;
        final SearchContextFactory searchContextFactory = null;
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
                optionsManager,
                permissionManager,
                projectManager,
                priorityManager,
                searchContextFactory,
                searchService,
                userService,
                versionManager);

        return new BoardManagerImpl(jiraInjectables, boardConfigurationManager, projectParallelTaskOptionsLoader);
    }
}

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

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.NextRankedIssueUtil;
import org.jirban.jira.impl.board.BoardProject;
import org.jirban.jira.impl.config.BoardProjectConfig;

import com.atlassian.greenhopper.model.lexorank.LexoRank;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.Consumer;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

/**
 * @author Kabir Khan
 */
@Named("nextRankedIssueUtilImpl")
public class NextRankedIssueUtilImpl implements NextRankedIssueUtil {
    private final JiraInjectables jiraInjectables;

    private final BoardConfigurationManager boardConfigurationManager;
    @Inject
    NextRankedIssueUtilImpl(JiraInjectables jiraInjectables, BoardConfigurationManager boardConfigurationManager) {
        this.jiraInjectables = jiraInjectables;
        this.boardConfigurationManager = boardConfigurationManager;
    }

    @Override
    public String findNextRankedIssue(BoardProjectConfig projectConfig, ApplicationUser boardOwner, String issueKey) throws SearchException {
        long rankCustomFieldId = boardConfigurationManager.getRankCustomFieldId();
        CustomFieldManager customFieldManager = jiraInjectables.getCustomFieldManager();
        CustomField customField = customFieldManager.getCustomFieldObject(rankCustomFieldId);
        Issue issue = jiraInjectables.getIssueService().getIssue(boardOwner, issueKey).getIssue();
        LexoRank lexoRank = (LexoRank) issue.getCustomFieldValue(customField);
        String rankValue = lexoRank.format();

        SearchService searchService = jiraInjectables.getSearchService();
        Query query = BoardProject.initialiseQuery(projectConfig, boardOwner, searchService,
                new Consumer<JqlQueryBuilder>() {
                    @Override
                    public void consume(@Nonnull JqlQueryBuilder jqlQueryBuilder) {
                        jqlQueryBuilder.where().and().customField(rankCustomFieldId).gt(rankValue);
                    }
                });

        SearchResults searchResults =
                searchService.search(boardOwner, query, PagerFilter.newPageAlignedFilter(0, 1));
        List<Issue> issueList = searchResults.getIssues();
        if (issueList.size() > 0) {
            return issueList.get(0).getKey();
        }
        return null;
    }
}

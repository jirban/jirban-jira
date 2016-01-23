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
package org.jirban.jira.impl.board;

import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.order.SortOrder;

/**
 * The data for a board project, i.e. a project whose issues should appear as cards on the board.
 *
 * @author Kabir Khan
 */
public class BoardProject {

    static Builder builder(SearchService searchService, User user, Board.Builder builder, BoardProjectConfig projectConfig) {
        return new Builder(searchService, user, builder, projectConfig);
    }

    public boolean isDataSame(BoardProject boardProject) {
        return false;
    }

    static class Builder {
        private final SearchService searchService;
        private final User user;
        private final Board.Builder boardBuilder;
        private final BoardProjectConfig projectConfig;

        public Builder(SearchService searchService, User user, Board.Builder boardBuilder, BoardProjectConfig projectConfig) {
            this.searchService = searchService;
            this.user = user;
            this.boardBuilder = boardBuilder;
            this.projectConfig = projectConfig;
        }

        public Builder addIssue(String state, Issue issue) {
            return this;
        }

        public BoardProject build(BoardConfig boardConfig, boolean owner) {
            return null;
        }

        public void load() throws SearchException {
            JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
            queryBuilder.where().project(projectConfig.getCode());
            if (projectConfig.getQueryFilter() != null) {
                queryBuilder.where().addCondition(projectConfig.getQueryFilter());
            }
            queryBuilder.orderBy().addSortForFieldName("Rank", SortOrder.ASC, true);

            SearchResults searchResults =
                    searchService.search(user, queryBuilder.buildQuery(), PagerFilter.getUnlimitedFilter());

            for (com.atlassian.jira.issue.Issue issue : searchResults.getIssues()) {
                Issue.Builder issueBuilder = Issue.builder(projectConfig);
                issueBuilder.load(issue);
            }
        }
    }
}

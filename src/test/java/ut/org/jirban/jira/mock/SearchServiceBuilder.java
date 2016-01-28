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
package ut.org.jirban.jira.mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.search.managers.SearchHandlerManager;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlClauseBuilderFactory;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

/**
 * @author Kabir Khan
 */
public class SearchServiceBuilder {
    private final SearchService searchService = mock(SearchService.class);
    private String project;


    public SearchService build(MockComponentWorker mockComponentWorker) throws SearchException {
        registerMockQueryManagers(mockComponentWorker);

        when(searchService.search(any(User.class), any(Query.class), any(PagerFilter.class)))
                .then(invocation -> getSearchResults(project));

        return searchService;
    }

    private SearchResults getSearchResults(String project) {
        SearchResults searchResults = mock(SearchResults.class);
        when(searchResults.getIssues()).thenReturn(getIssueList(project));
        return searchResults;
    }

    private List<Issue> getIssueList(String project) {
        return Collections.emptyList();
    }

    private void registerMockQueryManagers(MockComponentWorker mockComponentWorker) {
        mockComponentWorker.addMock(JqlClauseBuilderFactory.class, jqlQueryBuilder -> new ClauseBuilderFactory(jqlQueryBuilder).jqlClauseBuilder);
        mockComponentWorker.addMock(SearchHandlerManager.class, SearchHandlerManagerFactory.create());
    }

    private class ClauseBuilderFactory {
        final JqlQueryBuilder jqlQueryBuilder;
        final JqlClauseBuilder jqlClauseBuilder = mock(JqlClauseBuilder.class);

        ClauseBuilderFactory(JqlQueryBuilder jqlQueryBuilder) {
            this.jqlQueryBuilder = jqlQueryBuilder;
            when(jqlClauseBuilder.project(anyString())).then(invocation -> {
                project = (String) invocation.getArguments()[0];
                return jqlClauseBuilder;
            });

        }
    }

    private static class SearchHandlerManagerFactory {
        final SearchHandlerManager shm = mock(SearchHandlerManager.class);

        SearchHandlerManagerFactory() {
            when(shm.getJqlClauseNames(anyString())).thenReturn(Collections.emptySet());
        }
        static SearchHandlerManager create() {
            return new SearchHandlerManagerFactory().shm;
        }
    }

}

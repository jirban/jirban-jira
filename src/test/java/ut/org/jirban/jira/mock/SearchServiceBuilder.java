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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.search.managers.SearchHandlerManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlClauseBuilderFactory;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

/**
 * @author Kabir Khan
 */
public class SearchServiceBuilder {
    private final SearchService searchService = mock(SearchService.class);
    private final MockComponentWorker mockComponentWorker;

    private IssueRegistry issueRegistry;
    private String searchIssueKey;
    private String searchProject;
    private String searchStatus;
    private Collection<String> doneStatesFilter;
    private SearchCallback searchCallback;

    public SearchServiceBuilder(MockComponentWorker mockComponentWorker) {
        this.mockComponentWorker = mockComponentWorker;
    }

    public SearchServiceBuilder setIssueRegistry(IssueRegistry issueRegistry) {
        this.issueRegistry = issueRegistry;
        return this;
    }

    public SearchService build() throws SearchException {
        if (issueRegistry == null) {
            issueRegistry = new IssueRegistry(new UserManagerBuilder().addDefaultUsers().build(mockComponentWorker));
        }
        registerMockQueryManagers(mockComponentWorker);
        when(searchService.search(any(ApplicationUser.class), any(Query.class), any(PagerFilter.class)))
                .then(invocation -> getSearchResults());

        return searchService;
    }

    private SearchResults getSearchResults() {
        SearchResults searchResults = mock(SearchResults.class);
        when(searchResults.getIssues()).thenAnswer(invocationOnMock -> {
            if (searchCallback != null) {
                searchCallback.searching();
            }
            try {
                List<Issue> issues = issueRegistry.getIssueList(searchIssueKey, searchProject, searchStatus, doneStatesFilter);
                return issues;
            } finally {
                searchIssueKey = null;
                searchProject = null;
                searchStatus = null;
                doneStatesFilter = null;
            }
        });

        when(searchResults.toString()).thenReturn("<SNIP>");
        return searchResults;
    }

    private void registerMockQueryManagers(MockComponentWorker mockComponentWorker) {
        mockComponentWorker.addMock(JqlClauseBuilderFactory.class, jqlQueryBuilder -> new ClauseBuilderFactory(jqlQueryBuilder).jqlClauseBuilder);
        mockComponentWorker.addMock(SearchHandlerManager.class, SearchHandlerManagerFactory.create());
    }

    public SearchServiceBuilder setSearchCallback(SearchCallback searchCallback) {
        this.searchCallback = searchCallback;
        return this;
    }

    private class ClauseBuilderFactory {
        final JqlQueryBuilder jqlQueryBuilder;
        final JqlClauseBuilder jqlClauseBuilder = mock(JqlClauseBuilder.class);

        ClauseBuilderFactory(JqlQueryBuilder jqlQueryBuilder) {
            this.jqlQueryBuilder = jqlQueryBuilder;
            when(jqlClauseBuilder.project(anyString())).then(invocation -> {
                searchProject = (String) invocation.getArguments()[0];
                return jqlClauseBuilder;
            });
            when(jqlClauseBuilder.issue(any(String[].class))).then(invocation -> {
                searchIssueKey = (String)invocation.getArguments()[0];
                return jqlClauseBuilder;
            });
            when(jqlClauseBuilder.and()).then(invocation -> jqlClauseBuilder);
            when(jqlClauseBuilder.not()).then(invocation -> jqlClauseBuilder);
            when(jqlClauseBuilder.status(anyString())).then(invocation -> {
                searchStatus = (String) invocation.getArguments()[0];
                return jqlClauseBuilder;
            });
            when(jqlClauseBuilder.addStringCondition(anyString(), any(Set.class))).then(invocation -> {
                String clauseName = (String) invocation.getArguments()[0];
                if (clauseName.equals("status")) {
                    doneStatesFilter = (Collection<String>)invocation.getArguments()[1];
                }
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

    private static class IssueDetail {
        final Issue issue;

        final IssueType issueType;
        final Priority priority;
        final Status state;
        final ApplicationUser assignee = mock(ApplicationUser.class);

        public IssueDetail(String key, String issueType, String priority, String summary,
                           String state, String assignee, String[] components) {
            //Do the nested mocks first
            this.issueType = MockIssueType.create(issueType);
            this.priority = MockPriority.create(priority);
            this.state = MockStatus.create(state);
            when(this.assignee.getName()).thenReturn(assignee);

            Set<ProjectComponent> componentSet = null;
            if (components != null && components.length > 0) {
                componentSet = new HashSet<>();
                for (String componentName : components) {
                    ProjectComponent component = mock(ProjectComponent.class);
                    when(component.getName()).thenReturn(componentName);
                    componentSet.add(component);
                }
            }

            this.issue = new MockIssue(key, this.issueType, this.priority, summary, this.assignee, componentSet, this.state);
        }
    }

    public interface SearchCallback {
        void searching();
    }
}

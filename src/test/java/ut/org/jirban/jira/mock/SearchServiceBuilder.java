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
package ut.org.jirban.jira.mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.search.managers.SearchHandlerManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlClauseBuilderFactory;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.project.version.Version;
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
                           String state, String assignee, String[] components, String[] labels, String[] fixVersions) {
            //Do the nested mocks first
            this.issueType = MockIssueType.create(issueType);
            this.priority = MockPriority.create(priority);
            this.state = MockStatus.create(state);
            when(this.assignee.getName()).thenReturn(assignee);

            Set<ProjectComponent> componentSet = null;
            if (components != null && components.length > 0) {
                componentSet = MockProjectComponent.createProjectComponents(components);
            }
            Set<Label> labelSet = null;
            if (labels != null && labels.length > 0) {
                labelSet = MockLabel.createLabels(labels);
            }
            Set<Version> fixVersionSet = null;
            if (fixVersions != null && fixVersions.length > 0) {
                fixVersionSet = MockVersion.createVersions(fixVersions);
            }

            this.issue = new MockIssue(key, this.issueType, this.priority, summary, this.assignee, componentSet, labelSet, fixVersionSet, this.state);
        }
    }

    public interface SearchCallback {
        void searching();
    }
}

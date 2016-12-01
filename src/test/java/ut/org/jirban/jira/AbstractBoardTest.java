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
package ut.org.jirban.jira;

import static org.jirban.jira.impl.Constants.CUSTOM;
import static org.jirban.jira.impl.Constants.RANK_CUSTOM_FIELD_ID;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.api.NextRankedIssueUtil;
import org.jirban.jira.impl.BoardConfigurationManagerBuilder;
import org.jirban.jira.impl.BoardManagerBuilder;
import org.jirban.jira.impl.JirbanIssueEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.junit.rules.MockitoContainer;
import com.atlassian.jira.junit.rules.MockitoMocksInContainer;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;

import ut.org.jirban.jira.mock.CustomFieldManagerBuilder;
import ut.org.jirban.jira.mock.IssueLinkManagerBuilder;
import ut.org.jirban.jira.mock.IssueRegistry;
import ut.org.jirban.jira.mock.MockProjectComponent;
import ut.org.jirban.jira.mock.SearchServiceBuilder;
import ut.org.jirban.jira.mock.UserManagerBuilder;

/**
 * @author Kabir Khan
 */
public abstract class AbstractBoardTest {

    @Rule
    public MockitoContainer mockitoContainer = MockitoMocksInContainer.rule(this);

    protected BoardManager boardManager;
    protected UserManager userManager;
    protected IssueRegistry issueRegistry;
    protected NextRankedIssueUtil nextRankedIssueUtil;
    protected SearchCallback searchCallback = new SearchCallback();

    @Before
    public void initializeMocks() throws Exception {
        initializeMocks("config/board-tdp.json");
    }

    protected void initializeMocks(String cfgResource) throws Exception {
        initializeMocks(cfgResource, null);
    }

    protected void initializeMocks(String cfgResource, AdditionalBuilderInit init) throws Exception {
        BoardConfigurationManager cfgManager = new BoardConfigurationManagerBuilder()
                .addConfigActiveObjectsFromFile(cfgResource)
                .addSettingActiveObject(RANK_CUSTOM_FIELD_ID, "10000")
                .setCustomFieldManager(CustomFieldManagerBuilder.loadFromResource(cfgResource))
                .build();

        MockComponentWorker worker = new MockComponentWorker();
        userManager = new UserManagerBuilder()
                .addDefaultUsers()
                .build(worker);

        IssueRegistry issueRegistry = new IssueRegistry(userManager);
        this.issueRegistry = issueRegistry;
        this.nextRankedIssueUtil = issueRegistry;

        SearchService searchService = new SearchServiceBuilder(worker)
                .setIssueRegistry(issueRegistry)
                .setSearchCallback(searchCallback)
                .build();
        IssueLinkManager issueLinkManager = new IssueLinkManagerBuilder().build();
        worker.init();

        BoardManagerBuilder boardManagerBuilder = new BoardManagerBuilder()
                .setBoardConfigurationManager(cfgManager)
                .setUserManager(userManager)
                .setSearchService(searchService)
                .setIssueLinkManager(issueLinkManager)
                .setNextRankedIssueUtil(nextRankedIssueUtil);

        if (init != null) {
            init.initialise(boardManagerBuilder);
        }
        boardManager = boardManagerBuilder.build();
    }

    protected JirbanIssueEvent createCreateEventAndAddToRegistry(String issueKey,
                                                                 IssueType issueType, Priority priority, String summary,
                                                                 String username, String[] components, String state) {
        return createCreateEventAndAddToRegistry(issueKey, issueType.name, priority.name, summary, username, components, state, null);
    }

    protected JirbanIssueEvent createCreateEventAndAddToRegistry(String issueKey,
                                                                 IssueType issueType, Priority priority, String summary,
                                                                 String username, String[] components, String state,
                                                                 Map<Long, String> customFieldValues) {
        return createCreateEventAndAddToRegistry(
                issueKey, issueType.name, priority.name, summary, username, components, state, customFieldValues);
    }

    protected JirbanIssueEvent createCreateEventAndAddToRegistry(String issueKey,
                                                                 String issueType, String priority, String summary,
                                                                 String username, String[] components, String state) {
        return createCreateEventAndAddToRegistry(issueKey, issueType, priority, summary, username, components, state, null);
    }

    protected JirbanIssueEvent createCreateEventAndAddToRegistry(String issueKey,
                                                                 String issueType, String priority, String summary,
                                                                 String username, String[] components, String state,
                                                                 Map<Long, String> customFieldValues) {

        ApplicationUser user = userManager.getUserByKey(username);
        String projectCode = issueKey.substring(0, issueKey.indexOf("-"));
        JirbanIssueEvent create = JirbanIssueEvent.createCreateEvent(issueKey, projectCode, issueType, priority,
                summary, user, MockProjectComponent.createProjectComponents(components), state, customFieldValues);

        issueRegistry.addIssue(projectCode, issueType, priority, summary, username, components, state);
        if (customFieldValues != null) {
            for (Map.Entry<Long, String> entry : customFieldValues.entrySet()) {
                issueRegistry.setCustomField(issueKey, entry.getKey(), entry.getValue());
            }
        }

        return create;
    }

    protected JirbanIssueEvent createUpdateEventAndAddToRegistry(String issueKey, IssueType issueType,
                                                                 Priority priority, String summary, String username,
                                                                 boolean unassigned, String[] components,
                                                                 boolean clearComponents, String state, boolean rank) {
        String issueTypeName = issueType == null ? null : issueType.name;
        String priorityName = priority == null ? null : priority.name;
        return createUpdateEventAndAddToRegistry(issueKey, issueTypeName, priorityName, summary, username, unassigned,
                components, clearComponents, state, rank, Collections.emptyMap());
    }

    protected JirbanIssueEvent createUpdateEventAndAddToRegistry(String issueKey, String issueTypeName,
                                                                 String priorityName, String summary, String username,
                                                                 boolean unassigned, String[] components,
                                                                 boolean clearComponents, String state, boolean rank) {
        return createUpdateEventAndAddToRegistry(issueKey, issueTypeName, priorityName, summary, username, unassigned,
                components, clearComponents, state, rank, null);
    }

    protected JirbanIssueEvent createUpdateEventAndAddToRegistry(String issueKey, IssueType issueType,
                                                                 Priority priority, String summary, String username,
                                                                 boolean unassigned, String[] components,
                                                                 boolean clearComponents, String state, boolean rank,
                                                                 Map<Long, String> customFieldValues) {
        String issueTypeName = issueType == null ? null : issueType.name;
        String priorityName = priority == null ? null : priority.name;
        return createUpdateEventAndAddToRegistry(issueKey, issueTypeName, priorityName, summary, username, unassigned,
                components, clearComponents, state, rank, customFieldValues);
    }

    protected JirbanIssueEvent createUpdateEventAndAddToRegistry(String issueKey, String issueTypeName,
                                                                 String priorityName, String summary, String username,
                                                                 boolean unassigned, String[] components,
                                                                 boolean clearComponents, String state, boolean rank,
                                                                 Map<Long, String> customFieldValues) {
        Assert.assertFalse(username != null && unassigned);
        if (clearComponents) {
            Assert.assertNull(components);
        }

        ApplicationUser user;
        if (unassigned) {
            user = JirbanIssueEvent.UNASSIGNED;
        } else {
            user = userManager.getUserByKey(username);
        }
        String projectCode = issueKey.substring(0, issueKey.indexOf("-"));
        Collection<ProjectComponent> projectComponents = clearComponents ? Collections.emptySet() : MockProjectComponent.createProjectComponents(components);
        Issue issue = issueRegistry.getIssue(issueKey);
        JirbanIssueEvent update = JirbanIssueEvent.createUpdateEvent(issueKey, projectCode, issueTypeName,
                priorityName, summary, user, projectComponents, issue.getStatusObject().getName(),
                state, rank, customFieldValues);

        issueRegistry.updateIssue(issueKey, issueTypeName, priorityName, summary, username, components, state);
        if (customFieldValues != null) {
            for (Map.Entry<Long, String> entry : customFieldValues.entrySet()) {
                issueRegistry.setCustomField(issueKey, entry.getKey(), entry.getValue());
            }
        }
        return update;
    }

    protected enum IssueType {
        TASK(0),
        BUG(1),
        FEATURE(2);

        final int index;
        final String name;

        IssueType(int index) {
            this.index = index;
            this.name = super.name().toLowerCase();
        }
    }

    protected enum Priority {
        HIGHEST(0),
        HIGH(1),
        LOW(2),
        LOWEST(3);

        final int index;
        final String name;

        Priority(int index) {
            this.index = index;
            this.name = super.name().toLowerCase();
        }
    }

    protected static class SearchCallback implements SearchServiceBuilder.SearchCallback {
        public boolean searched = false;
        @Override
        public void searching() {
            searched = true;
        }
    }

    interface AdditionalBuilderInit {
        void initialise(BoardManagerBuilder boardManagerBuilder);
    }

    interface IssueChecker {
        void check(ModelNode issue);
    }

    static class NoIssueCustomFieldChecker implements IssueChecker {
        static final NoIssueCustomFieldChecker TESTER = new NoIssueCustomFieldChecker("Tester");
        static final NoIssueCustomFieldChecker DOCUMENTER = new NoIssueCustomFieldChecker("Documenter");

        private final String fieldName;

        private NoIssueCustomFieldChecker(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public void check(ModelNode issue) {
            Assert.assertFalse(issue.hasDefined(CUSTOM, fieldName));
        }
    }
}

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

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.junit.rules.MockitoContainer;
import com.atlassian.jira.junit.rules.MockitoMocksInContainer;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.util.UserManager;

import ut.org.jirban.jira.mock.CrowdUserBridge;
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

        CrowdUserBridge userBridge = new CrowdUserBridge(userManager);
        User user = userBridge.getUserByKey(username);
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

        User user;
        if (unassigned) {
            user = JirbanIssueEvent.UNASSIGNED;
        } else {
            CrowdUserBridge userBridge = new CrowdUserBridge(userManager);
            user = userBridge.getUserByKey(username);
        }
        String projectCode = issueKey.substring(0, issueKey.indexOf("-"));
        Collection<ProjectComponent> projectComponents = clearComponents ? Collections.emptySet() : MockProjectComponent.createProjectComponents(components);
        Issue issue = issueRegistry.getIssue(projectCode, issueKey);
        JirbanIssueEvent update = JirbanIssueEvent.createUpdateEvent(issueKey, projectCode, issueTypeName,
                priorityName, summary, user, projectComponents, issue.getStatusObject().getName(),
                state, state != null || rank, customFieldValues);

        issueRegistry.updateIssue(issueKey, projectCode, issueTypeName, priorityName, summary, username, components, state);
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

    interface IssueCustomFieldChecker {
        void check(ModelNode issue);
    }

    static class NoIssueCustomFieldChecker implements IssueCustomFieldChecker {
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

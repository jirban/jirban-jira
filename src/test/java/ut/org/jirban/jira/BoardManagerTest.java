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

import java.util.HashMap;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.impl.BoardConfigurationManagerBuilder;
import org.jirban.jira.impl.BoardManagerBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.junit.rules.MockitoContainer;
import com.atlassian.jira.junit.rules.MockitoMocksInContainer;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.util.UserManager;

import ut.org.jirban.jira.mock.IssueLinkManagerBuilder;
import ut.org.jirban.jira.mock.IssueRegistry;
import ut.org.jirban.jira.mock.SearchServiceBuilder;
import ut.org.jirban.jira.mock.UserManagerBuilder;

/**
 * @author Kabir Khan
 */
public class BoardManagerTest {

    @Rule
    public MockitoContainer mockitoContainer = MockitoMocksInContainer.rule(this);

    private BoardManager boardManager;
    private UserManager userManager;
    private IssueRegistry issueRegistry;

    @Before
    public void initializeMocks() throws Exception {

        BoardConfigurationManager cfgManager = new BoardConfigurationManagerBuilder()
                .addConfigActiveObjects("config/board-tdp.json")
                .build();

        MockComponentWorker worker = new MockComponentWorker();
        userManager = new UserManagerBuilder()
                .addDefaultUsers()
                .build(worker);

        issueRegistry = new IssueRegistry(userManager);
        SearchService searchService = new SearchServiceBuilder()
                .setIssueRegistry(issueRegistry)
                .build(worker);
        IssueLinkManager issueLinkManager = new IssueLinkManagerBuilder().build();
        worker.init();

        boardManager = new BoardManagerBuilder()
                .setBoardConfigurationManager(cfgManager)
                .setUserManager(userManager)
                .setSearchService(searchService)
                .setIssueLinkManager(issueLinkManager)
                .build();
    }

    @Test
    public void testLoadBoardOnlyOwnerProjectIssues() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "TDP-A", "kabir");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "TDP-B", "kabir");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "TDP-C", "kabir");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "TDP-D", "brian");
        issueRegistry.addIssue("TDP", "task", "highest", "One", "TDP-A", "kabir");
        issueRegistry.addIssue("TDP", "bug", "high", "Two", "TDP-B", "kabir");
        issueRegistry.addIssue("TDP", "feature", "low", "Three", "TDP-C", "kabir");

        String json = boardManager.getBoardJson(userManager.getUserByKey("kabir"), 0);
        Assert.assertNotNull(json);
        ModelNode boardNode = ModelNode.fromJSONString(json);
        checkUsers(boardNode, "kabir", "brian");
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");
    }

    private void checkUsers(ModelNode board, String...users) {
        List<ModelNode> assignees = board.get("assignees").asList();
        Assert.assertEquals(assignees.size(), users.length);
        HashMap<String, ModelNode> map = new HashMap<>();
        assignees.forEach(node -> map.put(node.get("key").asString(), node));

        for (String user : users) {
            ModelNode assignee = map.get(user);
            Assert.assertNotNull(assignee);
            Assert.assertEquals(user + "@example.com", assignee.get("email").asString());
            Assert.assertEquals("/avatars/" + user + ".png", assignee.get("avatar").asString());

            String displayName = assignee.get("name").toString().toLowerCase();
            Assert.assertTrue(displayName.length() > user.length());
            Assert.assertTrue(displayName.contains(user));
        }
    }

    private void checkNameAndIcon(ModelNode board, String type, String...names) {
        List<ModelNode> entries = board.get(type).asList();
        Assert.assertEquals(entries.size(), names.length);
        for (int i = 0 ; i < names.length ; i++) {
            ModelNode entry = entries.get(i);
            Assert.assertEquals(names[i], entry.get("name").asString());
            Assert.assertEquals("/icons/" + type + "/" + names[i] + ".png", entry.get("icon").asString());
        }
    }

}

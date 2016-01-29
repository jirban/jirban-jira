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
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "TDP-A", "kabir");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "TDP-B", "kabir");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", "TDP-C", null);

        String json = boardManager.getBoardJson(userManager.getUserByKey("kabir"), 0);
        Assert.assertNotNull(json);
        ModelNode boardNode = ModelNode.fromJSONString(json);
        Assert.assertEquals(0, boardNode.get("viewId").asInt());
        checkUsers(boardNode, "kabir", "brian");
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", 0, 0, "One", 0, 1);
        checkIssue(allIssues, "TDP-2", 0, 1, "Two", 1, 1);
        checkIssue(allIssues, "TDP-3", 0, 2, "Three", 2, 1);
        checkIssue(allIssues, "TDP-4", 0, 3, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", 0, 0, "Five", 0, 1);
        checkIssue(allIssues, "TDP-6", 1, 1, "Six", 1, 1);
        checkIssue(allIssues, "TDP-7", 2, 2, "Seven", 2, -1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1", "TDP-5"},
                {"TDP-2", "TDP-6"},
                {"TDP-3", "TDP-7"},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{{},{},{},{}});
    }

    @Test
    public void testLoadBoardOnlyNonOwnerProjectIssues() throws Exception {
        issueRegistry.addIssue("TBG", "task", "highest", "One", "TBG-X", "kabir");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "TBG-Y", "kabir");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", "TBG-X", null);
        issueRegistry.addIssue("TBG", "task", "lowest", "Four", "TBG-Y", "jason");

        String json = boardManager.getBoardJson(userManager.getUserByKey("kabir"), 0);
        Assert.assertNotNull(json);
        ModelNode boardNode = ModelNode.fromJSONString(json);
        Assert.assertEquals(0, boardNode.get("viewId").asInt());
        checkUsers(boardNode, "jason", "kabir");
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TBG-1", 0, 0, "One", 0, 1);
        checkIssue(allIssues, "TBG-2", 1, 1, "Two", 1, 1);
        checkIssue(allIssues, "TBG-3", 2, 2, "Three", 0, -11);
        checkIssue(allIssues, "TBG-4", 0, 3, "Four", 1, 0);

        checkProjectIssues(boardNode, "TDP", new String[][]{{}, {}, {}, {}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3"},
                {"TBG-2", "TBG-4"},
                {}});
    }

    @Test
    public void testLoadBoard() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "TDP-A", "kabir");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "TDP-B", "kabir");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "TDP-C", "kabir");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "TDP-D", "brian");
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "TDP-A", "kabir");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "TDP-B", "kabir");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", "TDP-C", null);
        issueRegistry.addIssue("TBG", "task", "highest", "One", "TBG-X", "kabir");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "TBG-Y", "kabir");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", "TBG-X", null);
        issueRegistry.addIssue("TBG", "task", "lowest", "Four", "TBG-Y", "jason");

        String json = boardManager.getBoardJson(userManager.getUserByKey("kabir"), 0);
        Assert.assertNotNull(json);
        ModelNode boardNode = ModelNode.fromJSONString(json);
        Assert.assertEquals(0, boardNode.get("viewId").asInt());
        checkUsers(boardNode, "brian", "jason", "kabir");
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 11);
        checkIssue(allIssues, "TDP-1", 0, 0, "One", 0, 2);
        checkIssue(allIssues, "TDP-2", 0, 1, "Two", 1, 2);
        checkIssue(allIssues, "TDP-3", 0, 2, "Three", 2, 2);
        checkIssue(allIssues, "TDP-4", 0, 3, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", 0, 0, "Five", 0, 2);
        checkIssue(allIssues, "TDP-6", 1, 1, "Six", 1, 2);
        checkIssue(allIssues, "TDP-7", 2, 2, "Seven", 2, -1);
        checkIssue(allIssues, "TBG-1", 0, 0, "One", 0, 2);
        checkIssue(allIssues, "TBG-2", 1, 1, "Two", 1, 2);
        checkIssue(allIssues, "TBG-3", 2, 2, "Three", 0, -1);
        checkIssue(allIssues, "TBG-4", 0, 3, "Four", 1, 1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1", "TDP-5"},
                {"TDP-2", "TDP-6"},
                {"TDP-3", "TDP-7"},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3"},
                {"TBG-2", "TBG-4"},
                {}});
    }

    private void checkProjectIssues(ModelNode boardNode, String project, String[][] issueTable) {
        List<ModelNode> issues = boardNode.get("projects", "main", project, "issues").asList();
        Assert.assertEquals(issueTable.length, issues.size());
        for (int i = 0 ; i < issueTable.length ; i++) {
            List<ModelNode> issuesForState = issues.get(i).asList();
            Assert.assertEquals(issueTable[i].length, issuesForState.size());
            for (int j = 0 ; j < issueTable[i].length ; j++) {
                Assert.assertEquals(issueTable[i][j], issuesForState.get(j).asString());
            }
        }

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

    private ModelNode getIssuesCheckingSize(ModelNode board, int expectedLength) {
        ModelNode issues = board.get("issues");
        Assert.assertEquals(expectedLength, issues.keys().size());
        return issues;
    }

    private void checkIssue(ModelNode issues, String key, int type, int priority, String summary, int state, int assignee) {
        ModelNode issue = issues.get(key);
        Assert.assertNotNull(issue);
        Assert.assertEquals(key, issue.get("key").asString());
        Assert.assertEquals(type, issue.get("type").asInt());
        Assert.assertEquals(priority, issue.get("priority").asInt());
        Assert.assertEquals(summary, issue.get("summary").asString());
        Assert.assertEquals(state, issue.get("state").asInt());
        if (assignee < 0) {
            Assert.assertFalse(issue.get("assignee").isDefined());
        } else {
            Assert.assertEquals(assignee, issue.get("assignee").asInt());
        }
    }
}

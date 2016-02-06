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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.JirbanIssueEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.issue.search.SearchException;

/**
 * @author Kabir Khan
 */
public class BoardChangeRegistryTest extends AbstractBoardTest {

    @Before
    public void setupInitialBoard() throws SearchException {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", "TDP-A");  //1
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", "TDP-B");     //2
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", "TDP-C");    //3
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", "TDP-D");  //4
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", "TDP-A"); //5
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", "TDP-B");      //6
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, "TDP-C");    //7

        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", "TBG-X");  //1
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", "TBG-Y");      //2
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, "TBG-X");    //3

        checkViewId(0);
        checkNoChanges(getChangesJson(0), 0);
    }

    @Test
    public void testFullRefreshOnTooHighView() throws Exception {
        ModelNode changes = getChangesJson(1);
        Assert.assertFalse(changes.hasDefined("changes"));
        //Check that the top-level fields of the board are there
        Assert.assertTrue(changes.hasDefined("view"));
        Assert.assertTrue(changes.hasDefined("assignees"));
        Assert.assertTrue(changes.hasDefined("priorities"));
        Assert.assertTrue(changes.hasDefined("issue-types"));
        Assert.assertTrue(changes.hasDefined("projects"));
        Assert.assertTrue(changes.hasDefined("issues"));

        //TODO should check the same when passing in an old view id that has been reaped after being too old
    }

    @Test
    public void testDeleteIssues() throws Exception {
        JirbanIssueEvent delete = JirbanIssueEvent.createDeleteEvent("TDP-3", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(1);
        checkAssignees(getChangesJson(0), 1);
        checkAdds(getChangesJson(0), 1);
        checkUpdates(getChangesJson(0), 1);
        checkDeletes(getChangesJson(0), 1, "TDP-3");

        delete = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(2);
        checkAssignees(getChangesJson(0), 2);
        checkAdds(getChangesJson(0), 2);
        checkUpdates(getChangesJson(0), 2);
        checkDeletes(getChangesJson(0), 2, "TDP-3", "TDP-7");
        checkDeletes(getChangesJson(1), 2, "TDP-7");

        delete = JirbanIssueEvent.createDeleteEvent("TBG-1", "TBG");
        boardManager.handleEvent(delete);
        checkViewId(3);
        checkAssignees(getChangesJson(0), 3);
        checkAdds(getChangesJson(0), 3);
        checkUpdates(getChangesJson(0), 3);
        checkDeletes(getChangesJson(0), 3, "TDP-3", "TDP-7", "TBG-1");
        checkDeletes(getChangesJson(1), 3, "TDP-7", "TBG-1");
        checkDeletes(getChangesJson(2), 3, "TBG-1");
        checkNoChanges(getChangesJson(3), 3);
    }

    @Test
    public void testCreateIssues() throws Exception {
        //Add an issue which does not bring in new assignees
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(1);
        checkAssignees(getChangesJson(0), 1);
        checkDeletes(getChangesJson(0), 1);
        checkUpdates(getChangesJson(0), 1);
        checkAdds(getChangesJson(0), 1, new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D"));

        //Now add an issue which brings in new assignees
        create = createCreateEventAndAddToRegistry("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X");
        boardManager.handleEvent(create);
        checkViewId(2);
        checkAssignees(getChangesJson(0), 2, "jason");
        checkDeletes(getChangesJson(0), 2);
        checkUpdates(getChangesJson(0), 2);
        checkAdds(getChangesJson(0), 2,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"));
        checkAssignees(getChangesJson(1), 2, "jason");
        checkDeletes(getChangesJson(1), 2);
        checkAdds(getChangesJson(1), 2,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"));

        //Add another one not bringing in new assignees
        create = createCreateEventAndAddToRegistry("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(3);
        checkAssignees(getChangesJson(0), 3, "jason");
        checkDeletes(getChangesJson(0), 3);
        checkUpdates(getChangesJson(0), 3);
        checkAdds(getChangesJson(0), 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkAssignees(getChangesJson(1), 3, "jason");
        checkDeletes(getChangesJson(1), 3);
        checkUpdates(getChangesJson(1), 3);
        checkAdds(getChangesJson(1), 3,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkAssignees(getChangesJson(2), 3);
        checkDeletes(getChangesJson(2), 3);
        checkUpdates(getChangesJson(2), 3);
        checkAdds(getChangesJson(2), 3,
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
    }

    @Test
    public void testUpdateSameIssueNonAssignees() throws Exception {
        //Do a noop update
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", null, null, null, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(0);
        checkNoChanges(getChangesJson(0), 0);

        update = createUpdateEventAndAddToRegistry("TDP-7", null, null, "Seven-1", null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        //Check assignees and deletes extra well here so we don't have to in the other tests
        checkAssignees(getChangesJson(0), 1);
        checkDeletes(getChangesJson(0), 1);
        checkUpdates(getChangesJson(0), 1, new IssueData("TDP-7", null, null, "Seven-1", null, null));

        update = createUpdateEventAndAddToRegistry("TDP-7", IssueType.BUG, null, null, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkAssignees(getChangesJson(0), 2);
        checkDeletes(getChangesJson(0), 2);
        checkUpdates(getChangesJson(0), 2, new IssueData("TDP-7", IssueType.BUG, null, "Seven-1", null, null));
        checkAssignees(getChangesJson(1), 2);
        checkDeletes(getChangesJson(1), 2);
        checkUpdates(getChangesJson(1), 2, new IssueData("TDP-7", IssueType.BUG, null, null, null, null));

        update = createUpdateEventAndAddToRegistry("TDP-7", null, Priority.HIGHEST, null, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(3);
        checkAssignees(getChangesJson(0), 3);
        checkDeletes(getChangesJson(0), 3);
        checkUpdates(getChangesJson(0), 3, new IssueData("TDP-7", IssueType.BUG, Priority.HIGHEST, "Seven-1", null, null));
        checkAssignees(getChangesJson(1), 3);
        checkDeletes(getChangesJson(1), 3);
        checkUpdates(getChangesJson(1), 3, new IssueData("TDP-7", IssueType.BUG, Priority.HIGHEST, null, null, null));
        checkAssignees(getChangesJson(2), 3);
        checkDeletes(getChangesJson(2), 3);
        checkUpdates(getChangesJson(2), 3, new IssueData("TDP-7", null, Priority.HIGHEST, null, null, null));

        //TODO States
    }


    @Test
    public void testSameIssueAssignees() throws Exception {
        //Do a noop update
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", null, null, null, "kabir", false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        checkAssignees(getChangesJson(0), 1);
        checkUpdates(getChangesJson(0), 1, new IssueData("TDP-7", null, null, null, "kabir", null));

        update = createUpdateEventAndAddToRegistry("TDP-7", null, null, null, null, true, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkAssignees(getChangesJson(0), 2);
        checkUpdates(getChangesJson(0), 2, new IssueData("TDP-7", null, null, null, null, true, null));
        checkAssignees(getChangesJson(1), 2);
        checkUpdates(getChangesJson(1), 2, new IssueData("TDP-7", null, null, null, null, true, null));

        update = createUpdateEventAndAddToRegistry("TDP-7", null, null, null, "jason", false, null, false);
        boardManager.handleEvent(update);
        checkViewId(3);
        checkAssignees(getChangesJson(0), 3, "jason");
        checkUpdates(getChangesJson(0), 3, new IssueData("TDP-7", null, null, null, "jason", false, null));
        checkAssignees(getChangesJson(1), 3, "jason");
        checkUpdates(getChangesJson(1), 3, new IssueData("TDP-7", null, null, null, "jason", false, null));
        checkAssignees(getChangesJson(2), 3, "jason");
        checkUpdates(getChangesJson(2), 3, new IssueData("TDP-7", null, null, null, "jason", false, null));

        update = createUpdateEventAndAddToRegistry("TDP-7", null, null, null, "brian", false, null, false);
        boardManager.handleEvent(update);
        checkViewId(4);
        checkAssignees(getChangesJson(0), 4);
        checkUpdates(getChangesJson(0), 4, new IssueData("TDP-7", null, null, null, "brian", false, null));
        checkAssignees(getChangesJson(1), 4);
        checkUpdates(getChangesJson(1), 4, new IssueData("TDP-7", null, null, null, "brian", false, null));
        checkAssignees(getChangesJson(2), 4);
        checkUpdates(getChangesJson(2), 4, new IssueData("TDP-7", null, null, null, "brian", false, null));
        checkAssignees(getChangesJson(3), 4);
        checkUpdates(getChangesJson(3), 4, new IssueData("TDP-7", null, null, null, "brian", false, null));
    }


    @Test
    public void testUpdateSeveralIssues() throws Exception {
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", null, null, "Seven-1", null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        checkUpdates(getChangesJson(0), 1, new IssueData("TDP-7", null, null, "Seven-1", null, null));

        update = createUpdateEventAndAddToRegistry("TBG-3", IssueType.BUG, null, null, "kabir", false, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkUpdates(getChangesJson(0), 2,
                new IssueData("TDP-7", null, null, "Seven-1", null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));

        //Create, update and delete one to make sure that does not affect the others
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(3);
        checkUpdates(getChangesJson(0), 3,
                new IssueData("TDP-7", null, null, "Seven-1", null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(getChangesJson(0), 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkUpdates(getChangesJson(1), 3,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(getChangesJson(1), 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkUpdates(getChangesJson(2), 3);
        checkAdds(getChangesJson(2), 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));

        //This should appear as an add for change sets including its previous create, and an update for change
        //sets not including the create
        update = createUpdateEventAndAddToRegistry("TDP-8", null, null, null, "jason", false, "TDP-C", false);
        boardManager.handleEvent(update);
        checkViewId(4);
        checkAssignees(getChangesJson(0), 4, "jason");
        checkUpdates(getChangesJson(0), 4,
                new IssueData("TDP-7", null, null, "Seven-1", null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(getChangesJson(0), 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", "TDP-C"));

        checkAssignees(getChangesJson(1), 4, "jason");
        checkUpdates(getChangesJson(1), 4,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(getChangesJson(1), 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", "TDP-C"));

        checkAssignees(getChangesJson(2), 4, "jason");
        checkUpdates(getChangesJson(2), 4);
        checkAdds(getChangesJson(2), 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", "TDP-C"));

        checkAssignees(getChangesJson(3), 4, "jason");
        checkUpdates(getChangesJson(3), 4,
                new IssueData("TDP-8", null, null, null, "jason", "TDP-C"));
        checkAdds(getChangesJson(3), 4);

        //This will not appear in change sets including the create, it becomes a noop
        JirbanIssueEvent delete = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(5);
        checkAssignees(getChangesJson(0), 5);
        checkUpdates(getChangesJson(0), 5,
                new IssueData("TDP-7", null, null, "Seven-1", null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(getChangesJson(0), 5);

        checkAssignees(getChangesJson(1), 5);
        checkUpdates(getChangesJson(1), 5,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(getChangesJson(1), 5);

        checkAssignees(getChangesJson(2), 5);
        checkUpdates(getChangesJson(2), 5);
        checkAdds(getChangesJson(2), 5);

        checkAssignees(getChangesJson(3), 5);
        checkUpdates(getChangesJson(3), 5);
        checkAdds(getChangesJson(3), 5);

        checkAssignees(getChangesJson(4), 5);
        checkUpdates(getChangesJson(4), 5);
        checkAdds(getChangesJson(4), 5);
    }

    @Test
    public void testBlacklist() throws SearchException {
        JirbanIssueEvent event = createUpdateEventAndAddToRegistry("TDP-6", null, null, null, null, false, "BAD-STATE", false);
        boardManager.handleEvent(event);
        checkViewId(1);
        checkUpdates(getChangesJson(0), 1);
    }

    private void checkNoChanges(ModelNode changesNode, int expectedView) {
        Assert.assertEquals(expectedView, changesNode.get("changes", "view").asInt());
        Assert.assertEquals(1, changesNode.keys().size());
    }

    private void checkDeletes(ModelNode changesNode, int expectedView, String...expectedKeys) {
        Assert.assertEquals(expectedView, changesNode.get("changes", "view").asInt());
        Assert.assertEquals(1, changesNode.keys().size());
        if (expectedKeys.length == 0) {
            Assert.assertFalse(changesNode.get("changes", "issues").hasDefined("delete"));
        } else {
            Set<String> expectedKeysSet = new HashSet<>(Arrays.asList(expectedKeys));
            List<ModelNode> list = changesNode.get("changes", "issues", "delete").asList();
            Assert.assertEquals(expectedKeys.length, list.size());
            for (ModelNode node : list) {
                Assert.assertTrue(expectedKeysSet.contains(node.asString()));
            }
        }
    }

    private void checkAdds(ModelNode changesNode, int expectedView, IssueData...expectedIssues) {
        Assert.assertEquals(expectedView, changesNode.get("changes", "view").asInt());
        Assert.assertEquals(1, changesNode.keys().size());
        if (expectedIssues.length == 0) {
            Assert.assertFalse(changesNode.get("changes", "issues").hasDefined("new"));
        } else {
            Map<String, IssueData> expectedIssuesMap = new HashMap<>();
            Arrays.asList(expectedIssues).forEach(ei -> expectedIssuesMap.put(ei.key, ei));
            List<ModelNode> list = changesNode.get("changes", "issues", "new").asList();
            Assert.assertEquals(expectedIssuesMap.size(), list.size());
            for (ModelNode node : list) {
                final String key = node.get("key").asString();
                IssueData expected = expectedIssuesMap.get(key);
                Assert.assertNotNull(expected);
                Assert.assertEquals(expected.type.name, nullOrString(node.get("type")));
                Assert.assertEquals(expected.priority.name, nullOrString(node.get("priority")));
                Assert.assertEquals(expected.summary, nullOrString(node.get("summary")));
                Assert.assertEquals(expected.assignee, nullOrString(node.get("assignee")));
                Assert.assertEquals(expected.state, nullOrString(node.get("state")));
            }
        }
    }

    private void checkUpdates(ModelNode changesNode, int expectedView, IssueData...expectedIssues) {
        Assert.assertEquals(expectedView, changesNode.get("changes", "view").asInt());
        Assert.assertEquals(1, changesNode.keys().size());
        if (expectedIssues.length == 0) {
            Assert.assertFalse(changesNode.get("changes", "issues").hasDefined("update"));
        } else {
            Map<String, IssueData> expectedIssuesMap = new HashMap<>();
            Arrays.asList(expectedIssues).forEach(ei -> expectedIssuesMap.put(ei.key, ei));
            List<ModelNode> list = changesNode.get("changes", "issues", "update").asList();
            Assert.assertEquals(expectedIssuesMap.size(), list.size());
            for (ModelNode node : list) {
                final String key = node.get("key").asString();
                IssueData expected = expectedIssuesMap.get(key);
                Assert.assertNotNull(expected);
                Assert.assertEquals(expected.type == null ? null : expected.type.name,
                        nullOrString(node.get("type")));
                Assert.assertEquals(expected.priority == null ? null : expected.priority.name,
                        nullOrString(node.get("priority")));
                Assert.assertEquals(expected.summary, nullOrString(node.get("summary")));
                Assert.assertEquals(expected.assignee, nullOrString(node.get("assignee")));
                Assert.assertEquals(expected.state, nullOrString(node.get("state")));
                if (expected.unassigned) {
                    Assert.assertTrue(node.get("unassigned").asBoolean());
                } else {
                    Assert.assertFalse(node.has("unassigned"));
                }
            }
        }
    }

    private void checkAssignees(ModelNode changesNode, int expectedView, String...expectedAssignees) {
        Assert.assertEquals(expectedView, changesNode.get("changes", "view").asInt());
        Assert.assertEquals(1, changesNode.keys().size());
        if (expectedAssignees.length == 0) {
            Assert.assertFalse(changesNode.get("changes").hasDefined("assignees"));
        } else {
            List<ModelNode> list = changesNode.get("changes", "assignees").asList();
            Assert.assertEquals(expectedAssignees.length, list.size());
            Set<String> assignees = new HashSet<>(Arrays.asList(expectedAssignees));
            for (ModelNode assigneeNode : list) {
                String key = assigneeNode.get("key").asString();
                Assert.assertTrue(assignees.contains(key));
                Assert.assertEquals(key + "@example.com", assigneeNode.get("email").asString());
                Assert.assertEquals("/avatars/" + key + ".png", assigneeNode.get("avatar").asString());
                String displayName = assigneeNode.get("name").toString().toLowerCase();
                Assert.assertTrue(displayName.length() > key.length());
                Assert.assertTrue(displayName.contains(key));
            }
        }
    }

    String nullOrString(ModelNode node) {
        if (node.isDefined()) {
            return node.asString();
        }
        return null;
    }



    private void checkViewId(int expectedViewId) throws SearchException {
        String json = boardManager.getBoardJson(userManager.getUserByKey("kabir"), 0);
        Assert.assertNotNull(json);
        ModelNode boardNode = ModelNode.fromJSONString(json);
        Assert.assertEquals(expectedViewId, boardNode.get("view").asInt());
    }

    private ModelNode getChangesJson(int fromView) throws SearchException {
        String json = boardManager.getChangesJson(userManager.getUserByKey("kabir"), 0, fromView);
        ModelNode changesNode = ModelNode.fromJSONString(json);
        return changesNode;
    }

    private static class IssueData {
        private final String key;
        private final IssueType type;
        private final Priority priority;
        private final String summary;
        private final String state;
        private final String assignee;
        private final boolean unassigned;

        IssueData(String key, IssueType type, Priority priority, String summary, String assignee, String state) {
            this(key, type, priority, summary, assignee, false, state);
        }

        IssueData(String key, IssueType type, Priority priority, String summary, String assignee, boolean unassigned, String state) {
            this.key = key;
            this.type = type;
            this.priority = priority;
            this.summary = summary;
            this.assignee = assignee;
            this.unassigned = unassigned;
            this.state = state;
        }
    }
}

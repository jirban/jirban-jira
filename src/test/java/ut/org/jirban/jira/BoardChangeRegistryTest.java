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
import org.junit.Test;

import com.atlassian.jira.issue.search.SearchException;

/**
 * @author Kabir Khan
 */
public class BoardChangeRegistryTest extends AbstractBoardTest {

    private void configureBoardStart() {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", "TDP-D");
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", "TDP-A");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", "TDP-B");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, "TDP-C");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, "TBG-X");
    }

    @Test
    public void testNoChanges() throws Exception {
        configureBoardStart();

        checkViewId(0);
        checkNoChanges(getChangesJson(0), 0);
    }

    @Test
    public void testFullRefreshOnTooHighView() throws Exception {
        configureBoardStart();

        checkViewId(0);
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
        configureBoardStart();

        checkViewId(0);
        checkNoChanges(getChangesJson(0), 0);

        JirbanIssueEvent delete = JirbanIssueEvent.createDeleteEvent("TDP-3", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(1);
        checkAssignees(getChangesJson(0), 1);
        checkAdds(getChangesJson(0), 1);
        checkDeletes(getChangesJson(0), 1, "TDP-3");

        delete = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(2);
        checkAssignees(getChangesJson(0), 2);
        checkAdds(getChangesJson(0), 2);
        checkDeletes(getChangesJson(0), 2, "TDP-3", "TDP-7");
        checkDeletes(getChangesJson(1), 2, "TDP-7");

        delete = JirbanIssueEvent.createDeleteEvent("TBG-1", "TBG");
        boardManager.handleEvent(delete);
        checkViewId(3);
        checkAssignees(getChangesJson(0), 3);
        checkAdds(getChangesJson(0), 3);
        checkDeletes(getChangesJson(0), 3, "TDP-3", "TDP-7", "TBG-1");
        checkDeletes(getChangesJson(1), 3, "TDP-7", "TBG-1");
        checkDeletes(getChangesJson(2), 3, "TBG-1");
        checkNoChanges(getChangesJson(3), 3);
    }

    @Test
    public void testCreateIssues() throws Exception {
        configureBoardStart();

        checkViewId(0);
        checkNoChanges(getChangesJson(0), 0);

        //Add an issue which does not bring in new assignees
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", "bug", "high", "Eight", "kabir", "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(1);
        checkAssignees(getChangesJson(0), 1);
        checkDeletes(getChangesJson(0), 1);
        checkAdds(getChangesJson(0), 1, new CreatedIssue("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D"));

        //Now add an issue which brings in new assignees
        create = createCreateEventAndAddToRegistry("TBG-4", "feature", "low", "Four", "jason", "TBG-X");
        boardManager.handleEvent(create);
        checkViewId(2);
        checkAssignees(getChangesJson(0), 2, "jason");
        checkDeletes(getChangesJson(0), 2);
        checkAdds(getChangesJson(0), 2,
                new CreatedIssue("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D"),
                new CreatedIssue("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"));
        checkAssignees(getChangesJson(1), 2, "jason");
        checkDeletes(getChangesJson(1), 2);
        checkAdds(getChangesJson(1), 2,
                new CreatedIssue("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"));

        //Add another one not bringing in new assignees
        create = createCreateEventAndAddToRegistry("TDP-9", "bug", "high", "Nine", null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(3);
        checkAssignees(getChangesJson(0), 3, "jason");
        checkDeletes(getChangesJson(0), 3);
        checkAdds(getChangesJson(0), 3,
                new CreatedIssue("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D"),
                new CreatedIssue("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"),
                new CreatedIssue("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkAssignees(getChangesJson(1), 3, "jason");
        checkDeletes(getChangesJson(1), 3);
        checkAdds(getChangesJson(1), 3,
                new CreatedIssue("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"),
                new CreatedIssue("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkAssignees(getChangesJson(2), 3);
        checkDeletes(getChangesJson(2), 3);
        checkAdds(getChangesJson(2), 3,
                new CreatedIssue("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
    }

    //Test create and add

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

    private void checkAdds(ModelNode changesNode, int expectedView, CreatedIssue...expectedIssues) {
        Assert.assertEquals(expectedView, changesNode.get("changes", "view").asInt());
        Assert.assertEquals(1, changesNode.keys().size());
        if (expectedIssues.length == 0) {
            Assert.assertFalse(changesNode.get("changes", "issues").hasDefined("new"));
        } else {
            Map<String, CreatedIssue> expectedIssuesMap = new HashMap<>();
            Arrays.asList(expectedIssues).forEach(ei -> expectedIssuesMap.put(ei.key, ei));
            List<ModelNode> list = changesNode.get("changes", "issues", "new").asList();
            Assert.assertEquals(expectedIssuesMap.size(), list.size());
            for (ModelNode node : list) {
                final String key = node.get("key").asString();
                CreatedIssue expected = expectedIssuesMap.get(key);
                Assert.assertNotNull(expected);
                Assert.assertEquals(expected.type.name, nullOrString(node.get("type")));
                Assert.assertEquals(expected.priority.name, nullOrString(node.get("priority")));
                Assert.assertEquals(expected.summary, nullOrString(node.get("summary")));
                Assert.assertEquals(expected.assignee, nullOrString(node.get("assignee")));
                Assert.assertEquals(expected.state, nullOrString(node.get("state")));
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

    private static class CreatedIssue {
        private final String key;
        private final IssueType type;
        private final Priority priority;
        private final String summary;
        private final String state;
        private final String assignee;

        CreatedIssue(String key, IssueType type, Priority priority, String summary, String assignee, String state) {
            this.key = key;
            this.type = type;
            this.priority = priority;
            this.summary = summary;
            this.assignee = assignee;
            this.state = state;
        }
    }
}

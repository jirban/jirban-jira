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
import java.util.HashSet;
import java.util.List;
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
        issueRegistry.addIssue("TBG", "task", "lowest", "Four", "jason", "TBG-Y");
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
        checkDeletes(getChangesJson(0), 1, "TDP-3");

        delete = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(2);
        checkDeletes(getChangesJson(0), 2, "TDP-3", "TDP-7");
        checkDeletes(getChangesJson(1), 2, "TDP-7");

        delete = JirbanIssueEvent.createDeleteEvent("TBG-1", "TBG");
        boardManager.handleEvent(delete);
        checkViewId(3);
        checkDeletes(getChangesJson(0), 3, "TDP-3", "TDP-7", "TBG-1");
        checkDeletes(getChangesJson(1), 3, "TDP-7", "TBG-1");
        checkDeletes(getChangesJson(2), 3, "TBG-1");
        checkNoChanges(getChangesJson(3), 3);
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
}

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

import static org.jirban.jira.impl.Constants.ASSIGNEE;
import static org.jirban.jira.impl.Constants.ASSIGNEES;
import static org.jirban.jira.impl.Constants.AVATAR;
import static org.jirban.jira.impl.Constants.BLACKLIST;
import static org.jirban.jira.impl.Constants.CHANGES;
import static org.jirban.jira.impl.Constants.CLEAR_COMPONENTS;
import static org.jirban.jira.impl.Constants.COMPONENTS;
import static org.jirban.jira.impl.Constants.DELETE;
import static org.jirban.jira.impl.Constants.EMAIL;
import static org.jirban.jira.impl.Constants.ISSUES;
import static org.jirban.jira.impl.Constants.ISSUE_TYPES;
import static org.jirban.jira.impl.Constants.KEY;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.NEW;
import static org.jirban.jira.impl.Constants.PRIORITIES;
import static org.jirban.jira.impl.Constants.PRIORITY;
import static org.jirban.jira.impl.Constants.REMOVED_ISSUES;
import static org.jirban.jira.impl.Constants.STATES;
import static org.jirban.jira.impl.Constants.SUMMARY;
import static org.jirban.jira.impl.Constants.TYPE;
import static org.jirban.jira.impl.Constants.UNASSIGNED;
import static org.jirban.jira.impl.Constants.VIEW;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.JirbanIssueEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.issue.search.SearchException;

/**
 * Tests the output of what happens when changes are made to the board issues.
 * {@link BoardManagerTest} tests the layout of the board on the server, and how it is serialized to the client on first load/full refresh.
 *
 * @author Kabir Khan
 */
public class BoardChangeRegistryTest extends AbstractBoardTest {

    @Before
    public void setupInitialBoard() throws SearchException {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", new String[]{"C1"}, "TDP-A");  //1
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", new String[]{"C2"}, "TDP-B");     //2
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");    //3
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "TDP-D");  //4
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", null, "TDP-A"); //5
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", null, "TDP-B");      //6
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, new String[]{"C1"}, "TDP-C");    //7

        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", new String[]{"C3"}, "TBG-X");  //1
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");      //2
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");    //3

        checkViewId(0);
        checkNoIssueChanges(0, 0);
        checkNoBlacklistChanges(0, 0);
        checkNoStateChanges(0, 0);
    }

    @Test
    public void testFullRefreshOnTooHighView() throws Exception {
        ModelNode changes = getChangesJson(1);
        Assert.assertFalse(changes.hasDefined(CHANGES));
        Assert.assertFalse(changes.hasDefined("blacklist"));
        //Check that the top-level fields of the board are there
        Assert.assertTrue(changes.hasDefined(VIEW));
        Assert.assertTrue(changes.hasDefined("assignees"));
        Assert.assertTrue(changes.hasDefined("priorities"));
        Assert.assertTrue(changes.hasDefined("issue-types"));
        Assert.assertTrue(changes.hasDefined("projects"));
        Assert.assertTrue(changes.hasDefined(ISSUES));

        //TODO should check the same when passing in an old view id that has been reaped after being too old
    }

    @Test
    public void testDeleteIssues() throws Exception {
        JirbanIssueEvent delete = JirbanIssueEvent.createDeleteEvent("TDP-3", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(1);
        checkAssignees(0, 1);
        checkComponents(0, 1);
        checkAdds(0, 1);
        checkUpdates(0, 1);
        checkDeletes(0, 1, "TDP-3");
        checkNoBlacklistChanges(0, 1);
        checkNoStateChanges(0, 1);

        delete = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(2);
        checkAssignees(0, 2);
        checkComponents(0, 2);
        checkAdds(0, 2);
        checkUpdates(0, 2);
        checkDeletes(0, 2, "TDP-3", "TDP-7");
        checkDeletes(1, 2, "TDP-7");
        checkNoBlacklistChanges(0, 2);
        checkNoStateChanges(0, 2);


        delete = JirbanIssueEvent.createDeleteEvent("TBG-1", "TBG");
        boardManager.handleEvent(delete);
        checkViewId(3);
        checkAssignees(0, 3);
        checkComponents(0, 3);
        checkAdds(0, 3);
        checkUpdates(0, 3);
        checkDeletes(0, 3, "TDP-3", "TDP-7", "TBG-1");
        checkDeletes(1, 3, "TDP-7", "TBG-1");
        checkDeletes(2, 3, "TBG-1");
        checkNoIssueChanges(3, 3);
        checkNoBlacklistChanges(0, 3);
        checkNoStateChanges(0, 3);
    }

    @Test
    public void testCreateIssuesAssignees() throws Exception {
        //Add an issue which does not bring in new assignees
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(1);
        checkAssignees(0, 1);
        checkComponents(0, 1);
        checkDeletes(0, 1);
        checkUpdates(0, 1);
        checkAdds(0, 1, new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D"));
        checkNoBlacklistChanges(0, 1);
        checkStateChanges(0, 1, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8"));

        //Now add an issue which brings in new assignees
        create = createCreateEventAndAddToRegistry("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", null, "TBG-X");
        boardManager.handleEvent(create);
        checkViewId(2);
        checkAssignees(0, 2, "jason");
        checkComponents(0, 2);
        checkDeletes(0, 2);
        checkUpdates(0, 2);
        checkAdds(0, 2,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", null, "TBG-X"));
        checkStateChanges(0, 2, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8"),
                new StateChangeData("TBG", "TBG-X", "TBG-1", "TBG-3", "TBG-4"));

        checkAssignees(1, 2, "jason");
        checkDeletes(1, 2);
        checkAdds(1, 2,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", null, "TBG-X"));
        checkNoBlacklistChanges(0, 2);
        checkStateChanges(1, 2, new StateChangeData("TBG", "TBG-X", "TBG-1", "TBG-3", "TBG-4"));


        //Add another one not bringing in new assignees
        create = createCreateEventAndAddToRegistry("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(3);
        checkAssignees(0, 3, "jason");
        checkComponents(0, 3);
        checkDeletes(0, 3);
        checkUpdates(0, 3);
        checkAdds(0, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", null, "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkStateChanges(0, 3, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8", "TDP-9"),
                new StateChangeData("TBG", "TBG-X", "TBG-1", "TBG-3", "TBG-4"));

        checkAssignees(1, 3, "jason");
        checkComponents(1, 3);
        checkDeletes(1, 3);
        checkUpdates(1, 3);
        checkAdds(1, 3,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", null, "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkStateChanges(1, 3, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8", "TDP-9"),
                new StateChangeData("TBG", "TBG-X", "TBG-1", "TBG-3", "TBG-4"));
        checkAssignees(2, 3);
        checkComponents(2, 3);
        checkDeletes(2, 3);
        checkUpdates(2, 3);
        checkAdds(2, 3,
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkStateChanges(0, 3, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8", "TDP-9"),
                new StateChangeData("TBG", "TBG-X", "TBG-1", "TBG-3", "TBG-4"));
        checkNoBlacklistChanges(0, 3);
    }

    @Test
    public void testCreateIssuesComponents() throws Exception {
        //Add an issue which does not bring in new components
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", new String[]{"C1", "C2"}, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(1);
        checkAssignees(0, 1);
        checkComponents(0, 1);
        checkDeletes(0, 1);
        checkUpdates(0, 1);
        checkAdds(0, 1, new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", new String[]{"C1", "C2"}, "TDP-D"));
        checkNoBlacklistChanges(0, 1);
        checkStateChanges(0, 1, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8"));

        //Now add an issue which brings in new components
        create = createCreateEventAndAddToRegistry("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "kabir", new String[]{"C5", "C6"}, "TBG-X");
        boardManager.handleEvent(create);
        checkViewId(2);
        checkAssignees(0, 2);
        checkComponents(0, 2, "C5", "C6");
        checkDeletes(0, 2);
        checkUpdates(0, 2);
        checkAdds(0, 2,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", new String[]{"C1", "C2"}, "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "kabir", new String[]{"C5", "C6"}, "TBG-X"));
        checkStateChanges(0, 2, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8"),
                new StateChangeData("TBG", "TBG-X", "TBG-1", "TBG-3", "TBG-4"));

        checkAssignees(1, 2);
        checkComponents(1, 2, "C5", "C6");
        checkDeletes(1, 2);
        checkAdds(1, 2,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "kabir", new String[]{"C5", "C6"}, "TBG-X"));
        checkNoBlacklistChanges(0, 2);
        checkStateChanges(1, 2, new StateChangeData("TBG", "TBG-X", "TBG-1", "TBG-3", "TBG-4"));


        //Add another one not bringing in new components
        create = createCreateEventAndAddToRegistry("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(3);
        checkAssignees(0, 3);
        checkComponents(0, 3, "C5", "C6");
        checkDeletes(0, 3);
        checkUpdates(0, 3);
        checkAdds(0, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", new String[]{"C1", "C2"}, "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "kabir", new String[]{"C5", "C6"}, "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkStateChanges(0, 3, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8", "TDP-9"),
                new StateChangeData("TBG", "TBG-X", "TBG-1", "TBG-3", "TBG-4"));

        checkAssignees(1, 3);
        checkComponents(1, 3, "C5", "C6");
        checkDeletes(1, 3);
        checkUpdates(1, 3);
        checkAdds(1, 3,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "kabir", new String[]{"C5", "C6"}, "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkStateChanges(1, 3, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8", "TDP-9"),
                new StateChangeData("TBG", "TBG-X", "TBG-1", "TBG-3", "TBG-4"));
        checkAssignees(2, 3);
        checkComponents(2, 3);
        checkDeletes(2, 3);
        checkUpdates(2, 3);
        checkAdds(2, 3,
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkStateChanges(0, 3, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8", "TDP-9"),
                new StateChangeData("TBG", "TBG-X", "TBG-1", "TBG-3", "TBG-4"));
        checkNoBlacklistChanges(0, 3);
    }



    @Test
    public void testUpdateSameIssueNonAssigneesOrComponents() throws Exception {
        //Do a noop update
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(0);
        checkNoIssueChanges(0, 0);
        checkNoStateChanges(0, 0);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, "Seven-1", null, false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        //Check assignees and deletes extra well here so we don't have to in the other tests
        checkAssignees(0, 1);
        checkComponents(0, 1);
        checkDeletes(0, 1);
        checkUpdates(0, 1, new IssueData("TDP-7", null, null, "Seven-1", null, null, null));
        checkNoBlacklistChanges(0, 1);
        checkNoStateChanges(0, 1);


        update = createUpdateEventAndAddToRegistry("TDP-7", IssueType.BUG, null, null, null, false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkAssignees(0, 2);
        checkComponents(0, 2);
        checkDeletes(0, 2);
        checkUpdates(0, 2, new IssueData("TDP-7", IssueType.BUG, null, "Seven-1", null, null, null));
        checkAssignees(1, 2);
        checkComponents(1, 2);
        checkDeletes(1, 2);
        checkUpdates(1, 2, new IssueData("TDP-7", IssueType.BUG, null, null, null, null, null));
        checkNoBlacklistChanges(0, 2);
        checkNoStateChanges(0, 2);


        update = createUpdateEventAndAddToRegistry("TDP-7", null, Priority.HIGHEST, null, null, false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(3);
        checkAssignees(0, 3);
        checkComponents(0, 3);
        checkDeletes(0, 3);
        checkUpdates(0, 3, new IssueData("TDP-7", IssueType.BUG, Priority.HIGHEST, "Seven-1", null, null, null));
        checkAssignees(1, 3);
        checkComponents(1, 3);
        checkDeletes(1, 3);
        checkUpdates(1, 3, new IssueData("TDP-7", IssueType.BUG, Priority.HIGHEST, null, null, null, null));
        checkAssignees(2, 3);
        checkComponents(2, 3);
        checkDeletes(2, 3);
        checkUpdates(2, 3, new IssueData("TDP-7", null, Priority.HIGHEST, null, null, null, null));
        checkNoBlacklistChanges(0, 3);
        checkNoStateChanges(0, 3);

    }


    @Test
    public void testSameIssueAssignees() throws Exception {
        //Do a noop update
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, "kabir", false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        checkAssignees(0, 1);
        checkComponents(0, 1);
        checkUpdates(0, 1, new IssueData("TDP-7", null, null, null, "kabir", null, null));
        checkNoBlacklistChanges(0, 1);
        checkNoStateChanges(0, 1);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, true, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkAssignees(0, 2);
        checkComponents(0, 2);
        checkUpdates(0, 2, new IssueData("TDP-7", null, null, null, null, true, null, false, null));
        checkUpdates(1, 2, new IssueData("TDP-7", null, null, null, null, true, null, false, null));
        checkNoBlacklistChanges(0, 2);
        checkNoStateChanges(0, 2);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, "jason", false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(3);
        checkAssignees(0, 3, "jason");
        checkComponents(0, 3);
        checkUpdates(0, 3, new IssueData("TDP-7", null, null, null, "jason", false, null, false, null));
        checkAssignees(1, 3, "jason");
        checkUpdates(1, 3, new IssueData("TDP-7", null, null, null, "jason", false, null, false, null));
        checkAssignees(2, 3, "jason");
        checkUpdates(2, 3, new IssueData("TDP-7", null, null, null, "jason", false, null, false, null));
        checkNoBlacklistChanges(0, 3);
        checkNoStateChanges(0, 3);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, "brian", false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(4);
        checkAssignees(0, 4);
        checkComponents(0, 4);
        checkUpdates(0, 4, new IssueData("TDP-7", null, null, null, "brian", false, null, false, null));
        checkAssignees(1, 4);
        checkUpdates(1, 4, new IssueData("TDP-7", null, null, null, "brian", false, null, false, null));
        checkAssignees(2, 4);
        checkUpdates(2, 4, new IssueData("TDP-7", null, null, null, "brian", false, null, false, null));
        checkAssignees(3, 4);
        checkUpdates(3, 4, new IssueData("TDP-7", null, null, null, "brian", false, null, false, null));
        checkNoBlacklistChanges(0, 4);
        checkNoStateChanges(0, 4);

    }

    @Test
    public void testSameIssueComponents() throws Exception {
        //Do a noop update
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false,
                new String[]{"C1"}, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        checkAssignees(0, 1);
        checkComponents(0, 1);
        checkUpdates(0, 1, new IssueData("TDP-7", null, null, null, null, new String[]{"C1"}, null));
        checkNoBlacklistChanges(0, 1);
        checkNoStateChanges(0, 1);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false, null, true, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkAssignees(0, 2);
        checkComponents(0, 2);
        checkUpdates(0, 2, new IssueData("TDP-7", null, null, null, null, false, null, true, null));
        checkUpdates(1, 2, new IssueData("TDP-7", null, null, null, null, false, null, true, null));
        checkNoBlacklistChanges(0, 2);
        checkNoStateChanges(0, 2);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false,
                new String[]{"C-10"}, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(3);
        checkAssignees(0, 3);
        checkComponents(0, 3, "C-10");
        checkUpdates(0, 3, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C-10"}, false, null));
        checkAssignees(1, 3);
        checkComponents(1, 3, "C-10");
        checkUpdates(1, 3, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C-10"}, false, null));
        checkAssignees(2, 3);
        checkComponents(2, 3, "C-10");
        checkUpdates(2, 3, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C-10"}, false, null));
        checkNoBlacklistChanges(0, 3);
        checkNoStateChanges(0, 3);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false, new String[]{"C1", "C2"}, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(4);
        checkAssignees(0, 4);
        checkComponents(0, 4);
        checkUpdates(0, 4, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C1", "C2"}, false, null));
        checkAssignees(1, 4);
        checkComponents(1, 4);
        checkUpdates(1, 4, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C1", "C2"}, false, null));
        checkAssignees(2, 4);
        checkComponents(2, 4);
        checkUpdates(2, 4, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C1", "C2"}, false, null));
        checkAssignees(3, 4);
        checkComponents(3, 4);
        checkUpdates(3, 4, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C1", "C2"}, false, null));
        checkNoBlacklistChanges(0, 4);
        checkNoStateChanges(0, 4);
    }

    @Test
    public void testUpdateSeveralIssues() throws Exception {
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, "Seven-1", null, false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        checkUpdates(0, 1, new IssueData("TDP-7", null, null, "Seven-1", null, null, null));
        checkNoBlacklistChanges(0, 1);
        checkNoStateChanges(0, 1);

        update = createUpdateEventAndAddToRegistry("TBG-3", IssueType.BUG, null, null, "kabir", false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkUpdates(0, 2,
                new IssueData("TDP-7", null, null, "Seven-1", null, null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkNoBlacklistChanges(0, 2);
        checkNoStateChanges(0, 2);

        //Create, update and delete TDP-8 to make sure that does not affect the others
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(3);
        checkUpdates(0, 3,
                new IssueData("TDP-7", null, null, "Seven-1", null, null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkAdds(0, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkStateChanges(0, 3, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8"));

        checkUpdates(1, 3,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkAdds(1, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkStateChanges(1, 3, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8"));

        checkUpdates(2, 3);
        checkAdds(2, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkStateChanges(2, 3, new StateChangeData("TDP", "TDP-D", "TDP-4", "TDP-8"));
        checkNoBlacklistChanges(0, 3);



        //This should appear as an add for change sets including its previous create, and an update for change
        //sets not including the create
        update = createUpdateEventAndAddToRegistry("TDP-8", (IssueType) null, null, null, "jason", false, null, false, "TDP-C", false);
        boardManager.handleEvent(update);
        checkViewId(4);
        checkAssignees(0, 4, "jason");
        checkUpdates(0, 4,
                new IssueData("TDP-7", null, null, "Seven-1", null, null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkAdds(0, 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", null, "TDP-C"));
        checkStateChanges(0, 4, new StateChangeData("TDP", "TDP-C", "TDP-3", "TDP-7", "TDP-8"));

        checkAssignees(1, 4, "jason");
        checkUpdates(1, 4,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkAdds(1, 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", null, "TDP-C"));
        checkStateChanges(1, 4, new StateChangeData("TDP", "TDP-C", "TDP-3", "TDP-7", "TDP-8"));

        checkAssignees(2, 4, "jason");
        checkUpdates(2, 4);
        checkAdds(2, 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", null, "TDP-C"));
        checkStateChanges(2, 4, new StateChangeData("TDP", "TDP-C", "TDP-3", "TDP-7", "TDP-8"));

        checkAssignees(3, 4, "jason");
        checkUpdates(3, 4,
                new IssueData("TDP-8", null, null, null, "jason", null, "TDP-C"));
        checkAdds(3, 4);
        checkStateChanges(3, 4, new StateChangeData("TDP", "TDP-C", "TDP-3", "TDP-7", "TDP-8"));

        //This will not appear in change sets including the create, it becomes a noop
        JirbanIssueEvent delete = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(5);
        checkAssignees(0, 5);
        checkUpdates(0, 5,
                new IssueData("TDP-7", null, null, "Seven-1", null, null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkAdds(0, 5);
        checkNoStateChanges(0, 5);


        checkAssignees(1, 5);
        checkUpdates(1, 5,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkAdds(1, 5);
        checkNoStateChanges(1, 5);

        checkAssignees(2, 5);
        checkUpdates(2, 5);
        checkAdds(2, 5);
        checkNoStateChanges(2, 5);

        checkAssignees(3, 5);
        checkUpdates(3, 5);
        checkAdds(3, 5);
        checkNoStateChanges(3, 5);

        checkAssignees(4, 5);
        checkUpdates(4, 5);
        checkAdds(4, 5);
        checkNoBlacklistChanges(4, 5);
        checkNoStateChanges(4, 5);
    }

    @Test
    public void testBlacklistBadCreatedState() throws SearchException {
        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-8", IssueType.FEATURE, Priority.HIGH, "Eight", null, null, "BadState");
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, new String[]{"BadState"}, null, null, new String[]{"TDP-8"}, null);
        checkNoStateChanges(0, 1);


        event = createUpdateEventAndAddToRegistry("TDP-8", (IssueType) null, null, "Eight-1", null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-8", "NewBadType", null, null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, new String[]{"BadState"}, null, null, null, new String[]{"TDP-8"});
        checkNoStateChanges(0, 2);
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-8"});
        checkNoStateChanges(1, 2);
    }

    @Test
    public void testBlacklistBadUpdatedState() throws SearchException {
        JirbanIssueEvent event = createUpdateEventAndAddToRegistry("TDP-7", (IssueType)null, null, null, null, false, null, false, "BadState", false);
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, new String[]{"BadState"}, null, null, new String[]{"TDP-7"}, null);
        checkNoStateChanges(0, 1);

        event = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-7", "NewBadType", null, null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, new String[]{"BadState"}, null, null, null, new String[]{"TDP-7"});
        checkNoStateChanges(0, 2);
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-7"});
        checkNoStateChanges(1, 2);
    }


    @Test
    public void testBlacklistBadCreatedIssueType() throws SearchException {
        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-8", "BadType", Priority.HIGH.name, "Eight", null, null, "TDP-C");
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, null, new String[]{"BadType"}, null, new String[]{"TDP-8"}, null);
        checkNoStateChanges(0, 1);

        event = createUpdateEventAndAddToRegistry("TDP-8", (IssueType) null, null, "Eight-1", null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-8", "NewBadType", null, null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, null, new String[]{"BadType"}, null, null, new String[]{"TDP-8"});
        checkNoStateChanges(0, 2);
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-8"});
        checkNoStateChanges(1, 2);
    }

    @Test
    public void testBlacklistBadUpdatedIssueType() throws SearchException {
        JirbanIssueEvent event = createUpdateEventAndAddToRegistry("TDP-7", "BadType", null, null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, null, new String[]{"BadType"}, null, new String[]{"TDP-7"}, null);
        checkNoStateChanges(0, 1);

        event = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-7", "NewBadType", null, null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, null, new String[]{"BadType"}, null, null, new String[]{"TDP-7"});
        checkNoStateChanges(0, 2);
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-7"});
        checkNoStateChanges(1, 2);
    }

    @Test
    public void testBlacklistBadCreatedPriority() throws SearchException {
        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-8", IssueType.FEATURE.name, "BadPriority", "Eight", null, null, "TDP-C");
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, null, null, new String[]{"BadPriority"}, new String[]{"TDP-8"}, null);
        checkNoStateChanges(0, 1);

        event = createUpdateEventAndAddToRegistry("TDP-8", null, (Priority)null, "Eight-1", null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-8", null, "NewBadPriority", null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, null, null, new String[]{"BadPriority"}, null, new String[]{"TDP-8"});
        checkNoStateChanges(0, 2);
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-8"});
        checkNoStateChanges(1, 2);
    }

    @Test
    public void testBlacklistBadUpdatedPriority() throws SearchException {
        JirbanIssueEvent event = createUpdateEventAndAddToRegistry("TDP-7", null, "BadPriority", null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, null, null, new String[]{"BadPriority"}, new String[]{"TDP-7"}, null);
        checkNoStateChanges(0, 1);

        event = createUpdateEventAndAddToRegistry("TDP-7", null, (Priority)null, "Eight-1", null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-7", null, "NewBadPriority", null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, null, null, new String[]{"BadPriority"}, null, new String[]{"TDP-7"});
        checkNoStateChanges(0, 2);
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-7"});
        checkNoStateChanges(1, 2);
    }

    private void checkNoIssueChanges(int fromView, int expectedView) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView);

        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        Assert.assertFalse(changesNode.hasDefined(CHANGES, ISSUES));
    }

    private void checkNoBlacklistChanges(int fromView, int expectedView) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView);

        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        Assert.assertFalse(changesNode.hasDefined(CHANGES, BLACKLIST));
    }

    private void checkNoStateChanges(int fromView, int expectedView) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView);

        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        Assert.assertFalse(changesNode.hasDefined(CHANGES, STATES));
    }

    private void checkDeletes(int fromView, int expectedView, String...expectedKeys) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView);

        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        Assert.assertEquals(1, changesNode.keys().size());
        if (expectedKeys.length == 0) {
            Assert.assertFalse(changesNode.get(CHANGES, ISSUES).hasDefined(DELETE));
        } else {
            Set<String> expectedKeysSet = new HashSet<>(Arrays.asList(expectedKeys));
            List<ModelNode> list = changesNode.get(CHANGES, ISSUES, DELETE).asList();
            Assert.assertEquals(expectedKeys.length, list.size());
            for (ModelNode node : list) {
                Assert.assertTrue(expectedKeysSet.contains(node.asString()));
            }
        }
    }

    private void checkAdds(int fromView, int expectedView, IssueData...expectedIssues) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView);

        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        Assert.assertEquals(1, changesNode.keys().size());
        if (expectedIssues.length == 0) {
            Assert.assertFalse(changesNode.get(CHANGES, ISSUES).hasDefined(NEW));
        } else {
            Map<String, IssueData> expectedIssuesMap = new HashMap<>();
            Arrays.asList(expectedIssues).forEach(ei -> expectedIssuesMap.put(ei.key, ei));
            List<ModelNode> list = changesNode.get(CHANGES, ISSUES, NEW).asList();
            Assert.assertEquals(expectedIssuesMap.size(), list.size());
            for (ModelNode node : list) {
                final String key = node.get(KEY).asString();
                IssueData expected = expectedIssuesMap.get(key);
                Assert.assertNotNull(expected);
                Assert.assertEquals(expected.type.name, nullOrString(node.get(TYPE)));
                Assert.assertEquals(expected.priority.name, nullOrString(node.get(PRIORITY)));
                Assert.assertEquals(expected.summary, nullOrString(node.get(SUMMARY)));
                Assert.assertEquals(expected.assignee, nullOrString(node.get(ASSIGNEE)));
                Assert.assertEquals(expected.state, nullOrString(node.get("state")));
                checkIssueComponents(expected.components, node);
            }
        }
    }

    private void checkUpdates(int fromView, int expectedView, IssueData...expectedIssues) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView);

        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        Assert.assertEquals(1, changesNode.keys().size());
        if (expectedIssues.length == 0) {
            Assert.assertFalse(changesNode.get(CHANGES, ISSUES).hasDefined("update"));
        } else {
            Map<String, IssueData> expectedIssuesMap = new HashMap<>();
            Arrays.asList(expectedIssues).forEach(ei -> expectedIssuesMap.put(ei.key, ei));
            List<ModelNode> list = changesNode.get(CHANGES, ISSUES, "update").asList();
            Assert.assertEquals(expectedIssuesMap.size(), list.size());
            for (ModelNode node : list) {
                final String key = node.get(KEY).asString();
                IssueData expected = expectedIssuesMap.get(key);
                Assert.assertNotNull(expected);
                Assert.assertEquals(expected.type == null ? null : expected.type.name,
                        nullOrString(node.get(TYPE)));
                Assert.assertEquals(expected.priority == null ? null : expected.priority.name,
                        nullOrString(node.get(PRIORITY)));
                Assert.assertEquals(expected.summary, nullOrString(node.get(SUMMARY)));
                Assert.assertEquals(expected.assignee, nullOrString(node.get(ASSIGNEE)));
                Assert.assertEquals(expected.state, nullOrString(node.get("state")));
                if (expected.unassigned) {
                    Assert.assertTrue(node.get(UNASSIGNED).asBoolean());
                } else {
                    Assert.assertFalse(node.has(UNASSIGNED));

                }
                checkIssueComponents(expected.components, node);
                if (expected.clearedComponents) {
                    Assert.assertTrue(node.get(CLEAR_COMPONENTS).asBoolean());
                } else {
                    Assert.assertFalse(node.has(CLEAR_COMPONENTS));
                }
            }
        }
    }

    private void checkAssignees(int fromView, int expectedView, String...expectedAssignees) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView);

        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        Assert.assertEquals(1, changesNode.keys().size());
        if (expectedAssignees.length == 0) {
            Assert.assertFalse(changesNode.get(CHANGES).hasDefined(ASSIGNEES));
        } else {
            List<ModelNode> list = changesNode.get(CHANGES, ASSIGNEES).asList();
            Assert.assertEquals(expectedAssignees.length, list.size());
            Set<String> assignees = new HashSet<>(Arrays.asList(expectedAssignees));
            for (ModelNode assigneeNode : list) {
                String key = assigneeNode.get(KEY).asString();
                Assert.assertTrue(assignees.contains(key));
                Assert.assertEquals(key + "@example.com", assigneeNode.get(EMAIL).asString());
                Assert.assertEquals("/avatars/" + key + ".png", assigneeNode.get(AVATAR).asString());
                String displayName = assigneeNode.get(NAME).toString().toLowerCase();
                Assert.assertTrue(displayName.length() > key.length());
                Assert.assertTrue(displayName.contains(key));
            }
        }
    }

    private void checkComponents(int fromView, int expectedView, String...expectedComponents) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView);

        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        Assert.assertEquals(1, changesNode.keys().size());
        if (expectedComponents.length == 0) {
            Assert.assertFalse(changesNode.get(CHANGES).hasDefined(COMPONENTS));
        } else {
            List<ModelNode> list = changesNode.get(CHANGES, COMPONENTS).asList();
            Assert.assertEquals(expectedComponents.length, list.size());
            Set<String> components = new HashSet<>(Arrays.asList(expectedComponents));
            for (ModelNode componentNode : list) {
                Assert.assertTrue(components.contains(componentNode.asString()));
            }
        }
    }

    private void checkIssueComponents(String[] expectedComponents, ModelNode issue) {
        if (expectedComponents == null || expectedComponents.length == 0) {
            Assert.assertFalse(issue.hasDefined(COMPONENTS));
        } else {
            List<ModelNode> issueComponents = issue.get(COMPONENTS).asList();
            Assert.assertEquals(expectedComponents.length, issueComponents.size());
            Set<String> expected = new HashSet<>(Arrays.asList(expectedComponents));
            for (ModelNode component : issueComponents) {
                Assert.assertTrue(expected.contains(component.asString()));
            }
        }
    }

    private void checkBlacklist(int fromView, int expectedView, String[] states, String[] issueTypes, String[] priorities, String[] issueKeys, String[] removedIssueKeys) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView);

        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        ModelNode blacklistNode = changesNode.get(CHANGES, BLACKLIST);
        checkEntries(blacklistNode, STATES, states);
        checkEntries(blacklistNode, ISSUE_TYPES, issueTypes);
        checkEntries(blacklistNode, PRIORITIES, priorities);
        checkEntries(blacklistNode, ISSUES, issueKeys);
        checkEntries(blacklistNode, REMOVED_ISSUES, removedIssueKeys);
    }

    private void checkEntries(ModelNode parent, String key, String... entries) {
        if (entries == null) {
            Assert.assertFalse(parent.hasDefined(key));
        } else {
            List<ModelNode> list = parent.get(key).asList();
            Assert.assertEquals(entries.length, list.size());
            Set<String> entrySet = list.stream().map(node -> node.asString()).collect(Collectors.toSet());
            for (String entry : entries) {
                Assert.assertTrue(entrySet.contains(entry));
            }
        }
    }

    private void checkStateChanges(int fromView, int expectedView, StateChangeData... expectedChanges) throws SearchException {
        Map<String, Map<String, List<String>>> expected = new HashMap<>();
        for (StateChangeData change : expectedChanges) {
            Map<String, List<String>> expectedForProject = expected.computeIfAbsent(change.projectCode, p -> new HashMap<>());
            expectedForProject.put(change.state, Arrays.asList(change.issues));
        }

        ModelNode changesNode = getChangesJson(fromView);
        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        ModelNode statesNode = changesNode.get(CHANGES, STATES);
        Assert.assertEquals(expected.size(), statesNode.keys().size());

        for (Map.Entry<String, Map<String, List<String>>> projectEntry : expected.entrySet()) {
            ModelNode projectChangeNode = statesNode.get(projectEntry.getKey());
            Assert.assertTrue(projectChangeNode.isDefined());

            Map<String, List<String>> projectChanges = projectEntry.getValue();
            Assert.assertEquals(projectChanges.size(), projectChangeNode.keys().size());

            for (Map.Entry<String, List<String>> stateEntry : projectChanges.entrySet()) {
                List<ModelNode> issuesList = projectChangeNode.get(stateEntry.getKey()).asList();
                List<String> expectedIssues = stateEntry.getValue();
                Assert.assertEquals(expectedIssues.size(), issuesList.size());

                for (int i = 0 ; i < issuesList.size() ; i++) {
                    Assert.assertEquals(expectedIssues.get(i), issuesList.get(i).asString());
                }
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
        Assert.assertEquals(expectedViewId, boardNode.get(VIEW).asInt());
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
        private final String[] components;
        private final boolean clearedComponents;

        IssueData(String key, IssueType type, Priority priority, String summary, String assignee, String[] components, String state) {
            this(key, type, priority, summary, assignee, false, components, false, state);
        }

        IssueData(String key, IssueType type, Priority priority, String summary, String assignee,
                  boolean unassigned, String[] components, boolean clearedComponents, String state) {
            this.key = key;
            this.type = type;
            this.priority = priority;
            this.summary = summary;
            this.assignee = assignee;
            this.unassigned = unassigned;
            this.state = state;
            this.components = components;
            this.clearedComponents = clearedComponents;
        }
    }

    private static class StateChangeData {
        private final String projectCode;
        private final String state;
        private final String[] issues;

        public StateChangeData(String projectCode, String state, String...issues) {
            this.projectCode = projectCode;
            this.state = state;
            this.issues = issues;
        }
    }
}

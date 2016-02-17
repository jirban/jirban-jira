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
        checkNoIssueChanges(0, 0);
        checkNoBlacklistChanges(0, 0);
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
        checkAdds(0, 1);
        checkUpdates(0, 1);
        checkDeletes(0, 1, "TDP-3");
        checkNoBlacklistChanges(0, 1);

        delete = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(2);
        checkAssignees(0, 2);
        checkAdds(0, 2);
        checkUpdates(0, 2);
        checkDeletes(0, 2, "TDP-3", "TDP-7");
        checkDeletes(1, 2, "TDP-7");
        checkNoBlacklistChanges(0, 2);

        delete = JirbanIssueEvent.createDeleteEvent("TBG-1", "TBG");
        boardManager.handleEvent(delete);
        checkViewId(3);
        checkAssignees(0, 3);
        checkAdds(0, 3);
        checkUpdates(0, 3);
        checkDeletes(0, 3, "TDP-3", "TDP-7", "TBG-1");
        checkDeletes(1, 3, "TDP-7", "TBG-1");
        checkDeletes(2, 3, "TBG-1");
        checkNoIssueChanges(3, 3);
        checkNoBlacklistChanges(0, 3);

    }

    @Test
    public void testCreateIssues() throws Exception {
        //Add an issue which does not bring in new assignees
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(1);
        checkAssignees(0, 1);
        checkDeletes(0, 1);
        checkUpdates(0, 1);
        checkAdds(0, 1, new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D"));
        checkNoBlacklistChanges(0, 1);

        //Now add an issue which brings in new assignees
        create = createCreateEventAndAddToRegistry("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X");
        boardManager.handleEvent(create);
        checkViewId(2);
        checkAssignees(0, 2, "jason");
        checkDeletes(0, 2);
        checkUpdates(0, 2);
        checkAdds(0, 2,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"));
        checkAssignees(1, 2, "jason");
        checkDeletes(1, 2);
        checkAdds(1, 2,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"));
        checkNoBlacklistChanges(0, 2);

        //Add another one not bringing in new assignees
        create = createCreateEventAndAddToRegistry("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(3);
        checkAssignees(0, 3, "jason");
        checkDeletes(0, 3);
        checkUpdates(0, 3);
        checkAdds(0, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkAssignees(1, 3, "jason");
        checkDeletes(1, 3);
        checkUpdates(1, 3);
        checkAdds(1, 3,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkAssignees(2, 3);
        checkDeletes(2, 3);
        checkUpdates(2, 3);
        checkAdds(2, 3,
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkNoBlacklistChanges(0, 3);
    }

    @Test
    public void testUpdateSameIssueNonAssignees() throws Exception {
        //Do a noop update
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(0);
        checkNoIssueChanges(0, 0);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, "Seven-1", null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        //Check assignees and deletes extra well here so we don't have to in the other tests
        checkAssignees(0, 1);
        checkDeletes(0, 1);
        checkUpdates(0, 1, new IssueData("TDP-7", null, null, "Seven-1", null, null));
        checkNoBlacklistChanges(0, 1);

        update = createUpdateEventAndAddToRegistry("TDP-7", IssueType.BUG, null, null, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkAssignees(0, 2);
        checkDeletes(0, 2);
        checkUpdates(0, 2, new IssueData("TDP-7", IssueType.BUG, null, "Seven-1", null, null));
        checkAssignees(1, 2);
        checkDeletes(1, 2);
        checkUpdates(1, 2, new IssueData("TDP-7", IssueType.BUG, null, null, null, null));
        checkNoBlacklistChanges(0, 2);

        update = createUpdateEventAndAddToRegistry("TDP-7", null, Priority.HIGHEST, null, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(3);
        checkAssignees(0, 3);
        checkDeletes(0, 3);
        checkUpdates(0, 3, new IssueData("TDP-7", IssueType.BUG, Priority.HIGHEST, "Seven-1", null, null));
        checkAssignees(1, 3);
        checkDeletes(1, 3);
        checkUpdates(1, 3, new IssueData("TDP-7", IssueType.BUG, Priority.HIGHEST, null, null, null));
        checkAssignees(2, 3);
        checkDeletes(2, 3);
        checkUpdates(2, 3, new IssueData("TDP-7", null, Priority.HIGHEST, null, null, null));
        checkNoBlacklistChanges(0, 3);

        //TODO States
    }


    @Test
    public void testSameIssueAssignees() throws Exception {
        //Do a noop update
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, "kabir", false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        checkAssignees(0, 1);
        checkUpdates(0, 1, new IssueData("TDP-7", null, null, null, "kabir", null));
        checkNoBlacklistChanges(0, 1);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, true, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkAssignees(0, 2);
        checkUpdates(0, 2, new IssueData("TDP-7", null, null, null, null, true, null));
        checkAssignees(1, 2);
        checkUpdates(1, 2, new IssueData("TDP-7", null, null, null, null, true, null));
        checkNoBlacklistChanges(0, 2);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, "jason", false, null, false);
        boardManager.handleEvent(update);
        checkViewId(3);
        checkAssignees(0, 3, "jason");
        checkUpdates(0, 3, new IssueData("TDP-7", null, null, null, "jason", false, null));
        checkAssignees(1, 3, "jason");
        checkUpdates(1, 3, new IssueData("TDP-7", null, null, null, "jason", false, null));
        checkAssignees(2, 3, "jason");
        checkUpdates(2, 3, new IssueData("TDP-7", null, null, null, "jason", false, null));
        checkNoBlacklistChanges(0, 3);


        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, "brian", false, null, false);
        boardManager.handleEvent(update);
        checkViewId(4);
        checkAssignees(0, 4);
        checkUpdates(0, 4, new IssueData("TDP-7", null, null, null, "brian", false, null));
        checkAssignees(1, 4);
        checkUpdates(1, 4, new IssueData("TDP-7", null, null, null, "brian", false, null));
        checkAssignees(2, 4);
        checkUpdates(2, 4, new IssueData("TDP-7", null, null, null, "brian", false, null));
        checkAssignees(3, 4);
        checkUpdates(3, 4, new IssueData("TDP-7", null, null, null, "brian", false, null));
        checkNoBlacklistChanges(0, 4);
    }


    @Test
    public void testUpdateSeveralIssues() throws Exception {
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, "Seven-1", null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        checkUpdates(0, 1, new IssueData("TDP-7", null, null, "Seven-1", null, null));
        checkNoBlacklistChanges(0, 1);

        update = createUpdateEventAndAddToRegistry("TBG-3", IssueType.BUG, null, null, "kabir", false, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkUpdates(0, 2,
                new IssueData("TDP-7", null, null, "Seven-1", null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkNoBlacklistChanges(0, 2);

        //Create, update and delete one to make sure that does not affect the others
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(3);
        checkUpdates(0, 3,
                new IssueData("TDP-7", null, null, "Seven-1", null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(0, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkUpdates(1, 3,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(1, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkUpdates(2, 3);
        checkAdds(2, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, "TDP-D"));
        checkNoBlacklistChanges(0, 3);

        //This should appear as an add for change sets including its previous create, and an update for change
        //sets not including the create
        update = createUpdateEventAndAddToRegistry("TDP-8", (IssueType) null, null, null, "jason", false, "TDP-C", false);
        boardManager.handleEvent(update);
        checkViewId(4);
        checkAssignees(0, 4, "jason");
        checkUpdates(0, 4,
                new IssueData("TDP-7", null, null, "Seven-1", null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(0, 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", "TDP-C"));

        checkAssignees(1, 4, "jason");
        checkUpdates(1, 4,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(1, 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", "TDP-C"));

        checkAssignees(2, 4, "jason");
        checkUpdates(2, 4);
        checkAdds(2, 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", "TDP-C"));

        checkAssignees(3, 4, "jason");
        checkUpdates(3, 4,
                new IssueData("TDP-8", null, null, null, "jason", "TDP-C"));
        checkAdds(3, 4);
        checkNoBlacklistChanges(0, 4);


        //This will not appear in change sets including the create, it becomes a noop
        JirbanIssueEvent delete = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(5);
        checkAssignees(0, 5);
        checkUpdates(0, 5,
                new IssueData("TDP-7", null, null, "Seven-1", null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(0, 5);

        checkAssignees(1, 5);
        checkUpdates(1, 5,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null));
        checkAdds(1, 5);

        checkAssignees(2, 5);
        checkUpdates(2, 5);
        checkAdds(2, 5);

        checkAssignees(3, 5);
        checkUpdates(3, 5);
        checkAdds(3, 5);

        checkAssignees(4, 5);
        checkUpdates(4, 5);
        checkAdds(4, 5);
        checkNoBlacklistChanges(0, 5);

    }

    @Test
    public void testBlacklistBadCreatedState() throws SearchException {
        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-8", IssueType.FEATURE, Priority.HIGH, "Eight", null, "BadState");
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, new String[]{"BadState"}, null, null, new String[]{"TDP-8"}, null);

        event = createUpdateEventAndAddToRegistry("TDP-8", (IssueType) null, null, "Eight-1", null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-8", "NewBadType", null, null, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, new String[]{"BadState"}, null, null, null, new String[]{"TDP-8"});
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-8"});
    }

    @Test
    public void testBlacklistBadUpdatedState() throws SearchException {
        JirbanIssueEvent event = createUpdateEventAndAddToRegistry("TDP-7", (IssueType)null, null, null, null, false, "BadState", false);
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, new String[]{"BadState"}, null, null, new String[]{"TDP-7"}, null);

        event = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-7", "NewBadType", null, null, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, new String[]{"BadState"}, null, null, null, new String[]{"TDP-7"});
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-7"});
    }


    @Test
    public void testBlacklistBadCreatedIssueType() throws SearchException {
        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-8", "BadType", Priority.HIGH.name, "Eight", null, "TDP-C");
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, null, new String[]{"BadType"}, null, new String[]{"TDP-8"}, null);

        event = createUpdateEventAndAddToRegistry("TDP-8", (IssueType) null, null, "Eight-1", null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-8", "NewBadType", null, null, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, null, new String[]{"BadType"}, null, null, new String[]{"TDP-8"});
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-8"});
    }

    @Test
    public void testBlacklistBadUpdatedIssueType() throws SearchException {
        JirbanIssueEvent event = createUpdateEventAndAddToRegistry("TDP-7", "BadType", null, null, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, null, new String[]{"BadType"}, null, new String[]{"TDP-7"}, null);

        event = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-7", "NewBadType", null, null, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, null, new String[]{"BadType"}, null, null, new String[]{"TDP-7"});
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-7"});
    }

    @Test
    public void testBlacklistBadCreatedPriority() throws SearchException {
        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-8", IssueType.FEATURE.name, "BadPriority", "Eight", null, "TDP-C");
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, null, null, new String[]{"BadPriority"}, new String[]{"TDP-8"}, null);

        event = createUpdateEventAndAddToRegistry("TDP-8", null, (Priority)null, "Eight-1", null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-8", null, "NewBadPriority", null, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, null, null, new String[]{"BadPriority"}, null, new String[]{"TDP-8"});
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-8"});
    }

    @Test
    public void testBlacklistBadUpdatedPriority() throws SearchException {
        JirbanIssueEvent event = createUpdateEventAndAddToRegistry("TDP-7", null, "BadPriority", null, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1);
        checkNoIssueChanges(0, 1);
        checkBlacklist(0, 1, null, null, new String[]{"BadPriority"}, new String[]{"TDP-7"}, null);

        event = createUpdateEventAndAddToRegistry("TDP-7", null, (Priority)null, "Eight-1", null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = createUpdateEventAndAddToRegistry("TDP-7", null, "NewBadPriority", null, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1); //No change, it was blacklisted already

        event = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(event);
        checkViewId(2);
        checkNoIssueChanges(0, 2);
        checkBlacklist(0, 2, null, null, new String[]{"BadPriority"}, null, new String[]{"TDP-7"});
        checkNoIssueChanges(1, 2);
        checkBlacklist(1, 2, null, null, null, null, new String[]{"TDP-7"});
    }

    @Test
    public void testChangeIssueState() throws SearchException {

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
                    Assert.assertTrue(node.get("unassigned").asBoolean());
                } else {
                    Assert.assertFalse(node.has("unassigned"));
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

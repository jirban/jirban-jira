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
import static org.jirban.jira.impl.Constants.CUSTOM;
import static org.jirban.jira.impl.Constants.DELETE;
import static org.jirban.jira.impl.Constants.EMAIL;
import static org.jirban.jira.impl.Constants.ISSUES;
import static org.jirban.jira.impl.Constants.ISSUE_TYPES;
import static org.jirban.jira.impl.Constants.KEY;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.NEW;
import static org.jirban.jira.impl.Constants.PRIORITIES;
import static org.jirban.jira.impl.Constants.PRIORITY;
import static org.jirban.jira.impl.Constants.PROJECTS;
import static org.jirban.jira.impl.Constants.REMOVED_ISSUES;
import static org.jirban.jira.impl.Constants.STATES;
import static org.jirban.jira.impl.Constants.SUMMARY;
import static org.jirban.jira.impl.Constants.TYPE;
import static org.jirban.jira.impl.Constants.UNASSIGNED;
import static org.jirban.jira.impl.Constants.VALUE;
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

    private void setupInitialBoard(String cfgResource) throws Exception {
        setupInitialBoard(cfgResource, null);
    }

    private void setupInitialBoard(String cfgResource, AdditionalSetup additionalSetup) throws Exception {
        initializeMocks(cfgResource);
        setupIssues(additionalSetup);
    }


    @Before
    public void setupIssues() throws SearchException {
        setupIssues(null);
    }

    public void setupIssues(AdditionalSetup additionalSetup) throws SearchException {
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

        if (additionalSetup != null) {
            additionalSetup.setup();
        }

        checkViewId(0);
        checkNoIssueChanges(0, 0);
        checkNoBlacklistChanges(0, 0);
        checkNoStateChanges(0, 0);
    }

    @Test
    public void testFullRefreshOnTooHighView() throws Exception {
        String json = boardManager.getChangesJson(userManager.getUserByKey("kabir"), false, "TST", 1);
        ModelNode changes = ModelNode.fromJSONString(json);

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
        checkNoCustomFields(0, 1);
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
        checkNoCustomFields(0, 2);
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
        checkNoCustomFields(0, 3);
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
        checkNoCustomFields(0, 1);
        checkDeletes(0, 1);
        checkUpdates(0, 1);
        checkAdds(0, 1, new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D"));
        checkNoBlacklistChanges(0, 1);

        //Now add an issue which brings in new assignees
        create = createCreateEventAndAddToRegistry("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", null, "TBG-X");
        boardManager.handleEvent(create);
        checkViewId(2);
        checkAssignees(0, 2, "jason");
        checkComponents(0, 2);
        checkNoCustomFields(0, 2);
        checkDeletes(0, 2);
        checkUpdates(0, 2);
        checkAdds(0, 2,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", null, "TBG-X"));

        checkAssignees(1, 2, "jason");
        checkDeletes(1, 2);
        checkAdds(1, 2,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", null, "TBG-X"));
        checkNoBlacklistChanges(0, 2);


        //Add another one not bringing in new assignees
        create = createCreateEventAndAddToRegistry("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(3);
        checkAssignees(0, 3, "jason");
        checkComponents(0, 3);
        checkNoCustomFields(0, 3);
        checkDeletes(0, 3);
        checkUpdates(0, 3);
        checkAdds(0, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", null, "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));

        checkAssignees(1, 3, "jason");
        checkComponents(1, 3);
        checkNoCustomFields(1, 3);
        checkDeletes(1, 3);
        checkUpdates(1, 3);
        checkAdds(1, 3,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "jason", null, "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkAssignees(2, 3);
        checkComponents(2, 3);
        checkNoCustomFields(2, 3);
        checkDeletes(2, 3);
        checkUpdates(2, 3);
        checkAdds(2, 3,
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
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
        checkNoCustomFields(0, 1);
        checkDeletes(0, 1);
        checkUpdates(0, 1);
        checkAdds(0, 1, new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", new String[]{"C1", "C2"}, "TDP-D"));
        checkNoBlacklistChanges(0, 1);

        //Now add an issue which brings in new components
        create = createCreateEventAndAddToRegistry("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "kabir", new String[]{"C5", "C6"}, "TBG-X");
        boardManager.handleEvent(create);
        checkViewId(2);
        checkAssignees(0, 2);
        checkComponents(0, 2, "C5", "C6");
        checkNoCustomFields(0, 2);
        checkDeletes(0, 2);
        checkUpdates(0, 2);
        checkAdds(0, 2,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", new String[]{"C1", "C2"}, "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "kabir", new String[]{"C5", "C6"}, "TBG-X"));

        checkAssignees(1, 2);
        checkComponents(1, 2, "C5", "C6");
        checkNoCustomFields(1, 2);
        checkDeletes(1, 2);
        checkAdds(1, 2,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "kabir", new String[]{"C5", "C6"}, "TBG-X"));
        checkNoBlacklistChanges(0, 2);


        //Add another one not bringing in new components
        create = createCreateEventAndAddToRegistry("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(3);
        checkAssignees(0, 3);
        checkComponents(0, 3, "C5", "C6");
        checkNoCustomFields(0, 3);
        checkDeletes(0, 3);
        checkUpdates(0, 3);
        checkAdds(0, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", new String[]{"C1", "C2"}, "TDP-D"),
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "kabir", new String[]{"C5", "C6"}, "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));

        checkAssignees(1, 3);
        checkComponents(1, 3, "C5", "C6");
        checkNoCustomFields(1, 3);
        checkDeletes(1, 3);
        checkUpdates(1, 3);
        checkAdds(1, 3,
                new IssueData("TBG-4", IssueType.FEATURE, Priority.LOW, "Four", "kabir", new String[]{"C5", "C6"}, "TBG-X"),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkAssignees(2, 3);
        checkComponents(2, 3);
        checkNoCustomFields(2, 3);
        checkDeletes(2, 3);
        checkUpdates(2, 3);
        checkAdds(2, 3,
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
        checkNoBlacklistChanges(0, 3);
    }

    @Test
    public void testCreateIssuesCustomFields() throws Exception {
        setupInitialBoard("config/board-custom.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        //Create an issue which does not bring in custom fields
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(1);
        ModelNode changes = getChangesJson(0, 1);
        checkAssignees(changes);
        checkComponents(changes);
        checkNoCustomFields(changes);
        checkDeletes(changes);
        checkUpdates(changes);
        checkAdds(changes, new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D"));
        checkNoBlacklistChanges(changes);

        //Create an issue which brings in a custom field
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "jason");
        create = createCreateEventAndAddToRegistry("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", "kabir", null, "TDP-D", customFieldValues);
        boardManager.handleEvent(create);
        checkViewId(2);
        changes = getChangesJson(0, 2);
        checkAssignees(changes);
        checkComponents(changes);
        checkCustomFields(changes, new String[]{"jason"}, null);
        checkDeletes(changes);
        checkUpdates(changes);
        checkAdds(changes,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D",
                        NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), NoIssueCustomFieldChecker.DOCUMENTER));
        checkNoBlacklistChanges(changes);
        changes = getChangesJson(1, 2);
        checkAssignees(changes);
        checkComponents(changes);
        checkCustomFields(changes, new String[]{"jason"}, null);
        checkDeletes(changes);
        checkUpdates(changes);
        checkAdds(changes,
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), NoIssueCustomFieldChecker.DOCUMENTER));
        checkNoBlacklistChanges(changes);

        //Create an issue which brings in a custom field and reuses one of the existing ones
        customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "jason");
        customFieldValues.put(documenterId, "kabir");
        create = createCreateEventAndAddToRegistry("TDP-10", IssueType.BUG, Priority.HIGH, "Ten", "kabir", null, "TDP-D", customFieldValues);
        boardManager.handleEvent(create);
        checkViewId(3);
        changes = getChangesJson(0, 3);
        checkAssignees(changes);
        checkComponents(changes);
        checkCustomFields(changes, new String[]{"jason"}, new String[]{"kabir"});
        checkDeletes(changes);
        checkUpdates(changes);
        checkAdds(changes,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D",
                        NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), NoIssueCustomFieldChecker.DOCUMENTER),
                new IssueData("TDP-10", IssueType.BUG, Priority.HIGH, "Ten", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), new DocumenterChecker("kabir")));
        checkNoBlacklistChanges(changes);
        changes = getChangesJson(1, 3);
        checkAssignees(changes);
        checkComponents(changes);
        checkCustomFields(changes, new String[]{"jason"}, new String[]{"kabir"});
        checkDeletes(changes);
        checkUpdates(changes);
        checkAdds(changes,
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), NoIssueCustomFieldChecker.DOCUMENTER),
                new IssueData("TDP-10", IssueType.BUG, Priority.HIGH, "Ten", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), new DocumenterChecker("kabir")));
        checkNoBlacklistChanges(changes);
        changes = getChangesJson(2, 3);
        checkAssignees(changes);
        checkComponents(changes);
        checkCustomFields(changes, null, new String[]{"kabir"});
        checkDeletes(changes);
        checkUpdates(changes);
        checkAdds(changes,
                new IssueData("TDP-10", IssueType.BUG, Priority.HIGH, "Ten", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), new DocumenterChecker("kabir")));
        checkNoBlacklistChanges(changes);

        //Create an issue which brings in no custom fields
        create = createCreateEventAndAddToRegistry("TDP-11", IssueType.BUG, Priority.HIGH, "Eleven", "kabir", null, "TDP-D");
        boardManager.handleEvent(create);
        checkViewId(4);
        changes = getChangesJson(0, 4);
        checkAssignees(changes);
        checkComponents(changes);
        checkCustomFields(changes, new String[]{"jason"}, new String[]{"kabir"});
        checkDeletes(changes);
        checkUpdates(changes);
        checkAdds(changes,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-D",
                        NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER),
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), NoIssueCustomFieldChecker.DOCUMENTER),
                new IssueData("TDP-10", IssueType.BUG, Priority.HIGH, "Ten", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), new DocumenterChecker("kabir")),
                new IssueData("TDP-11", IssueType.BUG, Priority.HIGH, "Eleven", "kabir", null, "TDP-D",
                        NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER));
        checkNoBlacklistChanges(changes);
        changes = getChangesJson(1, 4);
        checkAssignees(changes);
        checkComponents(changes);
        checkCustomFields(changes, new String[]{"jason"}, new String[]{"kabir"});
        checkDeletes(changes);
        checkUpdates(changes);
        checkAdds(changes,
                new IssueData("TDP-9", IssueType.BUG, Priority.HIGH, "Nine", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), NoIssueCustomFieldChecker.DOCUMENTER),
                new IssueData("TDP-10", IssueType.BUG, Priority.HIGH, "Ten", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), new DocumenterChecker("kabir")),
                new IssueData("TDP-11", IssueType.BUG, Priority.HIGH, "Eleven", "kabir", null, "TDP-D",
                        NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER));
        checkNoBlacklistChanges(changes);
        changes = getChangesJson(2, 4);
        checkAssignees(changes);
        checkComponents(changes);
        checkCustomFields(changes, null, new String[]{"kabir"});
        checkDeletes(changes);
        checkUpdates(changes);
        checkAdds(changes,
                new IssueData("TDP-10", IssueType.BUG, Priority.HIGH, "Ten", "kabir", null, "TDP-D",
                        new TesterChecker("jason"), new DocumenterChecker("kabir")),
                new IssueData("TDP-11", IssueType.BUG, Priority.HIGH, "Eleven", "kabir", null, "TDP-D",
                        NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER));
        checkNoBlacklistChanges(changes);
        changes = getChangesJson(3, 4);
        checkAssignees(changes);
        checkComponents(changes);
        checkNoCustomFields(changes);
        checkDeletes(changes);
        checkUpdates(changes);
        checkAdds(changes,
                new IssueData("TDP-11", IssueType.BUG, Priority.HIGH, "Eleven", "kabir", null, "TDP-D",
                        NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER));
        checkNoBlacklistChanges(changes);
    }

    @Test
    public void testUpdateSameIssueNoNewData() throws Exception {
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
        checkNoCustomFields(0, 1);
        checkDeletes(0, 1);
        checkUpdates(0, 1, new IssueData("TDP-7", null, null, "Seven-1", null, null, null));
        checkNoBlacklistChanges(0, 1);
        checkNoStateChanges(0, 1);


        update = createUpdateEventAndAddToRegistry("TDP-7", IssueType.BUG, null, null, null, false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkAssignees(0, 2);
        checkComponents(0, 2);
        checkNoCustomFields(0, 2);
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
        checkNoCustomFields(0, 3);
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
    public void testUpdateSameIssueAssignees() throws Exception {
        //Do an update not bringing in any new data
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, "kabir", false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        checkAssignees(0, 1);
        checkComponents(0, 1);
        checkNoCustomFields(0, 1);
        checkUpdates(0, 1, new IssueData("TDP-7", null, null, null, "kabir", null, null));
        checkNoBlacklistChanges(0, 1);
        checkNoStateChanges(0, 1);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, true, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkAssignees(0, 2);
        checkComponents(0, 2);
        checkNoCustomFields(0, 2);
        checkUpdates(0, 2, new IssueData("TDP-7", null, null, null, null, true, null, false, null));
        checkUpdates(1, 2, new IssueData("TDP-7", null, null, null, null, true, null, false, null));
        checkNoBlacklistChanges(0, 2);
        checkNoStateChanges(0, 2);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, "jason", false, null, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(3);
        checkAssignees(0, 3, "jason");
        checkComponents(0, 3);
        checkNoCustomFields(0, 3);
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
        checkAssignees(0, 4, "jason");
        checkComponents(0, 4);
        checkNoCustomFields(0, 4);
        checkUpdates(0, 4, new IssueData("TDP-7", null, null, null, "brian", false, null, false, null));
        checkAssignees(1, 4, "jason");
        checkUpdates(1, 4, new IssueData("TDP-7", null, null, null, "brian", false, null, false, null));
        checkAssignees(2, 4, "jason");
        checkUpdates(2, 4, new IssueData("TDP-7", null, null, null, "brian", false, null, false, null));
        checkAssignees(3, 4);
        checkUpdates(3, 4, new IssueData("TDP-7", null, null, null, "brian", false, null, false, null));
        checkNoBlacklistChanges(0, 4);
        checkNoStateChanges(0, 4);

    }

    @Test
    public void testUpdateSameIssueComponents() throws Exception {
        //Do an update not bringing in any new data
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false,
                new String[]{"C1"}, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(1);
        checkAssignees(0, 1);
        checkComponents(0, 1);
        checkNoCustomFields(0, 1);
        checkUpdates(0, 1, new IssueData("TDP-7", null, null, null, null, new String[]{"C1"}, null));
        checkNoBlacklistChanges(0, 1);
        checkNoStateChanges(0, 1);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false, null, true, null, false);
        boardManager.handleEvent(update);
        checkViewId(2);
        checkAssignees(0, 2);
        checkComponents(0, 2);
        checkNoCustomFields(0, 2);
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
        checkNoCustomFields(0, 3);
        checkUpdates(0, 3, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C-10"}, false, null));
        checkAssignees(1, 3);
        checkComponents(1, 3, "C-10");
        checkNoCustomFields(1, 3);
        checkUpdates(1, 3, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C-10"}, false, null));
        checkAssignees(2, 3);
        checkComponents(2, 3, "C-10");
        checkNoCustomFields(2, 3);
        checkUpdates(2, 3, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C-10"}, false, null));
        checkNoBlacklistChanges(0, 3);
        checkNoStateChanges(0, 3);

        update = createUpdateEventAndAddToRegistry("TDP-7", (IssueType) null, null, null, null, false, new String[]{"C1", "C2"}, false, null, false);
        boardManager.handleEvent(update);
        checkViewId(4);
        checkAssignees(0, 4);
        checkComponents(0, 4, "C-10");
        checkNoCustomFields(0, 4);
        checkUpdates(0, 4, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C1", "C2"}, false, null));
        checkAssignees(1, 4);
        checkComponents(1, 4, "C-10");
        checkNoCustomFields(1, 4);
        checkUpdates(1, 4, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C1", "C2"}, false, null));
        checkAssignees(2, 4);
        checkComponents(2, 4, "C-10");
        checkNoCustomFields(2, 4);
        checkUpdates(2, 4, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C1", "C2"}, false, null));
        checkAssignees(3, 4);
        checkComponents(3, 4);
        checkNoCustomFields(3, 4);
        checkUpdates(3, 4, new IssueData("TDP-7", null, null, null, null, false, new String[]{"C1", "C2"}, false, null));
        checkNoBlacklistChanges(0, 4);
        checkNoStateChanges(0, 4);
    }

    @Test
    public void testUpdateSameIssueCustomFields() throws Exception {
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;
        setupInitialBoard("config/board-custom.json", new AdditionalSetup() {
            @Override
            public void setup() {
                //Make sure that 'kabir' is in the list of custom fields
                issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("brian"));
                issueRegistry.setCustomField("TDP-1", documenterId, userManager.getUserByKey("stuart"));
            }
        });

        //Do an update not bringing in any new data
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "brian");
        customFieldValues.put(documenterId, "stuart");
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-7",
                (IssueType) null, null, null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update);
        checkViewId(1);
        ModelNode changes = getChangesJson(0, 1);
        checkAssignees(changes);
        checkComponents(changes);
        checkNoCustomFields(changes);
        checkUpdates(changes, new IssueData("TDP-7", null, null, null, null, null, null, new TesterChecker("brian"), new DocumenterChecker("stuart")));
        checkNoBlacklistChanges(changes);
        checkNoStateChanges(changes);

        //Clear one of the custom fields
        customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "");
        update = createUpdateEventAndAddToRegistry("TDP-7",
                (IssueType) null, null, null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update);
        checkViewId(2);
        changes = getChangesJson(0, 2);
        checkAssignees(changes);
        checkComponents(changes);
        checkNoCustomFields(changes);
        checkUpdates(changes, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker(null)));
        checkUpdates(1, 2, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker(null)));
        checkNoBlacklistChanges(changes);
        checkNoStateChanges(changes);

        //Clear the other custom field
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, "");
        update = createUpdateEventAndAddToRegistry("TDP-7",
                (IssueType) null, null, null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update);
        checkViewId(3);
        changes = getChangesJson(0, 3);
        checkAssignees(changes);
        checkComponents(changes);
        checkNoCustomFields(changes);
        checkUpdates(changes, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker(null), new DocumenterChecker(null)));
        checkUpdates(1, 3, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker(null), new DocumenterChecker(null)));
        checkUpdates(2, 3, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new DocumenterChecker(null)));
        checkNoBlacklistChanges(changes);
        checkNoStateChanges(changes);

        //Now add custom fields bringing in new data
        customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "james");
        customFieldValues.put(documenterId, "jason");
        update = createUpdateEventAndAddToRegistry("TDP-7",
                (IssueType) null, null, null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update);
        checkViewId(4);
        changes = getChangesJson(0, 4);
        checkAssignees(changes);
        checkComponents(changes);
        checkCustomFields(changes, new String[]{"james"}, new String[]{"jason"});
        checkUpdates(changes, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker("james"), new DocumenterChecker("jason")));
        checkUpdates(1, 4, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker("james"), new DocumenterChecker("jason")));
        checkUpdates(2, 4, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker("james"), new DocumenterChecker("jason")));
        checkUpdates(3, 4, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker("james"), new DocumenterChecker("jason")));
        checkNoBlacklistChanges(changes);
        checkNoStateChanges(changes);

        //Update other custom fields bringing in new data
        customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "jason");
        customFieldValues.put(documenterId, "james");
        update = createUpdateEventAndAddToRegistry("TDP-7",
                (IssueType) null, null, null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update);
        checkViewId(5);
        changes = getChangesJson(0, 5);
        checkAssignees(changes);
        checkComponents(changes);
        checkCustomFields(changes, new String[]{"james", "jason"}, new String[]{"james", "jason"});
        checkUpdates(changes, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker("jason"), new DocumenterChecker("james")));
        checkUpdates(1, 5, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker("jason"), new DocumenterChecker("james")));
        checkUpdates(2, 5, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker("jason"), new DocumenterChecker("james")));
        checkUpdates(3, 5, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker("jason"), new DocumenterChecker("james")));
        checkUpdates(4, 5, new IssueData("TDP-7", null, null, null, null, false, null, false, null, new TesterChecker("jason"), new DocumenterChecker("james")));
        checkNoBlacklistChanges(changes);
        checkNoStateChanges(changes);


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

        checkUpdates(1, 3,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkAdds(1, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));

        checkUpdates(2, 3);
        checkAdds(2, 3,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", null, null, "TDP-D"));
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

        checkAssignees(1, 4, "jason");
        checkUpdates(1, 4,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkAdds(1, 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", null, "TDP-C"));

        checkAssignees(2, 4, "jason");
        checkUpdates(2, 4);
        checkAdds(2, 4,
                new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Nine", "jason", null, "TDP-C"));

        checkAssignees(3, 4, "jason");
        checkUpdates(3, 4,
                new IssueData("TDP-8", null, null, null, "jason", null, "TDP-C"));
        checkAdds(3, 4);

        //This will not appear in change sets including the create, it becomes a noop
        JirbanIssueEvent delete = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(delete);
        checkViewId(5);
        checkAssignees(0, 5, "jason");
        checkUpdates(0, 5,
                new IssueData("TDP-7", null, null, "Seven-1", null, null, null),
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkAdds(0, 5);
        checkNoStateChanges(0, 5);


        checkAssignees(1, 5, "jason");
        checkUpdates(1, 5,
                new IssueData("TBG-3", IssueType.BUG, null, null, "kabir", null, null));
        checkAdds(1, 5);
        checkNoStateChanges(1, 5);

        checkAssignees(2, 5, "jason");
        checkUpdates(2, 5);
        checkAdds(2, 5);
        checkNoStateChanges(2, 5);

        checkAssignees(3, 5, "jason");
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
    public void testCreateAndDeleteIssueWithNewData() throws Exception {
        setupInitialBoard("config/board-custom.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "brian");
        customFieldValues.put(documenterId, "stuart");

        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-8",
                IssueType.BUG, Priority.HIGH, "Eight", "jason", new String[]{"C-X", "C-Y"}, "TDP-A", customFieldValues);
        boardManager.handleEvent(event);

        checkAdds(0, 1, new IssueData("TDP-8",
                IssueType.BUG, Priority.HIGH, "Eight", "jason", new String[]{"C-X", "C-Y"}, "TDP-A", new TesterChecker("brian"), new DocumenterChecker("stuart")));
        checkUpdates(0, 1);
        checkDeletes(0, 1);
        checkNoBlacklistChanges(0, 1);
        checkAssignees(0, 1, "jason");
        checkComponents(0, 1, new String[]{"C-X", "C-Y"});
        checkCustomFields(0, 1, new String[]{"brian"}, new String[]{"stuart"});


        event = JirbanIssueEvent.createDeleteEvent("TDP-8", "TDP");
        boardManager.handleEvent(event);

        //Although we have deleted the issue introducing the new assignee and components, we should still send those
        //down since they will exist on the server now, so if another issue changes to use those the clients will need a copy.
        checkAdds(0, 2);
        checkUpdates(0, 2);
        checkDeletes(0, 2);
        checkNoStateChanges(0, 2);
        checkNoBlacklistChanges(0, 2);
        checkAssignees(0, 2, "jason");
        checkComponents(0, 2, new String[]{"C-X", "C-Y"});
        checkCustomFields(0, 2, new String[]{"brian"}, new String[]{"stuart"});
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

    @Test
    public void testChangesToBackLogIssueWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        JirbanIssueEvent event = createUpdateEventAndAddToRegistry("TDP-1", null, "high", null, null, false, null, false, null, false);
        boardManager.handleEvent(event);
        checkViewId(1);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, Priority.HIGH, null, null, null, null));
        checkDeletes(backlogChanges);
        checkNoStateChanges(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);

        //Backlog not visible
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkNoIssueChanges(nonBacklogChanges);
        checkNoStateChanges(backlogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
    }

    @Test
    public void testCreateIssueInBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        //Do a create in the backlog
        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-A");
        boardManager.handleEvent(event);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges, new IssueData("TDP-8", IssueType.BUG, Priority.HIGH, "Eight", "kabir", null, "TDP-A"));
        checkUpdates(backlogChanges);
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);

        //Backlog not visible
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkNoIssueChanges(nonBacklogChanges);
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
    }

    @Test
    public void testMoveIssueFromBacklogToBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        JirbanIssueEvent create = createUpdateEventAndAddToRegistry("TDP-1", (String)null, null, null, null, false, null, false, "TDP-B", false);
        boardManager.handleEvent(create);
        checkViewId(1);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, null, null, null, null, "TDP-B"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);

        //Backlog not visible
        //An issue moved from the backlog to the non-backlog will appear as an add when the backlog is hidden
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkNoIssueChanges(nonBacklogChanges);
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
    }

    @Test
    public void testMoveIssueFromBacklogToNonBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        JirbanIssueEvent create = createUpdateEventAndAddToRegistry("TDP-2", (String)null, null, null, null, false, null, false, "TDP-C", false);
        boardManager.handleEvent(create);
        checkViewId(1);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-2", null, null, null, null, null, "TDP-C"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);

        //Backlog not visible
        //An issue moved from the backlog to the non-backlog will appear as an add when the backlog is hidden
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkAdds(nonBacklogChanges, new IssueData("TDP-2", IssueType.TASK, Priority.HIGH, "Two", "kabir", new String[]{"C2"}, "TDP-C"));
        checkDeletes(nonBacklogChanges);
        checkUpdates(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
    }

    @Test
    public void testMoveIssueFromNonBacklogToBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        JirbanIssueEvent create = createUpdateEventAndAddToRegistry("TDP-3", (String)null, null, null, null, false, null, false, "TDP-B", false);
        boardManager.handleEvent(create);
        checkViewId(1);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-3", null, null, null, null, null, "TDP-B"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);

        //Backlog not visible
        //An issue moved from the non-backlog to the backlog will appear as a delete when the backlog is hidden
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkAdds(nonBacklogChanges);
        checkDeletes(0, 1, "TDP-3");
        checkUpdates(nonBacklogChanges);
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
    }

    @Test
    public void testMoveIssueFromNonBacklogToNonBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        JirbanIssueEvent create = createUpdateEventAndAddToRegistry("TDP-3", (String)null, null, null, null, false, null, false, "TDP-D", false);
        boardManager.handleEvent(create);
        checkViewId(1);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-3", null, null, null, null, null, "TDP-D"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);

        //Backlog not visible
        //An issue moved from the non-backlog to the non-backlog will behave normally
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-3", null, null, null, null, null, "TDP-D"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
    }

    @Test
    public void testBlacklistWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        JirbanIssueEvent create = createUpdateEventAndAddToRegistry("TDP-2", "Bad Type", "Bad Priority", null, null, false, null, false, null, false);
        boardManager.handleEvent(create);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkNoIssueChanges(backlogChanges);
        checkBlacklist(backlogChanges, null, new String[]{"Bad Type"}, new String[]{"Bad Priority"}, new String[]{"TDP-2"}, null);

        //Backlog invisible
        //Having something blacklisted is a configuration problem, so report this although this issue is in the backlog and not visible
        ModelNode nonBacklogChanges = getChangesJson(0, 1, true);
        checkNoIssueChanges(nonBacklogChanges);
        checkBlacklist(nonBacklogChanges, null, new String[]{"Bad Type"}, new String[]{"Bad Priority"}, new String[]{"TDP-2"}, null);
    }

    @Test
    public void testNewAssigneesForNewIssueInBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", IssueType.TASK, Priority.HIGH, "Eight", "jason", null, "TDP-A");
        boardManager.handleEvent(create);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges, new IssueData("TDP-8", IssueType.TASK, Priority.HIGH, "Eight", "jason", null, "TDP-A"));
        checkUpdates(backlogChanges);
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges, "jason");
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);

        //Backlog invisible
        //Although the issue is hidden, pull down the new assignee. This is needed, since e.g. another visible issue might be
        //created using that assignee, and the server has no record of which clients have which assignee.
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkNoIssueChanges(nonBacklogChanges);
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges, "jason");
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
    }

    @Test
    public void testNewAssigneesForUpdatedIssueInBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1", (String)null, null, null, "jason", false, null, false, "TDP-B", false);
        boardManager.handleEvent(update);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, null, null, "jason", null, "TDP-B"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges, "jason");
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);

        //Backlog invisible
        //Although the issue is hidden, pull down the new assignee. This is needed, since e.g. another visible issue might be
        //created using that assignee, and the server has no record of which clients have which assignee.
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkNoIssueChanges(nonBacklogChanges);
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges, "jason");
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
    }

    @Test
    public void testNewComponentsForNewIssueInBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8", IssueType.TASK, Priority.HIGH, "Eight", "kabir", new String[]{"C-X", "C-Y"}, "TDP-A");
        boardManager.handleEvent(create);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges, new IssueData("TDP-8", IssueType.TASK, Priority.HIGH, "Eight", "kabir", new String[]{"C-X", "C-Y"}, "TDP-A"));
        checkUpdates(backlogChanges);
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges, new String[]{"C-X", "C-Y"});
        checkNoCustomFields(backlogChanges);

        //Backlog invisible
        //Although the issue is hidden, pull down the new component. This is needed, since e.g. another visible issue might be
        //created using that component, and the server has no record of which clients have which component.
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkNoIssueChanges(nonBacklogChanges);
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges, new String[]{"C-X", "C-Y"});
        checkNoCustomFields(nonBacklogChanges);
    }

    @Test
    public void testNewComponentsForUpdatedIssueInBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1", (String)null, null, null, null, false, new String[]{"C-X", "C-Y"}, false, "TDP-B", false);
        boardManager.handleEvent(update);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, null, null, null, new String[]{"C-X", "C-Y"}, "TDP-B"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges, new String[]{"C-X", "C-Y"});
        checkNoCustomFields(backlogChanges);

        //Backlog invisible
        //Although the issue is hidden, pull down the new component. This is needed, since e.g. another visible issue might be
        //created using that component, and the server has no record of which clients have which component.
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkNoIssueChanges(nonBacklogChanges);
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges, new String[]{"C-X", "C-Y"});
        checkNoCustomFields(nonBacklogChanges);
    }

    @Test
    public void testNewCustomFieldsForNewIssueInBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "kabir");
        customFieldValues.put(documenterId, "stuart");

        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-8",
                IssueType.TASK, Priority.HIGH, "Eight", "kabir", null, "TDP-A", customFieldValues);
        boardManager.handleEvent(create);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges, new IssueData("TDP-8", IssueType.TASK, Priority.HIGH, "Eight", "kabir", null, "TDP-A", new TesterChecker("kabir")));
        checkUpdates(backlogChanges);
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkCustomFields(backlogChanges, new String[]{"kabir"}, new String[]{"stuart"});

        //Backlog invisible
        //Although the issue is hidden, pull down the new assignee. This is needed, since e.g. another visible issue might be
        //created using that assignee, and the server has no record of which clients have which assignee.
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkNoIssueChanges(nonBacklogChanges);
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkCustomFields(nonBacklogChanges, new String[]{"kabir"}, new String[]{"stuart"});
    }

    @Test
    public void testNewCustomFieldsForUpdatedIssueInBacklogWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "kabir");
        customFieldValues.put(documenterId, "stuart");

        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1",
                (String)null, null, null, null, false, null, false, "TDP-B", false, customFieldValues);
        boardManager.handleEvent(update);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, null, null, null, null, "TDP-B", new TesterChecker("kabir"), new DocumenterChecker("stuart")));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkCustomFields(backlogChanges, new String[]{"kabir"}, new String[]{"stuart"});

        //Backlog invisible
        //Although the issue is hidden, pull down the new assignee. This is needed, since e.g. another visible issue might be
        //created using that assignee, and the server has no record of which clients have which assignee.
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        checkNoIssueChanges(nonBacklogChanges);
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkCustomFields(nonBacklogChanges, new String[]{"kabir"}, new String[]{"stuart"});
    }

    @Test
    public void testIssueMovedThroughSeveralStatesWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        setupInitialBoard("config/board-tdp-backlog.json");

        //Move to a non-backlog state
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1", (String)null, null, null, null, false, null, false, "TDP-C", false);
        boardManager.handleEvent(update);

        //Backlog visible
        ModelNode backlogChanges = getChangesJson(0, 1, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, null, null, null, null, "TDP-C"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);

        //Backlog invisible
        ModelNode nonBacklogChanges = getChangesJson(0, 1);
        //The move from the backlog to a nornal state appears as an add
        checkAdds(nonBacklogChanges, new IssueData("TDP-1", IssueType.TASK, Priority.HIGHEST, "One", "kabir", new String[]{"C1"}, "TDP-C"));
        checkUpdates(nonBacklogChanges);
        checkDeletes(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);

        //Move to another non-backlog state
        update = createUpdateEventAndAddToRegistry("TDP-1", (String)null, null, null, null, false, null, false, "TDP-D", false);
        boardManager.handleEvent(update);

        //Backlog visible
        backlogChanges = getChangesJson(0, 2, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, null, null, null, null, "TDP-D"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);
        backlogChanges = getChangesJson(1, 2, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, null, null, null, null, "TDP-D"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);

        //Backlog invisible
        nonBacklogChanges = getChangesJson(0, 2);
        //The move from the backlog to a nornal state appears as an add
        checkAdds(nonBacklogChanges, new IssueData("TDP-1", IssueType.TASK, Priority.HIGHEST, "One", "kabir", new String[]{"C1"}, "TDP-D"));
        checkUpdates(nonBacklogChanges);
        checkDeletes(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
        nonBacklogChanges = getChangesJson(1, 2);
        //From a non-backlog to a non-backlog state appears as an update
        checkAdds(nonBacklogChanges);
        checkUpdates(nonBacklogChanges, new IssueData("TDP-1", null, null, null, null, null, "TDP-D"));
        checkDeletes(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);

        //Move to a bl state
        update = createUpdateEventAndAddToRegistry("TDP-1", (String)null, null, null, null, false, null, false, "TDP-A", false);
        boardManager.handleEvent(update);

        //Backlog visible
        backlogChanges = getChangesJson(0, 3, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, null, null, null, null, "TDP-A"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);
        backlogChanges = getChangesJson(1, 3, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, null, null, null, null, "TDP-A"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);
        backlogChanges = getChangesJson(2, 3, true);
        checkAdds(backlogChanges);
        checkUpdates(backlogChanges, new IssueData("TDP-1", null, null, null, null, null, "TDP-A"));
        checkDeletes(backlogChanges);
        checkNoBlacklistChanges(backlogChanges);
        checkAssignees(backlogChanges);
        checkComponents(backlogChanges);
        checkNoCustomFields(backlogChanges);

        //Backlog invisible
        nonBacklogChanges = getChangesJson(0, 3);
        checkNoIssueChanges(nonBacklogChanges);
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
        nonBacklogChanges = getChangesJson(1, 3);
        //The non-blacklog->blacklog move appears as a delete
        checkAdds(nonBacklogChanges);
        checkUpdates(nonBacklogChanges);
        checkDeletes(nonBacklogChanges, "TDP-1");
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
        nonBacklogChanges = getChangesJson(2, 3);
        //The non-blacklog->blacklog move appears as a delete
        checkAdds(nonBacklogChanges);
        checkUpdates(nonBacklogChanges);
        checkDeletes(nonBacklogChanges, "TDP-1");
        checkNoStateChanges(nonBacklogChanges);
        checkNoBlacklistChanges(nonBacklogChanges);
        checkAssignees(nonBacklogChanges);
        checkComponents(nonBacklogChanges);
        checkNoCustomFields(nonBacklogChanges);
    }

    @Test
    public void testMoveIssueWithDoneStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with done states set up
        setupInitialBoard("config/board-tdp-done.json");

        //Move an issue into a done state should appear as a delete
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1", (String)null, null, null, null, false, null, false, "TDP-D", false);
        searchCallback.searched = false;
        boardManager.handleEvent(update);
        Assert.assertFalse(searchCallback.searched);

        ModelNode changes = getChangesJson(0, 1);
        checkAdds(changes);
        checkUpdates(changes);
        checkDeletes(changes, "TDP-1");
        checkNoStateChanges(changes);
        checkNoBlacklistChanges(changes);
        checkAssignees(changes);

        //Move an issue from a done state into a normal state will force a full refresh
        update = createUpdateEventAndAddToRegistry("TDP-4", (String)null, null, null, null, false, null, false, "TDP-A", false);
        searchCallback.searched = false;
        boardManager.handleEvent(update);
        Assert.assertTrue(searchCallback.searched);

        getChangesEnsuringFullRefresh(0);
    }

    @Test
    public void testMoveFromDoneToDoneWithDoneStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with done states set up
        setupInitialBoard("config/board-tdp-done.json");

        //Move an issue already in a done state into a done state should not appear as a change
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-3", (String)null, null, null, null, false, null, false, "TDP-D", false);
        searchCallback.searched = false;
        boardManager.handleEvent(update);
        Assert.assertFalse(searchCallback.searched);

        //No changes
        ModelNode changes = getChangesJson(0, 0);
        checkAdds(changes);
        checkUpdates(changes);
        checkDeletes(changes);
        checkNoStateChanges(changes);
        checkNoBlacklistChanges(changes);
        checkAssignees(changes);
        checkNoStateChanges(changes);
        checkNoBlacklistChanges(changes);
        checkAssignees(changes);
    }

    @Test
    public void testComplexMoveFromDoneResultingInCreateWithDoneStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with done states set up
        setupInitialBoard("config/board-tdp-done.json");

        //Moving a done issue to a non-done state should cause a full refresh
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-3", (String)null, null, null, null, false, null, false, "TDP-A", false);
        boardManager.handleEvent(update);
        getChangesEnsuringFullRefresh(0);

        //Moving the issue back to a done state should appear as a delete
        update = createUpdateEventAndAddToRegistry("TDP-3", (String)null, null, null, null, false, null, false, "TDP-D", false);
        boardManager.handleEvent(update);
        ModelNode changes = getChangesJson(0, 1);
        checkAdds(changes);
        checkUpdates(changes);
        checkDeletes(changes, "TDP-3");
        checkNoStateChanges(changes);
        checkNoBlacklistChanges(changes);
        checkAssignees(changes);


        //Moving the issue back to a non-done state should cause a full refresh
        update = createUpdateEventAndAddToRegistry("TDP-3", (String)null, null, null, null, false, null, false, "TDP-A", false);
        boardManager.handleEvent(update);
        getChangesEnsuringFullRefresh(0);
    }

    @Test
    public void testComplexMoveFromNonDoneResultingInCreateWithDoneStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with done states set up
        setupInitialBoard("config/board-tdp-done.json");

        //Moving a done issue to a done state should appear as a delete
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-2", (String)null, null, null, null, false, null, false, "TDP-D", false);
        boardManager.handleEvent(update);
        ModelNode changes = getChangesJson(0, 1);
        checkAdds(changes);
        checkUpdates(changes);
        checkDeletes(changes, "TDP-2");
        checkNoStateChanges(changes);
        checkNoBlacklistChanges(changes);
        checkAssignees(changes);

        //Moving the issue back to a non-done state should cause a full refresh
        update = createUpdateEventAndAddToRegistry("TDP-2", (String)null, null, null, null, false, null, false, "TDP-A", false);
        boardManager.handleEvent(update);
        getChangesEnsuringFullRefresh(0);


        //Moving the issue back to a done state should appear as a delete
        update = createUpdateEventAndAddToRegistry("TDP-2", (String)null, null, null, null, false, null, false, "TDP-C", false);
        boardManager.handleEvent(update);
        changes = getChangesJson(0, 1);
        checkAdds(changes);
        checkUpdates(changes);
        checkDeletes(changes, "TDP-2");
        checkNoStateChanges(changes);
        checkNoBlacklistChanges(changes);
        checkAssignees(changes);
    }

    private void checkNoIssueChanges(int fromView, int expectedView) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkNoIssueChanges(changesNode);
    }

    private void checkNoIssueChanges(ModelNode changesNode) throws SearchException {
        Assert.assertFalse(changesNode.hasDefined(CHANGES, ISSUES));
    }

    private void checkNoBlacklistChanges(int fromView, int expectedView) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkNoBlacklistChanges(changesNode);
    }

    private void checkNoBlacklistChanges(ModelNode changesNode) throws SearchException {
        Assert.assertFalse(changesNode.hasDefined(CHANGES, BLACKLIST));
    }

    private void checkNoStateChanges(int fromView, int expectedView) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkNoStateChanges(changesNode);
    }

    private void checkNoStateChanges(ModelNode changesNode) throws SearchException {
        Assert.assertFalse(changesNode.hasDefined(CHANGES, STATES));
    }

    private void checkDeletes(int fromView, int expectedView, String...expectedKeys) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkDeletes(changesNode, expectedKeys);
    }

    private void checkDeletes(ModelNode changesNode, String...expectedKeys) throws SearchException {
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
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkAdds(changesNode, expectedIssues);
    }

    private void checkAdds(ModelNode changesNode, IssueData...expectedIssues) throws SearchException {
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
                if (expected.customFieldCheckers.length == 0) {
                    Assert.assertFalse(node.hasDefined(CUSTOM));
                }
                for (IssueCustomFieldChecker checker : expected.customFieldCheckers) {
                    checker.check(node);
                }
            }
        }
    }

    private void checkUpdates(int fromView, int expectedView, IssueData...expectedIssues) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkUpdates(changesNode, expectedIssues);
    }

    private void checkUpdates(ModelNode changesNode, IssueData...expectedIssues) throws SearchException {

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

                if (expected.customFieldCheckers.length == 0) {
                    Assert.assertFalse(node.hasDefined(CUSTOM));
                }
                for (IssueCustomFieldChecker checker : expected.customFieldCheckers) {
                    checker.check(node);
                }
            }
        }
    }

    private void checkAssignees(int fromView, int expectedView, String...expectedAssignees) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkAssignees(changesNode, expectedAssignees);
    }

    private void checkAssignees(ModelNode changesNode, String...expectedAssignees) throws SearchException {
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


    private void checkNoCustomFields(int fromView, int expectedView) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkNoCustomFields(changesNode);
    }

    private void checkNoCustomFields(ModelNode changesNode) throws SearchException {
        Assert.assertEquals(1, changesNode.keys().size());
        Assert.assertFalse(changesNode.hasDefined(CHANGES, CUSTOM));
    }

    private void checkCustomFields(int fromView, int expectedView, String[] testers, String[] documenters) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkCustomFields(changesNode, testers, documenters);
    }

    private void checkCustomFields(ModelNode changesNode, String[] testers, String[] documenters) {
        Assert.assertEquals(1, changesNode.keys().size());
        ModelNode custom = changesNode.get(CHANGES, CUSTOM);
        Assert.assertTrue(custom.isDefined());

        checkCustomField(custom.get("Tester"), testers);
        checkCustomField(custom.get("Documenter"), documenters);
    }

    private void checkCustomField(ModelNode userNode, String[] expectedUsers) {
        if (expectedUsers == null) {
            Assert.assertFalse(userNode.isDefined());
            return;
        }
        Set<String> expected = new HashSet<>(Arrays.asList(expectedUsers));
        Assert.assertTrue(userNode.isDefined());
        List<ModelNode> users = userNode.asList();
        Assert.assertEquals(expected.size(), users.size());

        for (ModelNode user : users) {
            String key = user.get(KEY).asString();
            Assert.assertTrue(expected.remove(key));
            Assert.assertTrue(user.get(VALUE).asString().toLowerCase().startsWith(key));
        }
    }



    private void checkComponents(int fromView, int expectedView, String...expectedComponents) throws SearchException {
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkComponents(changesNode, expectedComponents);
    }

    private void checkComponents(ModelNode changesNode, String...expectedComponents) throws SearchException {
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
        ModelNode changesNode = getChangesJson(fromView, expectedView);
        checkBlacklist(changesNode, states, issueTypes, priorities, issueKeys, removedIssueKeys);
    }

    private void checkBlacklist(ModelNode changesNode, String[] states, String[] issueTypes, String[] priorities, String[] issueKeys, String[] removedIssueKeys) throws SearchException {
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
        String json = boardManager.getBoardJson(userManager.getUserByKey("kabir"), false, "TST");
        Assert.assertNotNull(json);
        ModelNode boardNode = ModelNode.fromJSONString(json);
        Assert.assertEquals(expectedViewId, boardNode.get(VIEW).asInt());
    }


    private ModelNode getChangesJson(int fromView, int expectedView) throws SearchException {
        return getChangesJson(fromView, expectedView, false);
    }

    private ModelNode getChangesJson(int fromView, int expectedView, boolean backlog) throws SearchException {
        String json = boardManager.getChangesJson(userManager.getUserByKey("kabir"), backlog, "TST", fromView);
        ModelNode changesNode = ModelNode.fromJSONString(json);
        Assert.assertEquals(expectedView, changesNode.get(CHANGES, VIEW).asInt());
        return changesNode;
    }

    private ModelNode getChangesEnsuringFullRefresh(int fromView) throws SearchException {
        String json = boardManager.getChangesJson(userManager.getUserByKey("kabir"), true, "TST", fromView);
        ModelNode fullRefreshNode = ModelNode.fromJSONString(json);
        Assert.assertEquals(0, fullRefreshNode.get(VIEW).asInt());

        //Make sure we have the top-level attributes at least
        Assert.assertTrue(fullRefreshNode.hasDefined(STATES));
        Assert.assertTrue(fullRefreshNode.hasDefined(PRIORITIES));
        Assert.assertTrue(fullRefreshNode.hasDefined(ISSUE_TYPES));
        Assert.assertTrue(fullRefreshNode.hasDefined(ASSIGNEES));
        Assert.assertTrue(fullRefreshNode.hasDefined(COMPONENTS));
        Assert.assertTrue(fullRefreshNode.hasDefined(PROJECTS));
        Assert.assertTrue(fullRefreshNode.hasDefined(ISSUES));

        return fullRefreshNode;
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
        private final IssueCustomFieldChecker[] customFieldCheckers;

        IssueData(String key, IssueType type, Priority priority, String summary, String assignee,
                  String[] components, String state, IssueCustomFieldChecker...customFieldCheckers) {
            this(key, type, priority, summary, assignee, false, components, false, state, customFieldCheckers);
        }

        IssueData(String key, IssueType type, Priority priority, String summary, String assignee,
                  boolean unassigned, String[] components, boolean clearedComponents, String state,
                  IssueCustomFieldChecker...customFieldCheckers) {
            this.key = key;
            this.type = type;
            this.priority = priority;
            this.summary = summary;
            this.assignee = assignee;
            this.unassigned = unassigned;
            this.state = state;
            this.components = components;
            this.clearedComponents = clearedComponents;
            this.customFieldCheckers = customFieldCheckers;
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

    static class DefaultIssueCustomFieldChecker implements IssueCustomFieldChecker {
        private final String fieldName;
        private final String key;

        public DefaultIssueCustomFieldChecker(String fieldName, String key) {
            this.fieldName = fieldName;
            this.key = key;
        }

        @Override
        public void check(ModelNode issue) {
            if (key != null) {
                Assert.assertTrue(issue.hasDefined(CUSTOM, fieldName));
                Assert.assertEquals(key, issue.get(CUSTOM, fieldName).asString());
            } else {
                Assert.assertTrue(issue.has(CUSTOM, fieldName));
                Assert.assertFalse(issue.hasDefined(CUSTOM, fieldName));
            }
        }
    }

    static class TesterChecker extends DefaultIssueCustomFieldChecker {
        public TesterChecker(String key) {
            super("Tester", key);
        }
    }

    static class DocumenterChecker extends DefaultIssueCustomFieldChecker {
        public DocumenterChecker(String key) {
            super("Documenter", key);
        }
    }

    static class ClearedCustomFieldsChecker implements IssueCustomFieldChecker {
        static final ClearedCustomFieldsChecker INSTANCE = new ClearedCustomFieldsChecker();

        @Override
        public void check(ModelNode issue) {
            Assert.assertFalse(issue.hasDefined(CUSTOM));
        }
    }

    interface AdditionalSetup {
        void setup();
    }
}

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

import static org.jirban.jira.impl.Constants.ASSIGNEE;
import static org.jirban.jira.impl.Constants.ASSIGNEES;
import static org.jirban.jira.impl.Constants.AVATAR;
import static org.jirban.jira.impl.Constants.BACKLOG;
import static org.jirban.jira.impl.Constants.BLACKLIST;
import static org.jirban.jira.impl.Constants.COMPONENTS;
import static org.jirban.jira.impl.Constants.CUSTOM;
import static org.jirban.jira.impl.Constants.DISPLAY;
import static org.jirban.jira.impl.Constants.DONE;
import static org.jirban.jira.impl.Constants.EMAIL;
import static org.jirban.jira.impl.Constants.FIX_VERSIONS;
import static org.jirban.jira.impl.Constants.ICON;
import static org.jirban.jira.impl.Constants.ISSUES;
import static org.jirban.jira.impl.Constants.ISSUE_TYPES;
import static org.jirban.jira.impl.Constants.KEY;
import static org.jirban.jira.impl.Constants.LABELS;
import static org.jirban.jira.impl.Constants.MAIN;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.OPTIONS;
import static org.jirban.jira.impl.Constants.PARALLEL_TASKS;
import static org.jirban.jira.impl.Constants.PRIORITIES;
import static org.jirban.jira.impl.Constants.PRIORITY;
import static org.jirban.jira.impl.Constants.PROJECTS;
import static org.jirban.jira.impl.Constants.RANK;
import static org.jirban.jira.impl.Constants.RANKED;
import static org.jirban.jira.impl.Constants.STATE;
import static org.jirban.jira.impl.Constants.STATES;
import static org.jirban.jira.impl.Constants.SUMMARY;
import static org.jirban.jira.impl.Constants.TYPE;
import static org.jirban.jira.impl.Constants.VALUE;
import static org.jirban.jira.impl.Constants.WIP;
import static org.jirban.jira.impl.board.CustomFieldValue.UNSET_VALUE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jirban.jira.impl.BoardManagerBuilder;
import org.jirban.jira.impl.JirbanIssueEvent;
import org.jirban.jira.impl.board.ProjectParallelTaskOptionsLoaderBuilder;
import org.junit.Assert;
import org.junit.Test;

import com.atlassian.jira.issue.search.SearchException;

import ut.org.jirban.jira.mock.PermissionManagerBuilder;

/**
 * Tests the layout of the board on the server, and how it is serialized to the client on first load/full refresh.
 * {@link BoardChangeRegistryTest} tests the output of what happens when changes are made to the board issues.
 * <p/>
 *
 * @author Kabir Khan
 */
public class BoardManagerTest extends AbstractBoardTest {

    @Test
    public void testStatesFields() throws Exception {
        ModelNode boardNode = getJson(0);
        //No 'special' states
        Assert.assertFalse(boardNode.hasDefined(BACKLOG));
        Assert.assertFalse(boardNode.hasDefined(DONE));

        initializeMocks("config/board-tdp-backlog.json");
        boardNode = getJson(0);
        //The first 2 states are 'backlog' states (they must always be at the start)
        Assert.assertEquals(2, boardNode.get(BACKLOG).asInt());
        Assert.assertFalse(boardNode.hasDefined(DONE));

        initializeMocks("config/board-tdp-done.json");
        boardNode = getJson(0);
        Assert.assertFalse(boardNode.hasDefined(BACKLOG));
        //The last 2 states are 'done' states (they must always be at the end)
        Assert.assertEquals(2, boardNode.get(DONE).asInt());

    }

    @Test
    public void testLoadBoardOnlyOwnerProjectIssues() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .assignee("kabir").components("C1").fixVersions("F1").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").components("C1").labels("L2").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").components("C1", "C2").labels("L1", "L2", "L3").fixVersions("F2", "F3").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "highest", "Five", "TDP-A")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "bug", "high", "Six", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "low", "Seven", "TDP-C")
                .buildAndRegister();

        ModelNode boardNode = getJson(0,
                new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"),
                new BoardLabelsChecker("L1", "L2", "L3"),
                new BoardFixVersionsChecker("F1", "F2", "F3"));
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1,
                new ComponentsChecker(0), new LabelsChecker(1), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2,
                new ComponentsChecker(0, 1), new LabelsChecker(0, 1, 2), new FixVersionsChecker(1, 2), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", 2);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG");
    }

    @Test
    public void testLoadBoardOnlyNonOwnerProjectIssues() throws Exception {
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").components("C1").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").components("C1", "C2").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "lowest", "Four", "TBG-Y")
                .assignee("jason").buildAndRegister();

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("jason", "kabir"),
                new BoardComponentsChecker("C1", "C2"));
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new ComponentsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new ComponentsChecker(0, 1), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, new AssigneeChecker(0));

        checkProjectRankedIssues(boardNode, "TDP");
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);
    }

    @Test
    public void testLoadBoard() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .assignee("kabir").components("C1").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").components("C2").labels("L1").fixVersions("F1").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").fixVersions("F1").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "TDP-D")
                .assignee("brian").labels("L2").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "highest", "Five", "TDP-A")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "bug", "high", "Six", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "low", "Seven", "TDP-C")
                .buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").components("C3").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").labels("L1", "L2").fixVersions("F1", "F2").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "lowest", "Four", "TBG-Y")
                .assignee("jason").buildAndRegister();

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("brian", "jason", "kabir"),
                new BoardComponentsChecker("C1", "C2", "C3"),
                new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 11);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new ComponentsChecker(0), new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1,
                new ComponentsChecker(1), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2,
                new FixVersionsChecker(0), new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3,
                new LabelsChecker(1), new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", 2);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new ComponentsChecker(2), new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1,
                new LabelsChecker(0, 1), new FixVersionsChecker(0, 1), new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, new AssigneeChecker(1));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);
    }

    @Test
    public void testLoadBoardWithWipLimits() throws Exception {
        //TODO this could be made into a better test covering more state/header/backlog/done/wip functionality
        initializeMocks("config/board-wip.json");
        ModelNode boardNode = getJson(0);

        ModelNode statesNode = boardNode.get(STATES);
        List<ModelNode> states = statesNode.asList();

        Assert.assertEquals(4, states.size());

        checkState(states.get(0), "S-A", 1);
        checkState(states.get(1), "S-B", 2);
        checkState(states.get(2), "S-C", null);
        checkState(states.get(3), "S-D", 10);

    }

    private void checkState(ModelNode state, String name, Integer wip) {
        Assert.assertEquals(name, state.get(NAME).asString());
        if (wip != null) {
            Assert.assertEquals(wip.intValue(), state.get(WIP).asInt());
        } else {
            Assert.assertFalse(state.get(WIP).isDefined());
        }
    }

    @Test
    public void testDeleteIssue() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "highest", "Five", "TDP-A")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "bug", "high", "Six", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "low", "Seven", "TDP-C")
                .buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "lowest", "Four", "TBG-Y")
                .assignee("jason").buildAndRegister();
        getJson(0, new BoardAssigneeChecker("brian", "jason", "kabir"));

        //Delete an issue in main project and check board
        JirbanIssueEvent delete = JirbanIssueEvent.createDeleteEvent("TDP-3", "TDP");
        boardManager.handleEvent(delete, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("brian", "jason", "kabir"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 10);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", 2);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, new AssigneeChecker(1));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

        //Delete an issue in main project and check board
        delete = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(delete, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "jason", "kabir"));

        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, new AssigneeChecker(1));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 4, 5, 6);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

        //Delete an issue in other project and check board
        delete = JirbanIssueEvent.createDeleteEvent("TBG-1", "TBG");
        boardManager.handleEvent(delete, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("brian", "jason", "kabir"));

        allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, new AssigneeChecker(1));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 4, 5, 6);
        checkProjectRankedIssues(boardNode, "TBG", 2, 3, 4);


        //Delete an issue in other project and check board
        delete = JirbanIssueEvent.createDeleteEvent("TBG-3", "TBG");
        boardManager.handleEvent(delete, nextRankedIssueUtil);
        boardNode = getJson(4, new BoardAssigneeChecker("brian", "jason", "kabir"));

        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, new AssigneeChecker(1));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 4, 5, 6);
        checkProjectRankedIssues(boardNode, "TBG", 2, 4);
    }

    @Test
    public void testAddIssuesNoNewUsersOrMultiSelectNameOnlyValues() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .components("C1").labels("L1").fixVersions("F1").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();

        getJson(0, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1"), new BoardLabelsChecker("L1"), new BoardFixVersionsChecker("F1"));

        JirbanIssueEvent create = createEventBuilder("TDP-5", IssueType.FEATURE, Priority.HIGH, "Five")
                .assignee("kabir")
                .components("C1")
                .state("TDP-B")
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1"), new BoardLabelsChecker("L1"), new BoardFixVersionsChecker("F1"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1, new ComponentsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);


        create = createEventBuilder("TBG-4", IssueType.FEATURE, Priority.HIGH, "Four")
                .components("C1")
                .state("TBG-X")
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1"), new BoardLabelsChecker("L1"), new BoardFixVersionsChecker("F1"));

        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1, new ComponentsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkIssue(allIssues, "TBG-4", IssueType.FEATURE, Priority.HIGH, "Four", 0, new ComponentsChecker(0));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

    }

    @Test
    public void testAddIssuesNewUsers() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();

        getJson(0, new BoardAssigneeChecker("brian", "kabir"));

        JirbanIssueEvent create = createEventBuilder("TDP-5", IssueType.FEATURE, Priority.HIGH, "Five")
                .assignee("james")
                .state("TDP-B")
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("brian", "james", "kabir"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);

        create = createEventBuilder("TBG-4", IssueType.FEATURE, Priority.HIGH, "Four")
                .assignee("stuart")
                .state("TBG-X")
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "james", "kabir", "stuart"));

        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkIssue(allIssues, "TBG-4", IssueType.FEATURE, Priority.HIGH, "Four", 0, new AssigneeChecker(3));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);
    }

    @Test
    public void testAddIssuesNewMultiSelectNameOnlyValues() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .components("CE", "CG").labels("LE", "LG").fixVersions("FE", "FG").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").components("CC").labels("LC").fixVersions("FC").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").components("CI").labels("LI").fixVersions("FI").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").components("CN").labels("LN").fixVersions("FN").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();

        getJson(0, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("CC", "CE", "CG", "CI", "CN"),
                new BoardLabelsChecker("LC", "LE", "LG", "LI", "LN"),
                new BoardFixVersionsChecker("FC", "FE", "FG", "FI", "FN"));

        //Add an issue with a single new component
        JirbanIssueEvent create = createEventBuilder("TDP-5", IssueType.FEATURE, Priority.HIGH, "Five")
                .assignee("brian")
                .components("CF")
                .state("TDP-B")
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("CC", "CE", "CF", "CG", "CI", "CN"),
                new BoardLabelsChecker("LC", "LE", "LG", "LI", "LN"),
                new BoardFixVersionsChecker("FC", "FE", "FG", "FI", "FN"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(1, 3), new LabelsChecker(1, 2), new FixVersionsChecker(1, 2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2,
                new ComponentsChecker(4), new LabelsChecker(3), new FixVersionsChecker(3), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1,
                new ComponentsChecker(2), new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(5), new LabelsChecker(4), new FixVersionsChecker(4), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);

        //Add an issue with a single new label
        create = createEventBuilder("TDP-6", IssueType.FEATURE, Priority.HIGH, "Five")
                .assignee("brian")
                .labels("LF")
                .state("TDP-B")
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("CC", "CE", "CF", "CG", "CI", "CN"),
                new BoardLabelsChecker("LC", "LE", "LF", "LG", "LI", "LN"),
                new BoardFixVersionsChecker("FC", "FE", "FG", "FI", "FN"));

        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(1, 3), new LabelsChecker(1, 3), new FixVersionsChecker(1, 2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2,
                new ComponentsChecker(4), new LabelsChecker(4), new FixVersionsChecker(3), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1,
                new ComponentsChecker(2), new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-6", IssueType.FEATURE, Priority.HIGH, "Five", 1,
                new LabelsChecker(2), new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(5), new LabelsChecker(5), new FixVersionsChecker(4), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);

        //Add an issue with a single new fix version
        create = createEventBuilder("TDP-7", IssueType.FEATURE, Priority.HIGH, "Five")
                .assignee("brian")
                .fixVersions("FF")
                .state("TDP-B")
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("CC", "CE", "CF", "CG", "CI", "CN"),
                new BoardLabelsChecker("LC", "LE", "LF", "LG", "LI", "LN"),
                new BoardFixVersionsChecker("FC", "FE", "FF", "FG", "FI", "FN"));

        allIssues = getIssuesCheckingSize(boardNode, 10);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(1, 3), new LabelsChecker(1, 3), new FixVersionsChecker(1, 3));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2,
                new ComponentsChecker(4), new LabelsChecker(4), new FixVersionsChecker(4), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1,
                new ComponentsChecker(2), new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-6", IssueType.FEATURE, Priority.HIGH, "Five", 1,
                new LabelsChecker(2), new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.HIGH, "Five", 1,
                new FixVersionsChecker(2), new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(5), new LabelsChecker(5), new FixVersionsChecker(5), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);

        //Add an issue with a several new components, labels and fix versions
        create = createEventBuilder("TBG-4", IssueType.FEATURE, Priority.HIGH, "Four")
                .assignee("brian")
                .components("CJ", "CK")
                .labels("LJ", "LK")
                .fixVersions("FJ", "FK")
                .state("TBG-X")
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(4, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("CC", "CE", "CF", "CG", "CI", "CJ", "CK", "CN"),
                new BoardLabelsChecker("LC", "LE", "LF", "LG", "LI", "LJ", "LK", "LN"),
                new BoardFixVersionsChecker("FC", "FE", "FF", "FG", "FI", "FJ", "FK", "FN"));

        allIssues = getIssuesCheckingSize(boardNode, 11);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(1, 3), new LabelsChecker(1, 3), new FixVersionsChecker(1, 3));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2,
                new ComponentsChecker(4), new LabelsChecker(4), new FixVersionsChecker(4), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1,
                new ComponentsChecker(2), new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-6", IssueType.FEATURE, Priority.HIGH, "Five", 1,
                new LabelsChecker(2), new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.HIGH, "Five", 1,
                new FixVersionsChecker(2), new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(7), new LabelsChecker(7), new FixVersionsChecker(7), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkIssue(allIssues, "TBG-4", IssueType.FEATURE, Priority.HIGH, "Four", 0,
                new ComponentsChecker(5, 6), new LabelsChecker(5, 6), new FixVersionsChecker(5, 6), new AssigneeChecker(0));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);
    }

    @Test
    public void testUpdateIssueNoNewUsersOrMultiSelectNameOnlyValues() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .components("C1").labels("L1").fixVersions("F1").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").components("C2").labels("L2").fixVersions("F2").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();
        getJson(0, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));


        //Update everything in one issue in one go
        JirbanIssueEvent update = updateEventBuilder("TDP-4")
                .issueType(IssueType.FEATURE)
                .priority(Priority.HIGH)
                .summary("Four-1")
                .assignee("kabir")
                .components("C1")
                .labels("L1")
                .fixVersions("F1")
                .state("TDP-B")
                .buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));


        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four-1", 1,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);

        //DO MORE UPDATES

        //Do updates of single fields, don't bother checking everything now. Just the issue tables and the changed issue
        //We will do a full check later

        //type
        update = updateEventBuilder("TDP-1")
                .issueType(IssueType.FEATURE)
                .buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.HIGHEST, "One", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0));

        //priority
        update = updateEventBuilder("TDP-1")
                .priority(Priority.LOW)
                .buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0));

        //summary
        update = updateEventBuilder("TDP-1")
                .summary("One-1")
                .buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(4, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0));

        //assign
        update = updateEventBuilder("TDP-1").assignee("brian").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(5, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(0));

        //No updated assignee, nor unassigned - and nothing else changed so the event is a noop and the view does not change
        update = updateEventBuilder("TDP-1").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(5, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(0));

        //Unassign
        update = updateEventBuilder("TDP-1").unassign().buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(6, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0));

        //Change state
        update = updateEventBuilder("TDP-1").state("TDP-D").rank().buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(7, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 3,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0));

        //Change component
        update = updateEventBuilder("TDP-1").components("C2").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(8, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 3,
                new ComponentsChecker(1), new LabelsChecker(0), new FixVersionsChecker(0));

        //Change label
        update = updateEventBuilder("TDP-1").labels("L2").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(9, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 3,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(0));

        //fix versions
        update = updateEventBuilder("TDP-1").fixVersions("F2").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(10, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 3,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1));

        //Change in the other project
        update = updateEventBuilder("TBG-3")
                .issueType(IssueType.BUG)
                .priority(Priority.HIGHEST)
                .summary("Three-1")
                .assignee("kabir")
                .components("C2")
                .labels("L2")
                .fixVersions("F2")
                .state("TBG-Y")
                .buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(11, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TBG-3", IssueType.BUG, Priority.HIGHEST, "Three-1", 1,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1), new AssigneeChecker(1));

        //Check full issue table
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 3,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four-1", 1,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.BUG, Priority.HIGHEST, "Three-1", 1,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1), new AssigneeChecker(1));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);
    }

    @Test
    public void testUpdateIssueNewUsers() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();

        getJson(0, new BoardAssigneeChecker("brian", "kabir"));

        JirbanIssueEvent update = updateEventBuilder("TDP-1").assignee("jason").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        getJson(1, new BoardAssigneeChecker("brian", "jason", "kabir"));

        update = updateEventBuilder("TBG-3").assignee("james").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        ModelNode boardNode = getJson(2, new BoardAssigneeChecker("brian", "james", "jason", "kabir"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(3));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(3));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(3));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(3));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, new AssigneeChecker(1));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);

    }

    @Test
    public void testUpdateIssueNewComponents() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .components("D").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").components("K").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();
        getJson(0, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("D", "K"));

        JirbanIssueEvent update = updateEventBuilder("TDP-1").components("E", "F").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("D", "E", "F", "K"));

        update = updateEventBuilder("TBG-3").components("L").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("D", "E", "F", "K", "L"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new ComponentsChecker(1, 2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new ComponentsChecker(3), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, new ComponentsChecker(4));

        //Clear the components from an issue
        update = updateEventBuilder("TDP-1").clearComponents().buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("D", "E", "F", "K", "L"));

        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new ComponentsChecker(3), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, new ComponentsChecker(4));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);
    }

    @Test
    public void testUpdateIssueNewLabels() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .labels("D").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").labels("K").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();
        getJson(0, new BoardAssigneeChecker("brian", "kabir"),
                new BoardLabelsChecker("D", "K"));

        JirbanIssueEvent update = updateEventBuilder("TDP-1").labels("E", "F").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        getJson(1, new BoardAssigneeChecker("brian", "kabir"),
                new BoardLabelsChecker("D", "E", "F", "K"));

        update = updateEventBuilder("TBG-3").labels("L").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        ModelNode boardNode = getJson(2, new BoardAssigneeChecker("brian", "kabir"),
                new BoardLabelsChecker("D", "E", "F", "K", "L"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new LabelsChecker(1, 2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new LabelsChecker(3), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, new LabelsChecker(4));

        //Clear the components from an issue
        update = updateEventBuilder("TDP-1").clearLabels().buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("brian", "kabir"),
                new BoardLabelsChecker("D", "E", "F", "K", "L"));

        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new LabelsChecker(3), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, new LabelsChecker(4));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);
    }


    @Test
    public void testUpdateIssueNewFixVersions() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .fixVersions("D").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").fixVersions("K").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();
        getJson(0, new BoardAssigneeChecker("brian", "kabir"),
                new BoardFixVersionsChecker("D", "K"));

        JirbanIssueEvent update = updateEventBuilder("TDP-1").fixVersions("E", "F").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        getJson(1, new BoardAssigneeChecker("brian", "kabir"),
                new BoardFixVersionsChecker("D", "E", "F", "K"));

        update = updateEventBuilder("TBG-3").fixVersions("L").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        ModelNode boardNode = getJson(2, new BoardAssigneeChecker("brian", "kabir"),
                new BoardFixVersionsChecker("D", "E", "F", "K", "L"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new FixVersionsChecker(1, 2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new FixVersionsChecker(3), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, new FixVersionsChecker(4));

        //Clear the components from an issue
        update = updateEventBuilder("TDP-1").clearFixVersions().buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("brian", "kabir"),
                new BoardFixVersionsChecker("D", "E", "F", "K", "L"));

        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new FixVersionsChecker(3), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, new FixVersionsChecker(4));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);
    }

    @Test
    public void testMissingState() throws SearchException {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "BAD")
                .buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "BAD")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "low", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().states("BAD").keys("TDP-1", "TBG-1"));
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Add another issue to the same bad state to check that this works on updating
        JirbanIssueEvent event = createEventBuilder("TDP-3", IssueType.TASK, Priority.HIGHEST, "Three")
                .state("BAD")
                .buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(1, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().states("BAD").keys("TDP-1", "TBG-1", "TDP-3"));
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Add another issue to another bad state
        event = createEventBuilder("TDP-4", IssueType.BUG, Priority.HIGH, "Four")
                .state("BADDER")
                .buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().states("BAD", "BADDER").keys("TDP-1", "TBG-1", "TDP-3", "TDP-4"));
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Move an issue from a bad state to a good state, this does not affect the blacklist which is ok since the config is broken anyway
        event = updateEventBuilder("TDP-4").state("TDP-A").buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        //Since the issue has been blacklisted the view id is the same
        getJson(2, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().states("BAD", "BADDER").keys("TDP-1", "TBG-1", "TDP-3", "TDP-4"));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Now delete a bad issue, this should work and remove it from the blacklist. We don't attempt to update the
        //bad configuration notices though so the bad state remains in the list
        event = JirbanIssueEvent.createDeleteEvent("TDP-4", "TDP");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().states("BAD", "BADDER").keys("TDP-1", "TBG-1", "TDP-3"));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
    }

    @Test
    public void testMissingIssueType() throws SearchException {
        issueRegistry.issueBuilder("TDP", "BAD", "highest", "One", "TDP-A")
                .buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "BAD", "highest", "One", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "low", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().types("BAD").keys("TDP-1", "TBG-1"));
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Add another issue to the same bad issue type to check that this works on updating
        JirbanIssueEvent event = createEventBuilder("TDP-3", "BAD", Priority.HIGHEST.name, "Three")
                .state("TDP-C")
                .buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(1, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().types("BAD").keys("TDP-1", "TBG-1", "TDP-3"));
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Add another issue to another bad issue type
        event = createEventBuilder("TDP-4", "BADDER", Priority.HIGH.name, "Four")
                .state("TDP-C")
                .buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().types("BAD", "BADDER").keys("TDP-1", "TBG-1", "TDP-3", "TDP-4"));
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Move an issue from a bad issue type to a good issue type, this does not affect the blacklist which is ok since the config is broken anyway
        event = updateEventBuilder("TDP-4").issueType(IssueType.TASK).buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        //Since the issue has been blacklisted the view id is the same
        getJson(2, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().types("BAD", "BADDER").keys("TDP-1", "TBG-1", "TDP-3", "TDP-4"));

        //Now delete a bad issue, this should work and remove it from the blacklist. We don't attempt to update the
        //bad configuration notices though so the bad issue type remains in the list
        event = JirbanIssueEvent.createDeleteEvent("TDP-4", "TDP");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().types("BAD", "BADDER").keys("TDP-1", "TBG-1", "TDP-3"));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
    }

    @Test
    public void testMissingPriority() throws SearchException {
        issueRegistry.issueBuilder("TDP", "feature", "BAD", "One", "TDP-A")
                .buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "BAD", "One", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "low", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().priorities("BAD").keys("TDP-1", "TBG-1"));
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Add another issue to the same bad priority to check that this works on updating
        JirbanIssueEvent event = createEventBuilder("TDP-3", IssueType.FEATURE.name, "BAD", "Three")
                .state("TDP-C")
                .buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(1, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().priorities("BAD").keys("TDP-1", "TBG-1", "TDP-3"));
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Add another issue to another bad priority
        event = createEventBuilder("TDP-4", IssueType.TASK.name, "BADDER", "Four")
                .state("TDP-C")
                .buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().priorities("BAD", "BADDER").keys("TDP-1", "TBG-1", "TDP-3", "TDP-4"));
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Move an issue from a bad priority to a good priority, this does not affect the blacklist which is ok since the config is broken anyway
        event = updateEventBuilder("TDP-4").priority(Priority.HIGH).buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        //Since the issue has been blacklisted the view id is the same
        getJson(2, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().priorities("BAD", "BADDER").keys("TDP-1", "TBG-1", "TDP-3", "TDP-4"));

        //Now delete a bad issue, this should work and remove it from the blacklist. We don't attempt to update the
        //bad configuration notices though so the bad priority remains in the list
        event = JirbanIssueEvent.createDeleteEvent("TDP-4", "TDP");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("kabir"),
                new BoardBlacklistChecker().priorities("BAD", "BADDER").keys("TDP-1", "TBG-1", "TDP-3"));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
    }

    @Test
    public void testLoadBoardWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        initializeMocks("config/board-tdp-backlog.json");

        issueRegistry.issueBuilder("TDP", "task", "high", "One", "TDP-A")
                .assignee("kabir").components("C1").labels("L1").buildAndRegister();      //1
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").components("C2").buildAndRegister();     //2
        issueRegistry.issueBuilder("TDP", "task", "high", "Three", "TDP-C")
                .assignee("brian").buildAndRegister();                      //3
        issueRegistry.issueBuilder("TDP", "task", "high", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();                      //4
        issueRegistry.issueBuilder("TBG", "task", "high", "One", "TBG-X")
                .assignee("kabir").components("C3").fixVersions("F1").buildAndRegister();     //1
        issueRegistry.issueBuilder("TBG", "task", "high", "Two", "TBG-Y")
                .assignee("brian").buildAndRegister();                      //2

        //Although not all the assignees and components, labels and fixVersions are used in the non-blacklist part of the board,
        //include them anyway
        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2", "C3"), new BoardLabelsChecker("L1"), new BoardFixVersionsChecker("F1"));
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 3);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.HIGH, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));

        checkProjectRankedIssues(boardNode, "TDP", 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 2);


        //Now check with the backlog
        boardNode = getJson(0, true, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1", "C2", "C3"), new BoardLabelsChecker("L1"), new BoardFixVersionsChecker("F1"));
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        allIssues = getIssuesCheckingSize(boardNode, 6);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new ComponentsChecker(1), new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.HIGH, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0,
                new ComponentsChecker(2), new FixVersionsChecker(0), new AssigneeChecker(1));
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2);

    }

    @Test
    public void testLoadBoardWithDoneStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with done states set up
        initializeMocks("config/board-tdp-done.json");

        issueRegistry.issueBuilder("TDP", "task", "high", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();      //1
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();      //2
        issueRegistry.issueBuilder("TDP", "task", "high", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();      //3
        issueRegistry.issueBuilder("TDP", "task", "high", "Four", "TDP-D")
                .assignee("brian").components("C1").labels("L1").fixVersions("F1").buildAndRegister(); //4
        issueRegistry.issueBuilder("TBG", "task", "high", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();                  //1
        issueRegistry.issueBuilder("TBG", "task", "high", "Two", "TBG-Y")
                .assignee("jason").components("C2").labels("L2").fixVersions("F2").buildAndRegister(); //2

        //Although the assignees and components used in the done part of the board should not be included, and neither
        //include them anyway
        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("kabir"));
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        //The issues in the 'done' columns should not be included in the board.
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 3);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //An event putting a 'done' issue into one of the normal states should result in the issue and any assignees/components being brought in

        //This one does not bring in any new assignees/components/labels/fix versions
        JirbanIssueEvent update = updateEventBuilder("TDP-3").state("TDP-A").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        //view id is 0 here because board has been recreated (due to moving issue out of 'done')
        boardNode = getJson(0, new BoardAssigneeChecker("kabir"));
        allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 0, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Bring in new assignees/components
        update = updateEventBuilder("TDP-4").state("TDP-A").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        //view id is 0 here because board has been recreated (due to moving issue out of 'done')
        boardNode = getJson(0, new BoardAssigneeChecker("brian", "kabir"),
                new BoardComponentsChecker("C1"), new BoardLabelsChecker("L1"), new BoardFixVersionsChecker("F1"));
        allIssues = getIssuesCheckingSize(boardNode, 5);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.HIGH, "Four", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1);


        update = updateEventBuilder("TBG-2").state("TBG-X").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        //view id is 0 here because board has been recreated (due to moving issue out of 'done')
        boardNode = getJson(0, new BoardAssigneeChecker("brian", "jason", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 6);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.HIGH, "Four", 0,
                new ComponentsChecker(0), new LabelsChecker(0), new FixVersionsChecker(0), new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", 0,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1), new AssigneeChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2);

        //Check moving an issue to a done state
        update = updateEventBuilder("TDP-4").state("TDP-C").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(1, new BoardAssigneeChecker("brian", "jason", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 5);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", 0,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1), new AssigneeChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2);

        update = updateEventBuilder("TBG-1").state("TBG-Y").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "jason", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", 0,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1), new AssigneeChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Check that moving an issue from a done state to another done state does not trigger a change
        update = updateEventBuilder("TDP-4").state("TDP-D").buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "jason", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", 0,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1), new AssigneeChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Test that updating an issue in a 'done' does not trigger a change
        update = updateEventBuilder("TDP-4")
                .issueType(IssueType.BUG)
                .priority(Priority.LOW)
                .components("C3")
                .labels("L3")
                .fixVersions("F3")
                .summary("Will be ignored")
                .assignee("nonexistent")
                .buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "jason", "kabir"),
                new BoardComponentsChecker("C1", "C2"), new BoardLabelsChecker("L1", "L2"), new BoardFixVersionsChecker("F1", "F2"));
        allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", 0,
                new ComponentsChecker(1), new LabelsChecker(1), new FixVersionsChecker(1), new AssigneeChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 2);
    }

    @Test
    public void testCanRankIssues() throws Exception {
        initializeMocks("config/board-tdp.json");
        checkRankIssuesPermissions(true);
    }

    @Test
    public void testCannotRankIssues() throws Exception {
        initializeMocks("config/board-tdp.json", new AdditionalBuilderInit() {
            @Override
            public void initialise(BoardManagerBuilder boardManagerBuilder) {
                boardManagerBuilder.setPermissionManager(PermissionManagerBuilder.getDeniessAll());
            }
        });
        checkRankIssuesPermissions(false);
    }

    private void checkRankIssuesPermissions(boolean allow) throws Exception {
        ModelNode boardNode = getJson(0);
        ModelNode projectParent = boardNode.get(PROJECTS, MAIN);
        for (String projectName : projectParent.keys()) {
            ModelNode rank = projectParent.get(projectName).get(RANK);
            Assert.assertTrue(rank.isDefined());
            Assert.assertEquals(allow, rank.asBoolean());
        }
    }

    @Test
    public void testLoadBoardWithCustomFields() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        initializeMocks("config/board-custom.json");
        final Long testerId = 121212121212L;


        issueRegistry.issueBuilder("TDP", "task", "high", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();      //1
        issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("jason"));
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();      //2
        issueRegistry.setCustomField("TDP-2", testerId, userManager.getUserByKey("brian"));
        issueRegistry.issueBuilder("TDP", "task", "high", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();      //3
        issueRegistry.issueBuilder("TBG", "task", "high", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();      //1

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "jason"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1);
    }

    @Test
    public void testAddIssuesNoNewCustomFieldData() throws Exception {
        initializeMocks("config/board-custom.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        issueRegistry.issueBuilder("TDP", "task", "high", "One", "TDP-A")
                .assignee("kabir").buildAndRegister(); //1
        issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("jason"));
        issueRegistry.setCustomField("TDP-1", documenterId, userManager.getUserByKey("jason"));
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister(); //2
        issueRegistry.setCustomField("TDP-2", testerId, userManager.getUserByKey("brian"));
        issueRegistry.setCustomField("TDP-2", documenterId, userManager.getUserByKey("brian"));
        issueRegistry.issueBuilder("TDP", "task", "high", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister(); //3
        issueRegistry.issueBuilder("TBG", "task", "high", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();  //1
        issueRegistry.setCustomField("TBG-1", testerId, userManager.getUserByKey("kabir"));
        issueRegistry.setCustomField("TBG-1", documenterId, userManager.getUserByKey("kabir"));

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "jason", "kabir"), new BoardDocumenterChecker("brian", "jason"/*, "kabir"*/)); //kabir is only for the TBG issue, which has no 'Documenters' configured

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(1), new DocumenterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(0), new DocumenterChecker(0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(2));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Add issues to main project
        //Add an issue
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, "jason");
        JirbanIssueEvent create = createEventBuilder("TDP-4", IssueType.FEATURE, Priority.HIGH, "Four")
                .assignee("kabir")
                .state("TDP-D")
                .customFieldValues(customFieldValues)
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(1, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "jason", "kabir"), new BoardDocumenterChecker("brian", "jason"/*, "kabir"*/));

        //Add another issue
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, "brian");
        customFieldValues.put(testerId, "brian");
        create = createEventBuilder("TDP-5", IssueType.FEATURE, Priority.HIGH, "Five")
                .assignee("kabir")
                .state("TDP-D")
                .customFieldValues(customFieldValues)
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "jason", "kabir"), new BoardDocumenterChecker("brian", "jason"/*, "kabir"*/));

        //And another....
        customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "jason");
        create = createEventBuilder("TDP-6", IssueType.FEATURE, Priority.HIGH, "Six")
                .assignee("kabir")
                .state("TDP-D")
                .customFieldValues(customFieldValues)
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "jason", "kabir"), new BoardDocumenterChecker("brian", "jason"/*, "kabir"*/));

        //Add issues to other project - this does NOT have the 'Documenter' custom field configured
        //Add an issue
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, "kabir");
        create = createEventBuilder("TBG-2", IssueType.FEATURE, Priority.HIGH, "Two")
                .assignee("kabir")
                .state("TBG-Y")
                .customFieldValues(customFieldValues)
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(4, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "jason", "kabir"), new BoardDocumenterChecker("brian", "jason"/*, "kabir"*/));

        //Add another issue
        customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "kabir");
        create = createEventBuilder("TBG-3", IssueType.FEATURE, Priority.HIGH, "Three")
                .assignee("kabir")
                .state("TBG-Y")
                .customFieldValues(customFieldValues)
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(5, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "jason", "kabir"), new BoardDocumenterChecker("brian", "jason"/*, "kabir"*/));


        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(1), new DocumenterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(0), new DocumenterChecker(0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", 3, new AssigneeChecker(0), new DocumenterChecker(1));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 3, new AssigneeChecker(0), new TesterChecker(0), new DocumenterChecker(0));
        checkIssue(allIssues, "TDP-6", IssueType.FEATURE, Priority.HIGH, "Six", 3, new AssigneeChecker(0), new TesterChecker(1));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.FEATURE, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.HIGH, "Three", 1, new AssigneeChecker(0), new TesterChecker(2));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);
    }

    @Test
    public void testAddIssuesNewCustomFieldData() throws Exception {
        initializeMocks("config/board-custom.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        issueRegistry.issueBuilder("TDP", "task", "high", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();      //1
        issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("jason"));
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();      //2
        issueRegistry.setCustomField("TDP-2", testerId, userManager.getUserByKey("jason"));
        issueRegistry.issueBuilder("TDP", "task", "high", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();      //3
        issueRegistry.issueBuilder("TBG", "task", "high", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();      //1

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("kabir"), new BoardTesterChecker("jason"));

        //Exactly the same initial board was checked in testLoadBoardWithCustomFields() so don't bother checking it all here

        //Create an issue in the main project with one custom field set
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "brian");
        JirbanIssueEvent create = createEventBuilder("TDP-4", IssueType.FEATURE, Priority.HIGH, "Four")
                .assignee("kabir")
                .state("TDP-D")
                .customFieldValues(customFieldValues)
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(1, new BoardAssigneeChecker("kabir"), new BoardTesterChecker("brian", "jason"));


        ModelNode allIssues = getIssuesCheckingSize(boardNode, 5);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", 3, new AssigneeChecker(0), new TesterChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1);


        //Create an issue in the main project with more custom fields set
        customFieldValues.put(testerId, "stuart");
        customFieldValues.put(documenterId, "kabir");
        create = createEventBuilder("TDP-5", IssueType.FEATURE, Priority.HIGH, "Five")
                .assignee("kabir")
                .state("TDP-A")
                .customFieldValues(customFieldValues)
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "jason", "stuart"), new BoardDocumenterChecker("kabir"));

        allIssues = getIssuesCheckingSize(boardNode, 6);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", 3, new AssigneeChecker(0), new TesterChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 0, new AssigneeChecker(0), new TesterChecker(2), new DocumenterChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Create an issue in the other project, which does NOT have the Documenter field configured for the board
        customFieldValues.put(testerId, "james");
        customFieldValues.put(documenterId, "james");
        create = createEventBuilder("TBG-2", IssueType.FEATURE, Priority.HIGH, "Two")
                .assignee("kabir")
                .state("TBG-Y")
                .customFieldValues(customFieldValues)
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "james", "jason", "stuart"), new BoardDocumenterChecker("kabir"));

        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", 3, new AssigneeChecker(0), new TesterChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 0, new AssigneeChecker(0), new TesterChecker(3), new DocumenterChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.FEATURE, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2);
    }


    @Test
    public void  testUpdateIssuesNoNewCustomFieldData() throws Exception {
        initializeMocks("config/board-custom.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        issueRegistry.issueBuilder("TDP", "task", "high", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();   //1
        issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("jason"));
        issueRegistry.setCustomField("TDP-1", documenterId, userManager.getUserByKey("jason"));
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();     //2
        issueRegistry.setCustomField("TDP-2", testerId, userManager.getUserByKey("kabir"));
        issueRegistry.setCustomField("TDP-2", documenterId, userManager.getUserByKey("kabir"));

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("jason", "kabir"), new BoardDocumenterChecker("jason", "kabir"));

        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "kabir");
        JirbanIssueEvent update = updateEventBuilder("TDP-1").customFieldValues(customFieldValues).buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(1, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("jason", "kabir"), new BoardDocumenterChecker("jason", "kabir"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(1), new DocumenterChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(1), new DocumenterChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);

        //Unset custom fields, this will not remove it from the lookup list of field values
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, UNSET_VALUE);
        update = updateEventBuilder("TDP-1").customFieldValues(customFieldValues).buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("jason", "kabir"), new BoardDocumenterChecker("jason", "kabir"));


        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(1), new DocumenterChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);

        //Clear all custom fields in an issue, they will stay in the lookup list of field values
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, UNSET_VALUE);
        customFieldValues.put(testerId, UNSET_VALUE);
        update = updateEventBuilder("TDP-2").customFieldValues(customFieldValues).buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("jason", "kabir"), new BoardDocumenterChecker("jason", "kabir"));

        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);

        customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "jason");
        update = updateEventBuilder("TDP-2").customFieldValues(customFieldValues).buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(4, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("jason", "kabir"), new BoardDocumenterChecker("jason", "kabir"));

        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);
    }

    @Test
    public void  testUpdateIssuesNewCustomFieldData() throws Exception {
        initializeMocks("config/board-custom.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        issueRegistry.issueBuilder("TDP", "task", "high", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();  //1
        issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("jason"));
        issueRegistry.setCustomField("TDP-1", documenterId, userManager.getUserByKey("jason"));
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();     //2
        issueRegistry.setCustomField("TDP-2", testerId, userManager.getUserByKey("kabir"));
        issueRegistry.setCustomField("TDP-2", documenterId, userManager.getUserByKey("kabir"));

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("jason", "kabir"), new BoardDocumenterChecker("jason", "kabir"));

        //Update and bring in a new tester, the unused 'jason' stays in the list
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "brian");
        JirbanIssueEvent update = updateEventBuilder("TDP-1").customFieldValues(customFieldValues).buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(1, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "jason", "kabir"), new BoardDocumenterChecker("jason", "kabir"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(0), new DocumenterChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(2), new DocumenterChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);

        //Update and bring in a new documenter, the unused 'jason' stays in the list
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, "brian");
        update = updateEventBuilder("TDP-1").customFieldValues(customFieldValues).buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("kabir"),
                new BoardTesterChecker("brian", "jason", "kabir"),
                new BoardDocumenterChecker("brian", "jason", "kabir"));

        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new TesterChecker(0), new DocumenterChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new TesterChecker(2), new DocumenterChecker(2));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);
    }

    @Test
    public void testLoadBoardWithParallelTasks() throws Exception {
        final Long upstreamId = 121212121212L;
        final Long downstreamId = 121212121213L;
        initializeMocks("config/board-parallel-tasks.json", new AdditionalBuilderInit() {
            @Override
            public void initialise(BoardManagerBuilder boardManagerBuilder) {
                boardManagerBuilder.setProjectParallelTaskOptionsLoader(
                        new ProjectParallelTaskOptionsLoaderBuilder()
                                .addCustomFieldOption("TDP", upstreamId, "NS", "Not Started")
                                .addCustomFieldOption("TDP", upstreamId, "IP", "In Progress")
                                .addCustomFieldOption("TDP", upstreamId, "M", "Merged")
                                .addCustomFieldOption("TDP", downstreamId, "TD", "TODO")
                                .addCustomFieldOption("TDP", downstreamId, "IP", "In Progress")
                                .addCustomFieldOption("TDP", downstreamId, "D", "Done")
                                .build()
                );
            }
        });


        issueRegistry.issueBuilder("TDP", "task", "high", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();      //1
        issueRegistry.setParallelTaskField("TDP-1", upstreamId, "IP");
        issueRegistry.setParallelTaskField("TDP-1", downstreamId, "IP");
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();      //2
        issueRegistry.setParallelTaskField("TDP-2", upstreamId, "M");
        issueRegistry.setParallelTaskField("TDP-2", downstreamId, "TD");
        issueRegistry.issueBuilder("TDP", "task", "high", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();      //3
        issueRegistry.issueBuilder("TBG", "task", "high", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();  //1

        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("kabir"));

        ModelNode parallelTasksNode = boardNode.get(PROJECTS, MAIN, "TDP", PARALLEL_TASKS);
        Assert.assertEquals(ModelType.LIST, parallelTasksNode.getType());
        List<ModelNode> parallelTasks = parallelTasksNode.asList();
        Assert.assertEquals(2, parallelTasks.size());
        checkParallelTaskFieldOptions(parallelTasks.get(0), "US", "Upstream", "Not Started", "In Progress", "Merged");
        checkParallelTaskFieldOptions(parallelTasks.get(1), "DS", "Downstream", "TODO", "In Progress", "Done");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(1, 1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1);
    }


    @Test
    public void testAddIssuesWithParallelTasks() throws Exception {
        final Long upstreamId = 121212121212L;
        final Long downstreamId = 121212121213L;
        initializeMocks("config/board-parallel-tasks.json", new AdditionalBuilderInit() {
            @Override
            public void initialise(BoardManagerBuilder boardManagerBuilder) {
                boardManagerBuilder.setProjectParallelTaskOptionsLoader(
                        new ProjectParallelTaskOptionsLoaderBuilder()
                                .addCustomFieldOption("TDP", upstreamId, "NS", "Not Started")
                                .addCustomFieldOption("TDP", upstreamId, "IP", "In Progress")
                                .addCustomFieldOption("TDP", upstreamId, "M", "Merged")
                                .addCustomFieldOption("TDP", downstreamId, "TD", "TODO")
                                .addCustomFieldOption("TDP", downstreamId, "IP", "In Progress")
                                .addCustomFieldOption("TDP", downstreamId, "D", "Done")
                                .build()
                );
            }
        });


        issueRegistry.issueBuilder("TDP", "task", "high", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();  //1
        issueRegistry.setParallelTaskField("TDP-1", upstreamId, "IP");
        issueRegistry.setParallelTaskField("TDP-1", downstreamId, "IP");
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();     //2
        issueRegistry.setParallelTaskField("TDP-2", upstreamId, "M");
        issueRegistry.setParallelTaskField("TDP-2", downstreamId, "TD");
        issueRegistry.issueBuilder("TDP", "task", "high", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister(); //3
        issueRegistry.issueBuilder("TBG", "task", "high", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();  //1

        getJson(0, new BoardAssigneeChecker("kabir"));
        //Layout of board is aleady checked by testLoadBoardWithParallelTasks

        //Add an issue with explicit parallel fields
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(upstreamId, "M");
        customFieldValues.put(downstreamId, "IP");
        JirbanIssueEvent create = createEventBuilder("TDP-4", IssueType.FEATURE, Priority.HIGH, "Four")
                .assignee("kabir")
                .state("TDP-D")
                .customFieldValues(customFieldValues)
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("kabir"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 5);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(1, 1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", 3, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(2, 1));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Add an issue with no parallel fields set, and make sure that they default to zero
        customFieldValues = new HashMap<>();
        create = createEventBuilder("TDP-5", IssueType.FEATURE, Priority.HIGH, "Five")
                .assignee("kabir")
                .state("TDP-D")
                .customFieldValues(customFieldValues)
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("kabir"));

        allIssues = getIssuesCheckingSize(boardNode, 6);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(1, 1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", 3, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(2, 1));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 3, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Add an issue in a project with no parallel fields and make sure that they are not set in the issue
        create = createEventBuilder("TBG-2", IssueType.FEATURE, Priority.HIGH,  "Two")
                .assignee("kabir")
                .state("TBG-X")
                .buildAndRegister();
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("kabir"));
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(1, 1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", 3, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(2, 1));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 3, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkIssue(allIssues, "TBG-2", IssueType.FEATURE, Priority.HIGH, "Two", 0, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2);


    }

    @Test
    public void testUpdateIssuesWithParallelTasks() throws Exception {
        final Long upstreamId = 121212121212L;
        final Long downstreamId = 121212121213L;
        initializeMocks("config/board-parallel-tasks.json", new AdditionalBuilderInit() {
            @Override
            public void initialise(BoardManagerBuilder boardManagerBuilder) {
                boardManagerBuilder.setProjectParallelTaskOptionsLoader(
                        new ProjectParallelTaskOptionsLoaderBuilder()
                                .addCustomFieldOption("TDP", upstreamId, "NS", "Not Started")
                                .addCustomFieldOption("TDP", upstreamId, "IP", "In Progress")
                                .addCustomFieldOption("TDP", upstreamId, "M", "Merged")
                                .addCustomFieldOption("TDP", downstreamId, "TD", "TODO")
                                .addCustomFieldOption("TDP", downstreamId, "IP", "In Progress")
                                .addCustomFieldOption("TDP", downstreamId, "D", "Done")
                                .build()
                );
            }
        });


        issueRegistry.issueBuilder("TDP", "task", "high", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();      //1
        issueRegistry.setParallelTaskField("TDP-1", upstreamId, "IP");
        issueRegistry.setParallelTaskField("TDP-1", downstreamId, "IP");
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();      //2
        issueRegistry.setParallelTaskField("TDP-2", upstreamId, "M");
        issueRegistry.setParallelTaskField("TDP-2", downstreamId, "TD");
        issueRegistry.issueBuilder("TDP", "task", "high", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();      //3
        issueRegistry.issueBuilder("TBG", "task", "high", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();;     //1


        getJson(0, new BoardAssigneeChecker("kabir"));
        //Layout of board is aleady checked by testLoadBoardWithParallelTasks

        //Update some of an issue's parallel fields
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(upstreamId, "M");
        JirbanIssueEvent update = updateEventBuilder("TDP-1").customFieldValues(customFieldValues).buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("kabir"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(2, 1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Update all the parallel fields in an issue
        customFieldValues = new HashMap<>();
        customFieldValues.put(upstreamId, "IP");
        customFieldValues.put(downstreamId, "D");
        update = updateEventBuilder("TDP-1").customFieldValues(customFieldValues).buildAndRegister();
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("kabir"));

        allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(1, 2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", 2, new AssigneeChecker(0), new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", 0, new AssigneeChecker(0));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1);

    }

    private void checkParallelTaskFieldOptions(ModelNode parallelTask, String code, String name, String...options) {
        Assert.assertEquals(code, parallelTask.get(DISPLAY).asString());
        Assert.assertEquals(name, parallelTask.get(NAME).asString());
        ModelNode optionsNode = parallelTask.get(OPTIONS);
        Assert.assertEquals(ModelType.LIST, optionsNode.getType());
        List<ModelNode> optionsList = optionsNode.asList();
        Assert.assertEquals(options.length, optionsList.size());
        for (int i = 0 ; i < options.length ; i++) {
            Assert.assertEquals(options[i], optionsList.get(i).asString());
        }
    }


    @Test
    public void testIrrelevantChange() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A").assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B").assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C").assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "TDP-D").assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "highest", "Five", "TDP-A").assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "bug", "high", "Six", "TDP-B").assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "low", "Seven", "TDP-C").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X").assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y").assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "lowest", "Four", "TBG-Y").assignee("jason").buildAndRegister();
        getJson(0, new BoardAssigneeChecker("brian", "jason", "kabir"));

        //Create an update event which doesn't change anything we are interested in and make sure the view id stays at zero
        JirbanIssueEvent event = updateEventBuilder("TDP-1").buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        ModelNode boardNode = getJson(0, new BoardAssigneeChecker("brian", "jason", "kabir"));

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 11);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", 2);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, new AssigneeChecker(1));

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

    }

    @Test
    public void testRankIssue() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "TDP-D")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "highest", "Five", "TDP-A")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "bug", "high", "Six", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "low", "Seven", "TDP-C")
                .buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "highest", "One", "TBG-X")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "bug", "high", "Two", "TBG-Y")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TBG", "feature", "low", "Three", "TBG-X")
                .buildAndRegister();
        issueRegistry.issueBuilder("TBG", "task", "lowest", "Four", "TBG-Y")
                .assignee("jason").buildAndRegister();
        getJson(0, new BoardAssigneeChecker("brian", "jason", "kabir"));

        //Rank an issue to somewhere in the middle in main project and check board
        issueRegistry.rerankIssue("TDP-1", "TDP-4");
        JirbanIssueEvent event = updateEventBuilder("TDP-1").rank().buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("brian", "jason", "kabir"));
        checkProjectRankedIssues(boardNode, "TDP", 2, 3, 1, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

        //Rank an issue to the start of the main project and check board
        issueRegistry.rerankIssue("TDP-1", "TDP-2");
        event = updateEventBuilder("TDP-1").rank().buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "jason", "kabir"));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3,  4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

        //Rank an issue to the end of the main project and check board
        issueRegistry.rerankIssue("TDP-1", null);
        event = updateEventBuilder("TDP-1").rank().buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("brian", "jason", "kabir"));
        checkProjectRankedIssues(boardNode, "TDP", 2, 3,  4, 5, 6, 7, 1);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

        //Rank an issue in the other project and check board
        issueRegistry.rerankIssue("TBG-2", "TBG-4");
        event = updateEventBuilder("TBG-2").rank().buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(4, new BoardAssigneeChecker("brian", "jason", "kabir"));
        checkProjectRankedIssues(boardNode, "TDP", 2, 3,  4, 5, 6, 7, 1);
        checkProjectRankedIssues(boardNode, "TBG", 1, 3, 2, 4);

        //Check that all the issue datas are as expected
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 11);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, new AssigneeChecker(0));
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", 2);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, new AssigneeChecker(2));
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, new AssigneeChecker(1));
    }

    @Test
    public void testRankIssueBeforeBlacklistedIssue() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "BAD")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "BAD")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "highest", "Five", "TDP-A")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "bug", "high", "Six", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "low", "Seven", "TDP-C")
                .buildAndRegister();
        getJson(0, new BoardAssigneeChecker("brian", "kabir"),
                new BoardBlacklistChecker().states("BAD").keys("TDP-3", "TDP-4"));

        //Rank an issue to before a blacklisted issue and check board
        issueRegistry.rerankIssue("TDP-1", "TDP-3");
        JirbanIssueEvent event = updateEventBuilder("TDP-1").rank().buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("brian", "kabir"),
                new BoardBlacklistChecker().states("BAD").keys("TDP-3", "TDP-4"));
        checkProjectRankedIssues(boardNode, "TDP", 2, 1, 5, 6, 7);

        //Try again, board should be the same
        issueRegistry.rerankIssue("TDP-1", "TDP-3");
        event = updateEventBuilder("TDP-1").rank().buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(2, new BoardAssigneeChecker("brian", "kabir"),
                new BoardBlacklistChecker().states("BAD").keys("TDP-3", "TDP-4"));
        checkProjectRankedIssues(boardNode, "TDP", 2, 1, 5, 6, 7);

        //Rank somewhere not blacklisted
        issueRegistry.rerankIssue("TDP-1", "TDP-7");
        event = updateEventBuilder("TDP-1").rank().buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJson(3, new BoardAssigneeChecker("brian", "kabir"),
                new BoardBlacklistChecker().states("BAD").keys("TDP-3", "TDP-4"));
        checkProjectRankedIssues(boardNode, "TDP", 2, 5, 6, 1, 7);

        //Check that all the issue datas are as expected
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 5);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", 2);
    }


    @Test
    public void testRankIssueBeforeBlacklistedIssueEnd() throws Exception {
        issueRegistry.issueBuilder("TDP", "task", "highest", "One", "TDP-A")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "high", "Two", "TDP-B")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "low", "Three", "TDP-C")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "lowest", "Four", "BAD")
                .assignee("brian").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "task", "highest", "Five", "BAD")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "bug", "high", "Six", "BAD")
                .assignee("kabir").buildAndRegister();
        issueRegistry.issueBuilder("TDP", "feature", "low", "Seven", "BAD")
                .buildAndRegister();
        getJson(0, new BoardAssigneeChecker("brian", "kabir"),
                new BoardBlacklistChecker().states("BAD").keys("TDP-4", "TDP-5", "TDP-6", "TDP-7"));

        //Rank an issue to before a blacklisted issue. There are only blacklisted issues left, so the issue should get inserted at the end
        issueRegistry.rerankIssue("TDP-1", "TDP-4");
        JirbanIssueEvent event = updateEventBuilder("TDP-1").rank().buildAndRegister();
        boardManager.handleEvent(event, nextRankedIssueUtil);
        ModelNode boardNode = getJson(1, new BoardAssigneeChecker("brian", "kabir"),
                new BoardBlacklistChecker().states("BAD").keys("TDP-4", "TDP-5", "TDP-6", "TDP-7"));
        checkProjectRankedIssues(boardNode, "TDP", 2, 3, 1);


        //Check that all the issue datas are as expected
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 3);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, new AssigneeChecker(1));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, new AssigneeChecker(1));
    }

    private ModelNode getJson(int expectedViewId, BoardDataChecker... checkers) throws SearchException {
        return getJson(expectedViewId, false, checkers);
    }

    private ModelNode getJson(int expectedViewId, boolean backlog, BoardDataChecker... checkers) throws SearchException {
        String json = boardManager.getBoardJson(userManager.getUserByKey("kabir"), backlog, "TST");
        Assert.assertNotNull(json);
        ModelNode boardNode = ModelNode.fromJSONString(json);
        Assert.assertEquals(expectedViewId, boardNode.get("view").asInt());

        Map<Class<? extends BoardDataChecker>, BoardDataChecker> checkerMap = new HashMap<>();
        checkerMap.put(BoardAssigneeChecker.class, BoardAssigneeChecker.NONE);
        checkerMap.put(BoardComponentsChecker.class, BoardComponentsChecker.NONE);
        checkerMap.put(BoardLabelsChecker.class, BoardLabelsChecker.NONE);
        checkerMap.put(BoardFixVersionsChecker.class, BoardFixVersionsChecker.NONE);
        checkerMap.put(BoardTesterChecker.class, BoardTesterChecker.NONE);
        checkerMap.put(BoardDocumenterChecker.class, BoardDocumenterChecker.NONE);
        checkerMap.put(BoardBlacklistChecker.class, BoardBlacklistChecker.NONE);
        boolean hasCustom = false;
        for (BoardDataChecker checker : checkers) {
            if (checker.getClass() == BoardTesterChecker.class && checker != BoardTesterChecker.NONE) {
                hasCustom = true;
            }
            checkerMap.put(checker.getClass(), checker);
        }
        if (!hasCustom) {
            Assert.assertFalse(boardNode.hasDefined(CUSTOM));
        }
        for (BoardDataChecker checker : checkerMap.values()) {
            checker.check(boardNode);
        }

        return boardNode;
    }

    private void checkProjectRankedIssues(ModelNode boardNode, String projectCode, int...issueNumbers) {
        Assert.assertTrue(boardNode.hasDefined(PROJECTS, MAIN, projectCode, RANKED));
        List<ModelNode> ranked = boardNode.get(PROJECTS, MAIN, projectCode, RANKED).asList();
        Assert.assertEquals(issueNumbers.length, ranked.size());
        for (int i = 0 ; i < issueNumbers.length ; i++) {
            Assert.assertEquals(projectCode + "-" + issueNumbers[i], ranked.get(i).asString());
        }
    }

    private void checkNameAndIcon(ModelNode board, String type, String...names) {
        List<ModelNode> entries = board.get(type).asList();
        Assert.assertEquals(entries.size(), names.length);
        for (int i = 0 ; i < names.length ; i++) {
            ModelNode entry = entries.get(i);
            Assert.assertEquals(names[i], entry.get("name").asString());
            Assert.assertEquals("/icons/" + type + "/" + names[i] + ".png", entry.get(ICON).asString());
        }
    }

    private ModelNode getIssuesCheckingSize(ModelNode board, int expectedLength) {
        ModelNode issues = board.get(ISSUES);
        Assert.assertEquals(expectedLength, issues.keys().size());
        return issues;
    }

    private void checkIssue(ModelNode issues, String key, IssueType type, Priority priority, String summary,
                            int state, IssueChecker... issueCheckers) {
        ModelNode issue = issues.get(key);
        Assert.assertNotNull(issue);
        Assert.assertEquals(key, issue.get(KEY).asString());
        Assert.assertEquals(type.index, issue.get(TYPE).asInt());
        Assert.assertEquals(priority.index, issue.get(PRIORITY).asInt());
        Assert.assertEquals(summary, issue.get(SUMMARY).asString());
        Assert.assertEquals(state, issue.get(STATE).asInt());

        //Assume that we should check for no data for each field, unless we have passed it in in the issueCheckers list
        Map<Class<? extends IssueChecker>, IssueChecker> checkerMap = new HashMap<>();
        checkerMap.put(AssigneeChecker.class, AssigneeChecker.NONE);
        checkerMap.put(ComponentsChecker.class, ComponentsChecker.NONE);
        checkerMap.put(LabelsChecker.class, LabelsChecker.NONE);
        checkerMap.put(FixVersionsChecker.class, FixVersionsChecker.NONE);
        checkerMap.put(TesterChecker.class, TesterChecker.NONE);
        checkerMap.put(DocumenterChecker.class, DocumenterChecker.NONE);
        checkerMap.put(ParallelTaskFieldValueChecker.class, ParallelTaskFieldValueChecker.NONE);
        for (IssueChecker checker : issueCheckers) {
            checkerMap.put(checker.getClass(), checker);
        }

        //Now run each checker
        for (IssueChecker checker : checkerMap.values()) {
            checker.check(issue);
        }
    }

    private static class DefaultIssueCustomFieldChecker implements IssueChecker {
        private final String fieldName;
        private final int id;

        DefaultIssueCustomFieldChecker(String fieldName, int id) {
            this.fieldName = fieldName;
            this.id = id;
        }

        @Override
        public void check(ModelNode issue) {
            if (id < 0) {
                Assert.assertFalse(issue.hasDefined(CUSTOM, fieldName));
            } else {
                Assert.assertTrue(issue.hasDefined(CUSTOM, fieldName));
                Assert.assertEquals(id, issue.get(CUSTOM, fieldName).asInt());
            }
        }
    }

    private static class TesterChecker extends DefaultIssueCustomFieldChecker {
        static final TesterChecker NONE = new TesterChecker(-1);

        TesterChecker(int testerId) {
            super("Tester", testerId);
        }
    }

    private static class DocumenterChecker extends DefaultIssueCustomFieldChecker {
        static final DocumenterChecker NONE = new DocumenterChecker(-1);

        DocumenterChecker(int documenterId) {
            super("Documenter", documenterId);
        }
    }

    private static class ParallelTaskFieldValueChecker implements IssueChecker {
        static final ParallelTaskFieldValueChecker NONE = new ParallelTaskFieldValueChecker(null);

        private int[] expected;

        ParallelTaskFieldValueChecker(int...expected) {
            this.expected = expected;
        }

        @Override
        public void check(ModelNode issue) {
            if (expected == null) {
                Assert.assertFalse(issue.hasDefined(PARALLEL_TASKS));
            } else {
                Assert.assertTrue(issue.hasDefined(PARALLEL_TASKS));
                List<ModelNode> values = issue.get(PARALLEL_TASKS).asList();
                Assert.assertEquals(expected.length, values.size());
                for (int i = 0 ; i < expected.length ; i++) {
                    Assert.assertEquals(expected[i], values.get(i).asInt());
                }
            }
        }
    }

    private static class AssigneeChecker implements IssueChecker {
        static final AssigneeChecker NONE = new AssigneeChecker(-1);
        private int expected;

        AssigneeChecker(int expected) {
            this.expected = expected;
        }

        @Override
        public void check(ModelNode issue) {
            if (expected < 0) {
                Assert.assertFalse(issue.hasDefined(ASSIGNEE));
            } else {
                Assert.assertEquals(expected, issue.get(ASSIGNEE).asInt());
            }

        }
    }

    private static abstract class MultiSelectNameOnlyChecker implements IssueChecker {
        private final String name;
        private int[] expected;

        MultiSelectNameOnlyChecker(String name, int[] expected) {
            this.name = name;
            this.expected = expected;
        }

        @Override
        public void check(ModelNode issue) {
            if (expected == null) {
                Assert.assertFalse(issue.hasDefined(name));
            } else {
                List<ModelNode> componentsNodes = issue.get(name).asList();
                Assert.assertEquals(expected.length, componentsNodes.size());
                for (int i = 0 ; i < expected.length ; i++) {
                    Assert.assertEquals(expected[i], componentsNodes.get(i).asInt());
                }
            }
        }
    }

    private static class ComponentsChecker extends MultiSelectNameOnlyChecker {
        static final ComponentsChecker NONE = new ComponentsChecker(null);

        ComponentsChecker(int...expected) {
            super(COMPONENTS, expected);
        }
    }

    private static class LabelsChecker extends MultiSelectNameOnlyChecker {
        static final LabelsChecker NONE = new LabelsChecker(null);

        LabelsChecker(int...expected) {
            super(LABELS, expected);
        }
    }

    private static class FixVersionsChecker extends MultiSelectNameOnlyChecker {
        static final FixVersionsChecker NONE = new FixVersionsChecker(null);

        FixVersionsChecker(int...expected) {
            super(FIX_VERSIONS, expected);
        }
    }

    private interface BoardDataChecker {
        void check(ModelNode board);
    }

    private static class BoardAssigneeChecker implements BoardDataChecker {
        static final BoardAssigneeChecker NONE = new BoardAssigneeChecker();
        private final String[] assignees;

        BoardAssigneeChecker(String...assignees) {
            this.assignees = assignees;
        }

        @Override
        public void check(ModelNode board) {
            List<ModelNode> assigneesList = board.get(ASSIGNEES).asList();
            Assert.assertEquals(assigneesList.size(), assignees.length);
            for (int i = 0 ; i < assignees.length ; i++) {
                ModelNode assignee = assigneesList.get(i);
                Assert.assertNotNull(assignee);
                Assert.assertEquals(assignees[i] + "@example.com", assignee.get(EMAIL).asString());
                Assert.assertEquals("/avatars/" + assignees[i] + ".png", assignee.get(AVATAR).asString());

                String displayName = assignee.get("name").toString().toLowerCase();
                Assert.assertTrue(displayName.length() > assignees[i].length());
                Assert.assertTrue(displayName.contains(assignees[i]));
            }
        }
    }

    private static abstract class BoardMultiSelectNameOnlyValueChecker implements BoardDataChecker {
        private final String name;
        private final String[] valueNames;

        BoardMultiSelectNameOnlyValueChecker(String name, String...valueNames) {
            this.name = name;
            this.valueNames = valueNames;
        }

        @Override
        public void check(ModelNode board) {
            if (valueNames.length == 0) {
                Assert.assertFalse(board.hasDefined(name));
            } else {
                List<ModelNode> values = board.get(name).asList();
                for (int i = 0; i < valueNames.length; i++) {
                    Assert.assertEquals(valueNames[i], values.get(i).asString());
                }
            }

        }
    }

    private static class BoardComponentsChecker extends BoardMultiSelectNameOnlyValueChecker {
        static final BoardComponentsChecker NONE = new BoardComponentsChecker();

        BoardComponentsChecker(String...componentNames) {
            super(COMPONENTS, componentNames);
        }
    }

    private static class BoardLabelsChecker extends BoardMultiSelectNameOnlyValueChecker {
        static final BoardLabelsChecker NONE = new BoardLabelsChecker();

        BoardLabelsChecker(String...labelNames) {
            super(LABELS, labelNames);
        }
    }

    private static class BoardFixVersionsChecker extends BoardMultiSelectNameOnlyValueChecker {
        static final BoardFixVersionsChecker NONE = new BoardFixVersionsChecker();

        BoardFixVersionsChecker(String... fixVersionNames) {
            super(FIX_VERSIONS, fixVersionNames);
        }
    }

    private static abstract class BoardCustomFieldChecker implements BoardDataChecker {
        private final String customFieldName;
        private final String[] keys;

        public BoardCustomFieldChecker(String customFieldName, String[] keys) {
            this.customFieldName = customFieldName;
            this.keys = keys;
        }

        @Override
        public void check(ModelNode board) {
            if (keys.length == 0) {
                Assert.assertFalse(board.hasDefined(CUSTOM, customFieldName));
            } else {

                List<ModelNode> fields = board.get(CUSTOM, customFieldName).asList();
                Assert.assertEquals(fields.size(), keys.length);
                for (int i = 0 ; i < keys.length ; i++) {
                    ModelNode field = fields.get(i);
                    Assert.assertNotNull(field);
                    Assert.assertEquals(keys[i], field.get(KEY).asString());
                    String displayName = field.get(VALUE).toString().toLowerCase();
                    Assert.assertTrue(displayName.length() > keys[i].length());
                    Assert.assertTrue(displayName.contains(keys[i]));
                }
            }

        }
    }

    private static class BoardTesterChecker extends BoardCustomFieldChecker {
        static final BoardTesterChecker NONE = new BoardTesterChecker();

        public BoardTesterChecker(String... keys) {
            super("Tester", keys);
        }
    }

    private static class BoardDocumenterChecker extends BoardCustomFieldChecker {
        static final BoardDocumenterChecker NONE = new BoardDocumenterChecker();

        public BoardDocumenterChecker(String... keys) {
            super("Documenter", keys);
        }
    }

    private static class BoardBlacklistChecker implements BoardDataChecker {
        static final BoardBlacklistChecker NONE = new BoardBlacklistChecker();
        String[] states;
        String[] issueTypes;
        String[] priorities;
        String[] issueKeys;

        BoardBlacklistChecker states(String... states) {
            this.states = states;
            return this;
        }

        BoardBlacklistChecker types(String... issueTypes) {
            this.issueTypes = issueTypes;
            return this;
        }

        BoardBlacklistChecker priorities(String... priorities) {
            this.priorities = priorities;
            return this;
        }

        BoardBlacklistChecker keys(String... issueKeys) {
            this.issueKeys = issueKeys;
            return this;
        }

        @Override
        public void check(ModelNode board) {
            String[] states = emptyIfNull(this.states);
            String[] issueTypes = emptyIfNull(this.issueTypes);
            String[] priorities = emptyIfNull(this.priorities);
            String[] issueKeys = emptyIfNull(this.issueKeys);

            if (states.length == 0 && issueTypes.length == 0 && priorities.length == 0 && issueKeys.length == 0) {
                Assert.assertFalse(board.has(BLACKLIST));
            } else {
                Assert.assertTrue(board.hasDefined(BLACKLIST));
                ModelNode blacklist = board.get(BLACKLIST);
                checkBlacklistEntry(blacklist, STATES, states);
                checkBlacklistEntry(blacklist, ISSUE_TYPES, issueTypes);
                checkBlacklistEntry(blacklist, PRIORITIES, priorities);
                checkBlacklistEntry(blacklist, ISSUES, issueKeys);
            }
        }

        private void checkBlacklistEntry(ModelNode blacklist, String key, String[] entries) {
            if (entries == null || entries.length == 0) {
                Assert.assertFalse(blacklist.hasDefined(key));
            } else {
                List<ModelNode> entryList = blacklist.get(key).asList();
                Assert.assertEquals(entries.length, entryList.size());
                Set<String> expectedSet = new HashSet<>(Arrays.asList(entries));
                for (ModelNode entry : entryList) {
                    Assert.assertTrue(expectedSet.contains(entry.asString()));
                }
            }
        }

    }
}

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
import static org.jirban.jira.impl.Constants.BACKLOG;
import static org.jirban.jira.impl.Constants.BLACKLIST;
import static org.jirban.jira.impl.Constants.COMPONENTS;
import static org.jirban.jira.impl.Constants.CUSTOM;
import static org.jirban.jira.impl.Constants.DISPLAY;
import static org.jirban.jira.impl.Constants.DONE;
import static org.jirban.jira.impl.Constants.EMAIL;
import static org.jirban.jira.impl.Constants.ICON;
import static org.jirban.jira.impl.Constants.ISSUES;
import static org.jirban.jira.impl.Constants.ISSUE_TYPES;
import static org.jirban.jira.impl.Constants.KEY;
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
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0);
        //No 'special' states
        Assert.assertFalse(boardNode.hasDefined(BACKLOG));
        Assert.assertFalse(boardNode.hasDefined(DONE));

        initializeMocks("config/board-tdp-backlog.json");
        boardNode = getJsonCheckingViewIdAndUsers(0);
        //The first 2 states are 'backlog' states (they must always be at the start)
        Assert.assertEquals(2, boardNode.get(BACKLOG).asInt());
        Assert.assertFalse(boardNode.hasDefined(DONE));

        initializeMocks("config/board-tdp-done.json");
        boardNode = getJsonCheckingViewIdAndUsers(0);
        Assert.assertFalse(boardNode.hasDefined(BACKLOG));
        //The last 2 states are 'done' states (they must always be at the end)
        Assert.assertEquals(2, boardNode.get(DONE).asInt());

    }

    @Test
    public void testLoadBoardOnlyOwnerProjectIssues() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", new String[]{"C1"}, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", new String[]{"C1"}, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", new String[]{"C1", "C2"}, "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, null, "TDP-C");

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "kabir");
        checkComponents(boardNode, "C1", "C2");
        checkNoBlacklist(boardNode);
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");
        checkNoCustomFields(boardNode);

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{0}, 0, 1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{0}, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", new int[]{0, 1}, 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", null, 0, 1);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", null, 1, 1);
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", null, 2, -1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG");
    }

    @Test
    public void testLoadBoardOnlyNonOwnerProjectIssues() throws Exception {
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", new String[]{"C1"}, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", new String[]{"C1", "C2"}, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");
        issueRegistry.addIssue("TBG", "task", "lowest", "Four", "jason", null, "TBG-Y");

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "jason", "kabir");
        checkComponents(boardNode, "C1", "C2");
        checkNoBlacklist(boardNode);
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");
        checkNoCustomFields(boardNode);

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{0}, 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", new int[]{0, 1}, 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -11);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", null, 1, 0);

        checkProjectRankedIssues(boardNode, "TDP");
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);
    }

    @Test
    public void testLoadBoard() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", new String[]{"C1"}, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", new String[]{"C2"}, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, null, "TDP-C");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", new String[]{"C3"}, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");
        issueRegistry.addIssue("TBG", "task", "lowest", "Four", "jason", null, "TBG-Y");

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "jason", "kabir");
        checkComponents(boardNode, "C1", "C2", "C3");
        checkNoBlacklist(boardNode);
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");
        checkNoCustomFields(boardNode);

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 11);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{0}, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{1}, 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", null, 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", null, 1, 2);
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", null, 2, -1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{2}, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", null, 1, 1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);
    }

    @Test
    public void testDeleteIssue() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, null, "TDP-C");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", null, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");
        issueRegistry.addIssue("TBG", "task", "lowest", "Four", "jason", null, "TBG-Y");
        getJsonCheckingViewIdAndUsers(0, "brian", "jason", "kabir");

        //Delete an issue in main project and check board
        JirbanIssueEvent delete = JirbanIssueEvent.createDeleteEvent("TDP-3", "TDP");
        boardManager.handleEvent(delete, nextRankedIssueUtil);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "jason", "kabir");
        checkNoBlacklist(boardNode);
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 10);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", null, 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", null, 1, 2);
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", null, 2, -1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", null, 1, 1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

        //Delete an issue in main project and check board
        delete = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(delete, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "jason", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", null, 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", null, 1, 2);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", null, 1, 1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 4, 5, 6);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

        //Delete an issue in other project and check board
        delete = JirbanIssueEvent.createDeleteEvent("TBG-1", "TBG");
        boardManager.handleEvent(delete, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "brian", "jason", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", null, 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", null, 1, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", null, 1, 1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 4, 5, 6);
        checkProjectRankedIssues(boardNode, "TBG", 2, 3, 4);


        //Delete an issue in other project and check board
        delete = JirbanIssueEvent.createDeleteEvent("TBG-3", "TBG");
        boardManager.handleEvent(delete, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(4, "brian", "jason", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", null, 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", null, 1, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", null, 1, 1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 4, 5, 6);
        checkProjectRankedIssues(boardNode, "TBG", 2, 4);
    }

    @Test
    public void testAddIssuesNoNewUsersOrComponents() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, new String[]{"C1"}, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", null, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "kabir");
        checkComponents(boardNode, "C1");

        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-5", IssueType.FEATURE, Priority.HIGH,
                "Five", "kabir", new String[]{"C1"}, "TDP-B");
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "kabir");
        checkComponents(boardNode, "C1");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        checkUsers(boardNode, "brian", "kabir");
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{0}, 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", new int[]{0}, 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);


        create = createCreateEventAndAddToRegistry("TBG-4", IssueType.FEATURE, Priority.HIGH,
                "Four", null, new String[]{"C1"}, "TBG-X");
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "kabir");
        checkComponents(boardNode, "C1");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        checkUsers(boardNode, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{0}, 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", new int[]{0}, 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.FEATURE, Priority.HIGH, "Four", new int[]{0}, 0, -1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

    }

    @Test
    public void testAddIssuesNewUsers() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", null, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");

        getJsonCheckingViewIdAndUsers(0, "brian", "kabir");

        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-5", IssueType.FEATURE, Priority.HIGH,
                "Five", "james", null, "TDP-B");
        boardManager.handleEvent(create, nextRankedIssueUtil);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "james", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", null, 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);

        create = createCreateEventAndAddToRegistry("TBG-4", IssueType.FEATURE, Priority.HIGH,
                "Four", "stuart", null, "TBG-X");
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "james", "kabir", "stuart");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        checkUsers(boardNode, "brian", "james", "kabir", "stuart");
        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", null, 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.FEATURE, Priority.HIGH, "Four", null, 0, 3);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);
    }

    @Test
    public void testAddIssuesNewComponents() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, new String[]{"E", "G"}, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", new String[]{"C"}, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", new String[]{"I"}, "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", new String[]{"N"}, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "kabir");
        checkComponents(boardNode, "C", "E", "G", "I", "N");
        checkNoCustomFields(boardNode);

        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-5", IssueType.FEATURE, Priority.HIGH,
                "Five", "brian", new String[]{"F"}, "TDP-B");
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "kabir");
        checkComponents(boardNode, "C", "E", "F", "G", "I", "N");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{1, 3}, 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{0}, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", new int[]{4}, 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", new int[]{2}, 1, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{5}, 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);


        create = createCreateEventAndAddToRegistry("TBG-4", IssueType.FEATURE, Priority.HIGH,
                "Four", "brian", new String[]{"J", "K"}, "TBG-X");
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "kabir");
        checkComponents(boardNode, "C", "E", "F", "G", "I", "J", "K", "N");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{1, 3}, 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{0}, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", new int[]{4}, 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", new int[]{2}, 1, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{7}, 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.FEATURE, Priority.HIGH, "Four", new int[]{5, 6}, 0, 0);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

    }

    @Test
    public void testUpdateIssueNoNewUsersOrComponents() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, new String[]{"C1"}, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", new String[]{"C2"}, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", null, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "kabir");
        checkComponents(boardNode, "C1", "C2");

        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-4", IssueType.FEATURE, Priority.HIGH,
                "Four-1", "kabir", false, new String[]{"C1"}, false, "TDP-B", true);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{0}, 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{1}, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four-1", new int[]{0}, 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);

        //Do updates of single fields, don't bother checking everything now. Just the issue tables and the changed issue
        //We will do a full check later

        //type
        update = createUpdateEventAndAddToRegistry("TDP-1", IssueType.FEATURE, null, null, null, false, null, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.HIGHEST, "One", new int[]{0}, 0, -1);

        //priority
        update = createUpdateEventAndAddToRegistry("TDP-1", null, Priority.LOW, null, null, false, null, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "brian", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One", new int[]{0}, 0, -1);

        //summary
        update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType) null, null, "One-1", null, false, null, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(4, "brian", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", new int[]{0}, 0, -1);

        //assign
        update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType) null, null, null, "brian", false, null, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(5, "brian", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", new int[]{0}, 0, 0);

        //No updated assignee, nor unassigned - and nothing else changed so the event is a noop and the view does not change
        update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType) null, null, null, null, false, null, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(5, "brian", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", new int[]{0}, 0, 0);

        //Unassign
        update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType) null, null, null, null, true, null, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(6, "brian", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", new int[]{0}, 0, -1);

        //Change state
        update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType) null, null, null, null, false, null, false, "TDP-D", true);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(7, "brian", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", new int[]{0}, 3, -1);

        //Change component
        update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType) null, null, null, null, false, new String[]{"C2"}, false, "TDP-D", true);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(8, "brian", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", new int[]{1}, 3, -1);

        //Change in the other project
        update = createUpdateEventAndAddToRegistry("TBG-3", IssueType.BUG, Priority.HIGHEST, "Three-1", "kabir", false, new String[]{"C2"}, false, "TBG-Y", true);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(9, "brian", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TBG-3", IssueType.BUG, Priority.HIGHEST, "Three-1", new int[]{1}, 1, 1);

        //Check full issue table
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", new int[]{1}, 3, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{1}, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four-1", new int[]{0}, 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.BUG, Priority.HIGHEST, "Three-1", new int[]{1}, 1, 1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);
    }

    @Test
    public void testUpdateIssueNewUsers() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");
        issueRegistry.addIssue("TDP", "feature", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", null, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");
        getJsonCheckingViewIdAndUsers(0, "brian", "kabir");

        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType) null, null, null, "jason", false, null, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        getJsonCheckingViewIdAndUsers(1, "brian", "jason", "kabir");

        update = createUpdateEventAndAddToRegistry("TBG-3", (IssueType) null, null, null, "james", false, null, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "james", "jason", "kabir");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 3);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 3);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 3);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 3);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, 1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);

    }

    @Test
    public void testUpdateIssueNewComponents() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, new String[]{"D"}, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", new String[]{"K"}, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");
        issueRegistry.addIssue("TDP", "feature", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", null, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");
        getJsonCheckingViewIdAndUsers(0, "brian", "kabir");

        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType) null, null, null, null,
                false, new String[]{"E", "F"}, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "kabir");
        checkComponents(boardNode, "D", "E", "F", "K");
        checkNoCustomFields(boardNode);

        update = createUpdateEventAndAddToRegistry("TBG-3", (IssueType) null, null, null, null,
                false, new String[]{"L"}, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "kabir");
        checkComponents(boardNode, "D", "E", "F", "K", "L");
        checkNoBlacklist(boardNode);

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", new int[]{1, 2}, 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{3}, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", new int[]{4}, 0, -1);

        //Clear the components from an issue
        update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType) null, null, null, null,
                false, null, true, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "brian", "kabir");
        checkComponents(boardNode, "D", "E", "F", "K", "L");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);

        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{3}, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", new int[]{4}, 0, -1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);
    }

    @Test
    public void testMissingState() throws SearchException {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, null, "BAD");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", null, "BAD");
        issueRegistry.addIssue("TBG", "bug", "low", "Two", "kabir", null, "TBG-Y");

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        checkBlacklist(boardNode, new String[]{"BAD"}, null, null, "TDP-1", "TBG-1");
        checkNoCustomFields(boardNode);

        //Add another issue to the same bad state to check that this works on updating
        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-3", IssueType.TASK, Priority.HIGHEST, "Three", null, null, "BAD");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
        checkBlacklist(boardNode, new String[]{"BAD"}, null, null, "TDP-1", "TBG-1", "TDP-3");
        checkNoCustomFields(boardNode);

        //Add another issue to another bad state
        event = createCreateEventAndAddToRegistry("TDP-4", IssueType.BUG, Priority.HIGH, "Four", null, null, "BADDER");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
        checkBlacklist(boardNode, new String[]{"BAD", "BADDER"}, null, null, "TDP-1", "TBG-1", "TDP-3", "TDP-4");
        checkNoCustomFields(boardNode);

        //Move an issue from a bad state to a good state, this does not affect the blacklist which is ok since the config is broken anyway
        event = createUpdateEventAndAddToRegistry("TDP-4", (IssueType) null, null, null, null, false, null, false, "TDP-A", false);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        //Since the issue has been blacklisted the view id is the same
        getJsonCheckingViewIdAndUsers(2, "kabir");
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Now delete a bad issue, this should work and remove it from the blacklist. We don't attempt to update the
        //bad configuration notices though so the bad state remains in the list
        event = JirbanIssueEvent.createDeleteEvent("TDP-4", "TDP");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "kabir");
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
        checkBlacklist(boardNode, new String[]{"BAD", "BADDER"}, null, null, "TDP-1", "TBG-1", "TDP-3");
    }

    @Test
    public void testMissingIssueType() throws SearchException {
        issueRegistry.addIssue("TDP", "BAD", "highest", "One", null, null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TBG", "BAD", "highest", "One", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "bug", "low", "Two", "kabir", null, "TBG-Y");

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        checkBlacklist(boardNode, null, new String[]{"BAD"}, null, "TDP-1", "TBG-1");
        checkNoCustomFields(boardNode);

        //Add another issue to the same bad issue type to check that this works on updating
        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-3", "BAD", Priority.HIGHEST.name, "Three", null, null, "TDP-C");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
        checkBlacklist(boardNode, null, new String[]{"BAD"}, null, "TDP-1", "TBG-1", "TDP-3");
        checkNoCustomFields(boardNode);

        //Add another issue to another bad issue type
        event = createCreateEventAndAddToRegistry("TDP-4", "BADDER", Priority.HIGH.name, "Four", null, null, "TDP-C");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
        checkBlacklist(boardNode, null, new String[]{"BAD", "BADDER"}, null, "TDP-1", "TBG-1", "TDP-3", "TDP-4");
        checkNoCustomFields(boardNode);

        //Move an issue from a bad issue type to a good issue type, this does not affect the blacklist which is ok since the config is broken anyway
        event = createUpdateEventAndAddToRegistry("TDP-4", IssueType.TASK, null, null, null, false, null, false, null, false);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        //Since the issue has been blacklisted the view id is the same
        getJsonCheckingViewIdAndUsers(2, "kabir");

        //Now delete a bad issue, this should work and remove it from the blacklist. We don't attempt to update the
        //bad configuration notices though so the bad issue type remains in the list
        event = JirbanIssueEvent.createDeleteEvent("TDP-4", "TDP");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "kabir");
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
        checkBlacklist(boardNode, null, new String[]{"BAD", "BADDER"}, null, "TDP-1", "TBG-1", "TDP-3");
    }

    @Test
    public void testMissingPriority() throws SearchException {
        issueRegistry.addIssue("TDP", "feature", "BAD", "One", null, null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TBG", "bug", "BAD", "One", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "bug", "low", "Two", "kabir", null, "TBG-Y");

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
        checkBlacklist(boardNode, null, null, new String[]{"BAD"}, "TDP-1", "TBG-1");
        checkNoCustomFields(boardNode);

        //Add another issue to the same bad priority to check that this works on updating
        JirbanIssueEvent event = createCreateEventAndAddToRegistry("TDP-3", IssueType.FEATURE.name, "BAD", "Three", null, null, "TDP-C");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
        checkBlacklist(boardNode, null, null, new String[]{"BAD"}, "TDP-1", "TBG-1", "TDP-3");
        checkNoCustomFields(boardNode);

        //Add another issue to another bad priority
        event = createCreateEventAndAddToRegistry("TDP-4", IssueType.TASK.name, "BADDER", "Four", null, null, "TDP-C");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
        checkBlacklist(boardNode, null, null, new String[]{"BAD", "BADDER"}, "TDP-1", "TBG-1", "TDP-3", "TDP-4");
        checkNoCustomFields(boardNode);


        //Move an issue from a bad priority to a good priority, this does not affect the blacklist which is ok since the config is broken anyway
        event = createUpdateEventAndAddToRegistry("TDP-4", null, Priority.HIGH, null, null, false, null, false, null, false);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        //Since the issue has been blacklisted the view id is the same
        getJsonCheckingViewIdAndUsers(2, "kabir");

        //Now delete a bad issue, this should work and remove it from the blacklist. We don't attempt to update the
        //bad configuration notices though so the bad priority remains in the list
        event = JirbanIssueEvent.createDeleteEvent("TDP-4", "TDP");
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "kabir");
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.LOW, "Two", null, 1, 0);
        checkProjectRankedIssues(boardNode, "TDP", 2);
        checkProjectRankedIssues(boardNode, "TBG", 2);
        checkBlacklist(boardNode, null, null, new String[]{"BAD", "BADDER"}, "TDP-1", "TBG-1", "TDP-3");
    }

    @Test
    public void testLoadBoardWithBacklogStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with backlog states set up
        initializeMocks("config/board-tdp-backlog.json");

        issueRegistry.addIssue("TDP", "task", "high", "One", "kabir", new String[]{"C1"}, "TDP-A");  //1
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", new String[]{"C2"}, "TDP-B");     //2
        issueRegistry.addIssue("TDP", "task", "high", "Three", "brian", null, "TDP-C");                  //3
        issueRegistry.addIssue("TDP", "task", "high", "Four", "brian", null, "TDP-D");                //4
        issueRegistry.addIssue("TBG", "task", "high", "One", "kabir", new String[]{"C3"}, "TBG-X");  //1
        issueRegistry.addIssue("TBG", "task", "high", "Two", "brian", null, "TBG-Y");                    //2

        //Although not all the assignees and components are used in the non-blacklist part of the board,
        //include them anyway
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "kabir");
        checkComponents(boardNode, "C1", "C2", "C3");
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 3);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.HIGH, "Four", null, 3, 0);
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);

        checkProjectRankedIssues(boardNode, "TDP", 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 2);


        //Now check with the backlog
        boardNode = getJsonCheckingViewIdAndUsers(0, true, "brian", "kabir");
        checkComponents(boardNode, "C1", "C2", "C3");
        checkNoCustomFields(boardNode);
        checkNoBlacklist(boardNode);
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        allIssues = getIssuesCheckingSize(boardNode, 6);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", new int[]{0}, 0, 1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{1}, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.HIGH, "Four", null, 3, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", new int[]{2}, 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2);

    }

    @Test
    public void testLoadBoardWithDoneStatesConfigured() throws Exception {
        //Override the default configuration set up by the @Before method to one with done states set up
        initializeMocks("config/board-tdp-done.json");

        issueRegistry.addIssue("TDP", "task", "high", "One", "kabir", null, "TDP-A");  //1
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");     //2
        issueRegistry.addIssue("TDP", "task", "high", "Three", "kabir", null, "TDP-C"); //3
        issueRegistry.addIssue("TDP", "task", "high", "Four", "brian", new String[]{"C1"}, "TDP-D"); //4
        issueRegistry.addIssue("TBG", "task", "high", "One", "kabir", null, "TBG-X");  //1
        issueRegistry.addIssue("TBG", "task", "high", "Two", "jason", new String[]{"C2"}, "TBG-Y");  //2

        //Although the assignees and components used in the done part of the board should not be included, and neither
        //include them anyway
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        checkComponents(boardNode);
        checkNoBlacklist(boardNode);
        checkNoCustomFields(boardNode);
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        //The issues in the 'done' columns should not be included in the board.
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 3);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //An event putting a 'done' issue into one of the normal states should result in the issue and any assignees/components being brought in

        //This one does not bring in any new assignees/components
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-3", (IssueType)null, null,
                null, null, false, null, false, "TDP-A", false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        //view id is 0 here because board has been recreated (due to moving issue out of 'done')
        boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 4);
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 0, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Bring in new assignees/components
        update = createUpdateEventAndAddToRegistry("TDP-4", (IssueType)null, null,
                null, null, false, null, false, "TDP-A", false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        //view id is 0 here because board has been recreated (due to moving issue out of 'done')
        boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 5);
        checkComponents(boardNode, "C1");
        checkNoCustomFields(boardNode);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 0, 1);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.HIGH, "Four", new int[]{0}, 0, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 1);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1);


        update = createUpdateEventAndAddToRegistry("TBG-2", (IssueType)null, null,
                null, null, false, null, false, "TBG-X", false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        //view id is 0 here because board has been recreated (due to moving issue out of 'done')
        boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "jason", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 6);
        checkComponents(boardNode, "C1", "C2");
        checkNoCustomFields(boardNode);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 0, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.HIGH, "Four", new int[]{0}, 0, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{1}, 0, 1);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2);

        //Check moving an issue to a done state
        update = createUpdateEventAndAddToRegistry("TDP-4", (IssueType)null, null,
                null, null, false, null, false, "TDP-C", false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "jason", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 5);
        checkComponents(boardNode, "C1", "C2");
        checkNoCustomFields(boardNode);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 0, 2);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{1}, 0, 1);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2);

        update = createUpdateEventAndAddToRegistry("TBG-1", (IssueType)null, null,
                null, null, false, null, false, "TBG-Y", false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "jason", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 4);
        checkComponents(boardNode, "C1", "C2");
        checkNoCustomFields(boardNode);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{1}, 0, 1);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Check that moving an issue from a done state to another done state does not trigger a change
        update = createUpdateEventAndAddToRegistry("TDP-4", (IssueType)null, null,
                null, null, false, null, false, "TDP-D", false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "jason", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 4);
        checkComponents(boardNode, "C1", "C2");
        checkNoCustomFields(boardNode);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{1}, 0, 1);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 2);

        //Test that updating an issue in a 'done' does not trigger a change
        update = createUpdateEventAndAddToRegistry("TDP-4", IssueType.BUG, Priority.LOW,
                "Will be ignored", "nonexistent", false, null, false, null, false);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "jason", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 4);
        checkComponents(boardNode, "C1", "C2");
        checkNoCustomFields(boardNode);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.TASK, Priority.HIGH, "Two", new int[]{1}, 0, 1);
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
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0);
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


        issueRegistry.addIssue("TDP", "task", "high", "One", "kabir", null, "TDP-A");  //1
        issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("jason"));
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");     //2
        issueRegistry.setCustomField("TDP-2", testerId, userManager.getUserByKey("brian"));
        issueRegistry.addIssue("TDP", "task", "high", "Three", "kabir", null, "TDP-C");                  //3
        issueRegistry.addIssue("TBG", "task", "high", "One", "kabir", null, "TBG-X");  //1

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        checkTesters(boardNode, "brian", "jason");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, NoIssueCustomFieldChecker.TESTER);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, NoIssueCustomFieldChecker.TESTER);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1);
    }

    @Test
    public void testAddIssuesNoNewCustomFieldData() throws Exception {
        initializeMocks("config/board-custom.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        issueRegistry.addIssue("TDP", "task", "high", "One", "kabir", null, "TDP-A");  //1
        issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("jason"));
        issueRegistry.setCustomField("TDP-1", documenterId, userManager.getUserByKey("jason"));
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");     //2
        issueRegistry.setCustomField("TDP-2", testerId, userManager.getUserByKey("brian"));
        issueRegistry.setCustomField("TDP-2", documenterId, userManager.getUserByKey("brian"));
        issueRegistry.addIssue("TDP", "task", "high", "Three", "kabir", null, "TDP-C"); //3
        issueRegistry.addIssue("TBG", "task", "high", "One", "kabir", null, "TBG-X");  //1
        issueRegistry.setCustomField("TBG-1", testerId, userManager.getUserByKey("kabir"));
        issueRegistry.setCustomField("TBG-1", documenterId, userManager.getUserByKey("kabir"));

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        checkTesters(boardNode, "brian", "jason", "kabir");
        checkDocumenters(boardNode, "brian", "jason"/*, "kabir"*/); //kabir is only for the TBG issue, which has no 'Documenters' configured

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(1), new DocumenterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(0), new TesterChecker(0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(2), NoIssueCustomFieldChecker.DOCUMENTER);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Add issues to main project
        //Add an issue
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, "jason");
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-4", IssueType.FEATURE, Priority.HIGH,
                "Four", "kabir", null, "TDP-D", customFieldValues);
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "kabir");
        checkTesters(boardNode, "brian", "jason", "kabir");
        checkDocumenters(boardNode, "brian", "jason"/*, "kabir"*/);

        //Add another issue
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, "brian");
        customFieldValues.put(testerId, "brian");
        create = createCreateEventAndAddToRegistry("TDP-5", IssueType.FEATURE, Priority.HIGH,
                "Five", "kabir", null, "TDP-D", customFieldValues);
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "kabir");
        checkTesters(boardNode, "brian", "jason", "kabir");
        checkDocumenters(boardNode, "brian", "jason"/*, "kabir"*/);

        //And another....
        customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "jason");
        create = createCreateEventAndAddToRegistry("TDP-6", IssueType.FEATURE, Priority.HIGH,
                "Six", "kabir", null, "TDP-D", customFieldValues);
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "kabir");
        checkTesters(boardNode, "brian", "jason", "kabir");
        checkDocumenters(boardNode, "brian", "jason"/*, "kabir"*/);

        //Add issues to other project - this does NOT have the 'Documenter' custom field configured
        //Add an issue
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, "kabir");
        create = createCreateEventAndAddToRegistry("TBG-2", IssueType.FEATURE, Priority.HIGH,
                "Two", "kabir", null, "TBG-Y", customFieldValues);
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(4, "kabir");
        checkTesters(boardNode, "brian", "jason", "kabir");
        checkDocumenters(boardNode, "brian", "jason"/*, "kabir"*/);

        //Add another issue
        customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "kabir");
        create = createCreateEventAndAddToRegistry("TBG-3", IssueType.FEATURE, Priority.HIGH,
                "Three", "kabir", null, "TBG-Y", customFieldValues);
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(5, "kabir");
        checkTesters(boardNode, "brian", "jason", "kabir");
        checkDocumenters(boardNode, "brian", "jason"/*, "kabir"*/);


        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(1), new DocumenterChecker(1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(0), new TesterChecker(0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", null, 3, 0, NoIssueCustomFieldChecker.TESTER, new DocumenterChecker(1));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", null, 3, 0, new TesterChecker(0), new DocumenterChecker(0));
        checkIssue(allIssues, "TDP-6", IssueType.FEATURE, Priority.HIGH, "Six", null, 3, 0, new TesterChecker(1), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(2), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TBG-2", IssueType.FEATURE, Priority.HIGH, "Two", null, 1, 0, NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.HIGH, "Three", null, 1, 0, new TesterChecker(2), NoIssueCustomFieldChecker.DOCUMENTER);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3);
    }

    @Test
    public void testAddIssuesNewCustomFieldData() throws Exception {
        initializeMocks("config/board-custom.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        issueRegistry.addIssue("TDP", "task", "high", "One", "kabir", null, "TDP-A");  //1
        issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("jason"));
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");     //2
        issueRegistry.setCustomField("TDP-2", testerId, userManager.getUserByKey("jason"));
        issueRegistry.addIssue("TDP", "task", "high", "Three", "kabir", null, "TDP-C"); //3
        issueRegistry.addIssue("TBG", "task", "high", "One", "kabir", null, "TBG-X");  //1

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        checkTesters(boardNode, "jason");

        //Exactly the same initial board was checked in testLoadBoardWithCustomFields() so don't bother checking it all here

        //Create an issue in the main project with one custom field set
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "brian");
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-4", IssueType.FEATURE, Priority.HIGH,
                "Four", "kabir", null, "TDP-D", customFieldValues);
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "kabir");
        checkComponents(boardNode);
        checkNoBlacklist(boardNode);
        checkTesters(boardNode, "brian", "jason");
        checkDocumenters(boardNode);


        ModelNode allIssues = getIssuesCheckingSize(boardNode, 5);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(1), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(1), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", null, 3, 0, new TesterChecker(0), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1);


        //Create an issue in the main project with more custom fields set
        customFieldValues.put(testerId, "stuart");
        customFieldValues.put(documenterId, "kabir");
        create = createCreateEventAndAddToRegistry("TDP-5", IssueType.FEATURE, Priority.HIGH,
                "Five", "kabir", null, "TDP-A", customFieldValues);
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "kabir");
        checkComponents(boardNode);
        checkNoBlacklist(boardNode);
        checkTesters(boardNode, "brian", "jason", "stuart");
        checkDocumenters(boardNode, "kabir");

        allIssues = getIssuesCheckingSize(boardNode, 6);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(1), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(1), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", null, 3, 0, new TesterChecker(0), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", null, 0, 0, new TesterChecker(2), new DocumenterChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Create an issue in the other project, which does NOT have the Documenter field configured for the board
        customFieldValues.put(testerId, "james");
        customFieldValues.put(documenterId, "james");
        create = createCreateEventAndAddToRegistry("TBG-2", IssueType.FEATURE, Priority.HIGH,
                "Two", "kabir", null, "TBG-Y", customFieldValues);
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "kabir");
        checkComponents(boardNode);
        checkNoBlacklist(boardNode);
        checkTesters(boardNode, "brian", "james", "jason", "stuart");
        checkDocumenters(boardNode, "kabir");

        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(2), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(2), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", null, 3, 0, new TesterChecker(0), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", null, 0, 0, new TesterChecker(3), new DocumenterChecker(0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TBG-2", IssueType.FEATURE, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(1), NoIssueCustomFieldChecker.DOCUMENTER);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2);
    }


    @Test
    public void  testUpdateIssuesNoNewCustomFieldData() throws Exception {
        initializeMocks("config/board-custom.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        issueRegistry.addIssue("TDP", "task", "high", "One", "kabir", null, "TDP-A");  //1
        issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("jason"));
        issueRegistry.setCustomField("TDP-1", documenterId, userManager.getUserByKey("jason"));
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");     //2
        issueRegistry.setCustomField("TDP-2", testerId, userManager.getUserByKey("kabir"));
        issueRegistry.setCustomField("TDP-2", documenterId, userManager.getUserByKey("kabir"));

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        checkComponents(boardNode);
        checkNoBlacklist(boardNode);
        checkDocumenters(boardNode, "jason", "kabir");
        checkTesters(boardNode, "jason", "kabir");

        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "kabir");
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType)null, null,
                null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "kabir");
        checkNoBlacklist(boardNode);
        checkDocumenters(boardNode, "jason", "kabir");
        checkTesters(boardNode, "jason", "kabir");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(1), new DocumenterChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(1), new DocumenterChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);

        //Unset custom fields, this will not remove it from the lookup list of field values
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, UNSET_VALUE);
        update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType)null, null,
                null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "kabir");
        checkNoBlacklist(boardNode);
        checkDocumenters(boardNode, "jason", "kabir");
        checkTesters(boardNode, "jason", "kabir");


        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(1), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(1), new DocumenterChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);

        //Clear all custom fields in an issue, they will stay in the lookup list of field values
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, UNSET_VALUE);
        customFieldValues.put(testerId, UNSET_VALUE);
        update = createUpdateEventAndAddToRegistry("TDP-2", (IssueType)null, null,
                null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "kabir");
        checkNoBlacklist(boardNode);
        checkDocumenters(boardNode, "jason", "kabir");
        checkTesters(boardNode, "jason", "kabir");

        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(1), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, NoIssueCustomFieldChecker.TESTER, NoIssueCustomFieldChecker.DOCUMENTER);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);

        customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "jason");
        update = createUpdateEventAndAddToRegistry("TDP-2", (IssueType)null, null,
                null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(4, "kabir");
        checkNoBlacklist(boardNode);
        checkDocumenters(boardNode, "jason", "kabir");
        checkTesters(boardNode, "jason", "kabir");

        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(1), NoIssueCustomFieldChecker.DOCUMENTER);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(0), NoIssueCustomFieldChecker.DOCUMENTER);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);
    }

    @Test
    public void  testUpdateIssuesNewCustomFieldData() throws Exception {
        initializeMocks("config/board-custom.json");
        final Long testerId = 121212121212L;
        final Long documenterId = 121212121213L;

        issueRegistry.addIssue("TDP", "task", "high", "One", "kabir", null, "TDP-A");  //1
        issueRegistry.setCustomField("TDP-1", testerId, userManager.getUserByKey("jason"));
        issueRegistry.setCustomField("TDP-1", documenterId, userManager.getUserByKey("jason"));
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");     //2
        issueRegistry.setCustomField("TDP-2", testerId, userManager.getUserByKey("kabir"));
        issueRegistry.setCustomField("TDP-2", documenterId, userManager.getUserByKey("kabir"));

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");
        checkComponents(boardNode);
        checkNoBlacklist(boardNode);
        checkDocumenters(boardNode, "jason", "kabir");
        checkTesters(boardNode, "jason", "kabir");

        //Update and bring in a new tester, the unused 'jason' stays in the list
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(testerId, "brian");
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType)null, null,
                null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(1, "kabir");
        checkNoBlacklist(boardNode);
        checkDocumenters(boardNode, "jason", "kabir");
        checkTesters(boardNode, "brian", "jason", "kabir");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(0), new DocumenterChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(2), new DocumenterChecker(1));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2);

        //Update and bring in a new documenter, the unused 'jason' stays in the list
        customFieldValues = new HashMap<>();
        customFieldValues.put(documenterId, "brian");
        update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType)null, null,
                null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "kabir");
        checkNoBlacklist(boardNode);
        checkDocumenters(boardNode, "brian", "jason", "kabir");
        checkTesters(boardNode, "brian", "jason", "kabir");

        allIssues = getIssuesCheckingSize(boardNode, 2);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new TesterChecker(0), new DocumenterChecker(0));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new TesterChecker(2), new DocumenterChecker(2));
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


        issueRegistry.addIssue("TDP", "task", "high", "One", "kabir", null, "TDP-A");  //1
        issueRegistry.setParallelTaskField("TDP-1", upstreamId, "IP");
        issueRegistry.setParallelTaskField("TDP-1", downstreamId, "IP");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");     //2
        issueRegistry.setParallelTaskField("TDP-2", upstreamId, "M");
        issueRegistry.setParallelTaskField("TDP-2", downstreamId, "TD");
        issueRegistry.addIssue("TDP", "task", "high", "Three", "kabir", null, "TDP-C"); //3
        issueRegistry.addIssue("TBG", "task", "high", "One", "kabir", null, "TBG-X");  //1

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "kabir");

        ModelNode parallelTasksNode = boardNode.get(PROJECTS, MAIN, "TDP", PARALLEL_TASKS);
        Assert.assertEquals(ModelType.LIST, parallelTasksNode.getType());
        List<ModelNode> parallelTasks = parallelTasksNode.asList();
        Assert.assertEquals(2, parallelTasks.size());
        checkParallelTaskFieldOptions(parallelTasks.get(0), "US", "Upstream", "Not Started", "In Progress", "Merged");
        checkParallelTaskFieldOptions(parallelTasks.get(1), "DS", "Downstream", "TODO", "In Progress", "Done");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(1, 1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(null));
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


        issueRegistry.addIssue("TDP", "task", "high", "One", "kabir", null, "TDP-A");  //1
        issueRegistry.setParallelTaskField("TDP-1", upstreamId, "IP");
        issueRegistry.setParallelTaskField("TDP-1", downstreamId, "IP");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");     //2
        issueRegistry.setParallelTaskField("TDP-2", upstreamId, "M");
        issueRegistry.setParallelTaskField("TDP-2", downstreamId, "TD");
        issueRegistry.addIssue("TDP", "task", "high", "Three", "kabir", null, "TDP-C"); //3
        issueRegistry.addIssue("TBG", "task", "high", "One", "kabir", null, "TBG-X");  //1

        getJsonCheckingViewIdAndUsers(0, "kabir");
        //Layout of board is aleady checked by testLoadBoardWithParallelTasks

        //Add an issue with explicit parallel fields
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(upstreamId, "M");
        customFieldValues.put(downstreamId, "IP");
        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-4", IssueType.FEATURE, Priority.HIGH,
                "Four", "kabir", null, "TDP-D", customFieldValues);
        boardManager.handleEvent(create, nextRankedIssueUtil);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "kabir");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 5);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(1, 1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", null, 3, 0, new ParallelTaskFieldValueChecker(2, 1));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(null));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Add an issue with no parallel fields set, and make sure that they default to zero
        customFieldValues = new HashMap<>();
        create = createCreateEventAndAddToRegistry("TDP-5", IssueType.FEATURE, Priority.HIGH,
                "Five", "kabir", null, "TDP-D", customFieldValues);
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "kabir");

        allIssues = getIssuesCheckingSize(boardNode, 6);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(1, 1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", null, 3, 0, new ParallelTaskFieldValueChecker(2, 1));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", null, 3, 0, new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(null));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Add an issue in a project with no parallel fields and make sure that they are not set in the issue
        create = createCreateEventAndAddToRegistry("TBG-2", IssueType.FEATURE, Priority.HIGH,  "Two", "kabir", null, "TBG-X");
        boardManager.handleEvent(create, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(1, 1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four", null, 3, 0, new ParallelTaskFieldValueChecker(2, 1));
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", null, 3, 0, new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(null));
        checkIssue(allIssues, "TBG-2", IssueType.FEATURE, Priority.HIGH, "Two", null, 0, 0, new ParallelTaskFieldValueChecker(null));
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


        issueRegistry.addIssue("TDP", "task", "high", "One", "kabir", null, "TDP-A");  //1
        issueRegistry.setParallelTaskField("TDP-1", upstreamId, "IP");
        issueRegistry.setParallelTaskField("TDP-1", downstreamId, "IP");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");     //2
        issueRegistry.setParallelTaskField("TDP-2", upstreamId, "M");
        issueRegistry.setParallelTaskField("TDP-2", downstreamId, "TD");
        issueRegistry.addIssue("TDP", "task", "high", "Three", "kabir", null, "TDP-C"); //3
        issueRegistry.addIssue("TBG", "task", "high", "One", "kabir", null, "TBG-X");  //1


        getJsonCheckingViewIdAndUsers(0, "kabir");
        //Layout of board is aleady checked by testLoadBoardWithParallelTasks

        //Update some of an issue's parallel fields
        Map<Long, String> customFieldValues = new HashMap<>();
        customFieldValues.put(upstreamId, "M");
        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType)null, null,
                null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "kabir");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(2, 1));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(null));
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3);
        checkProjectRankedIssues(boardNode, "TBG", 1);

        //Update all the parallel fields in an issue
        customFieldValues = new HashMap<>();
        customFieldValues.put(upstreamId, "IP");
        customFieldValues.put(downstreamId, "D");
        update = createUpdateEventAndAddToRegistry("TDP-1", (IssueType)null, null,
                null, null, false, null, false, null, false, customFieldValues);
        boardManager.handleEvent(update, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "kabir");

        allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(1, 2));
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 0, new ParallelTaskFieldValueChecker(2, 0));
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.HIGH, "Three", null, 2, 0, new ParallelTaskFieldValueChecker(0, 0));
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGH, "One", null, 0, 0, new ParallelTaskFieldValueChecker(null));
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
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, null, "TDP-C");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", null, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");
        issueRegistry.addIssue("TBG", "task", "lowest", "Four", "jason", null, "TBG-Y");
        getJsonCheckingViewIdAndUsers(0, "brian", "jason", "kabir");

        //Create an update event which doesn't change anything we are interested in and make sure the view id stays at zero
        JirbanIssueEvent event = JirbanIssueEvent.createUpdateEvent("TDP-1", "TDP", null, null, null, null, null, null, null, false, null);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "jason", "kabir");
        checkNoBlacklist(boardNode);
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 11);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", null, 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", null, 1, 2);
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", null, 2, -1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", null, 1, 1);

        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

    }

    @Test
    public void testRankIssue() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "TDP-D");
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, null, "TDP-C");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", null, "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", null, "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, null, "TBG-X");
        issueRegistry.addIssue("TBG", "task", "lowest", "Four", "jason", null, "TBG-Y");
        getJsonCheckingViewIdAndUsers(0, "brian", "jason", "kabir");

        //Rank an issue to somewhere in the middle in main project and check board
        issueRegistry.rerankIssue("TDP-1", "TDP-4");
        JirbanIssueEvent event = JirbanIssueEvent.createUpdateEvent("TDP-1", "TDP", null, null, null, null, null, null, null, true, null);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "jason", "kabir");
        checkNoBlacklist(boardNode);
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);
        checkProjectRankedIssues(boardNode, "TDP", 2, 3, 1, 4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

        //Rank an issue to the start of the main project and check board
        issueRegistry.rerankIssue("TDP-1", "TDP-2");
        event = JirbanIssueEvent.createUpdateEvent("TDP-1", "TDP", null, null, null, null, null, null, null, true, null);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "jason", "kabir");
        checkNoBlacklist(boardNode);
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);
        checkProjectRankedIssues(boardNode, "TDP", 1, 2, 3,  4, 5, 6, 7);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

        //Rank an issue to the end of the main project and check board
        issueRegistry.rerankIssue("TDP-1", null);
        event = JirbanIssueEvent.createUpdateEvent("TDP-1", "TDP", null, null, null, null, null, null, null, true, null);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "brian", "jason", "kabir");
        checkNoBlacklist(boardNode);
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);
        checkProjectRankedIssues(boardNode, "TDP", 2, 3,  4, 5, 6, 7, 1);
        checkProjectRankedIssues(boardNode, "TBG", 1, 2, 3, 4);

        //Rank an issue in the other project and check board
        issueRegistry.rerankIssue("TBG-2", "TBG-4");
        event = JirbanIssueEvent.createUpdateEvent("TBG-2", "TBG", null, null, null, null, null, null, null, true, null);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(4, "brian", "jason", "kabir");
        checkNoBlacklist(boardNode);
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);
        checkProjectRankedIssues(boardNode, "TDP", 2, 3,  4, 5, 6, 7, 1);
        checkProjectRankedIssues(boardNode, "TBG", 1, 3, 2, 4);

        //Check that all the issue datas are as expected
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 11);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", null, 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", null, 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", null, 1, 2);
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", null, 2, -1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", null, 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", null, 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", null, 1, 1);
    }

    @Test
    public void testRankIssueBeforeBlacklistedIssue() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "BAD");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "BAD");
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, null, "TDP-C");
        getJsonCheckingViewIdAndUsers(0, "brian", "kabir");

        //Rank an issue to before a blacklisted issue and check board
        issueRegistry.rerankIssue("TDP-1", "TDP-3");
        JirbanIssueEvent event = JirbanIssueEvent.createUpdateEvent("TDP-1", "TDP", null, null, null, null, null, null, null, true, null);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "kabir");
        checkBlacklist(boardNode, new String[]{"BAD"}, null, null, "TDP-3", "TDP-4");
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);
        checkProjectRankedIssues(boardNode, "TDP", 2, 1, 5, 6, 7);

        //Try again, board should be the same
        issueRegistry.rerankIssue("TDP-1", "TDP-3");
        event = JirbanIssueEvent.createUpdateEvent("TDP-1", "TDP", null, null, null, null, null, null, null, true, null);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "kabir");
        checkBlacklist(boardNode, new String[]{"BAD"}, null, null, "TDP-3", "TDP-4");
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);
        checkProjectRankedIssues(boardNode, "TDP", 2, 1, 5, 6, 7);

        //Rank somewhere not blacklisted
        issueRegistry.rerankIssue("TDP-1", "TDP-7");
        event = JirbanIssueEvent.createUpdateEvent("TDP-1", "TDP", null, null, null, null, null, null, null, true, null);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        boardNode = getJsonCheckingViewIdAndUsers(3, "brian", "kabir");
        checkBlacklist(boardNode, new String[]{"BAD"}, null, null, "TDP-3", "TDP-4");
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);
        checkProjectRankedIssues(boardNode, "TDP", 2, 5, 6, 1, 7);

        //Check that all the issue datas are as expected
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 5);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", null, 0, 1);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", null, 1, 1);
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", null, 2, -1);
    }


    @Test
    public void testRankIssueBeforeBlacklistedIssueEnd() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", null, "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", null, "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", null, "BAD");
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", null, "BAD");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", null, "BAD");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, null, "BAD");
        getJsonCheckingViewIdAndUsers(0, "brian", "kabir");

        //Rank an issue to before a blacklisted issue. There are only blacklisted issues left, so the issue should get inserted at the end
        issueRegistry.rerankIssue("TDP-1", "TDP-4");
        JirbanIssueEvent event = JirbanIssueEvent.createUpdateEvent("TDP-1", "TDP", null, null, null, null, null, null, null, true, null);
        boardManager.handleEvent(event, nextRankedIssueUtil);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "kabir");
        checkBlacklist(boardNode, new String[]{"BAD"}, null, null, "TDP-4", "TDP-5", "TDP-6", "TDP-7");
        checkComponents(boardNode);
        checkNoCustomFields(boardNode);
        checkProjectRankedIssues(boardNode, "TDP", 2, 3, 1);


        //Check that all the issue datas are as expected
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 3);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", null, 0, 1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", null, 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", null, 2, 1);
    }

    private ModelNode getJsonCheckingViewIdAndUsers(int expectedViewId, String...users) throws SearchException {
        return getJsonCheckingViewIdAndUsers(expectedViewId, false, users);
    }

    private ModelNode getJsonCheckingViewIdAndUsers(int expectedViewId, boolean backlog, String...users) throws SearchException {
        String json = boardManager.getBoardJson(userManager.getUserByKey("kabir"), backlog, "TST");
        Assert.assertNotNull(json);
        ModelNode boardNode = ModelNode.fromJSONString(json);
        Assert.assertEquals(expectedViewId, boardNode.get("view").asInt());
        checkUsers(boardNode, users);
        return boardNode;
    }

    private void checkNoCustomFields(ModelNode boardNode) {
        Assert.assertFalse(boardNode.hasDefined(CUSTOM));
    }

    private void checkTesters(ModelNode boardNode, String...testers) {
        if (testers.length == 0) {
            Assert.assertFalse(boardNode.hasDefined(CUSTOM, "Tester"));
            return;
        }

        checkCustomFields(boardNode.get(CUSTOM, "Tester"), testers);
    }

    private void checkDocumenters(ModelNode boardNode, String...testers) {
        if (testers.length == 0) {
            Assert.assertFalse(boardNode.hasDefined(CUSTOM, "Documenter"));
            return;
        }

        checkCustomFields(boardNode.get(CUSTOM, "Documenter"), testers);
    }

    private void checkCustomFields(ModelNode fieldList, String...keys) {
        List<ModelNode> fields = fieldList.asList();
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

    private void checkProjectIssues(ModelNode boardNode, String project, String[][] issueTable) {
        List<ModelNode> issues = boardNode.get(PROJECTS, MAIN, project, ISSUES).asList();
        Assert.assertEquals(issueTable.length, issues.size());
        for (int i = 0 ; i < issueTable.length ; i++) {
            List<ModelNode> issuesForState = issues.get(i).asList();
            Assert.assertEquals(issueTable[i].length, issuesForState.size());
            for (int j = 0 ; j < issueTable[i].length ; j++) {
                Assert.assertEquals(issueTable[i][j], issuesForState.get(j).asString());
            }
        }
    }

    private void checkProjectRankedIssues(ModelNode boardNode, String projectCode, int...issueNumbers) {
        Assert.assertTrue(boardNode.hasDefined(PROJECTS, MAIN, projectCode, RANKED));
        List<ModelNode> ranked = boardNode.get(PROJECTS, MAIN, projectCode, RANKED).asList();
        Assert.assertEquals(issueNumbers.length, ranked.size());
        for (int i = 0 ; i < issueNumbers.length ; i++) {
            Assert.assertEquals(projectCode + "-" + issueNumbers[i], ranked.get(i).asString());
        }
    }

    private void checkUsers(ModelNode board, String...users) {
        checkUsers(board.get(ASSIGNEES), true, users);
    }

    private void checkUsers(ModelNode userList, boolean avatar, String...users) {
        List<ModelNode> assignees = userList.asList();
        Assert.assertEquals(assignees.size(), users.length);
        for (int i = 0 ; i < users.length ; i++) {
            ModelNode assignee = assignees.get(i);
            Assert.assertNotNull(assignee);
            Assert.assertEquals(users[i] + "@example.com", assignee.get(EMAIL).asString());
            if (avatar) {
                Assert.assertEquals("/avatars/" + users[i] + ".png", assignee.get(AVATAR).asString());
            } else {
                Assert.assertFalse(assignee.get(AVATAR).isDefined());
            }

            String displayName = assignee.get("name").toString().toLowerCase();
            Assert.assertTrue(displayName.length() > users[i].length());
            Assert.assertTrue(displayName.contains(users[i]));
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
                            int[] components, int state, int assignee, IssueChecker... issueCheckers) {
        ModelNode issue = issues.get(key);
        Assert.assertNotNull(issue);
        Assert.assertEquals(key, issue.get(KEY).asString());
        Assert.assertEquals(type.index, issue.get(TYPE).asInt());
        Assert.assertEquals(priority.index, issue.get(PRIORITY).asInt());
        Assert.assertEquals(summary, issue.get(SUMMARY).asString());
        Assert.assertEquals(state, issue.get(STATE).asInt());
        if (assignee < 0) {
            Assert.assertFalse(issue.hasDefined(ASSIGNEE));
        } else {
            Assert.assertEquals(assignee, issue.get(ASSIGNEE).asInt());
        }
        if (components == null) {
            Assert.assertFalse(issue.hasDefined(COMPONENTS));
        } else {
            List<ModelNode> componentsNodes = issue.get(COMPONENTS).asList();
            Assert.assertEquals(components.length, componentsNodes.size());
            for (int i = 0 ; i < components.length ; i++) {
                Assert.assertEquals(components[i], componentsNodes.get(i).asInt());
            }
        }
        for (IssueChecker issueChecker : issueCheckers) {
            issueChecker.check(issue);
        }
    }

    private void checkBlacklist(ModelNode boardNode, String[] states, String[] issueTypes, String[] priorities, String... issues) {
        Assert.assertTrue(boardNode.hasDefined(BLACKLIST));
        ModelNode blacklist = boardNode.get(BLACKLIST);
        checkBlacklistEntry(blacklist, STATES, states);
        checkBlacklistEntry(blacklist, ISSUE_TYPES, issueTypes);
        checkBlacklistEntry(blacklist, PRIORITIES, priorities);
        checkBlacklistEntry(blacklist, ISSUES, issues);
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

    private void checkComponents(ModelNode boardNode, String...componentNames) {
        if (componentNames.length == 0) {
            Assert.assertFalse(boardNode.hasDefined(COMPONENTS));
        } else {
            List<ModelNode> components = boardNode.get(COMPONENTS).asList();
            for (int i = 0; i < componentNames.length; i++) {
                Assert.assertEquals(componentNames[i], components.get(i).asString());
            }
        }
    }

    private void checkNoBlacklist(ModelNode boardNode) {
        Assert.assertFalse(boardNode.has(BLACKLIST));
    }

    private static class DefaultIssueCustomFieldChecker implements IssueChecker {
        private final String fieldName;
        private final int id;

        public DefaultIssueCustomFieldChecker(String fieldName, int id) {
            this.fieldName = fieldName;
            this.id = id;
        }

        @Override
        public void check(ModelNode issue) {
            Assert.assertTrue(issue.hasDefined(CUSTOM, fieldName));
            Assert.assertEquals(id, issue.get(CUSTOM, fieldName).asInt());
        }
    }

    private static class TesterChecker extends DefaultIssueCustomFieldChecker {
        public TesterChecker(int testerId) {
            super("Tester", testerId);
        }
    }

    private static class DocumenterChecker extends DefaultIssueCustomFieldChecker {
        public DocumenterChecker(int documenterId) {
            super("Documenter", documenterId);
        }
    }

    private static class ParallelTaskFieldValueChecker implements IssueChecker {

        private int[] expected;

        public ParallelTaskFieldValueChecker(int...expected) {
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
}

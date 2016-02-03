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

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.JirbanIssueEvent;
import org.junit.Assert;
import org.junit.Test;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.search.SearchException;

import ut.org.jirban.jira.mock.CrowdUserBridge;

/**
 * @author Kabir Khan
 */
public class BoardManagerTest extends AbstractBoardTest {

    @Test
    public void testLoadBoardOnlyOwnerProjectIssues() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", "kabir", "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", "TDP-D");
        issueRegistry.addIssue("TDP", "task", "highest", "Five", "kabir", "TDP-A");
        issueRegistry.addIssue("TDP", "bug", "high", "Six", "kabir", "TDP-B");
        issueRegistry.addIssue("TDP", "feature", "low", "Seven", null, "TDP-C");

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "kabir");
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, 1);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, 1);
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", 2, -1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1", "TDP-5"},
                {"TDP-2", "TDP-6"},
                {"TDP-3", "TDP-7"},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{{},{},{},{}});
    }

    @Test
    public void testLoadBoardOnlyNonOwnerProjectIssues() throws Exception {
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, "TBG-X");
        issueRegistry.addIssue("TBG", "task", "lowest", "Four", "jason", "TBG-Y");

        ModelNode boardNode = boardNode = getJsonCheckingViewIdAndUsers(0, "jason", "kabir");
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 4);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, -11);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, 0);

        checkProjectIssues(boardNode, "TDP", new String[][]{{}, {}, {}, {}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3"},
                {"TBG-2", "TBG-4"},
                {}});
    }

    @Test
    public void testLoadBoard() throws Exception {
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

        ModelNode boardNode = getJsonCheckingViewIdAndUsers(0, "brian", "jason", "kabir");
        checkNameAndIcon(boardNode, "priorities", "highest", "high", "low", "lowest");
        checkNameAndIcon(boardNode, "issue-types", "task", "bug", "feature");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 11);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, 2);
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", 2, -1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, 1);

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

    @Test
    public void testDeleteIssue() throws Exception {
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
        getJsonCheckingViewIdAndUsers(0, "brian", "jason", "kabir");

        //Delete an issue in main project and check board
        JirbanIssueEvent delete = JirbanIssueEvent.createDeleteEvent("TDP-3", "TDP");
        boardManager.handleEvent(delete);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "jason", "kabir");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 10);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, 2);
        checkIssue(allIssues, "TDP-7", IssueType.FEATURE, Priority.LOW, "Seven", 2, -1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, 1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1", "TDP-5"},
                {"TDP-2", "TDP-6"},
                {"TDP-7"},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3"},
                {"TBG-2", "TBG-4"},
                {}});

        //Delete an issue in main project and check board
        delete = JirbanIssueEvent.createDeleteEvent("TDP-7", "TDP");
        boardManager.handleEvent(delete);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "jason", "kabir");

        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, 2);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, 1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1", "TDP-5"},
                {"TDP-2", "TDP-6"},
                {},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3"},
                {"TBG-2", "TBG-4"},
                {}});

        //Delete an issue in other project and check board
        delete = JirbanIssueEvent.createDeleteEvent("TBG-1", "TBG");
        boardManager.handleEvent(delete);
        boardNode = getJsonCheckingViewIdAndUsers(3, "brian", "jason", "kabir");

        allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, 1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1", "TDP-5"},
                {"TDP-2", "TDP-6"},
                {},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-3"},
                {"TBG-2", "TBG-4"},
                {}});

        //Delete an issue in other project and check board
        delete = JirbanIssueEvent.createDeleteEvent("TBG-3", "TBG");
        boardManager.handleEvent(delete);
        boardNode = getJsonCheckingViewIdAndUsers(4, "brian", "jason", "kabir");

        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.TASK, Priority.HIGHEST, "Five", 0, 2);
        checkIssue(allIssues, "TDP-6", IssueType.BUG, Priority.HIGH, "Six", 1, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TBG-4", IssueType.TASK, Priority.LOWEST, "Four", 1, 1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1", "TDP-5"},
                {"TDP-2", "TDP-6"},
                {},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {},
                {"TBG-2", "TBG-4"},
                {}});

        //TODO Check the diffs we will send to clients
    }

    @Test
    public void testAddIssuesNoNewUsers() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", "TDP-D");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, "TBG-X");
        getJsonCheckingViewIdAndUsers(0, "brian", "kabir");

        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-5", "TDP", "feature", "high",
                "Five", "kabir", "TDP-B");
        boardManager.handleEvent(create);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "kabir");

        checkUsers(boardNode, "brian", "kabir");
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, -1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1"},
                {"TDP-2", "TDP-5"},
                {"TDP-3"},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3"},
                {"TBG-2"},
                {}});

        create = createCreateEventAndAddToRegistry("TBG-4", "TBG", "feature", "high",
                "Four", null, "TBG-X");
        boardManager.handleEvent(create);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "kabir");

        checkUsers(boardNode, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.FEATURE, Priority.HIGH, "Four", 0, -1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1"},
                {"TDP-2", "TDP-5"},
                {"TDP-3"},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3", "TBG-4"},
                {"TBG-2"},
                {}});
    }

    @Test
    public void testAddIssuesNewUsers() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", "TDP-D");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, "TBG-X");

        getJsonCheckingViewIdAndUsers(0, "brian", "kabir");

        JirbanIssueEvent create = createCreateEventAndAddToRegistry("TDP-5", "TDP", "feature", "high",
                "Five", "james", "TDP-B");
        boardManager.handleEvent(create);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "james", "kabir");
        ModelNode allIssues = getIssuesCheckingSize(boardNode, 8);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, -1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1"},
                {"TDP-2", "TDP-5"},
                {"TDP-3"},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3"},
                {"TBG-2"},
                {}});

        create = createCreateEventAndAddToRegistry("TBG-4", "TBG", "feature", "high",
                "Four", "stuart", "TBG-X");
        boardManager.handleEvent(create);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "james", "kabir", "stuart");

        checkUsers(boardNode, "brian", "james", "kabir", "stuart");
        allIssues = getIssuesCheckingSize(boardNode, 9);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, 2);
        checkIssue(allIssues, "TDP-4", IssueType.TASK, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TDP-5", IssueType.FEATURE, Priority.HIGH, "Five", 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 2);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, -1);
        checkIssue(allIssues, "TBG-4", IssueType.FEATURE, Priority.HIGH, "Four", 0, 3);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1"},
                {"TDP-2", "TDP-5"},
                {"TDP-3"},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3", "TBG-4"},
                {"TBG-2"},
                {}});
    }

    @Test
    public void testUpdateIssueNoNewUsers() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", "TDP-C");
        issueRegistry.addIssue("TDP", "task", "lowest", "Four", "brian", "TDP-D");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, "TBG-X");
        getJsonCheckingViewIdAndUsers(0, "brian", "kabir");

        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-4", "TDP", "feature", "high",
                "Four-1", "kabir", false, "TDP-B", true);
        boardManager.handleEvent(update);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(1, "brian", "kabir");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four-1", 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, -1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1"},
                {"TDP-2", "TDP-4"},
                {"TDP-3"},
                {}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3"},
                {"TBG-2"},
                {}});

        //Do updates of single fields, don't bother checking everything now. Just the issue tables and the changed issue
        //We will do a full check later

        //type
        update = createUpdateEventAndAddToRegistry("TDP-1", "TDP", "feature", null, null, null, false, null, false);
        boardManager.handleEvent(update);
        boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.HIGHEST, "One", 0, -1);

        //priority
        update = createUpdateEventAndAddToRegistry("TDP-1", "TDP", null, "low", null, null, false, null, false);
        boardManager.handleEvent(update);
        boardNode = getJsonCheckingViewIdAndUsers(3, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One", 0, -1);

        //summary
        update = createUpdateEventAndAddToRegistry("TDP-1", "TDP", null, null, "One-1", null, false, null, false);
        boardManager.handleEvent(update);
        boardNode = getJsonCheckingViewIdAndUsers(4, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 0, -1);

        //assign
        update = createUpdateEventAndAddToRegistry("TDP-1", "TDP", null, null, null, "brian", false, null, false);
        boardManager.handleEvent(update);
        boardNode = getJsonCheckingViewIdAndUsers(5, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 0, 0);

        //No updated assignee, nor unassigned
        update = createUpdateEventAndAddToRegistry("TDP-1", "TDP", null, null, null, null, false, null, false);
        boardManager.handleEvent(update);
        boardNode = getJsonCheckingViewIdAndUsers(6, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 0, 0);

        //Unassign
        update = createUpdateEventAndAddToRegistry("TDP-1", "TDP", null, null, null, null, true, null, false);
        boardManager.handleEvent(update);
        boardNode = getJsonCheckingViewIdAndUsers(7, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 0, -1);

        //Change state
        update = createUpdateEventAndAddToRegistry("TDP-1", "TDP", null, null, null, null, false, "TDP-D", true);
        boardManager.handleEvent(update);
        boardNode = getJsonCheckingViewIdAndUsers(8, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 3, -1);

        //Change in the other project
        update = createUpdateEventAndAddToRegistry("TBG-3", "TBG", "bug", "highest", "Three-1", "kabir", false, "TBG-Y", true);
        boardManager.handleEvent(update);
        boardNode = getJsonCheckingViewIdAndUsers(9, "brian", "kabir");
        allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TBG-3", IssueType.BUG, Priority.HIGHEST, "Three-1", 1, 1);

        //Check full issue table
        checkIssue(allIssues, "TDP-1", IssueType.FEATURE, Priority.LOW, "One-1", 3, -1);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 1);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, 1);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.HIGH, "Four-1", 1, 1);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 1);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 1);
        checkIssue(allIssues, "TBG-3", IssueType.BUG, Priority.HIGHEST, "Three-1", 1, 1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {},
                {"TDP-2", "TDP-4"},
                {"TDP-3"},
                {"TDP-1"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1"},
                {"TBG-2", "TBG-3"},
                {}});
    }

    @Test
    public void testUpdateIssueNewUsers() throws Exception {
        issueRegistry.addIssue("TDP", "task", "highest", "One", null, "TDP-A");
        issueRegistry.addIssue("TDP", "task", "high", "Two", "kabir", "TDP-B");
        issueRegistry.addIssue("TDP", "task", "low", "Three", "kabir", "TDP-C");
        issueRegistry.addIssue("TDP", "feature", "lowest", "Four", "brian", "TDP-D");
        issueRegistry.addIssue("TBG", "task", "highest", "One", "kabir", "TBG-X");
        issueRegistry.addIssue("TBG", "bug", "high", "Two", "kabir", "TBG-Y");
        issueRegistry.addIssue("TBG", "feature", "low", "Three", null, "TBG-X");
        getJsonCheckingViewIdAndUsers(0, "brian", "kabir");

        JirbanIssueEvent update = createUpdateEventAndAddToRegistry("TDP-1", "TDP", null, null, null, "jason", false, null, false);
        boardManager.handleEvent(update);
        getJsonCheckingViewIdAndUsers(1, "brian", "jason", "kabir");

        update = createUpdateEventAndAddToRegistry("TBG-3", "TBG", null, null, null, "james", false, null, false);
        boardManager.handleEvent(update);
        ModelNode boardNode = getJsonCheckingViewIdAndUsers(2, "brian", "james", "jason", "kabir");

        ModelNode allIssues = getIssuesCheckingSize(boardNode, 7);
        checkIssue(allIssues, "TDP-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 2);
        checkIssue(allIssues, "TDP-2", IssueType.TASK, Priority.HIGH, "Two", 1, 3);
        checkIssue(allIssues, "TDP-3", IssueType.TASK, Priority.LOW, "Three", 2, 3);
        checkIssue(allIssues, "TDP-4", IssueType.FEATURE, Priority.LOWEST, "Four", 3, 0);
        checkIssue(allIssues, "TBG-1", IssueType.TASK, Priority.HIGHEST, "One", 0, 3);
        checkIssue(allIssues, "TBG-2", IssueType.BUG, Priority.HIGH, "Two", 1, 3);
        checkIssue(allIssues, "TBG-3", IssueType.FEATURE, Priority.LOW, "Three", 0, 1);

        checkProjectIssues(boardNode, "TDP", new String[][]{
                {"TDP-1"},
                {"TDP-2"},
                {"TDP-3"},
                {"TDP-4"}});
        checkProjectIssues(boardNode, "TBG", new String[][]{
                {},
                {"TBG-1", "TBG-3"},
                {"TBG-2"},
                {}});
    }

    private ModelNode getJsonCheckingViewIdAndUsers(int expectedViewId, String...users) throws SearchException {
        String json = boardManager.getBoardJson(userManager.getUserByKey("kabir"), 0);
        Assert.assertNotNull(json);
        ModelNode boardNode = ModelNode.fromJSONString(json);
        Assert.assertEquals(expectedViewId, boardNode.get("view").asInt());
        checkUsers(boardNode, users);
        return boardNode;
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
        for (int i = 0 ; i < users.length ; i++) {
            ModelNode assignee = assignees.get(i);
            Assert.assertNotNull(assignee);
            Assert.assertEquals(users[i] + "@example.com", assignee.get("email").asString());
            Assert.assertEquals("/avatars/" + users[i] + ".png", assignee.get("avatar").asString());

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
            Assert.assertEquals("/icons/" + type + "/" + names[i] + ".png", entry.get("icon").asString());
        }
    }

    private ModelNode getIssuesCheckingSize(ModelNode board, int expectedLength) {
        ModelNode issues = board.get("issues");
        Assert.assertEquals(expectedLength, issues.keys().size());
        return issues;
    }

    private void checkIssue(ModelNode issues, String key, IssueType type, Priority priority, String summary, int state, int assignee) {
        ModelNode issue = issues.get(key);
        Assert.assertNotNull(issue);
        Assert.assertEquals(key, issue.get("key").asString());
        Assert.assertEquals(type.index, issue.get("type").asInt());
        Assert.assertEquals(priority.index, issue.get("priority").asInt());
        Assert.assertEquals(summary, issue.get("summary").asString());
        Assert.assertEquals(state, issue.get("state").asInt());
        if (assignee < 0) {
            Assert.assertFalse(issue.get("assignee").isDefined());
        } else {
            Assert.assertEquals(assignee, issue.get("assignee").asInt());
        }
    }

    private JirbanIssueEvent createCreateEventAndAddToRegistry(String issueKey, String projectCode,
                                               String issueType, String priority, String summary, String username, String state) {
        CrowdUserBridge userBridge = new CrowdUserBridge(userManager);
        User user = userBridge.getUserByKey(username);
        JirbanIssueEvent create = JirbanIssueEvent.createCreateEvent(issueKey, projectCode, issueType, priority,
                summary, user, state);

        issueRegistry.addIssue(projectCode, issueType, priority, summary, username, state);
        return create;
    }

    private JirbanIssueEvent createUpdateEventAndAddToRegistry(String issueKey, String projectCode, String issueType,
                                                   String priority, String summary, String username, boolean unassigned, String state, boolean rank) {
        Assert.assertFalse(username != null && unassigned);

        User user;
        if (unassigned) {
            user = JirbanIssueEvent.UNASSIGNED;
        } else {
            CrowdUserBridge userBridge = new CrowdUserBridge(userManager);
            user = userBridge.getUserByKey(username);
        }
        JirbanIssueEvent update = JirbanIssueEvent.createUpdateEvent(issueKey, projectCode, issueType, priority,
                summary, user, state, rank);

        issueRegistry.updateIssue(issueKey, projectCode, issueType, priority, summary, username, state);
        return update;
    }



    private enum IssueType {
        TASK(0),
        BUG(1),
        FEATURE(2);

        private final int index;

        IssueType(int index) {
            this.index = index;
        }
    }

    private enum Priority {
        HIGHEST(0),
        HIGH(1),
        LOW(2),
        LOWEST(3);

        private final int index;

        Priority(int index) {
            this.index = index;
        }
    }

}

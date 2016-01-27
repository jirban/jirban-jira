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
package org.jirban.jira.impl;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

/**
 * The listener listening to issue events, and delegating relevant ones to the issue table.
 *
 * @author Kabir Khan
 */
@Named("jirbanIssueEventListener")
public class JirbanIssueEventListener  implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(JirbanIssueEventListener.class);

    private static final String CHANGE_LOG_FIELD = "field";
    private static final String CHANGE_LOG_ISSUETYPE = "issuetype";
    private static final String CHANGE_LOG_PRIORITY = "priority";
    private static final String CHANGE_LOG_SUMMARY = "summary";
    private static final String CHANGE_LOG_ASSIGNEE = "assignee";
    private static final String CHANGE_LOG_STATUS = "status";
    private static final String CHANGE_LOG_OLD_STRING = "oldstring";
    private static final String CHANGE_LOG_NEW_STRING = "newstring";
    private static final String CHANGE_LOG_RANK = "Rank";
    private static final String CHANGE_LOG_PROJECT = "project";
    private static final String CHANGE_LOG_OLD_VALUE = "oldvalue";

    @ComponentImport
    private final EventPublisher eventPublisher;

    @ComponentImport
    private final ProjectManager projectManager;

    /**
     * Constructor.
     * @param eventPublisher injected {@code EventPublisher} implementation.
     * @param projectManager injected {@code ProjectManager} implementation.
     */
    @Autowired
    public JirbanIssueEventListener(EventPublisher eventPublisher, ProjectManager projectManager) {
        this.eventPublisher = eventPublisher;
        this.projectManager = projectManager;
    }

    /**
     * Called when the plugin has been enabled.
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        // register ourselves with the EventPublisher
        System.out.println("-----> Registering listener");
        eventPublisher.register(this);
    }

    /**
     * Called when the plugin is being disabled or removed.
     * @throws Exception
     */
    public void destroy() throws Exception {
        // unregister ourselves with the EventPublisher
        System.out.println("-----> Unregistering listener");
        eventPublisher.unregister(this);
    }

    /**
     * Receives any {@code IssueEvent}s sent by JIRA.
     * @param issueEvent the IssueEvent passed to us
     */
    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        long eventTypeId = issueEvent.getEventTypeId();
        // if it's an event we're interested in, log it
        System.out.println("-----> Event " + issueEvent);

        //TODO For linked projects we only care about the issues actually linked to!

        //TODO There are no events for when updating linked issues, so we need to poll somewhere

        /*
            TODO The following things will need recalculation of the target state of the issue:
             -ISSUE_CREATED_ID
             -ISSUE_MOVED_ID
             -ISSUE_REOPENED_ID/ISSUE_GENERICEVENT_ID/ISSUE_RESOLVED_ID, if any of the following fields changed
                * status changed
                * Rank changed

         */

        //CREATED, DELETED and MOVED do not have a worklog
        if (eventTypeId == EventType.ISSUE_CREATED_ID) {
            //Does not have a worklog
            onCreateEvent(issueEvent);
        } else if (eventTypeId == EventType.ISSUE_DELETED_ID) {
            //Does not have a worklog
            onDeleteEvent(issueEvent);
        } else if (eventTypeId == EventType.ISSUE_MOVED_ID) {
            //Has a worklog. We need to take into account the old values to delete the issue from the old project boards,
            //while we use the issue in the event to create the issue in the new project boards.
            onMoveEvent(issueEvent);
        } else if (eventTypeId == EventType.ISSUE_ASSIGNED_ID ||
                eventTypeId == EventType.ISSUE_UPDATED_ID ||
                eventTypeId == EventType.ISSUE_GENERICEVENT_ID ||
                eventTypeId == EventType.ISSUE_RESOLVED_ID ||
                eventTypeId == EventType.ISSUE_CLOSED_ID ||
                eventTypeId == EventType.ISSUE_REOPENED_ID) {
            //Which of these events gets triggered depends on the workflow for the project, and other factors.
            //E.g. in a normal workflow project, the ISSUE_RESOLVED_ID, ISSUE_CLOSED_ID, ISSUE_REOPENED_ID events
            //are triggered, while in the Kanban workflow those events use the ISSUE_GENERIC_EVENT_ID.
            //The same underlying fields are reported changed in the worklog though.
            //Another example is that if you just change the assignee, you get an ISSUE_ASSIGNED_ID event, but if you
            //change several fields (including the assignee) you get an event with ISSUE_UPDATED_ID and all the fields
            //affected in the worklog
            onWorklogEvent(issueEvent);
        }
    }

    private void onCreateEvent(IssueEvent issueEvent) {
        final Issue issue = issueEvent.getIssue();
        if (!isAffectedProject(issue.getProjectObject().getKey())) {
            return;
        }

        final JirbanIssueEvent event = JirbanIssueEvent.createCreateEvent(issue.getKey(), issue.getProjectObject().getKey(),
                issue.getIssueTypeObject().getName(), issue.getPriorityObject().getName(), issue.getSummary(),
                issue.getAssignee(), issue.getStatusObject().getName());
        passEventToBoardManager(event);

        //TODO there could be linked issues
    }

    private void onDeleteEvent(IssueEvent issueEvent) {
        final Issue issue = issueEvent.getIssue();
        if (!isAffectedProject(issue.getProjectObject().getKey())) {
            return;
        }

        final JirbanIssueEvent event = JirbanIssueEvent.createDeleteEvent(issue.getKey(), issue.getProjectObject().getKey());
        passEventToBoardManager(event);
    }

    private void onWorklogEvent(IssueEvent issueEvent) {
        final Issue issue = issueEvent.getIssue();
        if (!isAffectedProject(issue.getProjectObject().getKey())) {
            return;
        }

        //All the fields that changed, and only those, are in the change log.
        //For our created event, only set the fields that actually changed.
        String issueType = null;
        String priority = null;
        String summary = null;
        User assignee = null;
        boolean unassigned = false;
        String state = null;
        boolean rankOrStateChanged = false;
        List<GenericValue> changeItems = getWorkLog(issueEvent);
        for (GenericValue change : changeItems) {
            final String field = change.getString(CHANGE_LOG_FIELD);
            if (field.equals(CHANGE_LOG_ISSUETYPE)) {
                issueType = issue.getIssueTypeObject().getName();
            } else if (field.equals(CHANGE_LOG_PRIORITY)) {
                priority = issue.getPriorityObject().getName();
            } else if (field.equals(CHANGE_LOG_SUMMARY)) {
                summary = issue.getSummary();
            } else if (field.equals(CHANGE_LOG_ASSIGNEE)) {
                assignee = issue.getAssignee();
                if (assignee == null) {
                    unassigned = true;
                }
            } else if (field.equals(CHANGE_LOG_STATUS)) {
                rankOrStateChanged = true;
                state = issue.getStatusObject().getName();
            } else if (field.equals(CHANGE_LOG_RANK)) {
                rankOrStateChanged = true;
            }
        }
        final JirbanIssueEvent event = JirbanIssueEvent.createUpdateEvent(
                issue.getKey(), issue.getProjectObject().getKey(), issueType, priority,
                summary, assignee, unassigned, state, rankOrStateChanged);
        passEventToBoardManager(event);
    }

    private void onMoveEvent(IssueEvent issueEvent) {
        //This is kind of the same as the 'onWorklogEvent' but we also need to take into account the old value of the project
        //and remove from there if it is a board project. Also, if the new value is a board project we need to add it there.
        //So, it is a bit like a delete (although we need the worklog for that), and a create.

        //1) We need to inspect the change log to find the project we are deleting from
        String oldProjectCode = null;
        String oldIssueKey = null;
        List<GenericValue> changeItems = getWorkLog(issueEvent);
        for (GenericValue change : changeItems) {
            final String field = change.getString(CHANGE_LOG_FIELD);
            if (field.equals(CHANGE_LOG_PROJECT)) {
                String oldProjectId = change.getString(CHANGE_LOG_OLD_VALUE);
                Project project = projectManager.getProjectObj(Long.valueOf(oldProjectId));
                oldProjectCode = project.getKey();
            } else if (field.equals("Key")) {
                oldIssueKey = change.getString(CHANGE_LOG_OLD_STRING);
            } else {
                System.out.println(field + ": " + change.getString(field));
            }
        }

        if (isAffectedProject(oldProjectCode)) {
            final JirbanIssueEvent event = JirbanIssueEvent.createDeleteEvent(oldIssueKey, oldProjectCode);
            passEventToBoardManager(event);
        }

        //2) Then we can do a create on the project with the issue in the event
        onCreateEvent(issueEvent);
    }

    private List<GenericValue> getWorkLog(IssueEvent issueEvent) {
        final GenericValue changeLog = issueEvent.getChangeLog();
        if (changeLog == null) {
            return Collections.emptyList();
        }

        final List<GenericValue> changeItems;
        try {
            changeItems = changeLog.getDelegator().findByAnd("ChangeItem", EasyMap.build("group", changeLog.get("id")));
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        return changeItems;
    }

    private void passEventToBoardManager(JirbanIssueEvent event) {
        System.out.println(event);
    }

    private boolean isAffectedProject(String projectCode) {
        //TODO do some filtering of projects from the board configs above

        return true;
    }
}

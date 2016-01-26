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
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

/**
 * The listener listening to issue events, and delegating relevant ones to the issue table.
 *
 * @author Kabir Khan
 */
@Named("jirbanIssueEventListener")
public class JirbanIssueEventListener  implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(JirbanIssueEventListener.class);

    @ComponentImport
    private final EventPublisher eventPublisher;

    /**
     * Constructor.
     * @param eventPublisher injected {@code EventPublisher} implementation.
     */
    @Autowired
    public JirbanIssueEventListener(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
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
        Issue issue = issueEvent.getIssue();
        // if it's an event we're interested in, log it
        System.out.println("-----> Event " + issueEvent);

        //TODO do some filtering of projects from the board configs above

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

        StringBuilder sb = new StringBuilder("Created " + issue.getKey());
        sb.append("\n\t");
        sb.append(issue.getSummary());
        sb.append("\n\t");
        debugIssueType(sb, issue.getIssueTypeObject());
        sb.append("\n\t");
        debugPriority(sb, issue.getPriorityObject());
        sb.append("\n\t");
        debugState(sb, issue.getStatusObject());
        sb.append("\n\t");
        debugAssignee(sb, issue.getAssignee());
        System.out.println(sb.toString());

        //This does not have a worklog
        System.out.println("Worklog " + getWorkLog(issueEvent));

        //TODO there could be linked issues
    }

    private void onWorklogEvent(IssueEvent issueEvent) {
        final Issue issue = issueEvent.getIssue();
        StringBuilder sb = new StringBuilder(getEventTypeString(issueEvent) + issue.getKey());
        sb.append("\n\t");
        debugAssignee(sb, issue.getAssignee());

        //All the fields are in the worklog
        //TODO check the names of the fields

        System.out.println("Worklog " + getWorkLog(issueEvent));
    }

    private void onDeleteEvent(IssueEvent issueEvent) {
        final Issue issue = issueEvent.getIssue();
        StringBuilder sb = new StringBuilder("Deleted " + issue.getKey());

        //This does not have a worklog (we only need the key)
        //System.out.println("Worklog " + getWorkLog(issueEvent));
    }

    private void onMoveEvent(IssueEvent issueEvent) {
        final Issue issue = issueEvent.getIssue();

        StringBuilder sb = new StringBuilder("Moved " + issue.getKey());
        sb.append("\n\t");
        sb.append(issue.getSummary());
        sb.append("\n\t");
        debugIssueType(sb, issue.getIssueTypeObject());
        sb.append("\n\t");
        debugPriority(sb, issue.getPriorityObject());
        sb.append("\n\t");
        debugState(sb, issue.getStatusObject());
        sb.append("\n\t");
        debugAssignee(sb, issue.getAssignee());
        System.out.println(sb.toString());

        System.out.println("Generic " + getWorkLog(issueEvent));
        //This is kind of the same as the 'onWorklogEvent' but we also need to take into account the old value of the project
        //and remove from there if it is a board project. Also, if the new value is a board project we need to add it there.
        //So, it is a bit like a delete (although we need the worklog for that), and a create.

        /*
         Moved SKBG-18
            sdsdsdsd
            type='Bug'
            priority='Major'
            state='Backlog'
            assignee='admin'
         Number of changes: 5
         changes Issue SKBG-18 field project has been updated from Sample Kanban Project to Sample Kanban Bugs
         changes Issue SKBG-18 field Key has been updated from SKP-23 to SKBG-18
         changes Issue SKBG-18 field issuetype has been updated from Story to Bug
         changes Issue SKBG-18 field Workflow has been updated from Agile Simplified Workflow for Project SKP to Agile Simplified Workflow for Project SKBG
       */
    }

    private String getEventTypeString(IssueEvent issueEvent) {
        if (issueEvent.getEventTypeId().equals(EventType.ISSUE_UPDATED_ID)) {
            return "Updated ";
        } else if (issueEvent.getEventTypeId().equals(EventType.ISSUE_ASSIGNED_ID)) {
            return "Assigned ";
        } else if (issueEvent.getEventTypeId().equals(EventType.ISSUE_GENERICEVENT_ID)) {
            return "Generic Event ";
        }

        return null;
    }


    private void debugIssueType(StringBuilder sb, IssueType issueType) {
        sb.append("type='");
        sb.append(issueType != null ? issueType.getName() : "None");
        sb.append("'");
    }

    private void debugPriority(StringBuilder sb, Priority priority) {
        sb.append("priority='");
        sb.append(priority != null ? priority.getName() : "None");
        sb.append("'");
    }

    private void debugState(StringBuilder sb, Status status) {
        sb.append("state='");
        sb.append(status != null ? status.getName() : "None");
        sb.append("'");
    }


    private void debugAssignee(StringBuilder sb, User assignee) {
        sb.append("assignee='");
        sb.append(assignee != null ? assignee.getName() : "None");
        sb.append("'");
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

        System.out.println("Number of changes: " + changeItems.size());
        for (GenericValue change : changeItems) {
            String field = change.getString("field");
            String oldstring = change.getString("oldstring");

            String newstring = change.getString("newstring");
            StringBuilder fullstring = new StringBuilder();
            fullstring.append("Issue ");
            fullstring.append(issueEvent.getIssue().getKey());
            fullstring.append(" field ");
            fullstring.append(field);
            fullstring.append(" has been updated from ");
            fullstring.append(oldstring);
            fullstring.append(" to ");
            fullstring.append(newstring);
            System.out.println("changes " + fullstring.toString());
        }
        return changeItems;
    }
}

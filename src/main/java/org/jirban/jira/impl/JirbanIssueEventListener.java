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
package org.jirban.jira.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.jirban.jira.JirbanLogger;
import org.jirban.jira.api.BoardManager;
import org.jirban.jira.api.NextRankedIssueUtil;
import org.jirban.jira.impl.board.CustomFieldUtil;
import org.jirban.jira.impl.board.CustomFieldValue;
import org.jirban.jira.impl.config.CustomFieldConfig;
import org.jirban.jira.impl.config.ParallelTaskCustomFieldConfig;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
//import com.atlassian.greenhopper.service.lexorank.balance.LexoRankBalanceEvent;
import com.atlassian.greenhopper.service.lexorank.balance.LexoRankChangeEvent;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.event.JiraEvent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.index.ReindexIssuesCompletedEvent;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

/**
 * The listener listening to issue events, and delegating relevant ones to the issue table.
 * When creating/updating an issue a series of events occur in the same thread as part of handling the request. The two
 * important ones for our purposes are:
 * <ol>
 *     <li>The {@code IssueEvent} - Here we grab the changes to occur in {@link #onIssueEvent(IssueEvent)} and
 *     construct the needed {@code JirbanIssueEvent} instances.</li>
 *     <li>The {@code ReindexIssuesCompletedEvent} - This is similar to an after commit, where Jira has completed
 *     updating the state of the issues.</li>
 * </ol>
 * <p/>
 * The {@code JirbanIssueEvent} instances created in the first step are used to update our board caches when receiving
 * the second event. Note that this split is *ONLY NECESSARY* when an action is performed which updates the status/rank
 * of an issue, since when rebuilding the board we need to query the issues by status, and the status updates are only
 * available after the second step. All other changed data (e.g. assignee, issue type, summary, priority etc.) is
 * available in the first step. So for a create, or an update involving a state change or a rank change we delay updating
 * the board caches until we received the {@code ReindexIssuesCompletedEvent}. For everything else, we update the board
 * caches when we receive the first {@code IssueEvent}.
 * <p/>
 * However, if an issue is only re-ranked via Jirban or Jira Agile's board, the {@code ReindexIssuesCompletedEvent} and
 * the {@code IssueEvent} can come in any order. However a 'pure' re-rank is initiated by a {@code LexoRankBalanceEvent},
 * so we use JirbanEventWrapper to track that we have all the needed information.
 *
 *
 *
 * @author Kabir Khan
 */
@Named("jirbanIssueEventListener")
public class JirbanIssueEventListener implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(JirbanIssueEventListener.class);

    private static final String CHANGE_LOG_FIELD = "field";
    private static final String CHANGE_LOG_ISSUETYPE = "issuetype";
    private static final String CHANGE_LOG_PRIORITY = "priority";
    private static final String CHANGE_LOG_SUMMARY = "summary";
    private static final String CHANGE_LOG_ASSIGNEE = "assignee";
    private static final String CHANGE_LOG_STATUS = "status";
    private static final String CHANGE_LOG_OLD_STRING = "oldstring";
    private static final String CHANGE_LOG_NEW_STRING = "newstring";
    private static final String CHANGE_LOG_NEW_VALUE = "newvalue";
    private static final String CHANGE_LOG_RANK = "Rank";
    private static final String CHANGE_LOG_PROJECT = "project";
    private static final String CHANGE_LOG_OLD_VALUE = "oldvalue";
    private static final String CHANGE_LOG_ISSUE_KEY = "Key";
    private static final String CHANGE_LOG_COMPONENT = "Component";
    private static final String CHANGE_LOG_FIELDTYPE = "fieldtype";
    private static final String CHANGE_LOG_CUSTOM = "custom";
    private static final String CHANGE_LOG_LABELS = "labels";
    private static final String CHANGE_LOG_FIX_VERSIONS = "Fix Version";

    @ComponentImport
    private final EventPublisher eventPublisher;

    @ComponentImport
    private final ProjectManager projectManager;

    private final BoardManager boardManager;

    private final NextRankedIssueUtil nextRankedIssueUtil;

    private final WrappedThreadLocal<JirbanEventWrapper> delayedEvents = new WrappedThreadLocal<>();


    /**
     * Constructor.
     * @param eventPublisher injected {@code EventPublisher} implementation.
     * @param projectManager injected {@code ProjectManager} implementation.
     * @param boardManager injected {@code BoardManager} implementation.
     */
    @Autowired
    public JirbanIssueEventListener(EventPublisher eventPublisher,
                                    ProjectManager projectManager, BoardManager boardManager, NextRankedIssueUtil nextRankedIssueUtil) {
        this.eventPublisher = eventPublisher;
        this.projectManager = projectManager;
        this.boardManager = boardManager;
        this.nextRankedIssueUtil = nextRankedIssueUtil;
    }

    /**
     * Called when the plugin has been enabled.
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        // register ourselves with the EventPublisher
        eventPublisher.register(this);
    }

    /**
     * Called when the plugin is being disabled or removed.
     * @throws Exception
     */
    public void destroy() throws Exception {
        // unregister ourselves with the EventPublisher
        eventPublisher.unregister(this);
        delayedEvents.clearAll();
    }

    /**
     * This is the event that initiates a re-rank.
     * This will be followed by both an IssueEvent and a ReindexIssuesCompletedEvent, although the order of the two
     * is not set in stone.
     *
     * @param event the rerank event
     */
    @EventListener
    public void onRankEvent(LexoRankChangeEvent event) {
        JirbanLogger.LOGGER.debug("LexoRankChangeEvent on thread {}", Thread.currentThread().getName());

        JirbanEventWrapper wrapper = new JirbanEventWrapper(true);
        delayedEvents.set(wrapper);

    }

    /**
     * Receives any {@code ReindexIssuesCompletedEvent}s sent by JIRA.
     *
     * @param event the event passed to us
     */
    @EventListener
    public void onEvent(ReindexIssuesCompletedEvent event) throws IndexException {
        JirbanLogger.LOGGER.debug("ReindexIssuesCompletedEvent on thread {}", Thread.currentThread().getName());

        JirbanEventWrapper delayedEvent = delayedEvents.get();
        if (delayedEvent != null) {
            delayedEvent.reindexed = true;
            if (delayedEvent.isComplete()) {
                try {
                    JirbanLogger.LOGGER.debug("Handle delayed event {}", delayedEvent.issueEvent.getIssueKey());
                    boardManager.handleEvent(delayedEvent.issueEvent, nextRankedIssueUtil);
                } finally {
                    delayedEvents.remove();
                }
            }
        }
    }

    /**
     * Receives any {@code IssueEvent}s sent by JIRA
     * @param issueEvent the event passed to us
     */
    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) throws IndexException {
        JirbanLogger.LOGGER.debug("IssueEvent {} on thread {}", issueEvent, Thread.currentThread().getName());

        long eventTypeId = issueEvent.getEventTypeId();
        // if it's an event we're interested in, log it

        //There are no events for when updating linked issues. Instead we invalidate the boards every five minutes in
        //BoardManagerImpl which forces a full refresh of the board which will bring in linked issues

        //CREATED, DELETED and MOVED do not have a worklog
        if (eventTypeId == EventType.ISSUE_CREATED_ID) {
            //Does not have a worklog
            onCreateEvent(issueEvent);

            //Only relevant for updates (of state/rank)
            delayedEvents.remove();
        } else if (eventTypeId == EventType.ISSUE_DELETED_ID) {
            //Does not have a worklog
            onDeleteEvent(issueEvent);

            //Only relevant for updates (of state/rank)
            delayedEvents.remove();
        } else if (eventTypeId == EventType.ISSUE_MOVED_ID) {
            //Has a worklog. We need to take into account the old values to delete the issue from the old project boards,
            //while we use the issue in the event to create the issue in the new project boards.
            onMoveEvent(issueEvent);

            //Only relevant for updates (of state/rank)
            delayedEvents.remove();
        } else if (eventTypeId == EventType.ISSUE_ASSIGNED_ID ||
                eventTypeId == EventType.ISSUE_UPDATED_ID ||
                eventTypeId == EventType.ISSUE_GENERICEVENT_ID ||
                eventTypeId == EventType.ISSUE_RESOLVED_ID ||
                eventTypeId == EventType.ISSUE_CLOSED_ID ||
                eventTypeId == EventType.ISSUE_REOPENED_ID ||
                eventTypeId == EventType.ISSUE_WORKSTARTED_ID ||
                eventTypeId == EventType.ISSUE_WORKSTOPPED_ID) {
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

    private void onCreateEvent(IssueEvent issueEvent) throws IndexException {
        final Issue issue = issueEvent.getIssue();
        if (!isAffectedProject(issue.getProjectObject().getKey())) {
            return;
        }

        final Set<CustomFieldConfig> customFields = boardManager.getCustomFieldsForCreateEvent(issue.getProjectObject().getKey());
        Map<Long, String> values = null;
        if (customFields.size() > 0) {
            values = new HashMap<>();
            for (CustomFieldConfig cfg : customFields) {
                if (!values.containsKey(cfg.getId())) {
                    final Object value = issue.getCustomFieldValue(cfg.getJiraCustomField());
                    CustomFieldUtil customFieldUtil = CustomFieldUtil.getUtil(cfg);
                    String stringValue = value == null ? null : customFieldUtil.getCreateEventValue(value);
                    values.put(cfg.getId(), stringValue);
                }
            }
        }

        final Set<ParallelTaskCustomFieldConfig> parallelFields = boardManager.getParallelTaskFieldsForCreateEvent(issue.getProjectObject().getKey());
        if (parallelFields.size() > 0) {
            if (values == null) {
                values = new HashMap<>();
            }
            for (ParallelTaskCustomFieldConfig cfg : parallelFields) {
                if (!values.containsKey(cfg.getId())) {
                    //A field cannot be both a custom field and a parallel task field
                    String value = CustomFieldValue.getParallelTaskCustomFieldValue(issue, cfg.getJiraCustomField(), cfg.getId().toString());
                    if (value != null) {
                        values.put(cfg.getId(), value);
                    }
                }
            }
        }

        final JirbanIssueEvent event = JirbanIssueEvent.createCreateEvent(issue.getKey(), issue.getProjectObject().getKey(),
                issue.getIssueTypeObject().getName(), issue.getPriorityObject().getName(), issue.getSummary(),
                issue.getAssignee(), issue.getComponentObjects(), issue.getLabels(), issue.getFixVersions(),
                issue.getStatusObject().getName(), values);

        passEventToBoardManagerOrDelay(event);

        //TODO there could be linked issues
    }

    private void onDeleteEvent(IssueEvent issueEvent) throws IndexException {
        final Issue issue = issueEvent.getIssue();
        if (!isAffectedProject(issue.getProjectObject().getKey())) {
            return;
        }

        final JirbanIssueEvent event = JirbanIssueEvent.createDeleteEvent(issue.getKey(), issue.getProjectObject().getKey());
        passEventToBoardManagerOrDelay(event);
    }

    private void onWorklogEvent(IssueEvent issueEvent) throws IndexException {
        final Issue issue = issueEvent.getIssue();
        if (!isAffectedProject(issue.getProjectObject().getKey())) {
            delayedEvents.remove();
            return;
        }

        //All the fields that changed, and only those, are in the change log.
        //For our created event, only set the fields that actually changed.
        String issueType = null;
        String priority = null;
        String summary = null;
        ApplicationUser assignee = null;
        Collection<ProjectComponent> components = null;
        Collection<Label> labels = null;
        Collection<Version> fixVersions = null;
        String oldState = null;
        String state = null;
        boolean reranked = false;
        Map<Long, String> customFieldValues = null;

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
                    assignee = JirbanIssueEvent.UNASSIGNED;
                }
            } else if (field.equals(CHANGE_LOG_STATUS)) {
                state = issue.getStatusObject().getName();
                oldState = change.getString(CHANGE_LOG_OLD_STRING);
            } else if (field.equals(CHANGE_LOG_RANK)) {
                reranked = true;
            } else if (field.equals(CHANGE_LOG_COMPONENT)) {
                components = issue.getComponentObjects();
            } else if (field.equals(CHANGE_LOG_LABELS)) {
                labels = issue.getLabels();
            } else if (field.equals(CHANGE_LOG_FIX_VERSIONS)) {
                fixVersions = issue.getFixVersions();
            } else if (change.get(CHANGE_LOG_FIELDTYPE).equals(CHANGE_LOG_CUSTOM)) {
                Set<CustomFieldConfig> customFieldConfigs = boardManager.getCustomFieldsForUpdateEvent(issue.getProjectObject().getKey(), field);
                if (customFieldConfigs.size() > 0) {
                    if (customFieldValues == null) {
                        customFieldValues = new HashMap<>();
                    }
                    for (CustomFieldConfig cfg : customFieldConfigs) {
                        CustomFieldUtil customFieldUtil = CustomFieldUtil.getUtil(cfg);
                        String key = customFieldUtil.getUpdateEventValue(
                                (String) change.get(CHANGE_LOG_NEW_VALUE), (String) change.get(CHANGE_LOG_NEW_STRING));
                        customFieldValues.put(cfg.getId(), key);
                    }
                }

                Set<ParallelTaskCustomFieldConfig> parallelTaskConfigs
                        = boardManager.getParallelTaskFieldsForUpdateEvent(issue.getProjectObject().getKey(), field);
                if (parallelTaskConfigs.size() > 0) {
                    if (customFieldValues == null) {
                        customFieldValues = new HashMap<>();
                    }
                    for (ParallelTaskCustomFieldConfig cfg : parallelTaskConfigs) {
                        customFieldValues.put(cfg.getId(), (String) change.get(CHANGE_LOG_NEW_VALUE));
                    }
                }
            }
        }
        final JirbanIssueEvent event = JirbanIssueEvent.createUpdateEvent(
                issue.getKey(), issue.getProjectObject().getKey(), issueType, priority,
                summary, assignee, components, labels, fixVersions,
                //Always pass in the existing/old state of the issue
                oldState != null ? oldState : issue.getStatusObject().getName(),
                state, reranked, customFieldValues);
        passEventToBoardManagerOrDelay(event);
    }

    private void onMoveEvent(IssueEvent issueEvent) throws IndexException {
        //This is kind of the same as the 'onWorklogEvent' but we also need to take into account the old value of the project
        //and remove from there if it is a board project. Also, if the new value is a board project we need to add it there.
        //So, it is a bit like a delete (although we need the worklog for that), and a create.

        //1) We need to inspect the change log to find the project we are deleting from
        String oldProjectCode = null;
        String oldIssueKey = null;
        String newState = null;
        List<GenericValue> changeItems = getWorkLog(issueEvent);
        for (GenericValue change : changeItems) {
            final String field = change.getString(CHANGE_LOG_FIELD);
            if (field.equals(CHANGE_LOG_PROJECT)) {
                String oldProjectId = change.getString(CHANGE_LOG_OLD_VALUE);
                Project project = projectManager.getProjectObj(Long.valueOf(oldProjectId));
                oldProjectCode = project.getKey();
            } else if (field.equals(CHANGE_LOG_ISSUE_KEY)) {
                oldIssueKey = change.getString(CHANGE_LOG_OLD_STRING);
            } else if (field.equals(CHANGE_LOG_ISSUETYPE)){
                newState = change.getString(CHANGE_LOG_NEW_STRING);
            }
        }

        if (isAffectedProject(oldProjectCode)) {
            final JirbanIssueEvent event = JirbanIssueEvent.createDeleteEvent(oldIssueKey, oldProjectCode);
            passEventToBoardManagerOrDelay(event);
        }

        //2) Then we can do a create on the project with the issue in the event
        final Issue issue = issueEvent.getIssue();
        onCreateEvent(issueEvent);
        if (!isAffectedProject(issue.getProjectObject().getKey())) {
            return;
        }
        //Note that the status column in the event issue isn't up to date yet, we need to get it from the change log
        //if it was updated
        newState = newState == null ? issue.getStatusObject().getName() : newState;

        final JirbanIssueEvent event = JirbanIssueEvent.createCreateEvent(issue.getKey(), issue.getProjectObject().getKey(),
                issue.getIssueTypeObject().getName(), issue.getPriorityObject().getName(), issue.getSummary(),
                issue.getAssignee(), issue.getComponentObjects(), issue.getLabels(), issue.getFixVersions(),
                newState, Collections.emptyMap());
        passEventToBoardManagerOrDelay(event);
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

    private void passEventToBoardManagerOrDelay(JirbanIssueEvent event) throws IndexException {
        if (event.isRecalculateState()) {
            JirbanLogger.LOGGER.debug("Possible delayed event {}", event.getIssueKey());
            //Possible delay
            JirbanEventWrapper wrapper = delayedEvents.get();
            if (wrapper == null) {
                //We are not a rerank, but a state move. Delay processing of the event until the ReindexIssuesCompletedEvent
                //is received. For this use-case we always receive the IssueEvent before the ReindexIssuesCompletedEvent.
                wrapper = new JirbanEventWrapper(false);
                wrapper.issueEvent = event;
                delayedEvents.set(wrapper);
                JirbanLogger.LOGGER.debug("Delaying event {}", event.getIssueKey());
            } else {
                //We already have an event wrapper. The only thing which would have put it here is if a ranking op was done
                //so that the LexoRankBalanceEvent was triggered.
                //The IssueEvent
                wrapper.issueEvent = event;
                if (wrapper.isComplete()) {
                    //It is complete
                    JirbanLogger.LOGGER.debug("Handle delayed event {}", event.getIssueKey());
                    boardManager.handleEvent(event, nextRankedIssueUtil);
                    delayedEvents.remove();
                }
            }
        } else {
            //We can handle the event right away
            JirbanLogger.LOGGER.debug("Handling immediate event {}", event.getIssueKey());
            boardManager.handleEvent(event, nextRankedIssueUtil);
        }
    }

    private boolean isAffectedProject(String projectCode) {
        return boardManager.hasBoardsForProjectCode(projectCode);
    }

    /**
     * Alternative thread local implementation to avoid possible memory leaks on undeploy
     *
     * @param <T>
     */
    private static class WrappedThreadLocal<T> {
        private final Map<Thread, T> delayedEvents = Collections.synchronizedMap(new IdentityHashMap<>());

        void set(T value) {
            JirbanLogger.LOGGER.debug("Setting item on thread {}", Thread.currentThread().getName());
            delayedEvents.put(Thread.currentThread(), value);
        }

        T get() {
            return delayedEvents.get(Thread.currentThread());
        }

        void remove() {
            JirbanLogger.LOGGER.debug("Removing item on thread {}", Thread.currentThread().getName());
            delayedEvents.remove(Thread.currentThread());
        }

        void clearAll() {
            delayedEvents.clear();
        }
    }

    /**
     * Deal with Jira's strange event mechanism
     *
     *
     */
    private static class JirbanEventWrapper {
        private final boolean rerankEvent;

        private volatile JirbanIssueEvent issueEvent;

        private volatile boolean reindexed;

        JirbanEventWrapper(boolean rerankEvent) {
            this.rerankEvent = rerankEvent;
        }

        public boolean isComplete() {
            /* Fix for issue #90. Keep this old code commented out, in case I really meant to do something else
            if (rerankEvent) {
                return issueEvent != null && reindexed;
            } else {
                return issueEvent != null && reindexed;
            }
            */
            return issueEvent != null && reindexed;
        }
    }
}

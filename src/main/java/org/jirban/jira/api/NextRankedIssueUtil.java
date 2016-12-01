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

package org.jirban.jira.api;

import org.jirban.jira.impl.config.BoardProjectConfig;

import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;

/**
 * When an issue is reranked this util figures out the following issue in the ranking order
 * to determine the insertion point. It is implemented as a bean to be able to mock it for unit tests.
 *
 * @author Kabir Khan
 */
public interface NextRankedIssueUtil {
    /**
     * Finds the next issue after the {@code issueKey} in the current ranking
     *
     *
     * @param projectConfig the project config of the project containing the issue
     * @param issueKey the issue key to determine the rank of
     * @return the issue key appearing after {@code issueKey} or {@code null} if this is the last issue
     */
    String findNextRankedIssue(BoardProjectConfig projectConfig, ApplicationUser boardOwner, String issueKey) throws SearchException;

}

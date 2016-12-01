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

package org.jirban.jira.impl.board;

/**
 * Strategy for how to load things like custom fields
 *
 * @author Kabir Khan
 */
interface IssueLoadStrategy {

    /**
     * Called for each issue
     * @param issue the Jira issue
     * @param builder the builder used to load the issue
     */
    void handle(com.atlassian.jira.issue.Issue issue, Issue.Builder builder);

    /**
     * Called when all issues have been loaded
     */
    void finish();
}

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

import java.util.Map;

import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.board.SortedParallelTaskFieldOptions;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;

/**
 * Used to determine the parallel tasks for a project. Implemented as a bean to be able to mock it out for unit tests
 * @author Kabir Khan
 */
public interface ProjectParallelTaskOptionsLoader {

    /**
     * Load the parallel task value lookup tables for a project. If there are none {@code null} is returned. Otherwise an
     * ordered map is returned. The key is the custom field name, the entry is another ordered map, where the key is the
     * option key, and the entry is the value.
     *
     * @param jiraInjectables
     * @param boardConfig
     * @param projectConfig
     * @return
     */
    Map<String, SortedParallelTaskFieldOptions> loadValues(JiraInjectables jiraInjectables, BoardConfig boardConfig, BoardProjectConfig projectConfig);
}

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

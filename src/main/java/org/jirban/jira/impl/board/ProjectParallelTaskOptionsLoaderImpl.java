package org.jirban.jira.impl.board;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Named;

import org.jirban.jira.api.ProjectParallelTaskOptionsLoader;
import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.ParallelTaskConfig;
import org.jirban.jira.impl.config.ParallelTaskCustomFieldConfig;

import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.search.SearchContext;
import com.atlassian.jira.project.Project;

/**
 * @author Kabir Khan
 */
@Named("JirbanProjectParallelTaskValueLoader")
public class ProjectParallelTaskOptionsLoaderImpl implements ProjectParallelTaskOptionsLoader {

    public Map<String, SortedParallelTaskFieldOptions> loadValues(JiraInjectables jiraInjectables, BoardConfig boardConfig, BoardProjectConfig projectConfig) {
        Map<String, SortedParallelTaskFieldOptions> parallelTaskValues = new LinkedHashMap<>();
        ParallelTaskConfig parallelTaskConfig = projectConfig.getParallelTaskConfig();
        if (parallelTaskConfig != null) {
            for (ParallelTaskCustomFieldConfig config : parallelTaskConfig.getConfigs().values()) {
                CustomField customField = config.getJiraCustomField();
                Project jiraProject = jiraInjectables.getProjectManager().getProjectByCurrentKey(projectConfig.getCode());

                SearchContext searchContext = jiraInjectables.getSearchContextFactory().create(null, Collections.singletonList(jiraProject.getId()), Collections.emptyList());
                FieldConfig fieldConfig = customField.getReleventConfig(searchContext);
                Options options = jiraInjectables.getOptionsManager().getOptions(fieldConfig);

                SortedParallelTaskFieldOptions.Builder builder = new SortedParallelTaskFieldOptions.Builder(config);
                for (Option option : options) {
                    CustomFieldValue value = new ParallelTaskProgressOption(config.getName(), option.getOptionId().toString(), option.getValue());
                    builder.addOption(value);
                }
                parallelTaskValues.put(config.getName(), builder.build());
            }
        }
        return Collections.unmodifiableMap(parallelTaskValues);
    }
}

package org.jirban.jira.impl.board;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jirban.jira.api.ProjectParallelTaskOptionsLoader;
import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.ParallelTaskConfig;
import org.jirban.jira.impl.config.ParallelTaskCustomFieldConfig;
import org.junit.Assert;

import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
public class ProjectParallelTaskOptionsLoaderBuilder {
    Map<String, Map<Long, Map<String, String>>> customFieldOptionsByProject = new LinkedHashMap<>();

    public ProjectParallelTaskOptionsLoaderBuilder addCustomFieldOption(String projectName, Long customFieldId, String optionKey, String optionName) {
        Map<Long, Map<String, String>> optionsByCustomField = customFieldOptionsByProject.computeIfAbsent(projectName, k -> new LinkedHashMap<Long, Map<String, String>>());
        Map<String, String> options = optionsByCustomField.computeIfAbsent(customFieldId, k -> new LinkedHashMap<String, String>());
        options.put(optionKey, optionName);
        return this;
    }

    public ProjectParallelTaskOptionsLoader build() {
        return new ProjectParallelTaskOptionsLoader() {
            @Override
            public Map<String, SortedParallelTaskFieldOptions> loadValues(JiraInjectables jiraInjectables, BoardConfig boardConfig, BoardProjectConfig projectConfig) {
                Map<String, SortedParallelTaskFieldOptions> parallelTaskValues = new LinkedHashMap<>();

                ParallelTaskConfig parallelTaskConfig = projectConfig.getParallelTaskConfig();

                if (parallelTaskConfig != null) {
                    Map<Long, Map<String, String>> optionsByCustomField = customFieldOptionsByProject.get(projectConfig.getCode());
                    Assert.assertNotNull(optionsByCustomField);

                    for (ParallelTaskCustomFieldConfig config : parallelTaskConfig.getConfigs().values()) {
                        CustomField customField = config.getJiraCustomField();

                        Map<String, String> options = optionsByCustomField.get(config.getId());
                        Assert.assertNotNull(options);

                        SortedParallelTaskFieldOptions.Builder builder = new SortedParallelTaskFieldOptions.Builder(config);
                        for (Map.Entry<String, String> option : options.entrySet()) {
                            CustomFieldValue value = new ParallelTaskProgressOption(config.getName(), option.getKey(), option.getValue());
                            builder.addOption(value);
                        }
                        parallelTaskValues.put(config.getName(), builder.build());
                    }
                }
                return Collections.unmodifiableMap(parallelTaskValues);            }
        };
    }
}

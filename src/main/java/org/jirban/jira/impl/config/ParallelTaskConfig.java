package org.jirban.jira.impl.config;

import static org.jirban.jira.impl.Constants.DISPLAY;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.util.IndexedMap;

/**
 * @author Kabir Khan
 */
public class ParallelTaskConfig {
    private final CustomFieldRegistry<ParallelTaskCustomFieldConfig> configs;
    private final IndexedMap<String, ParallelTaskCustomFieldConfig> indexedConfigs;


    ParallelTaskConfig(Map<String, ParallelTaskCustomFieldConfig> configs) {
        this.configs = new CustomFieldRegistry<>(configs);
        this.indexedConfigs = new IndexedMap<>(configs);
    }

    public CustomFieldRegistry<ParallelTaskCustomFieldConfig> getConfigs() {
        return configs;
    }

    public Integer getIndex(String key) {
        return indexedConfigs.getIndex(key);
    }

    public ParallelTaskCustomFieldConfig getCustomFieldObjectForJiraName(String jiraCustomFieldName) {
        return configs.getForJiraName(jiraCustomFieldName);
    }

    public static ParallelTaskConfig load(JiraInjectables jiraInjectables, CustomFieldRegistry<CustomFieldConfig> customFields, ModelNode parallelTask) {
        if (parallelTask.getType() != ModelType.LIST) {
            throw new JirbanValidationException("The parallel-tasks fields element must be an array");
        }

        Map<String, ParallelTaskCustomFieldConfig> configs = new LinkedHashMap<>();
        final List<ModelNode> customConfigs = parallelTask.asList();
        final Set<String> seenTaskCodes = new HashSet<>();
        final Set<String> seenTaskNames = new HashSet<>();
        for (ModelNode customConfig : customConfigs) {
            final ParallelTaskCustomFieldConfig customFieldConfig =
                    CustomFieldConfigImpl.loadParallelTaskCustomFieldConfig(jiraInjectables, customConfig);
            final CustomFieldConfig exisiting = customFields.getForJiraId(customFieldConfig.getId());
            if (exisiting != null) {
                throw new JirbanValidationException("The custom field with id " +
                        customFieldConfig.getId() + "used in parallel-tasks is already " +
                        "used in custom field " + exisiting.getName() + "' which is not allowed.");
            }
            configs.put(customFieldConfig.getName(), customFieldConfig);
            if (!seenTaskCodes.add(customFieldConfig.getCode())) {
                throw new JirbanValidationException("Codes must be unique within the parallel-tasks fields. '"
                        + customFieldConfig.getCode() + "' was used more than once.");
            }
            if (!seenTaskNames.add(customFieldConfig.getName())) {
                throw new JirbanValidationException("Names must be unique within the parallel-tasks fields. '"
                        + customFieldConfig.getName() + "' was used more than once.");
            }
        }

        return new ParallelTaskConfig(configs);
    }

    public ModelNode serializeForConfig() {
        ModelNode list = new ModelNode().setEmptyList();
        for (ParallelTaskCustomFieldConfig config : configs.values()) {
            ModelNode entry = config.serializeForConfig();
            entry.get(DISPLAY).set(config.getCode());
            list.add(entry);
        }
        return list;
    }
}

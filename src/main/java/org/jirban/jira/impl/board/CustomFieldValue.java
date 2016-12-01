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

import static org.jirban.jira.impl.Constants.KEY;
import static org.jirban.jira.impl.Constants.VALUE;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanLogger;
import org.jirban.jira.impl.config.CustomFieldConfig;
import org.jirban.jira.impl.config.ParallelTaskConfig;
import org.jirban.jira.impl.config.ParallelTaskCustomFieldConfig;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption;
import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
public class CustomFieldValue {

    private final String customFieldName;
    private final String key;
    private final String value;

    //Jira's event mechanism seems to use an empty string to unset custom fields
    public static final String UNSET_VALUE = "";

    protected CustomFieldValue(String customFieldName, String key, String value) {
        this.customFieldName = customFieldName;
        this.key = key;
        this.value = value;
    }

    static Map<String, CustomFieldValue> loadCustomFieldValues(final BoardProject.Accessor project, final Issue issue) {
        final List<String> customFieldNames = project.getConfig().getCustomFieldNames();
        if (customFieldNames.size() == 0) {
            return Collections.emptyMap();
        }

        final Map<String, CustomFieldValue> fields = new HashMap<>(customFieldNames.size());
        for (String customFieldName : customFieldNames) {
            CustomFieldConfig customFieldConfig = project.getBoard().getConfig().getCustomFieldConfigForJirbanName(customFieldName);
            Object customFieldValue = issue.getCustomFieldValue(customFieldConfig.getJiraCustomField());
            if (customFieldValue == null) {
                continue;
            }
            final CustomFieldValue customField = project.getCustomFieldValue(customFieldConfig, customFieldValue);
            fields.put(customFieldName, customField);
        }
        return fields.size() > 0 ? fields : Collections.emptyMap();
    }

    static void loadParallelTaskValues(BoardProject.Accessor project, Issue issue, org.jirban.jira.impl.board.Issue.Builder builder) {
        if (project.getConfig().getParallelTaskConfig() == null) {
            return;
        }
        ParallelTaskConfig parallelTaskConfig = project.getConfig().getParallelTaskConfig();

        Map<String, SortedParallelTaskFieldOptions> parallelTaskValues = project.getParallelTaskValues();
        for (Map.Entry<String, SortedParallelTaskFieldOptions> fieldEntry : parallelTaskValues.entrySet()) {
            CustomFieldConfig customFieldConfig = parallelTaskConfig.getConfigs().getForJirbanName(fieldEntry.getKey());
            String value = getParallelTaskCustomFieldValue(issue, customFieldConfig.getJiraCustomField(), fieldEntry.getKey());
            if (value == null) {
                continue;
            }
            final int optionIndex = fieldEntry.getValue().getIndex(value);
            final int taskFieldIndex = parallelTaskConfig.getIndex(fieldEntry.getKey());
            builder.setParallelTaskFieldValue(taskFieldIndex, optionIndex);
        }
    }

    public static String getParallelTaskCustomFieldValue(Issue issue, CustomField customField, String fieldKey) {
        Object customFieldValue = issue.getCustomFieldValue(customField);
        if (customFieldValue == null) {
            return null;
        }
        //The type of this varies across instances?
        if (customFieldValue instanceof String) {
            return  (String)customFieldValue;
        } else if (customFieldValue instanceof LazyLoadedOption) {
            LazyLoadedOption option = (LazyLoadedOption)customFieldValue;
            return String.valueOf(option.getOptionId());
        } else {
            JirbanLogger.LOGGER.warn("Unhandled field type " + customFieldValue.getClass());
        }
        return null;
    }

    static Map<String, CustomFieldValue> loadCustomFieldValues(final BoardProject.Accessor project, final Map<Long, String> customFieldValues) {
        final List<String> customFieldNames = project.getConfig().getCustomFieldNames();
        if (customFieldNames.size() == 0) {
            return Collections.emptyMap();
        }

        final Map<String, CustomFieldValue> fields = new HashMap<>(customFieldNames.size());
        for (String customFieldName : customFieldNames) {
            CustomFieldConfig customFieldConfig = project.getBoard().getConfig().getCustomFieldConfigForJirbanName(customFieldName);
            if (customFieldConfig != null) {
                String value = customFieldValues.get(customFieldConfig.getId());

                if (value != null) {
                    final CustomFieldValue customFieldValue;
                    if (value.equals("")) {
                        customFieldValue = null;
                    } else {
                        customFieldValue =
                                project.getCustomFieldValue(customFieldConfig, value);
                    }
                    fields.put(customFieldName, customFieldValue);
                }
            }
        }
        return fields.size() > 0 ? fields : Collections.emptyMap();
    }

    static Map<Integer, Integer> loadParallelTaskValues(boolean create, final BoardProject.Accessor project, final Map<Long, String> customFieldValues) {
        final Map<Integer, Integer> parallelTaskValues = new HashMap<>();

        if (project.getConfig().getParallelTaskConfig() == null) {
            return Collections.emptyMap();
        }

        ParallelTaskConfig parallelTaskConfig = project.getConfig().getParallelTaskConfig();

        if (parallelTaskConfig == null) {
            return Collections.emptyMap();
        }

        for (ParallelTaskCustomFieldConfig customFieldConfig : parallelTaskConfig.getConfigs().values()) {
            String value = customFieldValues.get(customFieldConfig.getId());

            if (value != null) {
                int fieldIndex = parallelTaskConfig.getIndex(customFieldConfig.getName());
                SortedParallelTaskFieldOptions options = project.getParallelTaskValues().get(customFieldConfig.getName());
                int optionIndex = options.getIndex(value);
                parallelTaskValues.put(fieldIndex, optionIndex);
            } else if (create) {
                int fieldIndex = parallelTaskConfig.getIndex(customFieldConfig.getName());
                parallelTaskValues.put(fieldIndex, 0);
            }
        }
        return parallelTaskValues;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    void serializeRegistry(ModelNode list) {
        ModelNode entry = new ModelNode();
        entry.get(KEY).set(key);
        entry.get(VALUE).set(value);
        list.add(entry);
    }

    public String getCustomFieldName() {
        return customFieldName;
    }

    public String getValueForComparator() {
        return value;
    }

}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.CustomFieldConfig;

/**
 * @author Kabir Khan
 */
public abstract class CustomFieldUtil {

    abstract CustomFieldValue loadCustomField(CustomFieldConfig customFieldConfig, Object customFieldValue);

    abstract String getKey(Object fieldValue);

    public abstract String getCreateEventValue(Object fieldValue);

    abstract CustomFieldValue loadCustomFieldFromKey(JiraInjectables jiraInjectables, CustomFieldConfig customFieldConfig, String key);

    public abstract String getUpdateEventValue(String changeKey, String changeValue);

    abstract BulkLoadContext<?> createBulkLoadContext(BoardProject.Builder project, CustomFieldConfig customFieldConfig);

    public Map<String, CustomFieldValue> sortFields(Map<String, CustomFieldValue> fields) {
        List<CustomFieldValue> fieldValues = new ArrayList<>(fields.values());
        Collections.sort(fieldValues, Comparator.comparing(CustomFieldValue::getValueForComparator, String.CASE_INSENSITIVE_ORDER));
        LinkedHashMap<String, CustomFieldValue> result = new LinkedHashMap<>();
        for (CustomFieldValue field : fieldValues) {
            result.put(field.getKey(), field);
        }
        return result;
    }

    public static CustomFieldUtil getUtil(CustomFieldConfig config) {
        switch (config.getType()) {
            case USER:
                return USER;
            case VERSION:
                return VERSION;
            default:
                throw new IllegalStateException("No util class for type " + config.getType());
        }
    }

    public static final CustomFieldUtil USER = new CustomFieldUtil() {
        @Override
        public CustomFieldValue loadCustomField(CustomFieldConfig customFieldConfig, Object customFieldValue) {
            return UserCustomFieldValue.load(customFieldConfig, customFieldValue);
        }

        @Override
        public String getKey(Object fieldValue) {
            return UserCustomFieldValue.getKeyForValue(fieldValue);
        }

        @Override
        public String getCreateEventValue(Object fieldValue) {
            return UserCustomFieldValue.getChangeValue(fieldValue);
        }

        @Override
        public CustomFieldValue loadCustomFieldFromKey(JiraInjectables jiraInjectables, CustomFieldConfig customFieldConfig, String key) {
            return UserCustomFieldValue.load(jiraInjectables, customFieldConfig, key);
        }

        @Override
        public String getUpdateEventValue(String changeKey, String changeValue) {
            return changeKey;
        }

        @Override
        BulkLoadContext<?> createBulkLoadContext(BoardProject.Builder project, CustomFieldConfig customFieldConfig) {
            return new BulkLoadContext<String>(customFieldConfig) {
                @Override
                String getCacheKey(String stringValue, Long numericValue) {
                    return stringValue;
                }

                @Override
                CustomFieldValue loadCustomFieldValue(String key) {
                    return UserCustomFieldValue.load(project.getJiraInjectables(), customFieldConfig, key);
                }
            };
        }
    };

    public static final CustomFieldUtil VERSION = new CustomFieldUtil() {
        @Override
        public CustomFieldValue loadCustomField(CustomFieldConfig customFieldConfig, Object customFieldValue) {
            return VersionCustomFieldValue.load(customFieldConfig, customFieldValue);
        }

        @Override
        public String getKey(Object fieldValue) {
            return VersionCustomFieldValue.getKeyForValue(fieldValue);
        }

        @Override
        public String getCreateEventValue(Object fieldValue) {
            return VersionCustomFieldValue.getChangeValue(fieldValue);
        }

        @Override
        public CustomFieldValue loadCustomFieldFromKey(JiraInjectables jiraInjectables, CustomFieldConfig customFieldConfig, String key) {
            return VersionCustomFieldValue.load(customFieldConfig, key);
        }

        @Override
        public String getUpdateEventValue(String changeKey, String changeValue) {
            return changeValue;
        }

        @Override
        BulkLoadContext<?> createBulkLoadContext(BoardProject.Builder project, CustomFieldConfig customFieldConfig) {
            return new BulkLoadContext<Long>(customFieldConfig) {
                @Override
                Long getCacheKey(String stringValue, Long numericValue) {
                    return numericValue;
                }

                @Override
                CustomFieldValue loadCustomFieldValue(Long id) {
                    return VersionCustomFieldValue.load(project.getJiraInjectables(), customFieldConfig, id);
                }
            };
        }
    };
}

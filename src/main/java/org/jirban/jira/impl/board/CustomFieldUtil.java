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

    abstract BulkLoadContext<?> createBulkLoadContext(JiraInjectables jiraInjectables, CustomFieldConfig customFieldConfig);

    public Map<String, CustomFieldValue> sortFields(Map<String, CustomFieldValue> fields) {
        List<CustomFieldValue> fieldValues = new ArrayList<>(fields.values());
        Collections.sort(fieldValues, Comparator.comparing(CustomFieldValue::getValueForComparator, String.CASE_INSENSITIVE_ORDER));
        LinkedHashMap<String, CustomFieldValue> result = new LinkedHashMap<>();
        for (CustomFieldValue field : fieldValues) {
            result.put(field.getKey(), field);
        }
        return result;
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
        BulkLoadContext<?> createBulkLoadContext(JiraInjectables jiraInjectables, CustomFieldConfig customFieldConfig) {
            return new BulkLoadContext<String>(jiraInjectables, customFieldConfig) {
                @Override
                String getCacheKey(String stringValue, Long numericValue) {
                    return stringValue;
                }

                @Override
                CustomFieldValue loadCustomFieldValue(String key) {
                    return UserCustomFieldValue.load(jiraInjectables, customFieldConfig, key);
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
        BulkLoadContext<?> createBulkLoadContext(JiraInjectables jiraInjectables, CustomFieldConfig customFieldConfig) {
            return new BulkLoadContext<Long>(jiraInjectables, customFieldConfig) {
                @Override
                Long getCacheKey(String stringValue, Long numericValue) {
                    return numericValue;
                }

                @Override
                CustomFieldValue loadCustomFieldValue(Long id) {
                    return VersionCustomFieldValue.load(jiraInjectables, customFieldConfig, id);
                }
            };
        }
    };
}

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

    Map<String, CustomFieldValue> sortFields(Map<String, CustomFieldValue> fields) {
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
        CustomFieldValue loadCustomField(CustomFieldConfig customFieldConfig, Object customFieldValue) {
            return UserCustomFieldValue.load(customFieldConfig, customFieldValue);
        }

        @Override
        String getKey(Object fieldValue) {
            return UserCustomFieldValue.getKeyForValue(fieldValue);
        }

        @Override
        public String getCreateEventValue(Object fieldValue) {
            return UserCustomFieldValue.getChangeValue(fieldValue);
        }

        @Override
        CustomFieldValue loadCustomFieldFromKey(JiraInjectables jiraInjectables, CustomFieldConfig customFieldConfig, String key) {
            return UserCustomFieldValue.load(jiraInjectables, customFieldConfig, key);
        }

        @Override
        public String getUpdateEventValue(String changeKey, String changeValue) {
            return changeKey;
        }

    };

    public static final CustomFieldUtil VERSION = new CustomFieldUtil() {
        @Override
        CustomFieldValue loadCustomField(CustomFieldConfig customFieldConfig, Object customFieldValue) {
            return VersionCustomFieldValue.load(customFieldConfig, customFieldValue);
        }

        @Override
        String getKey(Object fieldValue) {
            return VersionCustomFieldValue.getKeyForValue(fieldValue);
        }

        @Override
        public String getCreateEventValue(Object fieldValue) {
            return VersionCustomFieldValue.getChangeValue(fieldValue);
        }

        @Override
        CustomFieldValue loadCustomFieldFromKey(JiraInjectables jiraInjectables, CustomFieldConfig customFieldConfig, String key) {
            return VersionCustomFieldValue.load(jiraInjectables, customFieldConfig, key);
        }

        @Override
        public String getUpdateEventValue(String changeKey, String changeValue) {
            return changeValue;
        }

    };

}

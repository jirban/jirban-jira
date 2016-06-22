package org.jirban.jira.impl.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jirban.jira.impl.config.CustomFieldConfig;

/**
 * @author Kabir Khan
 */
public abstract class CustomFieldUtil {

    abstract CustomFieldValue loadCustomField(CustomFieldConfig customFieldConfig, Object customFieldValue);

    abstract String getKey(Object fieldValue);

    public abstract String getChangeValue(Object fieldValue);

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
            return UserCustomFieldValue.getKey(fieldValue);
        }

        @Override
        public String getChangeValue(Object fieldValue) {
            return UserCustomFieldValue.getChangeValue(fieldValue);
        }
    };

    public static final CustomFieldUtil PREDEFINED_LIST = new CustomFieldUtil() {
        @Override
        CustomFieldValue loadCustomField(CustomFieldConfig customFieldConfig, Object customFieldValue) {
            return null;
        }

        @Override
        String getKey(Object fieldValue) {
            return null;
        }

        @Override
        public String getChangeValue(Object fieldValue) {
            return null;
        }
    };

}

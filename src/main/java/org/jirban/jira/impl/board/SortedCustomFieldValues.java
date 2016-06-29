package org.jirban.jira.impl.board;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.CustomFieldConfig;

/**
 * Sorted values for a given custom field used as the 'registry' in the overall board data.
 *
 * @author Kabir Khan
 */
public class SortedCustomFieldValues {
    private final CustomFieldConfig config;
    private final Map<String, CustomFieldValue> sortedFields;
    private final Map<String, Integer> fieldIndices;

    private SortedCustomFieldValues(CustomFieldConfig config, Map<String, CustomFieldValue> sortedFields) {
        this.config = config;
        this.sortedFields = sortedFields;
        this.fieldIndices = Board.getIndices(sortedFields);
    }

    String getFieldName() {
        return config.getName();
    }

    CustomFieldValue getCustomFieldValue(String key) {
        return sortedFields.get(key);
    }

    int getCustomFieldIndex(CustomFieldValue customFieldValue) {
        return fieldIndices.get(customFieldValue.getKey());
    }

    public void serialize(ModelNode parentNode) {
        ModelNode fieldList = new ModelNode();
        for (CustomFieldValue customFieldValue : sortedFields.values()) {
            customFieldValue.serializeRegistry(fieldList);
        }
        if (fieldList.isDefined()) {
            parentNode.get(config.getName()).set(fieldList);
        }
    }

    static abstract class Accessor {
        protected final CustomFieldConfig config;
        protected final Map<String, CustomFieldValue> fields;

        protected Accessor(CustomFieldConfig config) {
            this(config, null);
        }

        protected Accessor(CustomFieldConfig config, Map<String, CustomFieldValue> fields) {
            this.config = config;
            this.fields = fields == null ? new HashMap<>() : new HashMap<>(fields);
        }
    }

    static class Builder extends Accessor {

        Builder(CustomFieldConfig config) {
            super(config, new HashMap<>());
        }

        CustomFieldValue getCustomFieldValue(Object customFieldValue) {
            final CustomFieldUtil util = config.getUtil();
            return fields.computeIfAbsent(
                    util.getKey(customFieldValue), s -> util.loadCustomField(config, customFieldValue));
        }

        SortedCustomFieldValues build() {
            final Map<String, CustomFieldValue> sortedFields = config.getUtil().sortFields(fields);
            return new SortedCustomFieldValues(config, Collections.unmodifiableMap(sortedFields));
        }
    }

    static class Updater extends Accessor {
        Updater(CustomFieldConfig config, SortedCustomFieldValues sortedCustomFieldValues) {
            super(config, sortedCustomFieldValues.sortedFields);
        }

        CustomFieldValue getCustomFieldValue(JiraInjectables jiraInjectables, String key) {
            final CustomFieldUtil util = config.getUtil();
            return fields.computeIfAbsent(
                    key, s -> util.loadCustomFieldFromKey(jiraInjectables, config, key));
        }

        static Map<String, SortedCustomFieldValues> merge(Map<Long, SortedCustomFieldValues.Updater> updates, Map<String, SortedCustomFieldValues> original) {
            if (updates == null) {
                return original;
            }
            Map<String, SortedCustomFieldValues> result = new HashMap<>(original);
            for (SortedCustomFieldValues.Updater updater : updates.values()) {
                final Map<String, CustomFieldValue> sortedFields =
                        updater.config.getUtil().sortFields(updater.fields);

                result.put(
                        updater.config.getName(),
                        new SortedCustomFieldValues(updater.config, Collections.unmodifiableMap(updater.fields)));
            }
            return result;
        }
    }

}

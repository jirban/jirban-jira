package org.jirban.jira.impl.board;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;
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
        private final CustomFieldConfig config;
        private final Map<String, CustomFieldValue> fields;

        public Accessor(CustomFieldConfig config, Map<String, CustomFieldValue> fields) {
            this.config = config;
            this.fields = fields;
        }

        public CustomFieldConfig getConfig() {
            return config;
        }

        public Map<String, CustomFieldValue> getFields() {
            return fields;
        }

        CustomFieldValue getCustomField(Object customFieldValue) {
            final CustomFieldUtil util = config.getUtil();
            return fields.computeIfAbsent(
                    util.getKey(customFieldValue), s -> util.loadCustomField(config, customFieldValue));
        }
    }

    static class Builder extends Accessor {

        Builder(CustomFieldConfig config) {
            super(config, new HashMap<>());
        }

        SortedCustomFieldValues build() {
            final Map<String, CustomFieldValue> sortedFields = getConfig().getUtil().sortFields(getFields());
            return new SortedCustomFieldValues(getConfig(), Collections.unmodifiableMap(sortedFields));
        }
    }

}

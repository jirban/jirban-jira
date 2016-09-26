package org.jirban.jira.impl.board;

import static org.jirban.jira.impl.Constants.DISPLAY;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.OPTIONS;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.config.ParallelTaskCustomFieldConfig;
import org.jirban.jira.impl.util.IndexedMap;

/**
 * The sorted parallel field options for a project
 *
 * @author Kabir Khan
 */
public class SortedParallelTaskFieldOptions {
    private final ParallelTaskCustomFieldConfig config;
    private final IndexedMap<String, CustomFieldValue> sortedFields;

    private SortedParallelTaskFieldOptions(ParallelTaskCustomFieldConfig config, IndexedMap<String, CustomFieldValue> sortedFields) {
        this.config = config;
        this.sortedFields = sortedFields;
    }

    CustomFieldValue get(String key) {
        return sortedFields.get(key);
    }

    Integer getIndex(String key) {
        return sortedFields.getIndex(key);
    }

    public void serialize(ModelNode list) {
        ModelNode entry = new ModelNode();
        entry.get(NAME).set(config.getName());
        entry.get(DISPLAY).set(config.getCode());
        ModelNode options = new ModelNode().setEmptyList();
        for (CustomFieldValue value : sortedFields.values()) {
            options.add(value.getValue());
        }
        entry.get(OPTIONS).set(options);
        list.add(entry);
    }

    static class Builder {
        private final ParallelTaskCustomFieldConfig config;
        private Map<String, CustomFieldValue> options = new LinkedHashMap<>();
        Builder(ParallelTaskCustomFieldConfig config) {
            this.config = config;
        }

        public Builder addOption(CustomFieldValue value) {
            this.options.put(value.getKey(), value);
            return this;
        }

        SortedParallelTaskFieldOptions build() {
            return new SortedParallelTaskFieldOptions(config, new IndexedMap<>(options));
        }
    }
}

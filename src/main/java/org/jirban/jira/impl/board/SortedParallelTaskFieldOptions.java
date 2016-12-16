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

    public CustomFieldValue forIndex(int index) {
        return sortedFields.forIndex(index);
    }

    public void serialize(ModelNode list) {
        ModelNode entry = new ModelNode();
        entry.get(NAME).set(config.getName());
        entry.get(DISPLAY).set(config.getCode());
        ModelNode options = new ModelNode().setEmptyList();
        sortedFields.values().forEach(customFieldValue -> options.add(customFieldValue.getValue()));
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

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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.CustomFieldConfig;
import org.jirban.jira.impl.util.IndexedMap;

/**
 * Sorted values for a given custom field used as the 'registry' in the overall board data.
 * The sort order is according to the key or the value.
 *
 * @author Kabir Khan
 */
public class SortedCustomFieldValues {
    private final CustomFieldConfig config;
    private final IndexedMap<String, CustomFieldValue> sortedFields;

    private SortedCustomFieldValues(CustomFieldConfig config, IndexedMap<String, CustomFieldValue> sortedFields) {
        this.config = config;
        this.sortedFields = sortedFields;
    }


    String getFieldName() {
        return config.getName();
    }

    CustomFieldValue getCustomFieldValue(String key) {
        return sortedFields.get(key);
    }

    int getCustomFieldIndex(CustomFieldValue customFieldValue) {
        return sortedFields.getIndex(customFieldValue.getKey());
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

        protected Accessor(CustomFieldConfig config, Map<String, CustomFieldValue> fields) {
            this.config = config;
            this.fields = fields == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fields);
        }

        CustomFieldUtil getUtil() {
            return CustomFieldUtil.getUtil(config);
        }
    }

    static class Builder extends Accessor {

        Builder(CustomFieldConfig config) {
            super(config, new HashMap<>());
        }

        CustomFieldValue getCustomFieldValue(Object customFieldValue) {
            return fields.computeIfAbsent(
                    getUtil().getKey(customFieldValue), s -> getUtil().loadCustomField(config, customFieldValue));
        }

        void addBulkLoadedCustomFieldValue(CustomFieldValue customFieldValue) {
            fields.put(customFieldValue.getKey(), customFieldValue);
        }

        SortedCustomFieldValues build() {
            final Map<String, CustomFieldValue> sortedFields;
            sortedFields = getUtil().sortFields(fields);
            return new SortedCustomFieldValues(config, new IndexedMap<>(sortedFields));
        }
    }

    static class Updater extends Accessor {
        Updater(CustomFieldConfig config, SortedCustomFieldValues sortedCustomFieldValues) {
            super(
                    config,
                    sortedCustomFieldValues == null ? null : sortedCustomFieldValues.sortedFields.map());
        }

        CustomFieldValue getCustomFieldValue(JiraInjectables jiraInjectables, String key) {
            return fields.computeIfAbsent(
                    key, s -> getUtil().loadCustomFieldFromKey(jiraInjectables, config, key));
        }

        static Map<String, SortedCustomFieldValues> merge(Map<Long, SortedCustomFieldValues.Updater> updates, Map<String, SortedCustomFieldValues> original) {
            if (updates == null) {
                return original;
            }
            Map<String, SortedCustomFieldValues> result = new HashMap<>(original);
            for (SortedCustomFieldValues.Updater updater : updates.values()) {
                final Map<String, CustomFieldValue> sortedFields;
                sortedFields = updater.getUtil().sortFields(updater.fields);
                result.put(
                        updater.config.getName(),
                        new SortedCustomFieldValues(updater.config, new IndexedMap<>(sortedFields)));
            }
            return result;
        }
    }

}

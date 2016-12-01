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
import java.util.Map;

import org.jirban.jira.impl.config.CustomFieldConfig;

/**
 * Caches values loaded up by a field
 *
 * @author Kabir Khan
 */
public abstract class BulkLoadContext<T> {
    private final CustomFieldConfig config;
    private final Map<T, CustomFieldValue> cachedValues = new HashMap<T, CustomFieldValue>();

    public BulkLoadContext(CustomFieldConfig config) {
        this.config = config;
    }

    public CustomFieldConfig getConfig() {
        return config;
    }

    CustomFieldValue getCachedCustomFieldValue(String stringValue, Long numericValue) {
        return cachedValues.get(getCacheKey(stringValue, numericValue));
    }

    CustomFieldValue loadAndCacheCustomFieldValue(String stringValue, Long numericValue) {
        T cacheKey = getCacheKey(stringValue, numericValue);
        CustomFieldValue customFieldValue = loadCustomFieldValue(cacheKey);
        cachedValues.put(cacheKey, customFieldValue);
        return customFieldValue;
    }

    abstract T getCacheKey(String stringValue, Long numericValue);

    abstract CustomFieldValue loadCustomFieldValue(T key);
}

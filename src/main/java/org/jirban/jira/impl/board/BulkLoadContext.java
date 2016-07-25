package org.jirban.jira.impl.board;

import java.util.HashMap;
import java.util.Map;

import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.CustomFieldConfig;

/**
 * @author Kabir Khan
 */
public abstract class BulkLoadContext<T> {
    private final CustomFieldConfig config;
    private final Map<T, CustomFieldValue> cachedValues = new HashMap<T, CustomFieldValue>();

    public BulkLoadContext(JiraInjectables jiraInjectables, CustomFieldConfig config) {
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

package org.jirban.jira.impl.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kabir Khan
 */
public class IndexedMap<K, V> {
    private final Map<K, V> map;
    private final Map<K, Integer> indices;

    public IndexedMap(Map<K, V> map) {
        this.map = Collections.unmodifiableMap(map);
        this.indices = getIndices(map);
    }

    public static <K,V> Map<K, Integer> getIndices(Map<K, V> map) {
        Map<K, Integer> indices = new HashMap<>();
        int i = 0;
        for (K key : map.keySet()) {
            indices.put(key, i++);
        }
        return indices;
    }

    public Collection<V> values() {
        return map.values();
    }

    public Integer getIndex(K key) {
        return indices.get(key);
    }

    public V get(K key) {
        return map.get(key);
    }

    public Map<K, V> map() {
        return map;
    }

    public int size() {
        return map.size();
    }
}

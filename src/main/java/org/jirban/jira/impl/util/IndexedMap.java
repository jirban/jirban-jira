package org.jirban.jira.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kabir Khan
 */
public class IndexedMap<K, V> {
    private final Map<K, V> map;
    private final Map<K, Integer> indices;
    private final List<V> byIndex;

    public IndexedMap(Map<K, V> map) {
        this.map = Collections.unmodifiableMap(map);
        Map<K, Integer> indices = new HashMap<>();
        List<V> byIndex = new ArrayList<V>();
        int i = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            indices.put(entry.getKey(), i++);
            byIndex.add(entry.getValue());
        }
        this.indices = Collections.unmodifiableMap(indices);
        this.byIndex = Collections.unmodifiableList(byIndex);

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

    public V forIndex(int i) {
        return byIndex.get(i);
    }

    public int size() {
        return map.size();
    }
}

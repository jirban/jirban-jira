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

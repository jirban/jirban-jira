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

package org.jirban.jira.impl.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kabir Khan
 */
public class CustomFieldRegistry<C extends CustomFieldConfig> {
    /** Custom field configs by the display name used in the Jirban config */
    final Map<String, C> customFieldConfigsByJirbanName;
    /** Custom field configs by the id used by Jira internally */
    final Map<Long, C> customFieldConfigsByJiraId;
    /** Custom field configs by the name used by Jira internally */
    final Map<String, C> customFieldConfigsByJiraName;

    CustomFieldRegistry(Map<String, C> customFieldConfigsByJirbanName) {
        this.customFieldConfigsByJirbanName = customFieldConfigsByJirbanName;

        Map<Long, C> customFieldConfigsByJiraId = new HashMap<>();
        Map<String, C> customFieldConfigsByJiraName = new HashMap<>();
        for (C cfg : customFieldConfigsByJirbanName.values()) {
            customFieldConfigsByJiraId.put(cfg.getId(), cfg);
            customFieldConfigsByJiraName.put(cfg.getJiraCustomField().getName(), cfg);
        }
        this.customFieldConfigsByJiraId = Collections.unmodifiableMap(customFieldConfigsByJiraId);
        this.customFieldConfigsByJiraName = Collections.unmodifiableMap(customFieldConfigsByJiraName);
    }

    public C getForJirbanName(String name) {
        return customFieldConfigsByJirbanName.get(name);
    }

    public C getForJiraId(Long id) {
        return customFieldConfigsByJiraId.get(id);
    }

    public C getForJiraName(String name) {
        return customFieldConfigsByJiraName.get(name);
    }

    boolean hasConfigs() {
        return customFieldConfigsByJiraName.size() > 0;
    }

    public Collection<C> values() {
        return customFieldConfigsByJirbanName.values();
    }

    public int size() {
        return customFieldConfigsByJirbanName.size();
    }
}

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

package ut.org.jirban.jira.mock;

import static org.jirban.jira.impl.Constants.CUSTOM;
import static org.jirban.jira.impl.Constants.FIELD_ID;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.PARALLEL_TASKS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.BoardConfigurationManagerBuilder;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
public class CustomFieldManagerBuilder {
    private final CustomFieldManager customFieldManager = mock(CustomFieldManager.class);

    private Map<Long, CustomField> customFields = new HashMap<>();

    public static CustomFieldManager getDefaultCustomFieldManager() {
        return new CustomFieldManagerBuilder().build();
    }

    public static CustomFieldManager loadFromResource(String resource) throws IOException {
        ModelNode config = BoardConfigurationManagerBuilder.loadConfig(resource);
        CustomFieldManagerBuilder builder = new CustomFieldManagerBuilder();
        if (config.get(CUSTOM).isDefined()) {
            List<ModelNode> list = config.get(CUSTOM).asList();
            for (ModelNode custom : list) {
                long id = custom.get(FIELD_ID).asLong();
                String name = custom.get(NAME).asString();
                MockCustomField customField = new MockCustomField(id, name);
                builder.addCustomField(customField);
            }
        }
        if (config.get(PARALLEL_TASKS).isDefined()) {
            for (String stKey : config.get(PARALLEL_TASKS).keys()) {
                List<ModelNode> list = config.get(PARALLEL_TASKS, stKey).asList();
                for (ModelNode custom : list) {
                    long id = custom.get(FIELD_ID).asLong();
                    String name = custom.get(NAME).asString();
                    MockCustomField customField = new MockCustomField(id, name);
                    builder.addCustomField(customField);

                }
            }
        }
        return builder.build();
    }

    public CustomFieldManagerBuilder addCustomField(CustomField customField) {
        customFields.put(customField.getIdAsLong(), customField);
        return this;
    }

    public CustomFieldManager build() {
        //getCustomFieldObject(Long)
        when(customFieldManager.getCustomFieldObject(any(Long.class))).then(invocation -> {
            Long id = (Long)invocation.getArguments()[0];
            return customFields.get(id);
        });
        return customFieldManager;
    }
}

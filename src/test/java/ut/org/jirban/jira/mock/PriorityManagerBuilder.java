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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.config.PriorityManager;
import com.atlassian.jira.issue.priority.Priority;

/**
 * @author Kabir Khan
 */
public class PriorityManagerBuilder {

    private static final Map<String, Priority> PRIORITIES = new HashMap<>();

    private final PriorityManager priorityManager = mock(PriorityManager.class);
    public Map<String, Priority> priorities = new LinkedHashMap<>();

    public PriorityManagerBuilder addPriority(String name) {
        priorities.put(name, MockPriority.create(name));
        return this;
    }

    PriorityManager build() {
        when(priorityManager.getPriorities()).then(invocation -> {
            List<Priority> priorities = new ArrayList<>();
            for (Priority priority : this.priorities.values()) {
                priorities.add(priority);
            }
            return priorities;
        });
        return priorityManager;
    }

    public static PriorityManager getDefaultPriorityManager() {
        PriorityManagerBuilder builder = new PriorityManagerBuilder();
        builder.addPriority("highest");
        builder.addPriority("high");
        builder.addPriority("low");
        builder.addPriority("lowest");

        return builder.build();
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

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

import java.util.LinkedHashSet;
import java.util.Set;

import org.ofbiz.core.entity.GenericValue;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public class MockProjectComponent implements ProjectComponent {
    private final String componentName;

    public MockProjectComponent(String componentName) {
        this.componentName = componentName;
    }

    public static Set<ProjectComponent> createProjectComponents(String[] names) {
        if (names == null || names.length == 0) {
            return null;
        }
        Set<ProjectComponent> components = new LinkedHashSet<>();
        for (String name : names) {
            components.add(new MockProjectComponent(name));
        }
        return components;
    }

    @Override
    public int hashCode() {
        return componentName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MockProjectComponent == false) {
            return false;
        }
        MockProjectComponent other = (MockProjectComponent)obj;
        if (this.componentName != null && other.componentName == null ||
                this.componentName == null && other.componentName != null) {
            return false;
        }
        if (this.componentName == null) {
            return true;
        }
        return this.componentName.equals(other.componentName);
    }

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getLead() {
        return null;
    }

    @Override
    public ApplicationUser getComponentLead() {
        return null;
    }

    @Override
    public String getName() {
        return componentName;
    }

    @Override
    public Long getProjectId() {
        return null;
    }

    @Override
    public long getAssigneeType() {
        return 0;
    }

    @Override
    public GenericValue getGenericValue() {
        return null;
    }
}

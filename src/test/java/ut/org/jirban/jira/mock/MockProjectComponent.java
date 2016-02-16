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

import java.util.HashSet;
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
        Set<ProjectComponent> components = new HashSet<>();
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

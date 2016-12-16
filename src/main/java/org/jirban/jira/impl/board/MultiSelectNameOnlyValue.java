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
package org.jirban.jira.impl.board;

import org.jboss.dmr.ModelNode;

/**
 * Base class for things like Component, Fix Version, Label which can all have more than one entry set per issue
 *
 * @author Kabir Khan
 */
public abstract class MultiSelectNameOnlyValue {
    private final String name;

    protected MultiSelectNameOnlyValue(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void serialize(ModelNode multiSelectNode) {
        multiSelectNode.add(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        MultiSelectNameOnlyValue other = (MultiSelectNameOnlyValue)obj;
        if (this.name != null && other.name == null || this.name == null && other.name != null) {
            return false;
        }
        if (this.name == null) {
            return true;
        }
        return this.name.equals(other.name);
    }

    public static class Component extends MultiSelectNameOnlyValue {
        public Component(String name) {
            super(name);
        }
    }

    public static class Label extends MultiSelectNameOnlyValue {
        public Label(String name) {
            super(name);
        }
    }

    public static class FixVersion extends MultiSelectNameOnlyValue {
        public FixVersion(String name) {
            super(name);
        }
    }
}

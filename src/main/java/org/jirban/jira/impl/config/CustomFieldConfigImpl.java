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

import static org.jirban.jira.impl.Constants.DISPLAY;
import static org.jirban.jira.impl.Constants.FIELD_ID;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.TYPE;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.impl.JiraInjectables;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
abstract class CustomFieldConfigImpl implements CustomFieldConfig {
    private final String name;
    private final Type type;
    private final CustomField customField;

    protected CustomFieldConfigImpl(String name, Type type, CustomField customField) {
        this.name = name;
        this.type = type;
        this.customField = customField;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public CustomField getJiraCustomField() {
        return customField;
    }

    static CustomFieldConfig loadCustomFieldConfig(JiraInjectables jiraInjectables, ModelNode customFieldCfgNode) {
        final String name = loadName(customFieldCfgNode);
        final Type type = loadType(customFieldCfgNode, name);
        final CustomField customField = loadCustomField(jiraInjectables, customFieldCfgNode, name);

        switch (type) {
            case USER: {
                return new UserCustomFieldConfig(name, type, customField);
            }
            case VERSION: {
                return new VersionCustomFieldConfig(name, type, customField);
            }
            default:
                throw new JirbanValidationException("Invalid type for 'custom' field config: " + type);
        }
    }

    static ParallelTaskCustomFieldConfig loadParallelTaskCustomFieldConfig(JiraInjectables jiraInjectables, ModelNode customFieldCfgNode) {
        final String name = loadName(customFieldCfgNode);
        final Type type = loadType(customFieldCfgNode, name);
        final CustomField customField = loadCustomField(jiraInjectables, customFieldCfgNode, name);


        String parallelTaskCode = null;
        if (type == Type.PARALLEL_TASK_PROGRESS) {
            if (!customFieldCfgNode.hasDefined(DISPLAY)) {
                throw new JirbanValidationException("The 'display' field for the \"" + name + "\" parallel-task element is required");
            }
            parallelTaskCode = customFieldCfgNode.get(DISPLAY).asString();
            if (parallelTaskCode.length() != 2) {
                throw new JirbanValidationException("The 'code' field for the \"" + name + "\" parallel-task element should be 2 characters");
            }
        }

        switch (type) {
            case PARALLEL_TASK_PROGRESS: {
                return new ParallelTaskProgressCustomFieldConfig(name, type, parallelTaskCode, customField);
            }
            default:
                throw new JirbanValidationException("Invalid type for 'custom' field config: " + type);
        }
    }

    private static String loadName(ModelNode customFieldCfgNode) {
        if (!customFieldCfgNode.hasDefined(NAME)) {
            throw new JirbanValidationException("All 'custom' and 'parallel-tasks' field definitions must have a 'name'");
        }
        return customFieldCfgNode.get(NAME).asString();
    }

    private static final Type loadType(ModelNode customFieldCfgNode, String name) {
        if (!customFieldCfgNode.hasDefined(TYPE)) {
            throw new JirbanValidationException("'custom' or 'parallel-tasks' field config \"" + name + "\" does not have a 'type'");
        }
        final String typeName = customFieldCfgNode.get(TYPE).asString();
        final Type type = Type.parse(typeName);
        if (type == null) {
            throw new JirbanValidationException("Unknown 'type' in \": " + typeName);
        }
        return type;
    }

    private static CustomField loadCustomField(JiraInjectables jiraInjectables, ModelNode customFieldCfgNode, String name) {
        if (!customFieldCfgNode.hasDefined(FIELD_ID)) {
            throw new JirbanValidationException("'custom' or 'parallel-tasks' field config \"" + name + "\" does not have a \"field-name\"");
        }
        final long fieldId;
        try {
            fieldId = customFieldCfgNode.get(FIELD_ID).asLong();
        } catch (Exception e) {
            throw new JirbanValidationException("\"field-id\" must be a long, in custom or parallel-tasks field config \"" + name + "\"");
        }
        final CustomFieldManager customFieldMgr = jiraInjectables.getCustomFieldManager();
        CustomField customField = customFieldMgr.getCustomFieldObject(fieldId);
        if (customField == null) {
            throw new JirbanValidationException("No custom field found with id \"" + fieldId + "\" (\"" + name + "\" ");
        }
        return customField;
    }



    public ModelNode serializeForConfig() {
        ModelNode modelNode = new ModelNode();
        modelNode.get(NAME).set(name);
        modelNode.get(TYPE).set(type.getName());
        modelNode.get(FIELD_ID).set(customField.getIdAsLong());
        return modelNode;
    }

    public Long getId() {
        return customField.getIdAsLong();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomFieldConfigImpl)) return false;

        CustomFieldConfigImpl that = (CustomFieldConfigImpl) o;

        if (!name.equals(that.name)) return false;
        if (type != that.type) return false;
        return customField.getIdAsLong().equals(that.customField.getIdAsLong());

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + customField.getIdAsLong().intValue();
        return result;
    }
}

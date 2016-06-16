package org.jirban.jira.impl.config;

import static org.jirban.jira.impl.Constants.FIELD_ID;
import static org.jirban.jira.impl.Constants.NAME;
import static org.jirban.jira.impl.Constants.TYPE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;

/**
 * @author Kabir Khan
 */
public abstract class CustomFieldConfig {
    private final String name;
    private final Type type;
    private final long fieldId;

    CustomFieldConfig(String name, Type type, long fieldId) {
        this.name = name;
        this.type = type;
        this.fieldId = fieldId;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public static CustomFieldConfig load(ModelNode customFieldCfgNode) {
        if (!customFieldCfgNode.hasDefined(NAME)) {
            throw new JirbanValidationException("All \"custom\" field definitions must have a 'name'");
        }
        final String name = customFieldCfgNode.get(NAME).asString();

        if (!customFieldCfgNode.hasDefined(TYPE)) {
            throw new JirbanValidationException("All \"custom\" field definitions must have a 'type'");
        }
        final String typeName = customFieldCfgNode.get(TYPE).asString();
        final Type type = Type.parse(typeName);
        if (type == null) {
            throw new JirbanValidationException("Unknown 'type': " + typeName);
        }

        if (!customFieldCfgNode.hasDefined(FIELD_ID)) {
            throw new JirbanValidationException("\"custom\" field config \"" + name + "\" does not have a \"field-name\"");
        }
        final long fieldId;
        try {
            fieldId = customFieldCfgNode.get(FIELD_ID).asLong();
        } catch (Exception e) {
            throw new JirbanValidationException("\"field-id\" must be a long, in custom field config \"" + name + "\"");
        }

        switch (type) {
            case USER:
                return new UserCustomFieldConfig(name, type, fieldId);
            case PREDEFINED_LIST:
                return PredefinedListCustomFieldConfig.load(name, type, fieldId, customFieldCfgNode);
            default:
                throw new JirbanValidationException("Unknown 'type': " + typeName);
        }
    }

    public ModelNode serializeForConfig() {
        ModelNode modelNode = new ModelNode();
        modelNode.get(NAME).set(name);
        modelNode.get(TYPE).set(type.name);
        modelNode.get(FIELD_ID).set(fieldId);
        return modelNode;
    }

    public enum Type {
        USER("user"),
        PREDEFINED_LIST("predefined-list");

        private static final Map<String, Type> TYPES_BY_NAME;
        static {
            Map<String, Type> map = new HashMap<>();
            for (Type type : values()) {
                map.put(type.getName(), type);
            }
            TYPES_BY_NAME = Collections.unmodifiableMap(map);
        }


        private final String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        static Type parse(String name) {
            return TYPES_BY_NAME.get(name);
        }
    }
}

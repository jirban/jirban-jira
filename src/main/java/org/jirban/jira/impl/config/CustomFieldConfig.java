package org.jirban.jira.impl.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;

import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
public interface CustomFieldConfig {
    /**
     * Get the name of the custom field set up in the configuration
     * @return the name
     */
    String getName();

    /**
     * Get the type of the custom field set up in the configuration
     *
     * @return the name
     */
    Type getType();

    /**
     * Get the underlying Jira custom field
     *
     * @return the custom field
     */
    CustomField getJiraCustomField();

    /**
     * Get the Jira id of the custom
     * @return the jira id
     */
    Long getId();

    /**
     * Serialize the custom field for use in the configuration
     *
     * @return the serialized model node
     */
    ModelNode serializeForConfig();

    enum Type {
        //A Jira custom field of type 'User Picker'
        USER("user"),
        //A Jira custom field of type 'Version'
        VERSION("version"),
        //A Jira custom field of type 'Select List (single choice)'. Make sure that the field configuration
        //scheme for projects using this field for use as a parallel task in Jirban is set up to be Required
        //(otherwise you get an additional 'None' entry)
        //TODO Perhaps that doesn't matter? We can just map None to the first entry in the list
        PARALLEL_TASK_PROGRESS("parallel-task-progress");

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

package org.jirban.jira.impl.board;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.config.CustomFieldConfig;

import com.atlassian.jira.issue.Issue;

/**
 * @author Kabir Khan
 */
public abstract class CustomFieldValue {

    private final String customFieldName;

    //Jira's event mechanism seems to use an empty string to unset custom fields
    public static final String UNSET_VALUE = "";

    protected CustomFieldValue(String customFieldName) {
        this.customFieldName = customFieldName;
    }

    static Map<String, CustomFieldValue> loadCustomFieldValues(final BoardProject.Accessor project, final Issue issue) {
        final List<String> customFieldNames = project.getConfig().getCustomFieldNames();
        if (customFieldNames.size() == 0) {
            return Collections.emptyMap();
        }

        final Map<String, CustomFieldValue> fields = new HashMap<>(customFieldNames.size());
        for (String customFieldName : customFieldNames) {
            CustomFieldConfig customFieldConfig = project.getBoard().getConfig().getCustomFieldConfigForJirbanId(customFieldName);
            Object customFieldValue = issue.getCustomFieldValue(customFieldConfig.getJiraCustomField());
            if (customFieldValue == null) {
                continue;
            }
            final CustomFieldValue customField = project.getCustomFieldValue(customFieldConfig, customFieldValue);
            fields.put(customFieldName, customField);
        }
        return fields.size() > 0 ? fields : Collections.emptyMap();
    }

    static Map<String, CustomFieldValue> loadCustomFieldValues(final BoardProject.Accessor project, final Map<Long, String> customFieldValues) {
        final List<String> customFieldNames = project.getConfig().getCustomFieldNames();
        if (customFieldNames.size() == 0) {
            return Collections.emptyMap();
        }

        final Map<String, CustomFieldValue> fields = new HashMap<>(customFieldNames.size());
        for (String customFieldName : customFieldNames) {
            CustomFieldConfig customFieldConfig = project.getBoard().getConfig().getCustomFieldConfigForJirbanId(customFieldName);
            String value = customFieldValues.get(customFieldConfig.getId());

            if (value != null) {
                final CustomFieldValue customFieldValue;
                if (value.equals("")) {
                    customFieldValue = null;
                } else {
                    customFieldValue =
                            project.getCustomFieldValue(customFieldConfig, value);
                }
                fields.put(customFieldName, customFieldValue);
            }
        }
        return fields.size() > 0 ? fields : Collections.emptyMap();
    }

    abstract String getKey();

    abstract void serializeRegistry(ModelNode list);

    public String getCustomFieldName() {
        return customFieldName;
    }

    public abstract String getValueForComparator();
}

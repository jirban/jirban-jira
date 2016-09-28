package org.jirban.jira.impl.config;

import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
class VersionCustomFieldConfig extends CustomFieldConfigImpl {
    VersionCustomFieldConfig(String name, Type type, CustomField customField) {
        super(name, type, customField);
    }
}

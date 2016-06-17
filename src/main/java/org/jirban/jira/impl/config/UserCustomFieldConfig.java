package org.jirban.jira.impl.config;

import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
public class UserCustomFieldConfig extends CustomFieldConfig {
    UserCustomFieldConfig(String name, Type type, CustomField customField) {
        super(name, type, customField);
    }
}

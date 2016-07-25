package org.jirban.jira.impl.config;

import org.jirban.jira.impl.board.CustomFieldUtil;

import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
public class UserCustomFieldConfig extends CustomFieldConfig {
    UserCustomFieldConfig(String name, Type type, CustomField customField) {
        super(name, type, customField, CustomFieldUtil.USER);
    }
}

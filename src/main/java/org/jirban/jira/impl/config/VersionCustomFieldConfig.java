package org.jirban.jira.impl.config;

import org.jirban.jira.impl.board.CustomFieldUtil;

import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
public class VersionCustomFieldConfig extends CustomFieldConfig {
    VersionCustomFieldConfig(String name, Type type, CustomField customField) {
        super(name, type, customField);
    }

    @Override
    public CustomFieldUtil getUtil() {
        return CustomFieldUtil.VERSION;
    }

}

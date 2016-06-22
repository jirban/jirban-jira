package org.jirban.jira.impl.config;

import java.util.Map;

import org.jirban.jira.impl.board.CustomFieldUtil;

import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
public class UserCustomFieldConfig extends CustomFieldConfig {
    UserCustomFieldConfig(String name, Type type, CustomField customField) {
        super(name, type, customField);
    }

    @Override
    public CustomFieldUtil getUtil() {
        return CustomFieldUtil.USER;
    }

    @Override
    public void sortCustomFields(Map<String, CustomField> customFields) {


    }
}

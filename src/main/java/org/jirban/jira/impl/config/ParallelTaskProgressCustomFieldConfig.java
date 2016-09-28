package org.jirban.jira.impl.config;

import com.atlassian.jira.issue.fields.CustomField;

/**
 * @author Kabir Khan
 */
class ParallelTaskProgressCustomFieldConfig extends CustomFieldConfigImpl implements ParallelTaskCustomFieldConfig {
    private final String code;
    ParallelTaskProgressCustomFieldConfig(String name, Type type, String code, CustomField customField) {
        super(name, type, customField);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

}

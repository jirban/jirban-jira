package org.jirban.jira.impl.board;

import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.CustomFieldConfig;

import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
class UserCustomFieldValue extends CustomFieldValue {

    private UserCustomFieldValue(String customFieldName, User user) {
        super(customFieldName, user.getKey(), user.getDisplayName());
    }

    static UserCustomFieldValue load(CustomFieldConfig config, Object customFieldValue) {
        ApplicationUser au = (ApplicationUser) customFieldValue;
        return load(config, au);
    }

    public static CustomFieldValue load(JiraInjectables jiraInjectables,
                                        CustomFieldConfig config, String key) {
        ApplicationUser au = jiraInjectables.getJiraUserManager().getUserByKey(key);
        if (au == null) {
            throw new JirbanValidationException("No user exists with the key " + key);
        }
        return load(config, au);
    }

    private static UserCustomFieldValue load(CustomFieldConfig config, ApplicationUser au) {
        User user = User.create(au);
        return new UserCustomFieldValue(config.getName(), user);
    }

    public static String getKeyForValue(Object fieldValue) {
        return ((ApplicationUser) fieldValue).getKey();
    }

    public static String getChangeValue(Object fieldValue) {
        return getKeyForValue(fieldValue);
    }

}

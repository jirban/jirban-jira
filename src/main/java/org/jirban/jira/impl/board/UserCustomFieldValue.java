package org.jirban.jira.impl.board;

import static org.jirban.jira.impl.Constants.EMAIL;
import static org.jirban.jira.impl.Constants.KEY;
import static org.jirban.jira.impl.Constants.NAME;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.CustomFieldConfig;

import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
class UserCustomFieldValue extends CustomFieldValue {

    private final User user;

    private UserCustomFieldValue(String customFieldName, User user) {
        super(customFieldName);
        this.user = user;
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

    @Override
    public String getKey() {
        return user.getKey();
    }

    @Override
    void serializeRegistry(ModelNode list) {
        ModelNode entry = new ModelNode();
        entry.get(KEY).set(user.getKey());
        entry.get(EMAIL).set(user.getEmail());
        entry.get(NAME).set(user.getDisplayName());
        list.add(entry);
    }

    public static String getKey(Object fieldValue) {
        return ((ApplicationUser) fieldValue).getKey();
    }

    public String getValueForComparator() {
        return user.getDisplayName();
    }

    public static String getChangeValue(Object fieldValue) {
        return getKey(fieldValue);
    }

}

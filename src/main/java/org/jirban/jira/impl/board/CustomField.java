package org.jirban.jira.impl.board;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.config.BoardConfig;
import org.jirban.jira.impl.config.BoardProjectConfig;
import org.jirban.jira.impl.config.CustomFieldConfig;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public abstract class CustomField<T> {



    static Map<String, CustomField> loadCustomFields(final BoardConfig boardConfig, final BoardProjectConfig projectConfig, final Issue issue) {
        final List<String> customFieldNames = projectConfig.getCustomFieldNames();
        if (customFieldNames.size() == 0) {
            return Collections.emptyMap();
        }

        final Map<String, CustomField> fields = new HashMap<>(customFieldNames.size());
        for (String customFieldName : customFieldNames) {
            CustomFieldConfig customFieldConfig = boardConfig.getCustomFieldConfig(customFieldName);
            Object customFieldValue = issue.getCustomFieldValue(customFieldConfig.getCustomField());
            switch (customFieldConfig.getType()) {
                case PREDEFINED_LIST:

                    break;
                case USER:
                    ApplicationUser au = (ApplicationUser)customFieldValue;
                    User user = User.create(au);
                    // TODO use ApplicationUser to get the information. It should be added to a list associated
                    // with the board (similar to the assignees).

                    break;
            }
        }
        return fields;
    }

    void serialize(ModelNode issue) {

    }
}

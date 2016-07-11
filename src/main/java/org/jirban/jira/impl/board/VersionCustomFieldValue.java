package org.jirban.jira.impl.board;

import java.util.List;

import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.CustomFieldConfig;

import com.atlassian.jira.project.version.Version;

/**
 * @author Kabir Khan
 */
public class VersionCustomFieldValue extends CustomFieldValue {
    private VersionCustomFieldValue(String customFieldName, String key, String value) {
        super(customFieldName, key, value);
    }

    static VersionCustomFieldValue load(CustomFieldConfig config, Object customFieldValue) {
        return loadValue(config, (List<Version>)customFieldValue);
    }

    public static CustomFieldValue load(JiraInjectables jiraInjectables, CustomFieldConfig customFieldConfig, String key) {
        return new VersionCustomFieldValue(customFieldConfig.getName(), key, key);
    }

    private static VersionCustomFieldValue loadValue(CustomFieldConfig config, List<Version> customFieldValue) {
        //For now just use one version (our aim is the JBoss Target Release field). If more versions are needed
        // in the future, we can create a new 'version-list' type or something like that
        Version version = customFieldValue.get(0);
        return new VersionCustomFieldValue(config.getName(), version.getName(), version.getName());

    }

    public static String getKeyForValue(Object fieldValue) {
        //For now just use one version (our aim is the JBoss Target Release field). If more versions are needed
        // in the future, we can create a new 'version-list' type or something like that
        List<Version> list = (List<Version>)fieldValue;
        Version version = list.get(0);
        return version.getName();
    }


    public static String getChangeValue(Object fieldValue) {
        return getKeyForValue(fieldValue);
    }

}

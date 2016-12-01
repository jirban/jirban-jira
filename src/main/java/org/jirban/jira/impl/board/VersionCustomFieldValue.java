/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jirban.jira.impl.board;

import java.util.List;

import org.jirban.jira.impl.JiraInjectables;
import org.jirban.jira.impl.config.CustomFieldConfig;

import com.atlassian.jira.project.version.Version;

/**
 * @author Kabir Khan
 */
class VersionCustomFieldValue extends CustomFieldValue {
    private VersionCustomFieldValue(String customFieldName, String key, String value) {
        super(customFieldName, key, value);
    }

    static VersionCustomFieldValue load(CustomFieldConfig config, Object customFieldValue) {
        return loadValue(config, (List<Version>)customFieldValue);
    }

    public static CustomFieldValue load(CustomFieldConfig customFieldConfig, String key) {
        return new VersionCustomFieldValue(customFieldConfig.getName(), key, key);
    }

    public static CustomFieldValue load(JiraInjectables jiraInjectables, CustomFieldConfig customFieldConfig, Long id) {
        final Version version = jiraInjectables.getVersionManager().getVersion(id);
        return new VersionCustomFieldValue(customFieldConfig.getName(), version.getName(), version.getName());
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

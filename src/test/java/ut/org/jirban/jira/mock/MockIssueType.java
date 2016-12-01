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
package ut.org.jirban.jira.mock;

import javax.annotation.Nullable;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.util.I18nHelper;

/**
 * Mockito seems to get confused with deep mocks, so hardcode these value objects
 *
 * @author Kabir Khan
 */
public class MockIssueType implements IssueType {
    private final String issueTypeName;
    private final String iconUrl;

    private MockIssueType(String issueTypeName) {
        this.issueTypeName = issueTypeName;
        this.iconUrl = "/icons/issue-types/" + issueTypeName + ".png";
    }


    static IssueType create(String issueTypeName) {
        return new MockIssueType(issueTypeName);
    }

    @Override
    public boolean isSubTask() {
        return false;
    }

    @Nullable
    @Override
    public Avatar getAvatar() {
        return null;
    }

    @Override
    public String getId() {
        return issueTypeName;
    }

    @Override
    public String getName() {
        return issueTypeName;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Long getSequence() {
        return null;
    }

    @Override
    public String getCompleteIconUrl() {
        return null;
    }

    @Override
    public String getIconUrl() {
        return iconUrl;
    }

    @Override
    public String getIconUrlHtml() {
        return null;
    }

    @Override
    public String getNameTranslation() {
        return null;
    }

    @Override
    public String getDescTranslation() {
        return null;
    }

    @Override
    public String getNameTranslation(String s) {
        return null;
    }

    @Override
    public String getDescTranslation(String s) {
        return null;
    }

    @Override
    public String getNameTranslation(I18nHelper i18nHelper) {
        return null;
    }

    @Override
    public String getDescTranslation(I18nHelper i18nHelper) {
        return null;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

}
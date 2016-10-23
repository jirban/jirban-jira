/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
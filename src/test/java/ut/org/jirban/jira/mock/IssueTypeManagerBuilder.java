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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.issuetype.IssueType;

/**
 * @author Kabir Khan
 */
public class IssueTypeManagerBuilder {

    private static final Map<String, IssueType> ISSUE_TYPES = new HashMap<>();
    private final IssueTypeManager issueTypeManager = mock(IssueTypeManager.class);
    private final Map<String, IssueType> issueTypeMocks = new HashMap<>();

    public IssueTypeManagerBuilder addIssueType(String name) {
        issueTypeMocks.put(name, MockIssueType.create(name));
        return this;
    }

    IssueTypeManager build() {
        when(issueTypeManager.getIssueTypes()).then(invocation -> {
            List<IssueType> issueTypes = new ArrayList<>();
            for (Map.Entry<String, IssueType> entry : issueTypeMocks.entrySet()) {
                issueTypes.add(entry.getValue());
            }
            return issueTypes;
        });
        return issueTypeManager;
    }

    public static IssueTypeManager getDefaultIssueTypeManager() {
        IssueTypeManagerBuilder builder = new IssueTypeManagerBuilder();
        builder.addIssueType("task");
        builder.addIssueType("bug");
        builder.addIssueType("feature");

        return builder.build();
    }
}

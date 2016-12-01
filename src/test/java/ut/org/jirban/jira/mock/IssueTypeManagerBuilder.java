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

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

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.atlassian.jira.issue.link.IssueLinkManager;

/**
 * @author Kabir Khan
 */
public class IssueLinkManagerBuilder {
    private final IssueLinkManager issueLinkManager = mock(IssueLinkManager.class);

    private List<String> outwardLinks = new ArrayList<>();
    private List<String> inwardLinks = new ArrayList<>();

    public IssueLinkManager build() {
        //TODO configure some links
        when(issueLinkManager.getInwardLinks(anyLong())).thenReturn(Collections.emptyList());
        when(issueLinkManager.getOutwardLinks(anyLong())).thenReturn(Collections.emptyList());
        return issueLinkManager;
    }
}

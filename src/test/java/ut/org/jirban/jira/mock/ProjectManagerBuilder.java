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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.project.ProjectManager;

/**
 * @author Kabir Khan
 */
public class ProjectManagerBuilder {
    private final ProjectManager projectManager = mock(ProjectManager.class);

    private ProjectManagerBuilder() {
    }

    public static ProjectManager getAnyProjectManager() {
        ProjectManagerBuilder builder = new ProjectManagerBuilder();
        return builder.buildAnyProject();
    }

    private ProjectManager buildAnyProject() {
        when(projectManager.getProjectByCurrentKey(anyString())).then(invocation -> {
            String key = (String)invocation.getArguments()[0];
            return new MockProject(key);
        });
        return projectManager;
    }
}

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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public class PermissionManagerBuilder {
    private final PermissionManager permissionManager = Mockito.mock(PermissionManager.class);
    private final Callback callback;

    private PermissionManagerBuilder(Callback callback) {
        this.callback = callback;
    }

    public static PermissionManager getAllowsAll() {
        return new PermissionManagerBuilder(ALLOWS_ALL).build();
    }

    public static PermissionManager getDeniessAll() {
        return new PermissionManagerBuilder(DENIES_ALL).build();
    }

    public static PermissionManager getForCallback(Callback callback) {
        return new PermissionManagerBuilder(callback).build();
    }

    private PermissionManager build() {
        when(permissionManager.hasPermission(any(ProjectPermissionKey.class), any(Project.class), any(ApplicationUser.class)))
                .then(invocation -> callback.hasPermission(
                        (ProjectPermissionKey)invocation.getArguments()[0],
                        (Project) invocation.getArguments()[1],
                        (ApplicationUser) invocation.getArguments()[2]));
        return permissionManager;
    }

    public interface Callback {
        boolean hasPermission(ProjectPermissionKey permission, Project project, ApplicationUser user);
    }

    private static Callback ALLOWS_ALL = (permission, project, user) -> true;
    private static Callback DENIES_ALL = (permission, project, user) -> false;
}

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

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public class GlobalPermissionManagerBuilder {
    private final GlobalPermissionManager permissionManager = Mockito.mock(GlobalPermissionManager.class);
    private final Callback callback;

    private GlobalPermissionManagerBuilder(Callback callback) {
        this.callback = callback;
    }

    public static GlobalPermissionManager getAllowsAll() {
        return new GlobalPermissionManagerBuilder(ALLOWS_ALL).build();
    }

    public static GlobalPermissionManager getDeniessAll() {
        return new GlobalPermissionManagerBuilder(DENIES_ALL).build();
    }

    public static GlobalPermissionManager getForCallback(Callback callback) {
        return new GlobalPermissionManagerBuilder(callback).build();
    }

    private GlobalPermissionManager build() {
        when(permissionManager.hasPermission(any(GlobalPermissionKey.class), any(ApplicationUser.class)))
                .then(invocation -> callback.hasPermission(
                        (GlobalPermissionKey) invocation.getArguments()[0],
                        (ApplicationUser) invocation.getArguments()[1]));
        return permissionManager;
    }

    public interface Callback {
        boolean hasPermission(GlobalPermissionKey key, ApplicationUser user);
    }

    private static Callback ALLOWS_ALL = (key, user) -> true;
    private static Callback DENIES_ALL = (key, user) -> false;
}

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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
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

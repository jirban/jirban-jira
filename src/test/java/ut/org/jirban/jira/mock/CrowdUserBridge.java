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

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;

/**
 * @author Kabir Khan
 */
public class CrowdUserBridge {
    private final UserManager userManager;

    public CrowdUserBridge(UserManager userManager) {
        this.userManager = userManager;
    }

    public User getUserByKey(String key) {
        ApplicationUser applicationUser = userManager.getUserByKey(key);
        if (applicationUser == null) {
            return null;
        }
        return mapUser(applicationUser);
    }

    public

    static User mapUser(final ApplicationUser applicationUser) {
        if (applicationUser == null) {
            return null;
        }
        return new User() {
            public long getDirectoryId() {
                return 0;
            }

            public boolean isActive() {
                return applicationUser.isActive();
            }

            public String getEmailAddress() {
                return applicationUser.getEmailAddress();
            }

            public String getDisplayName() {
                return applicationUser.getDisplayName();
            }

            public int compareTo(User user) {
                return 0;
            }

            public String getName() {
                return applicationUser.getName();
            }

            @Override
            public String toString() {
                return super.toString();
            }
        };
    }


}

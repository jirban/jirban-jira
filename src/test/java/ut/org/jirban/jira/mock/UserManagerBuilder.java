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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;

/**
 * @author Kabir Khan
 */
public class UserManagerBuilder {
    private final UserManager userManager = mock(UserManager.class);
    private static long counter;

    private final HashMap<String, String> users = new HashMap<>();

    public UserManagerBuilder addUser(String username, String displayName) {
        users.put(username, displayName);
        return this;
    }

    public UserManagerBuilder addDefaultUsers() {
        addUser("kabir", "Kabir Khan");
        addUser("brian", "Brian Stansberry");
        addUser("jason", "Jason Greene");
        addUser("stuart", "Stuart Douglas");
        addUser("james", "James Perkins");
        return this;
    }

    public UserManager build() {
        return build(null);
    }

    public UserManager build(MockComponentWorker worker) {
        when(userManager.getUserByKey(any(String .class))).then(invocation -> {
            String name = (String)invocation.getArguments()[0];
            String displayName = users.get(name);
            if (displayName == null) {
                return null;
            }
            return new ApplicationUser() {

                @Override
                public Long getId() {
                    return counter++;
                }

                public String getKey() {
                    return name;
                }

                public String getUsername() {
                    return name;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public long getDirectoryId() {
                    return 0;
                }

                @Override
                public boolean isActive() {
                    return true;
                }

                @Override
                public String getEmailAddress() {
                    return name + "@example.com";
                }

                @Override
                public String getDisplayName() {
                    return displayName;
                }

                @Override
                public User getDirectoryUser() {
                    return null;
                }
            };
        });
        if (worker != null) {
            worker.addMock(UserManager.class, userManager);
        }
        return userManager;
    }


//    private static class UserInfo {
//        final String username;
//        final String displayName;
//    }
}

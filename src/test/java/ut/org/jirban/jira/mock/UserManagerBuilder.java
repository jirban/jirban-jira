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

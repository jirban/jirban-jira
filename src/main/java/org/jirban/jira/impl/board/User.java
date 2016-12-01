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

package org.jirban.jira.impl.board;

import static org.jirban.jira.impl.Constants.EMAIL;
import static org.jirban.jira.impl.Constants.NAME;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.Constants;

import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public class User {
    static final User UNASSIGNED = new User(null, null, null);

    private final String key;
    private final String email;
    private final String displayName;

    protected User(String key, String email, String displayName) {
        this.key = key;
        this.email = email;
        this.displayName = displayName;
    }

    static User create(ApplicationUser user) {
        return new User(
                user.getName(),
                user.getEmailAddress(),
                user.getDisplayName());
    }

    public void serialize(ModelNode parent) {
        parent.add(createSerializedNode());
    }

    protected ModelNode createSerializedNode() {
        ModelNode modelNode = new ModelNode();
        modelNode.get(Constants.KEY).set(key);
        modelNode.get(EMAIL).set(email);
        modelNode.get(NAME).set(displayName);
        return modelNode;
    }

    public String getKey() {
        return key;
    }

    String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }
}

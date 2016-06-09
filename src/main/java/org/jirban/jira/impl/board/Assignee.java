/*
 *
 *  JBoss, Home of Professional Open Source
 *  Copyright 2016, Red Hat, Inc., and individual contributors as indicated
 *  by the @authors tag.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.jirban.jira.impl.board;

import static org.jirban.jira.impl.Constants.AVATAR;
import static org.jirban.jira.impl.Constants.EMAIL;
import static org.jirban.jira.impl.Constants.NAME;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.Constants;

import com.atlassian.crowd.embedded.api.User;

/**
 * @author Kabir Khan
 */
public class Assignee {
    static final Assignee UNASSIGNED = new Assignee(null, null, null, null);

    private final String key;
    private final String email;
    private final String avatarUrl;
    private final String displayName;

    private Assignee(String key, String email, String avatarUrl, String displayName) {
        this.key = key;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.displayName = displayName;
    }

    static Assignee create(User user, String avatarUrl) {
        return new Assignee(
                user.getName(),
                user.getEmailAddress(),
                avatarUrl,
                user.getDisplayName());
    }

    public void serialize(ModelNode parent) {
        ModelNode modelNode = new ModelNode();
        modelNode.get(Constants.KEY).set(key);
        modelNode.get(EMAIL).set(email);
        modelNode.get(AVATAR).set(avatarUrl);
        modelNode.get(NAME).set(displayName);
        parent.add(modelNode);
    }

    public String getKey() {
        return key;
    }

    String getDisplayName() {
        return displayName;
    }
}

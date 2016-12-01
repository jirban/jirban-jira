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

import static org.jirban.jira.impl.Constants.AVATAR;

import org.jboss.dmr.ModelNode;

import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public class Assignee extends org.jirban.jira.impl.board.User {
    static final Assignee UNASSIGNED = new Assignee(null, null, null, null);

    private final String avatarUrl;

    private Assignee(String key, String email, String avatarUrl, String displayName) {
        super(key, email, displayName);
        this.avatarUrl = avatarUrl;
    }

    static Assignee create(ApplicationUser user, String avatarUrl) {
        return new Assignee(
                user.getName(),
                user.getEmailAddress(),
                avatarUrl,
                user.getDisplayName());
    }

    protected ModelNode createSerializedNode() {
        ModelNode modelNode = super.createSerializedNode();
        modelNode.get(AVATAR).set(avatarUrl);
        return modelNode;
    }
}

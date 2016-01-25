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

import org.jboss.dmr.ModelNode;

import com.atlassian.crowd.embedded.api.User;

/**
 * @author Kabir Khan
 */
class Assignee {
    private final String key;
    private final String email;
    private final String avatarUrl;
    private final String displayName;

    public Assignee(String key, String email, String avatarUrl, String displayName) {
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

    void serialize(int index, ModelNode parent) {
        ModelNode modelNode = new ModelNode();
        modelNode.get("key").set(key);
        modelNode.get("email").set(email);
        modelNode.get("avatar").set(avatarUrl);
        modelNode.get("name").set(displayName);
        parent.add(modelNode);
    }

    String getKey() {
        return key;
    }

    String getDisplayName() {
        return displayName;
    }

    public boolean isDataSame(Assignee that) {
        if (that == null) {
            return false;
        }
        //I don't want to do a standard equals() since I am not comparing all the data
        if (!key.equals(that.key)) return false;
        if (!email.equals(that.email)) return false;
        if (!avatarUrl.equals(that.avatarUrl)) return false;
        return displayName.equals(that.displayName);
    }

}

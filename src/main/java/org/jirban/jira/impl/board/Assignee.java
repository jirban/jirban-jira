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

/**
 * @author Kabir Khan
 */
class Assignee {
    private final String key;
    private final String email;
    private final String avatarUrl;
    private final String displayName;
    private volatile int index;

    Assignee(ModelNode assigneeNode) {
        this.key = assigneeNode.get("name").asString(); //There is also a 'key' field but it looks the same as 'name'
        email = assigneeNode.get("emailAddress").asString();
        avatarUrl = assigneeNode.get("avatarUrls", "32x32").asString();
        displayName = assigneeNode.get("displayName").asString();
    }

    void serialize(int index, ModelNode parent) {
        this.index = index;
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

    public int getIndex() {
        return index;
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

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

package org.jirban.jira.impl;

import java.sql.Date;
import java.text.SimpleDateFormat;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.api.UserAccessManager;
import org.jirban.jira.impl.activeobjects.UserAccess;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;

import net.java.ao.DBParam;
import net.java.ao.Query;

/**
 * @author Kabir Khan
 */
@Named("userAccessManager")
public class UserAccessManagerImpl implements UserAccessManager {

    @ComponentImport
    private final ActiveObjects activeObjects;

    @Inject
    public UserAccessManagerImpl(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    @Override
    public void logUserAccess(ApplicationUser user, String boardCode, String userAgent) {
        activeObjects.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                final UserAccess userAccess = activeObjects.create(
                        UserAccess.class,
                        new DBParam("USER_KEY", user.getKey()),
                        new DBParam("USER_NAME", user.getDisplayName()),
                        new DBParam("BOARD_CODE", boardCode),
                        new DBParam("TIME", new Date(System.currentTimeMillis())),
                        new DBParam("USER_AGENT", userAgent));

                userAccess.save();
                return null;
            }
        });
    }

    @Override
    public String getUserAccessJson(ApplicationUser user) {
        UserAccess[] accesses = activeObjects.executeInTransaction(new TransactionCallback<UserAccess[]>() {
            @Override
            public UserAccess[] doInTransaction() {
                //Just return the 1000 latest entries
                UserAccess[] accesses = activeObjects.find(UserAccess.class, Query.select().order("TIME desc").limit(1000));
                return accesses;
            }
        });

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");

        ModelNode list = new ModelNode();
        list.setEmptyList();
        for (UserAccess access : accesses) {
            ModelNode entry = new ModelNode();
            entry.get("user", "key").set(access.getUserKey());
            entry.get("user", "name").set(access.getUserName());
            entry.get("board").set(access.getBoardCode());
            entry.get("agent").set(access.getUserAgent() == null ? "n/a" : access.getUserAgent());

            String formattedDate = format.format(access.getTime());
            entry.get("time").set(formattedDate);
            list.add(entry);
        }
        return list.toJSONString(true);
    }
}

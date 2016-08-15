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
    public void logUserAccess(ApplicationUser user, String boardCode) {
        activeObjects.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                final UserAccess userAccess = activeObjects.create(
                        UserAccess.class,
                        new DBParam("USER_KEY", user.getKey()),
                        new DBParam("USER_NAME", user.getDisplayName()),
                        new DBParam("BOARD_CODE", boardCode),
                        new DBParam("TIME", new Date(System.currentTimeMillis())));

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

            String formattedDate = format.format(access.getTime());
            entry.get("time").set(formattedDate);
            list.add(entry);
        }
        return list.toJSONString(true);
    }
}

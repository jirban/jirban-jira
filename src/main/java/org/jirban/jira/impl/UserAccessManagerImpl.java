package org.jirban.jira.impl;

import java.sql.Date;

import javax.inject.Inject;

import org.jirban.jira.api.UserAccessManager;
import org.jirban.jira.impl.activeobjects.UserAccess;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;

import net.java.ao.DBParam;

/**
 * @author Kabir Khan
 */
public class UserAccessManagerImpl implements UserAccessManager {

    @ComponentImport
    private final ActiveObjects activeObjects;

    @Inject
    public UserAccessManagerImpl(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    @Override
    public void logUserAccess(ApplicationUser user) {
        activeObjects.executeInTransaction(new TransactionCallback<Object>() {
            @Override
            public Void doInTransaction() {
                final UserAccess userAccess = activeObjects.create(
                        UserAccess.class,
                        new DBParam("USER", user.getName()),
                        new DBParam("TIME", new Date(System.currentTimeMillis())));
                userAccess.save();
                return null;
            }
        });
    }
}

package org.jirban.jira.impl.activeobjects;

import java.sql.Date;

import com.atlassian.jira.user.ApplicationUser;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.NotNull;

/**
 * @author Kabir Khan
 */
@Preload
public interface UserAccess extends Entity {

    @NotNull
    ApplicationUser getUser();
    void setUser(ApplicationUser user);

    @NotNull
    Date getTime();
    void setTime(Date time);
}

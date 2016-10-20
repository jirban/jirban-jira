package org.jirban.jira.impl.activeobjects;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.NotNull;

/**
 * @author Kabir Khan
 */
@Preload
public interface UserAccess extends Entity {

    @NotNull
    String getUserKey();
    void setUserKey(String key);

    @NotNull
    String getUserName();
    void setUserName(String name);

    @NotNull
    String getBoardCode();
    void setBoardCode(String code);

    @NotNull
    Date getTime();
    void setTime(Date time);

    // old data can be null
    String getUserAgent();
    void setUserAgent(String agent);
}

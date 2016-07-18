package org.jirban.jira.api;

import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public interface UserAccessManager {
    void logUserAccess(ApplicationUser user);
}

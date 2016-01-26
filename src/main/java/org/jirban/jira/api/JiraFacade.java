package org.jirban.jira.api;

import org.jboss.dmr.ModelNode;

import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;

public interface JiraFacade
{
    /**
     * Gets the board configurations
     * @param user the logged in user
     * @return the boards that the user is allowed to configure
     */
    String getBoardConfigurations(ApplicationUser user);

    /**
     * Creates a new or saves a board configuration
     * @param user the logged in user
     * @param id the id of the board
     * @param jiraUrl the url of jira
     * @param config the configuration to save
     */
    void saveBoardConfiguration(ApplicationUser user, int id, String jiraUrl, ModelNode config);

    /**
     * Deletes a board from the storage
     * @param user the logged in user
     * @param id the id of the board
     */
    void deleteBoardConfiguration(ApplicationUser user, int id);

    /**
     * Gets the boards visible to the user.
     * @param user
     * @return the json of the boards
     */
    String getBoardsForDisplay(ApplicationUser user);

    /**
     * Gets a board for displaying to the user
     * @param user the user
     * @param id the id
     * @return the board's json
     * @throws SearchException
     */
    String getBoardJson(ApplicationUser user, int id) throws SearchException;
}
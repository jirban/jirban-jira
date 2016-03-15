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
     * @param backlog if {@true} we will include issues belonging to the backlog states
     * @param id the id
     * @return the board's json
     * @throws SearchException
     */
    String getBoardJson(ApplicationUser user, boolean backlog, int id) throws SearchException;

    /**
     * Gets the changes for a board. The client passes in their view id, and the delta is passed back to the client in
     * json format so they can apply it to their own model.
     *
     * @param user the logged in user
     * @param backlog if {@true} we will include issues belonging to the backlog states
     * @param id the board id
     * @param viewId the view id of the client.
     * @return the json containing the changes
     */
    String getChangesJson(ApplicationUser user, boolean backlog, int id, int viewId) throws SearchException;
}
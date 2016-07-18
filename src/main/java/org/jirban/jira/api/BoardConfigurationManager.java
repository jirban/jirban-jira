/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jirban.jira.api;

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.config.BoardConfig;

import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public interface BoardConfigurationManager {
    /**
     * Gets all the boards. This does not do any attempt to validate the JSON, so that following a structure change
     * for the config, we can still go in and edit everything. It filters boards which the user can not see from
     * the view, but does not throw any permission exceptions.
     *
     * @param user      the logged in user
     * @param forConfig whether this is to edit/view the config (in which case we return everything),
     *                  or to display the list of boards in view mode (in which case we return id/name pairs)
     * @return the json for the boards
     */
    String getBoardsJson(ApplicationUser user, boolean forConfig);

    /**
     * Saves a new board (if {@code id < 0}, or updates an exisiting one. Permissions are checked to see if the user
     * can update anything
     * @param user    the logged in user
     * @param id      the id of the board
     * @param config  the configuration
     * @throws org.jirban.jira.JirbanPermissionException if the user does not have the correct permissions
     * @throws org.jirban.jira.JirbanValidationException if the input is bad
     */
    BoardConfig saveBoard(ApplicationUser user, int id, ModelNode config);


    /**
     * Deletes a board. Permissions are checked to see if the user
     * can delete it anything
     * @param user    the logged in user
     * @param id      the id of the board
     * @throws org.jirban.jira.JirbanPermissionException if the user does not have the correct permissions
     */
    String deleteBoard(ApplicationUser user, int id);

    /**
     * Loads the board configuration. Permissions are checked to see if the user
     * can update anything.
     * @param user the user
     * @param code the configuration code
     * @return the configuration
     * @throws org.jirban.jira.JirbanPermissionException if the user does not have the correct permissions
     * @throws org.jirban.jira.JirbanValidationException if the structure of the config is bad
     */
    BoardConfig getBoardConfigForBoardDisplay(ApplicationUser user, String code);

    /**
     * Gets all the boards set up for a given project
     *
     * @param projectCode the project code
     * @return the board codes
     */
    List<String> getBoardCodesForProjectCode(String projectCode);

    /**
     * Saves the id of the custom field that Jira Agile uses for its 'Rank'.
     *
     * @param user the logged in user
     * @param idNode an object containing the id
     */
    void saveCustomFieldId(ApplicationUser user, ModelNode idNode);


    String getBoardJsonConfig(ApplicationUser user, int boardId);

    String getStateHelpTextsJson(ApplicationUser user, String boardCode);
}


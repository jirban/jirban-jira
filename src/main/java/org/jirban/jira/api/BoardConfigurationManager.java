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

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.config.BoardConfig;

import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public interface BoardConfigurationManager {
    /**
     * Gets all the boards.
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
     *  @param user    the logged in user
     * @param id      the id of the board
     * @param config  the configuration
     */
    void saveBoard(ApplicationUser user, int id, ModelNode config);

    void deleteBoard(ApplicationUser user, int id);

    /**
     * Loads the board configuration
     * @param user the user
     * @param id the id of the configuration
     * @return the configuration
     */
    BoardConfig getBoardConfig(ApplicationUser user, int id);
}


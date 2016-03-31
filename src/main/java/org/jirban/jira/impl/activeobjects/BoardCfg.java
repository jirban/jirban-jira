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
package org.jirban.jira.impl.activeobjects;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Unique;

/**
 * @author Kabir Khan
 */
@Preload
public interface BoardCfg extends Entity {
    /**
     * The name of the board that will appear in the overview lists of boards
     * @return the name
     */
    @NotNull
    @Unique
    String getName();
    void setName(String name);

    /**
     * The code of the board that will be used when accessing the boards. Using ID is a bit strict, in case we need to
     * drop a board for whatever reason.
     * @return the code
     */
    @NotNull
    @Unique
    String getCode();
    void setCode(String name);

    /**
     * The key of the ApplicationUser who created the board configuration.
     * That user will be used to load the boards.
     * @return the ApplicationUser key
     */
    @NotNull
    String getOwningUser();
    void setOwningUserKey(String name);




    @NotNull
    @StringLength(StringLength.UNLIMITED)
    String getConfigJson();
    void setConfigJson(String json);
}

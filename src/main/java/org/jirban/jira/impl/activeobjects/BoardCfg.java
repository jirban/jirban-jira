/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jirban.jira.impl.activeobjects;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Unique;

/**
 * @author Kabir Khan
 */
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

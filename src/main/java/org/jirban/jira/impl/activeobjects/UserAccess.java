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

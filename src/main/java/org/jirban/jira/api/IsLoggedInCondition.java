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

import java.util.Map;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

/**
 * Reimplementation of com.atlassian.jira.plugin.webfragment.conditions.UserLoggedInCondition since Jira seems to be
 * having some problems loading up the main conditions. Using UserLoggedInCondition, I bump into:
 * https://answers.atlassian.com/questions/32978766/jira-7-plugin---no-conditions-available-for-web-items
 *
 * @author Kabir Khan
 */
public class IsLoggedInCondition implements Condition {

    @Override
    public void init(Map<String, String> map) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> context) {
        ApplicationUser appUser = (ApplicationUser)context.get("user");
        if(appUser == null) {
            String username = (String)context.get("username");
            appUser = ComponentAccessor.getUserUtil().getUserByName(username);
        }

        return appUser != null;
    }
}

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

package org.jirban.jira.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserProfile;

@Named("jirbanAuthFilter")
public class AuthFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final UserManager jiraUserManager;

    @ComponentImport
    private final com.atlassian.sal.api.user.UserManager salUserManager;

    @Inject
    public AuthFilter(final com.atlassian.sal.api.user.UserManager salUserManager) {
        //It does not seem to like me trying to inject both user managers
        this.jiraUserManager = ComponentAccessor.getUserManager();
        this.salUserManager = salUserManager;
    }

    public void init(FilterConfig filterConfig)throws ServletException{
    }

    public void destroy(){
    }


    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)throws IOException,ServletException{
        final HttpServletRequest req = (HttpServletRequest)request;
        final ApplicationUser user = getUserByKey(req);

        //continue the request
        if (user != null) {
            chain.doFilter(request, response);
        } else {
            Util.sendErrorJson((HttpServletResponse)response, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private ApplicationUser getUserByKey(HttpServletRequest request) {
        UserProfile userProfile = salUserManager.getRemoteUser(request);
        if (userProfile == null) {
            return null;
        }
        return jiraUserManager.getUserByKey(userProfile.getUserKey().getStringValue());
    }


}
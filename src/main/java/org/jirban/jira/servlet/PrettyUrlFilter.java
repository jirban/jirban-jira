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

import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jirban.jira.JirbanLogger;

/**
 * @author Kabir Khan
 */
@Named("jirbanPrettyUrlFilter")
public class PrettyUrlFilter implements Filter {

    private static final String JIRBAN = "/jirban";
    private static final String JIRBAN_RESOURCES = "/download/resources/org.jirban.jirban-jira/webapp";
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest == false) {
            chain.doFilter(request, response);
            return;
        }

        final HttpServletRequest req = (HttpServletRequest)request;
        final String originalPath = req.getServletPath();
        if (!originalPath.startsWith(JIRBAN)) {
            JirbanLogger.LOGGER.debug("Jirban pretty url filter using original path {}", originalPath);
            chain.doFilter(request, response);
            return;
        }

        String subPath = originalPath.substring(JIRBAN.length());
        boolean redirect = false;
        if (subPath.length() == 0 || subPath.equals("/")) {
            subPath = JIRBAN + "/index.html";
            JirbanLogger.LOGGER.debug("Jirban pretty url filter empty path will be redirected");
            redirect = true;
        }

        if (redirect) {
            HttpServletResponse resp = (HttpServletResponse)response;
            subPath = req.getContextPath() + subPath;
            JirbanLogger.LOGGER.debug("Jirban pretty url filter redirecting {} to {}", originalPath, subPath);
            resp.sendRedirect(subPath);
        } else {
            final String realPath = JIRBAN_RESOURCES + subPath;
            JirbanLogger.LOGGER.debug("Jirban pretty url filter forwarding {} to {}", originalPath, subPath);
            final RequestDispatcher rd = request.getRequestDispatcher(realPath);
            rd.forward(request, response);
        }
    }

    @Override
    public void destroy() {

    }
}

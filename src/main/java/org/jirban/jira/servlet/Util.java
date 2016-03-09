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
package org.jirban.jira.servlet;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanLogger;

import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
class Util {
    static final String CONTENT_APP_JSON = "application/json";

    private static volatile String BASE_URL;

    static void sendErrorJson(final HttpServletResponse response, final int error) throws IOException {
        sendErrorJson(response, error, null);
    }

    static void sendErrorJson(final HttpServletResponse response, final int error, final String message) throws IOException {

        ModelNode msgNode = new ModelNode();
        if (message == null) {
            msgNode.get("message").set("");
        } else {
            msgNode.get("messsage").set(message);
        }
        final String msg = msgNode.toJSONString(true);
        JirbanLogger.LOGGER.debug("Sending error Json. code={}; message={}", error, msg);
        response.setContentType(CONTENT_APP_JSON);
        response.sendError(error, msg);
        response.flushBuffer();
    }

    static String getDeployedUrl(HttpServletRequest request) {
        if (BASE_URL == null) {
            String contextPath = request.getContextPath();
            String uri = request.getRequestURI();
            String url = request.getRequestURL().toString();

            String base = url.substring(0, url.indexOf(uri));
            base += contextPath;
            BASE_URL = base;
        }

        return BASE_URL;
    }
}

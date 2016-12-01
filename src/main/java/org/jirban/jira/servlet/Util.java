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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanLogger;

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

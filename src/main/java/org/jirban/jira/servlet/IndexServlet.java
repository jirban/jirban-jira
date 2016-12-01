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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;

/**
 * Loads up the init.vm template which drives the angular app, initialised with links to the correct directories.
 */
@Named("jirbanIndexServlet")
public class IndexServlet extends HttpServlet {

    private static final String INDEX_BROWSER_TEMPLATE = "/webapp/index.vm";

    @ComponentImport
    private final TemplateRenderer templateRenderer;

    private final ApplicationProperties applicationProperties;

    @Inject
    public IndexServlet(final TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
        this.applicationProperties = ComponentAccessor.getApplicationProperties();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder webappRoot = new StringBuilder(applicationProperties.getString(APKeys.JIRA_BASEURL));
        if (webappRoot.charAt(webappRoot.length() - 1) != '/') {
            webappRoot.append('/');
        }
        webappRoot.append("jirban");

        Map<String, Object> context = new HashMap<>();
        context.put("webappRoot", webappRoot.toString());
        templateRenderer.render(INDEX_BROWSER_TEMPLATE, context, resp.getWriter());
    }

}
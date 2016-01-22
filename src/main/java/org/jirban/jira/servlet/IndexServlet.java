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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(IndexServlet.class);

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
        System.out.println("Initialising the template");

        StringBuilder webappRoot = new StringBuilder(applicationProperties.getString(APKeys.JIRA_BASEURL));
        if (webappRoot.charAt(webappRoot.length() - 1) != '/') {
            webappRoot.append('/');
        }
        webappRoot.append("download/resources/org.jirban.jirban-jira/webapp");

        Map<String, Object> context = new HashMap<>();
        context.put("webappRoot", webappRoot.toString());
        templateRenderer.render(INDEX_BROWSER_TEMPLATE, context, resp.getWriter());
    }

}
package org.jirban.jira.impl;

import javax.inject.Inject;
import javax.inject.Named;

import org.jirban.jira.api.JiraFacade;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;

@ExportAsService ({JiraFacade.class})
@Named ("myPluginComponent")
public class JiraFacadeImpl implements JiraFacade
{
    @ComponentImport
    private final ApplicationProperties applicationProperties;

    @Inject
    public JiraFacadeImpl(final ApplicationProperties applicationProperties)
    {
        this.applicationProperties = applicationProperties;
    }

    public String getName()
    {
        if(null != applicationProperties)
        {
            return "myComponent:" + applicationProperties.getDisplayName();
        }
        
        return "myComponent";
    }
}
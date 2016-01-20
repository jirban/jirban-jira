package it.org.jirban.jira;

import static org.junit.Assert.assertEquals;

import org.jirban.jira.api.JiraFacade;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.atlassian.sal.api.ApplicationProperties;

@RunWith(AtlassianPluginsTestRunner.class)
public class MyComponentWiredTest
{
    private final ApplicationProperties applicationProperties;
    private final JiraFacade jiraFacade;

    public MyComponentWiredTest(ApplicationProperties applicationProperties,JiraFacade jiraFacade)
    {
        this.applicationProperties = applicationProperties;
        this.jiraFacade = jiraFacade;
    }

    @Test
    public void testMyName()
    {
        assertEquals("names do not match!", "myComponent:" + applicationProperties.getDisplayName(), jiraFacade.getName());
    }
}
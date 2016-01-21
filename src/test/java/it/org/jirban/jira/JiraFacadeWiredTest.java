package it.org.jirban.jira;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;

@RunWith(AtlassianPluginsTestRunner.class)
public class JiraFacadeWiredTest
{
/*    @ComponentImport
    private final ApplicationProperties applicationProperties;
    @ComponentImport
    private final JiraFacade jiraFacade;

    @Autowired
    public JiraFacadeWiredTest(ApplicationProperties applicationProperties,JiraFacade jiraFacade)
    {
        this.applicationProperties = applicationProperties;
        this.jiraFacade = jiraFacade;
    }*/

    @Test
    public void testMyName()
    {
//        assertEquals("names do not match!", "myComponent:" + applicationProperties.getDisplayName(), jiraFacade.getName());
    }
}
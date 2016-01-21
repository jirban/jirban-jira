package it.org.jirban.jira;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;

@RunWith(AtlassianPluginsTestRunner.class)
public class MyComponentWiredTest
{
/*    @ComponentImport
    private final ApplicationProperties applicationProperties;
    @ComponentImport
    private final JiraFacade jiraFacade;

    @Autowired
    public MyComponentWiredTest(ApplicationProperties applicationProperties,JiraFacade jiraFacade)
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
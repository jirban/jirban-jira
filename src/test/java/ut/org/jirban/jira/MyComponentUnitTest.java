package ut.org.jirban.jira;

import static org.junit.Assert.assertEquals;

import org.jirban.jira.api.JiraFacade;
import org.jirban.jira.impl.JiraFacadeImpl;
import org.junit.Test;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        JiraFacade component = new JiraFacadeImpl(null, null, null, null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}
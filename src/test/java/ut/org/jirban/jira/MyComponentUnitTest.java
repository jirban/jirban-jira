package ut.org.jirban.jira;

import org.junit.Test;
import org.jirban.jira.api.MyPluginComponent;
import org.jirban.jira.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}
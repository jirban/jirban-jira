package ut.org.jirban.jira.servlet;

import static org.mockito.Mockito.mock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AuthFilterTest {

    HttpServletRequest mockRequest;
    HttpServletResponse mockResponse;

    @Before
    public void setup() {
        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testSomething() {
//        String expected = "test";
//        when(mockRequest.getParameter(Mockito.anyString())).thenReturn(expected);
//        assertEquals(expected,mockRequest.getParameter("some string"));

    }
}

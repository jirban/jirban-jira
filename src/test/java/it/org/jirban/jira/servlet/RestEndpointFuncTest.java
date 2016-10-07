package it.org.jirban.jira.servlet;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;


public class RestEndpointFuncTest {

    DefaultHttpClient httpClient;
    String baseUrl;

    @Before
    public void setup() {
        httpClient = new DefaultHttpClient();
        baseUrl = System.getProperty("baseurl");

    }

    @After
    public void tearDown() {
        httpClient.getConnectionManager().shutdown();
    }

    @Test
    public void versionAvailable() throws IOException {

        // to achieve simple auth via http://username:password@ADDRESS
        String endpointUrl = baseUrl.replaceAll("//", "//" + AuthConstants.USERNAME + ":" + AuthConstants.PASSWORD + "@")
                + "/rest/jirban/1.0/version";
        HttpGet httpget = new HttpGet(endpointUrl);

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpClient.execute(httpget, responseHandler);

        assertTrue(null != responseBody && !"".equals(responseBody) && responseBody.contains("jirban-version"));

    }
}

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

package it.org.jirban.jira.servlet;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


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

/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2019 51 Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY.
 *
 * This Original Work is licensed under the European Union Public Licence (EUPL) 
 * v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 * 
 * If using the Work as, or as part of, a network application, by 
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading, 
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.pipeline.cloudrequestengine;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClient;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CloudRequestEngineTests {
    HttpClient httpClient;
    private TestLoggerFactory loggerFactory;

    private URL expectedUrl = new URL("https://cloud.51degrees.com/api/v4/json");

    public CloudRequestEngineTests() throws MalformedURLException {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }

    @Before
    public void Init() throws IOException {
        // ARRANGE
        httpClient = mock(HttpClient.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new Exception("The method '" +
                invocationOnMock.getMethod().getName() + "' should not have been called.");
            }
        });
        final HttpURLConnection connection = mock(HttpURLConnection.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new Exception("The method '" +
                    invocationOnMock.getMethod().getName() + "' should not have been called.");
            }
        });
        doNothing().when(connection).setConnectTimeout(anyInt());
        doNothing().when(connection).setReadTimeout(anyInt());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                doReturn((URL)invocationOnMock.getArgument(0)).when(connection).getURL();
                return (Object)connection;
            }
        }).when(httpClient).connect(any(URL.class));
        doReturn(200).when(connection).getResponseCode();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                URL url = ((HttpURLConnection)invocationOnMock.getArgument(0)).getURL();
                if (url.toString().endsWith("properties")) {
                    return "{'Products': {}}";
                }
                else if (url.toString().endsWith("evidencekeys")) {
                    return "[]";
                }
                else {
                    throw new Exception("A request was made with the URL '" +
                    url + "'");
                }
            }
        }).when(httpClient).getResponseString(any(HttpURLConnection.class));

        doReturn("{'device':{'value':'1'}}")
            .when(httpClient)
            .postData(
                any(HttpURLConnection.class),
                ArgumentMatchers.<String, String>anyMap(),
                (byte[])any());
    }

    /**
     * Test cloud request engine adds correct information to post request
     * and returns the response in the ElementData
     */
    @Test
    public void Process() throws Exception {
        final String resourceKey = "resource_key";
        final String userAgent = "iPhone";

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey(resourceKey)
            .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(engine).build()) {
            FlowData data = pipeline.createFlowData();
            data.addEvidence("query.User-Agent", userAgent);

            data.process();

            String result = data.getFromElement(engine).getJsonResponse();
            assertEquals("{'device':{'value':'1'}}", result);

            JSONObject jsonObj = new JSONObject(result);
            assertEquals(1, jsonObj.getJSONObject("device").getInt("value"));
        }

        verify(httpClient, times(1)) // we expected a single external POST request
            .postData(
            argThat(new ArgumentMatcher<HttpURLConnection>() {
                @Override
                public boolean matches(HttpURLConnection c) {
                    return c.getURL().equals(expectedUrl); // to this uri
                }}),
            ArgumentMatchers.<String, String>anyMap(),
            argThat(new ArgumentMatcher<byte[]>() {
                @Override
                public boolean matches(byte[] bytes) {
                    String string = new String(bytes);
                    return string.contains("resource=" + resourceKey) && // content contains resource key
                        string.contains("User-Agent=" + userAgent); // content contains user agent
                }}));
    }

    /**
     * Test cloud request engine adds correct information to post request
     * and returns the response in the ElementData
     */
    @Test
    public void Process_LicenseKey() throws Exception {
        final String resourceKey = "resource_key";
        final String userAgent = "iPhone";
        final String licenseKey = "ABCDEFG";

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey(resourceKey)
            .setLicenseKey(licenseKey)
            .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(engine)
            .build()) {
            FlowData data = pipeline.createFlowData();
            data.addEvidence("query.User-Agent", userAgent);

            data.process();

            String result = data.getFromElement(engine).getJsonResponse();
            assertEquals("{'device':{'value':'1'}}", result);

            JSONObject jsonObj = new JSONObject(result);
            assertEquals(1, jsonObj.getJSONObject("device").getInt("value"));
        }

        verify(httpClient, times(1)) // we expected a single external POST request
            .postData(
                argThat(new ArgumentMatcher<HttpURLConnection>() {
                    @Override
                    public boolean matches(HttpURLConnection c) {
                        return c.getURL().equals(expectedUrl); // to this uri
                    }}),
                ArgumentMatchers.<String, String>anyMap(),
                argThat(new ArgumentMatcher<byte[]>() {
                    @Override
                    public boolean matches(byte[] bytes) {
                        String string = new String(bytes);
                        return string.contains("resource=" + resourceKey) && // content contains resource key
                            string.contains("license=" + licenseKey) && // content contains license key
                            string.contains("User-Agent=" + userAgent); // content contains user agent
                    }}));
    }
}

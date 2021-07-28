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
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClient;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class CloudRequestEngineTests {
    HttpClient httpClient;
    private TestLoggerFactory loggerFactory;

    private URL expectedUrl = new URL("https://cloud.51degrees.com/api/v4/resource_key.json");
    private String jsonResponse = "{'device':{'value':'1'}}";
    private String evidenceKeysResponse = "['query.User-Agent']";
    private String accessiblePropertiesResponse =
            "{'Products': {'device': {'DataTier': 'tier','Properties': [{'Name': 'value','Type': 'String','Category': 'Device'}]}}}";
    private int accessiblePropertiesResponseCode = 200;

    public CloudRequestEngineTests() throws MalformedURLException {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }

    /**
     * Test cloud request engine adds correct information to post request
     * and returns the response in the ElementData
     */
    @Test
    public void Process() throws Exception {
        final String resourceKey = "resource_key";
        final String userAgent = "iPhone";

        configureMockedClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey(resourceKey)
            .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(engine).build();
            FlowData data = pipeline.createFlowData()) {
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

        configureMockedClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey(resourceKey)
            .setLicenseKey(licenseKey)
            .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(engine)
            .build();
            FlowData data = pipeline.createFlowData()) {
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


    /**
     * Verify that the CloudRequestEngine can correctly parse a
     * response from the accessible properties endpoint that contains
     * meta-data for sub-properties.
     */
    @Test
    public void subProperties() throws Exception {
        accessiblePropertiesResponse =
        "{\n" +
        "    \"Products\": {\n" +
        "        \"device\": {\n" +
        "            \"DataTier\": \"CloudV4TAC\",\n" +
        "            \"Properties\": [\n" +
        "                {\n" +
        "                    \"Name\": \"IsMobile\",\n" +
        "                        \"Type\": \"Boolean\",\n" +
        "                        \"Category\": \"Device\"\n" +
        "                },\n" +
        "                {\n" +
        "                    \"Name\": \"IsTablet\",\n" +
        "                        \"Type\": \"Boolean\",\n" +
        "                        \"Category\": \"Device\"\n" +
        "                }\n" +
        "            ]\n" +
        "        },\n" +
        "        \"devices\": {\n" +
        "            \"DataTier\": \"CloudV4TAC\",\n" +
        "            \"Properties\": [\n" +
        "                {\n" +
        "                    \"Name\": \"Devices\",\n" +
        "                    \"Type\": \"Array\",\n" +
        "                    \"Category\": \"Unspecified\",\n" +
        "                    \"ItemProperties\": [\n" +
        "                        {\n" +
        "                            \"Name\": \"IsMobile\",\n" +
        "                            \"Type\": \"Boolean\",\n" +
        "                            \"Category\": \"Device\"\n" +
        "                        },\n" +
        "                        {\n" +
        "                            \"Name\": \"IsTablet\",\n" +
        "                            \"Type\": \"Boolean\",\n" +
        "                            \"Category\": \"Device\"\n" +
        "                        }\n" +
        "                    ]\n" +
        "                }\n" +
        "            ]\n" +
        "        }\n" +
        "    }\n" +
        "}";
        configureMockedClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("key")
                .build();

        assertEquals(2, engine.getPublicProperties().size());
        AccessiblePropertyMetaData.ProductMetaData deviceProperties = engine.getPublicProperties().get("device");
        assertEquals(2, deviceProperties.properties.size());
        assertTrue(propertiesContainName(deviceProperties.properties, "IsMobile"));
        assertTrue(propertiesContainName(deviceProperties.properties, "IsTablet"));
        AccessiblePropertyMetaData.ProductMetaData devicesProperties = engine.getPublicProperties().get("devices");
        assertEquals(1, devicesProperties.properties.size());
        assertEquals("Devices", devicesProperties.properties.get(0).name);
        assertTrue(propertiesContainName(devicesProperties.properties.get(0).itemProperties, "IsMobile"));
        assertTrue(propertiesContainName(devicesProperties.properties.get(0).itemProperties, "IsTablet"));
    }

    private boolean propertiesContainName(
            List<AccessiblePropertyMetaData.PropertyMetaData> properties,
            String name) {
        for (AccessiblePropertyMetaData.PropertyMetaData property : properties) {
            if (property.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
       
    
    /** 
     * Test cloud request engine handles errors from the cloud service 
     * as expected.
     * An AggregateException should be thrown by the cloud request engine
     * containing the errors from the cloud service
     * and the pipeline is configured to throw any exceptions up 
     * the stack in an AggregateException.
     * We also check that the exception message includes the content 
     * from the JSON response.
     */ 
    @Test
    public void validateErrorHandling_InvalidResourceKey() throws Exception
    {
        final String resourceKey = "resource_key";
        accessiblePropertiesResponse = "{ \"errors\":[\"58982060: resource_key not a valid resource key\"]}";
        accessiblePropertiesResponseCode = 400;

        configureMockedClient();

        Exception exception = null;

        try { 
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey(resourceKey)
                .build();
        }
        catch (Exception ex)
        {
            exception = ex;
        }

        assertNotNull("Expected exception to occur", exception);
        assertTrue(exception instanceof RuntimeException);
        Exception aggEx = (RuntimeException)exception;
        assertEquals(1, aggEx.getSuppressed().length);
        Throwable realEx = aggEx.getSuppressed()[0];
        assertTrue(realEx instanceof Exception);
        assertTrue("Exception message did not contain the expected text.", 
                realEx.getMessage().contains(
            "resource_key not a valid resource key"));
    }    

    /**
     * Verify that the request to the cloud service will contain 
     * the configured origin header value.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void OriginHeader() throws Exception {
        final String resourceKey = "resource_key";
        final String userAgent = "iPhone";
        final String origin = "51degrees.com";

        configureMockedClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey(resourceKey)
            .setCloudRequestOrigin(origin)
            .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(engine).build();
            FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", userAgent);

            data.process();
        }

        ArgumentCaptor<Map<String, String>> argumentsCaptured = ArgumentCaptor.forClass(Map.class);

        verify(httpClient, times(1)) // we expected a single external POST request
            .postData(
            argThat(new ArgumentMatcher<HttpURLConnection>() {
                @Override
                public boolean matches(HttpURLConnection c) {
                    return c.getURL().equals(expectedUrl); // to this uri
                }}),
            argumentsCaptured.capture(),
            argThat(new ArgumentMatcher<byte[]>() {
                @Override
                public boolean matches(byte[] bytes) {
                    String string = new String(bytes);
                    return string.contains("resource=" + resourceKey) && // content contains resource key
                        string.contains("User-Agent=" + userAgent); // content contains user agent
                }}));
        
        // Verify that the origin header has the expected value.
        Map<String, String> headers = argumentsCaptured.getValue();
        assertTrue(headers.containsKey(Constants.OriginHeaderName)); 
        assertEquals(origin, headers.get(Constants.OriginHeaderName)); 
    }

    private void configureMockedClient() throws IOException {
        // ARRANGE
        httpClient = mock(HttpClient.class, new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new Exception("The method '" +
                        invocationOnMock.getMethod().getName() + "' should not have been called.");
            }
        });
        final HttpURLConnection connection = mock(HttpURLConnection.class, new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new Exception("The method '" +
                        invocationOnMock.getMethod().getName() + "' should not have been called.");
            }
        });
        doNothing().when(connection).setConnectTimeout(anyInt());
        doNothing().when(connection).setReadTimeout(anyInt());
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                URL url = (URL)invocationOnMock.getArgument(0);
                doReturn(url).when(connection).getURL();
                if (url.getPath().endsWith("properties")) {
                    doReturn(accessiblePropertiesResponseCode).when(connection).getResponseCode();
                }
                else {
                    doReturn(200).when(connection).getResponseCode();
                }
                return (Object)connection;
            }
        }).when(httpClient).connect(any(URL.class));
        
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                URL url = ((HttpURLConnection)invocationOnMock.getArgument(0)).getURL();
                if (url.getPath().endsWith("properties")) {
                    return accessiblePropertiesResponse;
                }
                else if (url.getPath().endsWith("evidencekeys")) {
                    return evidenceKeysResponse;
                }
                else {
                    throw new Exception("A request was made with the URL '" +
                            url + "'");
                }
            }
        }).when(httpClient).getResponseString(
            any(HttpURLConnection.class), 
            ArgumentMatchers.<String, String>anyMap());

        doReturn(jsonResponse)
                .when(httpClient)
                .postData(
                        any(HttpURLConnection.class),
                        ArgumentMatchers.<String, String>anyMap(),
                        (byte[])any());
    }
}

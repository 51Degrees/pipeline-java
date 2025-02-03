/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2025 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.pipeline.cloudrequestengine.CloudRequestException;
import fiftyone.pipeline.cloudrequestengine.Constants;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.exceptions.PropertyNotLoadedException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CloudRequestEngineTests extends CloudRequestEngineTestsBase {
    public CloudRequestEngineTests() throws MalformedURLException {
        super();
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


        Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build();
        FlowData data = pipeline.createFlowData();
        data.addEvidence("query.User-Agent", userAgent);
        data.process();

        String result = data.getFromElement(engine).getJsonResponse();
        assertEquals("{'device':{'value':'1'}}", result);

        JSONObject jsonObj = new JSONObject(result);
        assertEquals(1, jsonObj.getJSONObject("device").getInt("value"));

        verify(httpClient, times(1)) // we expected a single external POST request
                .postData(
                        argThat(c -> {
                            return c.getURL().equals(expectedUrl); // to this uri
                        }),
                        ArgumentMatchers.anyMap(),
                        argThat(bytes -> {
                            String string = new String(bytes);
                            return string.contains("resource=" + resourceKey) && // content contains resource key
                                    string.contains("User-Agent=" + userAgent); // content contains user agent
                        }));
    }

    /**
     * Test errors thrown by cloud request engine are added to flow.errors when SuppressProcessExceptions == true
     */
    @Test
    public void Process_SuppressProcessExceptions_Exceptions_Added_To_Errors() throws Exception {
        final String resourceKey = "resourcekey";
        final String userAgent = "iPhone";
        configureFailingMockClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey(resourceKey)
                .build();

        Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .build();
        FlowData data = pipeline.createFlowData();

        data.addEvidence("query.User-Agent", userAgent);
        data.process();

        assertFalse(data.getErrors().isEmpty());
        ArrayList<FlowError> errors = (ArrayList<FlowError>) data.getErrors();
        Throwable throwable = errors.get(0).getThrowable();
        assertInstanceOf(IOException.class, throwable);
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
                        argThat(c -> {
                            return c.getURL().equals(expectedUrl); // to this uri
                        }),
                        ArgumentMatchers.anyMap(),
                        argThat(bytes -> {
                            String string = new String(bytes);
                            return string.contains("resource=" + resourceKey) && // content contains resource key
                                    string.contains("license=" + licenseKey) && // content contains license key
                                    string.contains("User-Agent=" + userAgent); // content contains user agent
                        }));
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

    private static Stream<Arguments> getValidateErrorArgs() {
        return Stream.of(
                Arguments.of("include message", 400, true),
                Arguments.of("no message", 400, false),
                Arguments.of("include message", 200, true));
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
    @ParameterizedTest(name = "response code {1} - {0}")
    @MethodSource("getValidateErrorArgs")
    public void validateErrorHandling(String name, int responseCode, boolean includeMessage) throws Exception {
        final String resourceKey = "resource_key";
        String errorMessage = "some error message";
        if (includeMessage) {
            accessiblePropertiesResponse = "{ \"errors\":[\"" + errorMessage + "\"]}";
        }
        accessiblePropertiesResponseCode = responseCode;

        configureMockedClient();

        Exception exception = null;

        try {
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                    .setResourceKey(resourceKey)
                    .build()
                    .getPublicProperties();
        } catch (Exception ex) {
            exception = ex;
        }

        assertNotNull(exception, "Expected exception to occur");
        assertTrue(exception instanceof CloudRequestException);
        CloudRequestException cloudEx = (CloudRequestException) exception;
        if (includeMessage) {
            assertTrue(cloudEx.getMessage().contains(errorMessage),
                    "Exception message did not contain the expected text.");
        }
    }

    private static Stream<Arguments> getPrecidenceArgs() {
        return Stream.of(
                Arguments.of("query+header (no conflict)", false, "query.User-Agent=iPhone", "header.User-Agent=iPhone"),
                Arguments.of("query+cookie (no conflict)", false, "query.User-Agent=iPhone", "cookie.User-Agent=iPhone"),
                Arguments.of("header+cookie (conflict)", true, "header.User-Agent=iPhone", "cookie.User-Agent=iPhone"),
                Arguments.of("query+a (no conflict)", false, "query.value=1", "a.value=1"),
                Arguments.of("a+b (conflict)", true, "a.value=1", "b.value=1"),
                Arguments.of("e+f (conflict)", true, "e.value=1", "f.value=1"));
    }

    /**
     * Test cloud request engine adds correct information to post request
     * following the order of precedence when processing evidence and
     * returns the response in the ElementData. Evidence parameters
     * should be added in descending order of precedence.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getPrecidenceArgs")
    public void evidencePrecidence(String name, boolean shouldWarn, String evidence1, String evidence2) throws Exception {
        loggerFactory.loggers.clear();
        String[] evidence1Parts = evidence1.split("=");
        String[] evidence2Parts = evidence2.split("=");
        configureMockedClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();

        Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();

        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence(evidence1Parts[0], evidence1Parts[1]);

            flowData.addEvidence(evidence2Parts[0], evidence2Parts[1]);

            flowData.process();

            if (shouldWarn) {
                // If warn is expected then check for warnings from cloud request
                // engine.
                loggerFactory.assertMaxWarnings(1);
                String warning = "";
                for (TestLogger logger : loggerFactory.loggers) {
                    if (logger.warningsLogged.size() == 1) {
                        warning = logger.warningsLogged.get(0);
                        break;
                    }
                }
                assertEquals(
                        String.format(Constants.Messages.EvidenceConflict,
                                evidence1Parts[0],
                                evidence1Parts[1],
                                String.format("%s:%s", evidence2Parts[0], evidence2Parts[1])),
                        warning);
            } else {
                loggerFactory.assertMaxWarnings(0);
            }
        }
    }

    @SuppressWarnings("serial")
    private static Stream<Arguments> getSelectedEvidenceArgs() {
        return Stream.of(
                Arguments.of(
                        "query",
                        new HashMap<String, Object>() {{
                            put("query.User-Agent", "iPhone");
                            put("header.User-Agent", "iPhone");
                        }},
                        "query",
                        new HashMap<String, Object>() {{
                            put("query.User-Agent", "iPhone");
                        }}),
                Arguments.of(
                        "other",
                        new HashMap<String, Object>() {{
                            put("header.User-Agent", "iPhone");
                            put("a.User-Agent", "iPhone");
                            put("z.User-Agent", "iPhone");
                        }},
                        "other",
                        new HashMap<String, Object>() {{
                            put("z.User-Agent", "iPhone");
                            put("a.User-Agent", "iPhone");
                        }}));
    }

    /**
     * Test evidence of specific type is returned from all
     * the evidence passed, if type is not from query, header
     * or cookie then evidences are returned sorted in descensing order
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getSelectedEvidenceArgs")
    public void getSelectedEvidence(String name, Map<String, Object> evidence, String type, Map<String, Object> expectedValue) throws Exception {
        configureMockedClient();
        CloudRequestEngineDefault engine = (CloudRequestEngineDefault) new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();

        Map<String, Object> result = engine.getSelectedEvidence(evidence, type);

        assertEquals(expectedValue, result);
    }

    @SuppressWarnings("serial")
    private static Stream<Arguments> getFormDataArgs() {
        return Stream.of(
                Arguments.of(
                        "query > header",
                        new HashMap<String, Object>() {{
                            put("query.User-Agent", "query-iPhone");
                            put("header.User-Agent", "header-iPhone");
                        }},
                        "query-iPhone"),
                Arguments.of(
                        "header > cookie",
                        new HashMap<String, Object>() {{
                            put("header.User-Agent", "header-iPhone");
                            put("cookie.User-Agent", "cookie-iPhone");
                        }},
                        "header-iPhone"),
                Arguments.of(
                        "a > b > z",
                        new HashMap<String, Object>() {{
                            put("a.User-Agent", "a-iPhone");
                            put("b.User-Agent", "b-iPhone");
                            put("z.User-Agent", "z-iPhone");
                        }},
                        "a-iPhone"));
    }

    /**
     * Test Content to send in the POST request is generated as
     * per the precedence rule of The evidence keys. These are
     * added to the evidence in reverse order, if there is conflict then
     * the queryData value is overwritten.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getFormDataArgs")
    public void getFormData(String name, Map<String, Object> evidence, String expectedValue) throws Exception {
        configureMockedClient();
        CloudRequestEngineDefault engine = (CloudRequestEngineDefault) new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();

        Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();

        try (FlowData data = pipeline.createFlowData()) {
            for (Map.Entry<String, Object> entry : evidence.entrySet()) {
                data.addEvidence(entry.getKey(), entry.getValue());
            }

            Map<String, Object> result = engine.getFormData(data);
            assertEquals(expectedValue, result.get("User-Agent"));
        }
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
                        argThat(c -> {
                            return c.getURL().equals(expectedUrl); // to this uri
                        }),
                        argumentsCaptured.capture(),
                        argThat(bytes -> {
                            String string = new String(bytes);
                            return string.contains("resource=" + resourceKey) && // content contains resource key
                                    string.contains("User-Agent=" + userAgent); // content contains user agent
                        }));

        // Verify that the origin header has the expected value.
        Map<String, String> headers = argumentsCaptured.getValue();
        assertTrue(headers.containsKey(Constants.OriginHeaderName));
        assertEquals(origin, headers.get(Constants.OriginHeaderName));
    }

    /**
     * Verify that the request to the cloud service will contain
     * the configured origin header value.
     */
    @Test
    public void HttpDataSetInException() throws Exception {
        final String resourceKey = "resource_key";

        try {
            new CloudRequestEngineBuilder(loggerFactory, new HttpClientDefault())
                    .setResourceKey(resourceKey)
                    .build()
                    .getPublicProperties();
            fail("Expected exception was not thrown");
        } catch (CloudRequestException ex) {
            assertTrue(ex.getHttpStatusCode() > 0, "Status code should not be 0");
            assertNotNull(ex.getResponseHeaders(), "Response headers not populated");
            assertTrue(ex.getResponseHeaders().size() > 0, "Response headers not populated");
        }
    }

    /**
     * Verify that resource key is set in propertiesEndPoint when
     * requested.
     */
    @Test
    public void getPublicProperties_Set_Resource_Key() {
        final String resourceKey = "resource_key";

        try {
            configureMockedClient();
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                    .setResourceKey(resourceKey)
                    .build()
                    .getPublicProperties();

            assertTrue(propertiesEndPoint.contains(resourceKey),
                    "Resource key is not set in properties endpoint.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown");
        }
    }

    /**
     * Verify that getPublicProperties contains test data
     */
    @Test
    public void getPublicProperties() throws Exception {
        configureMockedClient();
        final String resourceKey = "resource_key";
        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey(resourceKey)
                .build();
        Map<String, AccessiblePropertyMetaData.ProductMetaData> properties = engine.getPublicProperties();
        assertTrue(properties.containsKey("device"));
    }

    /**
     * Verify that PropertyNotLoadedException is thrown on getPublicProperties if remote server is unavailable
     */
    @Test
    public void getPublicProperties_Throw_Exception_When_Server_Unavailable() throws IOException {
        final String resourceKey = "resource_key";
        configureFailingMockClient();
        try {
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                    .setResourceKey(resourceKey)
                    .build()
                    .getPublicProperties();

            fail("Expected exception was not thrown");
        } catch (Exception e) {
            assertInstanceOf(PropertyNotLoadedException.class, e);
        }
    }

    /**
     * Verify that getEvidenceKeyFilter returns test data
     */
    @Test
    public void getEvidenceKeyFilter() throws Exception {
        configureMockedClient();
        final String resourceKey = "resource_key";
        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey(resourceKey)
                .build();
        EvidenceKeyFilter evidenceKeyFilter = engine.getEvidenceKeyFilter();
        assertTrue(evidenceKeyFilter.include("query.User-Agent"));
    }

    /**
     * Verify that PropertyNotLoadedException is thrown on getEvidenceKeyFilter if remote server is unavailable
     */
    @Test
    public void getEvidenceKeyFilter_Throw_Exception_When_Server_Unavailable() throws IOException {
        final String resourceKey = "resource_key";
        configureFailingMockClient();
        try {
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                    .setResourceKey(resourceKey)
                    .build()
                    .getEvidenceKeyFilter();

            fail("Expected exception was not thrown");
        } catch (Exception e) {
            assertInstanceOf(PropertyNotLoadedException.class, e);
        }
    }
}

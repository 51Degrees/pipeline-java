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

package fiftyone.pipeline.javascriptbuilder;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.javascriptbuilder.helpers.TcpHelper;
import fiftyone.pipeline.javascriptbuilder.helpers.TestServer;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.stubbing.Answer;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fiftyone.pipeline.core.Constants.EVIDENCE_PROTOCOL;
import static fiftyone.pipeline.core.Constants.EVIDENCE_QUERY_USERAGENT_KEY;
import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.EVIDENCE_SEQUENCE;
import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.EVIDENCE_SESSIONID;
import static fiftyone.pipeline.javascriptbuilder.Constants.EVIDENCE_HOST_KEY;
import static fiftyone.pipeline.javascriptbuilder.Constants.EVIDENCE_OBJECT_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class JavaScriptBuilderTests {
    private TestLoggerFactory loggerFactory;
    private JsonBuilder jsonBuilderElement;
    private ElementData elementDataMock;
    private FlowData flowData;
    private JavaScriptBuilderData result;
    private JavaScriptBuilderElement javaScriptBuilderElement;

    private TestServer testServer;
    private ChromeDriver driver;


    private JSONObject json;
    private Map<String, Object> evidence;
    private String protocol = "https";
    private String host = "localhost";
    private String userAgent = "iPhone";
    private String latitude = "51";
    private String longitude = "-1";
    private int sequence = 2;
    private String sessionId = "abcdefg-hijklmn-opqrst-uvwxyz";

    public JavaScriptBuilderTests() {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }

    @BeforeEach
    public void Init() throws IOException {
        flowData = mock(FlowData.class);
        jsonBuilderElement = mock(JsonBuilder.class);
        javaScriptBuilderElement = new JavaScriptBuilderElementBuilder(loggerFactory).build();
        evidence = new HashMap<>();

        List<ElementPropertyMetaData> properties = new ArrayList<>();
        properties.add(new ElementPropertyMetaDataDefault("property", jsonBuilderElement, "", String.class, true));
        doReturn(properties).when(jsonBuilderElement).getProperties();

        elementDataMock = mock(ElementData.class);
        Map<String,Object> data = new HashMap<>();
        data.put("property", "thisIsAValue" );
        doReturn(data).when(elementDataMock).asKeyMap();

        int port = TcpHelper.getAvailablePort();
        testServer = new TestServer(port);
        testServer.start();

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--headless");
        driver = new ChromeDriver(options);
        driver.navigate().to("http://localhost:" + port + "/");
    }

    @AfterEach
    public void Cleanup() {
        json = null;
        testServer.stop();
        driver.quit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void Valid_Js() throws Exception {
        configureMocks();

        javaScriptBuilderElement.process(flowData);

        assertTrue(isValidFodObject(result.getJavaScript(), "device", "ismobile", true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void JavaScriptBuilder_NoJson() {
        configureMocks();
        json = null;
        assertThrows(
            PipelineConfigurationException.class,
            () -> javaScriptBuilderElement.process(flowData));
    }

    /**
     * Verify that valid JavaScript is produced when there are delayed execution
     * properties in the payload.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void JavaScriptBuilderElement_DelayExecution() throws Exception {
        configureMocks();

        JSONObject locationData = new JSONObject();
        locationData.put("postcode", (String)null);
        locationData.put("postcodenullreason",
            "Evidence for this property has not been retrieved. Ensure the 'complete' method is called, passing the name of this property in the second parameter.");
        locationData.put("postcodeevidenceproperties", new String[] { "location.javascript" });
        locationData.put("javascript", "if (navigator.geolocation) { navigator.geolocation.getCurrentPosition(function() { // 51D replace this comment with callback function. }); }");
        locationData.put("javascriptdelayexecution", true);
        json.put("location", locationData);

        javaScriptBuilderElement =
                new JavaScriptBuilderElementBuilder(loggerFactory)
                        .build();
        javaScriptBuilderElement.process(flowData);

        assertTrue(
                result.getJavaScript().contains("getEvidencePropertiesFromObject"),
            "Expected the generated JavaScript to contain the " +
                "'getEvidencePropertiesFromObject' function but it does not.");
    }

    /**
     * Verify that the JavaScript contains the Session ID and Sequence
     */
    @Test
    @SuppressWarnings("unchecked")
    public void JavaScriptBuilderElement_VerifySession() throws Exception {
        configureMocks();

        javaScriptBuilderElement =
                new JavaScriptBuilderElementBuilder(loggerFactory)
                        .build();
        javaScriptBuilderElement.process(flowData);

        String javaScript = result.getJavaScript();
        assertTrue(javaScript.contains(sessionId), "JavaScript does not contain expected session id '" + sessionId + "'.");
        assertTrue(javaScript.contains("var sequence = " + sequence + ";"), "JavaScript does not contain expected sequence '1'.");
    }

    /**
     * Check that the callback URL is generated correctly.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void JavaScriptBuilderElement_VerifyUrl() throws Exception {
        configureMocks();

        javaScriptBuilderElement =
                new JavaScriptBuilderElementBuilder(loggerFactory)
                        .setEndpoint("/json")
                        .build();
        javaScriptBuilderElement.process(flowData);

        String expectedUrl = "https://localhost/json";
        String javaScript = result.getJavaScript();
        assertTrue(javaScript.contains(expectedUrl), "JavaScript does not contain expected URL '" + expectedUrl + "'.");
    }

    /**
     * Verify that parameters are set in the JavaScript payload and if the
     * query parameters are in the evidence
     */
    @ParameterizedTest
    @CsvSource(
            {"iPhone,51.12345,-1.92173272",
                    "Samsung,1.09199,2.1121121",
                    "Sony,3.123455,44.1123111"}
    )
    @SuppressWarnings("unchecked")
    public void JavaScriptBuilderElement_VerifyParameters(String userAgent, String lat, String lon) throws Exception {
        this.userAgent = userAgent;
        latitude = lat;
        longitude = lon;
        configureMocks();

        javaScriptBuilderElement =
                new JavaScriptBuilderElementBuilder(loggerFactory)
                        .build();
        javaScriptBuilderElement.process(flowData);
        String javaScript = result.getJavaScript();
        assertTrue(javaScript.contains(userAgent), "JavaScript does not contain expected user agent query parameter '" + userAgent + "'.");
        assertTrue(javaScript.contains(lat), "JavaScript does not contain expected latitude query parameter '" + lat + "'.");
        assertTrue(javaScript.contains(lon), "JavaScript does not contain expected longitude query parameter '" + lon + "'.");
    }

    public enum ExceptionCase {

        PROPERTY_MISSING(new PropertyMissingException(), false),
        PIPELINE_DATA_EXCEPTION(new PipelineDataException(), false),
        EXCEPTION(new RuntimeException(), true),
        NONE(null, false);

        public Throwable throwable;
        public boolean expected;

        ExceptionCase(Throwable throwable, boolean expected) {
            this.throwable = throwable;
            this.expected = expected;
        }
    }

    /**
     * Check that accessing the 'Promise' property works as intended in a range of scenarios
     */
    @ParameterizedTest
    @EnumSource(ExceptionCase.class)
    @SuppressWarnings("unchecked")
    public void JavaScriptBuilderElement_Promise(ExceptionCase exceptionCase) {
        configureMocks();

        switch (exceptionCase) {
            case PROPERTY_MISSING:
            case PIPELINE_DATA_EXCEPTION:
            case EXCEPTION:
                when(flowData.getAs("Promise", AspectPropertyValue.class))
                        .thenThrow(exceptionCase.throwable);
                break;
            case NONE:
                when(flowData.getAs("Promise", AspectPropertyValue.class))
                        .thenReturn(new AspectPropertyValueDefault<>("None"));
                break;
        }

        Exception thrown = null;
        javaScriptBuilderElement =
                new JavaScriptBuilderElementBuilder(loggerFactory)
                        .build();
        try {
            javaScriptBuilderElement.process(flowData);
        } catch (Exception e) {
            thrown = e;
        }

        if (exceptionCase.expected) {
            assertNotNull(thrown, "Expected an exception to be visible externally but it was not.");
        } else {
            assertNull(thrown, "Did not expect an exception to be visible externally but one was.");
            assertNotNull(result.getJavaScript(), "Expected JavaScript output to be populated but it was not.");
            assertNotEquals("", "Expected JavaScript output to be populated but it was not.");
        }
    }

    /**
     * Check that the JavaScript object name can be overridden successfully.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void JavaScriptBuilderElement_VerifyObjName() throws Exception {
        configureMocks();
        String jsonObjName = "testObj";
        evidence.put(EVIDENCE_OBJECT_NAME, jsonObjName);

        javaScriptBuilderElement =
                new JavaScriptBuilderElementBuilder(loggerFactory)
                        .build();
        javaScriptBuilderElement.process(flowData);

        driver.executeScript(result.getJavaScript() + "; window.testObj = testObj;");
    }

    private boolean isValidFodObject(String javaScript, String key, String property, Object value) {
        driver.executeScript(javaScript + "; window.fod = fod;");
        Object res = driver.executeScript("return fod." + key + "." + property + ";");

        return res.toString().equals(value.toString());
    }

    @SuppressWarnings("unchecked")
    private void configureMocks() {
        if (json == null) {
            json = new JSONObject();

            JSONObject device = new JSONObject();
            device.put("ismobile", true);
            device.put("browsername", JSONObject.NULL);

            json.put("device", device);
            json.put("nullValueReasons", new JSONObject().put("device.browsername", "property missing"));
        }

        doAnswer((Answer<Object>) invocation -> {
            JsonBuilderData result = mock(JsonBuilderData.class);
            if (json != null) {
                when(result.getJson()).thenReturn(json.toString(0));
            } else {
                when(result.getJson()).thenThrow(new PipelineDataException("nope"));
            }
            return result;
        }).when(flowData).get(JsonBuilderData.class);

        evidence.put(EVIDENCE_HOST_KEY, host);
        evidence.put(EVIDENCE_PROTOCOL, protocol);
        evidence.put(EVIDENCE_SEQUENCE, sequence);
        evidence.put(EVIDENCE_SESSIONID, sessionId);
        evidence.put(EVIDENCE_QUERY_USERAGENT_KEY, userAgent);
        evidence.put("query.latitude", latitude);
        evidence.put("query.longitude", longitude);

        Evidence evidenceObj = mock(Evidence.class);
        when(evidenceObj.asKeyMap()).thenReturn(evidence);
        when(flowData.getEvidence()).thenReturn(evidenceObj);
        when(flowData.tryGetEvidence(anyString(), any(Class.class))).thenAnswer(
                invocation -> {
                    TryGetResult<Object> getResult = new TryGetResult<>();
                    if (evidence.containsKey((String) invocation.getArgument(0))) {
                        getResult.setValue(evidence.get((String) invocation.getArgument(0)));
                    }
                    return getResult;
                }
        );

        when(flowData.get(anyString())).thenReturn(elementDataMock);

        when(flowData.getAs(anyString(), any(Class.class))).thenReturn(new AspectPropertyValueDefault<>("None"));

        doAnswer((Answer<Object>) invocation -> {
            FlowElement.DataFactory<JavaScriptBuilderData> factory =
                    invocation.getArgument(1);
            result = factory.create(flowData);
            return result;
        }).when(flowData).getOrAdd(anyString(), any(FlowElement.DataFactory.class));
    }
}

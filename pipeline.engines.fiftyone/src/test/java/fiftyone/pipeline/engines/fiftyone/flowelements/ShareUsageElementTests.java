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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.testhelpers.data.MockFlowData;
import fiftyone.pipeline.engines.trackers.Tracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import static fiftyone.pipeline.core.Constants.*;
import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static fiftyone.pipeline.engines.Constants.FIFTYONE_COOKIE_PREFIX;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"rawtypes", "unused"})
public class ShareUsageElementTests {

    // Share usage instance that is being tested
    private ShareUsageElement shareUsageElement;

    // XML builder factory used to parse XML for validation.
    private DocumentBuilderFactory docBuilderFactory =
        DocumentBuilderFactory.newInstance();

    // Mocks and dependencies
    private Pipeline pipeline;
    private Tracker tracker;
    private HttpClient httpClient;
    private HttpURLConnection httpClientConnection;

    // Test instance data.
    private List<String> xmlContent = new ArrayList<>();
    private SequenceElement sequenceElement;

    @BeforeEach
    public void Init() throws IOException {
        // Create the HttpClient using the mock handler
        httpClient = mock(HttpClient.class);

        // Create the HttpURLConnection which will be returned by the HTTP
        // client
        httpClientConnection = mock(HttpURLConnection.class);
        doReturn(httpClientConnection).when(httpClient).connect(any(URL.class));

        // Configure the mock handler to store the XML content of requests
        // in the _xmlContent list and return an 'OK' status code.
        when(httpClient.postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class)))
            .then(new Answer<byte[]>() {
                @Override
                public byte[] answer(InvocationOnMock invocationOnMock) throws Throwable {
                    storeRequestXml((byte[])invocationOnMock.getArgument(2));
                    return null;
                }
            });

        // Configure the pipeline to return an empty list of flow elements
        pipeline = mock(Pipeline.class);
        when(pipeline.getFlowElements()).thenReturn(Collections.<FlowElement>emptyList());

        // Configure the tracker to always allow sharing.
        tracker = mock(Tracker.class);
        when(tracker.track(any(FlowData.class))).thenReturn(true);
    }

    private void createShareUsage(
        double sharePercentage,
        int minimumEntriesPerMessage,
        int interval,
        List<String> blockedHeaders,
        List<String> includedQueryStringParams,
        List<Entry<String, String>> ignoreDataEvidenceFiler) {
        sequenceElement = new SequenceElement(mock(Logger.class));
        sequenceElement.addPipeline(pipeline);
        shareUsageElement = new ShareUsageElement(
            mock(Logger.class),
            httpClient,
            sharePercentage,
            minimumEntriesPerMessage,
            minimumEntriesPerMessage * 2,
            100,
            100,
            interval,
            true,
            "http://51Degrees.com/test",
            blockedHeaders,
            includedQueryStringParams,
            ignoreDataEvidenceFiler,
            DEFAULT_SESSION_COOKIE_NAME,
            tracker);
        shareUsageElement.addPipeline(pipeline);
    }

    @Test
    public void ShareUsageElement_SingleEvent_ClientIPAndHeader() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<Entry<String, String>>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "x-forwarded-for", "5.6.7.8");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "forwarded-for", "2001::");
        evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + FIFTYONE_COOKIE_PREFIX + "Profile", "123456");
        evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + "RemoveMe", "123456");

        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        while (shareUsageElement.getSendDataFuture().isDone() == false) {

        }

        // Assert
        // Check that one and only one HTTP message was sent
        verify(httpClient, times(1)).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));
        assertEquals(1, xmlContent.size());

        // Validate that the XML is well formed by passing it through a reader
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
            xmlContent.get(0).getBytes("UTF-8")));

        // Check that the expected values are populated.
        assertTrue("XML did not contain the client IP. XML was : '" + xmlContent.get(0) + "'", xmlContent.get(0).contains("<ClientIP>1.2.3.4</ClientIP>"));
        assertTrue("XML did not contain the x-forwarded-for header. XML was : '" + xmlContent.get(0) + "'", xmlContent.get(0).contains("<header Name=\"x-forwarded-for\"><![CDATA[5.6.7.8]]></header>"));
        assertTrue("XML did not contain the forwarded-for header. XML was : '" + xmlContent.get(0) + "'", xmlContent.get(0).contains("<header Name=\"forwarded-for\"><![CDATA[2001::]]></header>"));
        assertTrue("XML did not contain the 51D_Profile cookie. XML was : '" + xmlContent.get(0) + "'", xmlContent.get(0).contains("<cookie Name=\"" + FIFTYONE_COOKIE_PREFIX + "Profile\"><![CDATA[123456]]></cookie>"));
        assertFalse("XML contained the RemoveMe cookie. XML was : '" + xmlContent.get(0) + "'", xmlContent.get(0).contains("<cookie Name=\"RemoveMe\">"));
    }

    @Test
    public void ShareUsageElement_TwoEvents_FirstEvent() throws Exception {
        // Arrange
        createShareUsage(
            1,
            2,
            1,
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<Entry<String, String>>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        shareUsageElement.process(data);

        // Assert
        // Check that no HTTP messages were sent.
        verify(httpClient, never()).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));
    }

    @Test
    public void ShareUsageElement_TwoEvents_SecondEvent() throws Exception {
        // Arrange
        createShareUsage(
            1,
            2,
            1,
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<Entry<String, String>>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        shareUsageElement.process(data);
        shareUsageElement.process(data);

        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        while (shareUsageElement.getSendDataFuture().isDone() == false) {

        }

        // Assert
        // Check that one and only one HTTP message was sent.
        verify(httpClient, times(1)).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));
        assertEquals(1, xmlContent.size());

        // Validate that the XML is well formed by passing it through a reader
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
            xmlContent.get(0).getBytes("UTF-8")));

        // Make sure there are 2 'Device' nodes
        int count = 0;
        int index = 0;
        while (index >= 0) {
            index = xmlContent.get(0).indexOf("<Device>", index + 1);
            if (index > 0) {
                count++;
            }
        }
        assertEquals(2, count);
    }

    @Test
    public void ShareUsageElement_RestrictedHeaders() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
            new ArrayList<>(Arrays.asList("x-forwarded-for", "forwarded-for")),
            new ArrayList<String>(),
            new ArrayList<Entry<String, String>>());

        String useragent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0 Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:42.0) Gecko/20100101 Firefox/42.0.";
        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "x-forwarded-for", "5.6.7.8");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "forwarded-for", "2001::");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "user-agent", useragent);
        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        while (shareUsageElement.getSendDataFuture().isDone() == false) {

        }

        // Assert
        // Check that one and only one HTTP message was sent.
        verify(httpClient, times(1)).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));
        assertEquals(1, xmlContent.size());

        // Validate that the XML is well formed by passing it through a reader
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
            xmlContent.get(0).getBytes("UTF-8")));
        // Check that the expected values are populated.
        assertTrue(
            "XML did not contain the client IP. XML was : '" + xmlContent.get(0) + "'",
            xmlContent.get(0).contains("<ClientIP>1.2.3.4</ClientIP>"));
        assertTrue(
            "XML did not contain User-Agent header. XML was : '" + xmlContent.get(0) + "'",
            xmlContent.get(0).contains("<header Name=\"user-agent\"><![CDATA[" + useragent + "]]></header>"));
        assertFalse(
            "XML contained the x-forwarded-for header. XML was : '" + xmlContent.get(0) + "'",
            xmlContent.get(0).contains("<header Name=\"x-forwarded-for\">"));
        assertFalse(
            "XML contained the forwarded-for header. XML was : '" + xmlContent.get(0) + "'",
            xmlContent.get(0).contains("<header Name=\"forwarded-for\">"));
    }

    @Test
    public void ShareUsageElement_LowPercentage() throws Exception {
        // Arrange
        createShareUsage(
            0.001,
            100,
            1,
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<Entry<String, String>>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        int requiredEvents = 0;
        while (xmlContent.size() == 0 &&
            requiredEvents <= 1000000) {
            shareUsageElement.process(data);
            requiredEvents++;
        }
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        while (shareUsageElement.getSendDataFuture().isDone() == false) {

        }

        // Assert
        // On average, the number of required events should be around
        // 100,000. However, as it's chance based it can vary
        // significantly. We only want to catch any gross errors so just
        // make sure the value is of the expected order of magnitude.
        assertTrue("Expected the number of required events to be at least " +
                "10,000, but was actually '" + requiredEvents + "'",
            requiredEvents > 10000);
        assertTrue("Expected the number of required events to be less than " +
                "1,000,000, but was actually '" + requiredEvents + "'",
            requiredEvents < 1000000);
        // Check that one and only one HTTP message was sent.
        verify(httpClient, times(1)).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));
        assertEquals(1, xmlContent.size());
    }

    @Test
    public void ShareUsageElement_SendOnCleanup() throws Exception {
        // Arrange
        createShareUsage(
            1,
            2,
            1,
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<Entry<String, String>>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        shareUsageElement.process(data);

        // No data should be being sending yet.
        assertNull(shareUsageElement.getSendDataFuture());
        verify(httpClient, never()).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));

        // Dispose of the element.
        shareUsageElement.close();

        // Assert
        // Check that no HTTP messages were sent.
        verify(httpClient, never()).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));
    }

    @Test
    public void ShareUsageElement_CancelOnServerError() throws Exception {
        // Arrange
        when(httpClient.postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class)))
            .then(new Answer<byte[]>() {
                @Override
                public byte[] answer(InvocationOnMock invocationOnMock) throws Throwable {
                    HttpURLConnection connection = invocationOnMock.getArgument(0);
                    when(connection.getResponseCode()).thenReturn(500);
                    return null;
                }
            });
        createShareUsage(
            1,
            1,
            1,
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<Entry<String, String>>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        while (shareUsageElement.getSendDataFuture().isDone() == false) {

        }

        // Assert
        // Check that no HTTP messages were sent.
        verify(httpClient, times(1)).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));
        assertTrue(shareUsageElement.isCanceled());
    }

    @Test
    public void ShareUsageElement_IgnoreOnEvidence() throws Exception {
        // Arrange
        when(httpClient.postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class)))
            .then(new Answer<byte[]>() {
                @Override
                public byte[] answer(InvocationOnMock invocationOnMock) throws Throwable {
                    HttpURLConnection connection = invocationOnMock.getArgument(0);
                    when(connection.getResponseCode()).thenReturn(200);
                    return null;
                }
            });
        createShareUsage(
            1,
            1,
            1,
            new ArrayList<String>(),
            new ArrayList<String>(),
            Arrays.asList(
                (Map.Entry<String, String>) new AbstractMap.SimpleEntry<String, String>("header.User-Agent", "Azure Traffic Manager Endpoint Monitor")
            ));

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put("header.User-Agent", "Azure Traffic Manager Endpoint Monitor");
        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        shareUsageElement.process(data);
        // Check that the consumer task did not start.
        assertNull(shareUsageElement.getSendDataFuture());

        // Assert
        // Check that no HTTP messages were sent.
        verify(httpClient, never()).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));
    }

    @Test
    public void ShareUsageBuilder_IgnoreData_InvalidFilter() throws IOException {

        for (String config : new String[]{"user-agent=iPhone", "user-agent,iPhone", "test,iPhone,block"}) {
            ILoggerFactory internalLoggerFactory = mock(ILoggerFactory.class);
            when(internalLoggerFactory.getLogger(anyString())).thenReturn(mock(Logger.class));
            TestLoggerFactory loggerFactory = new TestLoggerFactory(internalLoggerFactory);
            TestLogger logger = new TestLogger("test", mock(Logger.class));

            ShareUsageBuilder builder = new ShareUsageBuilder(loggerFactory, logger, httpClient);
            ShareUsageElement element = builder
                .setSharePercentage(1)
                .setMinimumEntriesPerMessage(1)
                .setRepeatEvidenceIntervalMinutes(1)
                .setIgnoreFlowDataEvidenceFilter(config)
                .build();

            assertTrue(logger.warningsLogged.size() > 0);
            assertTrue(logger.errorsLogged.size() == 0);
        }

    }

    @Test
    public void ShareUsageBuilder_IgnoreData_ValidFilter() throws IOException {
        for (String config : new String[]{"user-agent:iPhone", "user-agent:iPhone,host:bacon.com", "user-agent:iPhone,host:bacon.com,license:ABCDEF"}) {
            ILoggerFactory internalLoggerFactory = mock(ILoggerFactory.class);
            when(internalLoggerFactory.getLogger(anyString())).thenReturn(mock(Logger.class));
            TestLoggerFactory loggerFactory = new TestLoggerFactory(internalLoggerFactory);
            TestLogger logger = new TestLogger("test", mock(Logger.class));


            ShareUsageBuilder builder = new ShareUsageBuilder(loggerFactory, logger, httpClient);
            ShareUsageElement element = builder
                .setSharePercentage(1)
                .setMinimumEntriesPerMessage(1)
                .setRepeatEvidenceIntervalMinutes(1)
                .setIgnoreFlowDataEvidenceFilter(config)
                .build();

            assertTrue(logger.warningsLogged.size() == 0);
            assertTrue(logger.errorsLogged.size() == 0);
        }
    }


    /**
     * Check that the usage element can handle invalid xml chars.
     */
    @Test
    public void ShareUsageElement_BadSchema() throws Exception {
        // Arrange
        createShareUsage(1, 1, 1, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<Entry<String, String>>());

        Map<String, Object> evidenceData = new HashMap<>();

        // Contains hidden character at the end of the string.
        // (0x0018) - Cancel control character
        evidenceData.put(
            EVIDENCE_HEADER_USERAGENT_KEY,
            "iPhone\u0018");

        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        while (shareUsageElement.getSendDataFuture().isDone() == false) {
            Thread.sleep(10000);
        }

        // Assert
        // Check that one and only one HTTP message was sent.
        verify(httpClient, times(1)).connect(any(URL.class));
        assertEquals(1, xmlContent.size());

        // Validate that the XML is well formed by passing it through a reader
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
            xmlContent.get(0).getBytes("UTF-8")));

        // Check that the expected values are populated.
        assertTrue(xmlContent.get(0).contains("iPhone\\x0018"));
        assertTrue(xmlContent.get(0).contains("<BadSchema>true</BadSchema>"));

    }
    
    /**
     * Test that the ShareUsageElement generates a session id if one is not
     * contained in the evidence and adds it to the results.
     * @throws Exception 
     */
    @Test
    public void ShareUsageElement_SessionIdAndSequence_None() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<Entry<String, String>>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        
        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        sequenceElement.process(data);
        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        while (shareUsageElement.getSendDataFuture().isDone() == false) {

        }

        // Assert
        // Check that one and only one HTTP message was sent
        verify(httpClient, times(1)).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));
        assertEquals(1, xmlContent.size());

        // Validate that the XML is well formed by passing it through a reader
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
            xmlContent.get(0).getBytes("UTF-8")));

        // Check that the expected values are populated.
        assertTrue(data.getEvidence().asKeyMap().containsKey(fiftyone.pipeline.engines.fiftyone.flowelements.Constants.EVIDENCE_SESSIONID));
        assertTrue(data.getEvidence().asKeyMap().containsKey(fiftyone.pipeline.engines.fiftyone.flowelements.Constants.EVIDENCE_SEQUENCE));
        assertTrue("XML did not contain the SessionId. XML was : '" + xmlContent.get(0) + "'", xmlContent.get(0).contains("<SessionId>"));
        assertTrue("XML did not contain the correct Sequence. XML was : '" + xmlContent.get(0) + "'", xmlContent.get(0).contains("<Sequence>1</Sequence>"));
    }
    
    /**
     * Test that if a session id and sequence exists in the evidence the 
     * ShareUsageElement persists the session id and increments the 
     * sequence.
     * @throws Exception 
     */
    @Test
    public void ShareUsageElement_SessionIdAndSequence_Existing() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<Entry<String, String>>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(Constants.EVIDENCE_SESSIONID, "abcdefg-hijklmn-opqrst-uvwxyz");
        evidenceData.put(Constants.EVIDENCE_SEQUENCE, 2);
        
        FlowData data = MockFlowData.createFromEvidence(evidenceData, false);

        // Act
        sequenceElement.process(data);
        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        while (shareUsageElement.getSendDataFuture().isDone() == false) {

        }

        // Assert
        // Check that one and only one HTTP message was sent
        verify(httpClient, times(1)).postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class));
        assertEquals(1, xmlContent.size());

        // Validate that the XML is well formed by passing it through a reader
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
            xmlContent.get(0).getBytes("UTF-8")));

        // Check that the expected values are populated.
        assertTrue(data.getEvidence().asKeyMap().containsKey(fiftyone.pipeline.engines.fiftyone.flowelements.Constants.EVIDENCE_SESSIONID));
        assertTrue(data.getEvidence().asKeyMap().containsKey(fiftyone.pipeline.engines.fiftyone.flowelements.Constants.EVIDENCE_SEQUENCE));
        assertTrue("XML did not contain the SessionId. XML was : '" + xmlContent.get(0) + "'", xmlContent.get(0).contains("<SessionId>abcdefg-hijklmn-opqrst-uvwxyz</SessionId>"));
        assertTrue("XML did not contain the correct Sequence. XML was : '" + xmlContent.get(0) + "'", xmlContent.get(0).contains("<Sequence>3</Sequence>"));
    }

    private void storeRequestXml(byte[] request) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(request)) {
            try (GZIPInputStream gis = new GZIPInputStream(bis)) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(gis))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    xmlContent.add(sb.toString());
                }
            }
        }
    }
}

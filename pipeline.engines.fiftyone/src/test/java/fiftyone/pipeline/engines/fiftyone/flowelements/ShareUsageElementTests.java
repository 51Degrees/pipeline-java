/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2023 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.common.testhelpers.LogbackHelper;
import fiftyone.common.testhelpers.TestLogger;
import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.DataUploader;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.trackers.Tracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import static fiftyone.pipeline.core.Constants.*;
import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static fiftyone.pipeline.engines.Constants.FIFTYONE_COOKIE_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

public class ShareUsageElementTests {
    /**
     * Test implementation that outputs to byte array rather than HTTP
     */
    private static class TestConnector implements DataUploader {
        ByteArrayOutputStream baos= new ByteArrayOutputStream();
        private int code = 200;
        private int delay;

        @Override
        public OutputStream getOutputStream() {
            return baos;
        }
        @Override
        public int getResponseCode() {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {
            }
            return code;
        }

        public ByteArrayOutputStream getBaos() {
            return baos;
        }

        public void setResponseCode(int code) {
            this.code = code;
        }

        public void setResponseDelay(int delay) {
            this.delay = delay;
        }
    }

    /**
     * Mock HttpClient writes zipped data to a buffer
     */
    private static class TestHttpClient implements HttpClient {
        @Override
        public HttpURLConnection connect(URL url) {
            // mock HttpUrlConnection
            return new HttpURLConnection(url) {
                @Override
                public int getResponseCode()  {
                    return 200;
                }

                @Override
                public String getResponseMessage()  {
                    return "";
                }

                @Override
                public void disconnect() { }

                @Override
                public boolean usingProxy() {
                    return false;
                }

                @Override
                public void connect() {}
            };
        }
        byte[] buffer;
        @Override
        public String postData(HttpURLConnection connection, Map<String, String> headers, byte[] data)  {
            buffer = data;
            return "";
        }
        @Override
        public String getResponseString(HttpURLConnection connection)  {
            return "";
        }
        @Override
        public String getResponseString(HttpURLConnection connection, Map<String, String> headers)  {
            return "Happy Days";
        }

        public byte[] getBuffer(){
            return buffer;
        }
    }
    private static class TestTracker implements Tracker {
        @Override
        public boolean track(FlowData flowData) {
            return true;
        }
    }

    Logger logger = LoggerFactory.getLogger(this.getClass());

    // Share usage instance that is being tested
    private ShareUsageElement shareUsageElement;

    // XML builder factory used to parse XML for validation.
    private final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    private final XPath xpath = XPathFactory.newInstance().newXPath();

    // Dependencies
    private Pipeline pipeline;
    private Tracker tracker;

    TestConnector connector;
    // Test instance data.
    private SequenceElement sequenceElement;

    @BeforeEach
    public void Init() throws Exception {

        // Configure the pipeline to return an empty list of flow elements
        pipeline = new PipelineBuilder().build();

        // Configure the tracker to always allow sharing.
        tracker = new TestTracker();
    }

    private void createShareUsage(
            double sharePercentage,
            int minimumEntriesPerMessage,
            @SuppressWarnings("SameParameterValue") int interval,
            List<String> blockedHeaders,
            List<String> includedQueryStringParams,
            List<Entry<String, String>> ignoreDataEvidenceFiler) {
        sequenceElement = new SequenceElement(LoggerFactory.getLogger(SequenceElement.class));
        sequenceElement.addPipeline(pipeline);
        shareUsageElement = new ShareUsageElement(
            LoggerFactory.getLogger(ShareUsageElement.class),
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
        connector= new TestConnector();
        shareUsageElement.dataUploader = connector;
    }

    @Test
    public void ShareUsageElement_SingleEvent_ClientIPAndHeader() throws Exception {
        // Arrange
        createShareUsage(
                1,
                1,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "x-forwarded-for", "5.6.7.8");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "forwarded-for", "2001::");
        evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + FIFTYONE_COOKIE_PREFIX + "Profile", "123456");
        evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + "RemoveMe", "123456");

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);
        // Act
        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        shareUsageElement.getSendDataFuture().get();

        ByteArrayOutputStream baos = connector.getBaos();
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        NodeList devices = (NodeList) xpath.compile("//Devices/Device").evaluate(doc, XPathConstants.NODESET);
        assertEquals(1, devices.getLength(), "One device expected");

        NodeList headers = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX).evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals(2, headers.getLength(), "2 headers expected");
        NodeList header1 = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX+"[@Name='x-forwarded-for']").evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals("5.6.7.8",header1.item(0).getTextContent());
        NodeList header2 = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX+"[@Name='forwarded-for']").evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals("2001::",header2.item(0).getTextContent());

        NodeList cookies = (NodeList) xpath.compile(EVIDENCE_COOKIE_PREFIX).evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals(1, cookies.getLength(), "1 cookie expected");
        assertEquals("51d_Profile", cookies.item(0).getAttributes().getNamedItem("Name").getTextContent());
        assertEquals("123456", cookies.item(0).getFirstChild().getTextContent());

        NodeList clientIp = (NodeList) xpath.compile("ClientIP").evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals(1, clientIp.getLength(), "1 client IP expected");
        assertEquals("1.2.3.4", clientIp.item(0).getTextContent());


    }

    /**
     * Java 8 needs a utility to read from InputStream to OutputStream
     * @param source an input stream
     * @param target an output stream
     */
    private void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }

    // duplicate above test, only using legacy HttpClient approach
    @Test
    public void ShareUsageElement_SingleEvent_ClientIPAndHeader_HttpClient() throws Exception {
        // Arrange
        createShareUsage(
                1,
                1,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        shareUsageElement.httpClient = new TestHttpClient();
        shareUsageElement.dataUploader = null;

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "x-forwarded-for", "5.6.7.8");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "forwarded-for", "2001::");
        evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + FIFTYONE_COOKIE_PREFIX + "Profile", "123456");
        evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + "RemoveMe", "123456");

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);
        // Act
        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        shareUsageElement.getSendDataFuture().get();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] zip = ((TestHttpClient)shareUsageElement.httpClient).getBuffer();
        // unzip the buffer
        copy(new GZIPInputStream(new ByteArrayInputStream(zip)), baos);
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        NodeList devices = (NodeList) xpath.compile("//Devices/Device").evaluate(doc, XPathConstants.NODESET);
        assertEquals(1, devices.getLength(), "One device expected");

        NodeList headers = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX).evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals(2, headers.getLength(), "2 headers expected");
        NodeList header1 = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX+"[@Name='x-forwarded-for']").evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals("5.6.7.8",header1.item(0).getTextContent());
        NodeList header2 = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX+"[@Name='forwarded-for']").evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals("2001::",header2.item(0).getTextContent());

        NodeList cookies = (NodeList) xpath.compile(EVIDENCE_COOKIE_PREFIX).evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals(1, cookies.getLength(), "1 cookie expected");
        assertEquals("51d_Profile", cookies.item(0).getAttributes().getNamedItem("Name").getTextContent());
        assertEquals("123456", cookies.item(0).getFirstChild().getTextContent());

        NodeList clientIp = (NodeList) xpath.compile("ClientIP").evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals(1, clientIp.getLength(), "1 client IP expected");
        assertEquals("1.2.3.4", clientIp.item(0).getTextContent());


    }

    @Test
    public void ShareUsageElement_TwoEvents_FirstEvent() throws Exception {
        // Arrange
        createShareUsage(
            1,
            2,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // Act
        shareUsageElement.process(data);

        // sending of data never gets triggered
        assertNull(shareUsageElement.getSendDataFuture());
    }

    @Test
    public void ShareUsageElement_TwoEvents_SecondEvent() throws Exception {
        // Arrange
        createShareUsage(
            1,
            2,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // Act
        shareUsageElement.process(data);
        shareUsageElement.process(data);

        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        shareUsageElement.getSendDataFuture().get();

        ByteArrayOutputStream baos = connector.getBaos();
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        // Verify that there is only one devices element
        NodeList devices = (NodeList) xpath.compile("//Devices").evaluate(doc, XPathConstants.NODESET);
        assertEquals(1, devices.getLength(), "1 devices node expected");

        // Make sure there are 2 'Device' nodes
        NodeList device = (NodeList) xpath.compile("//Device").evaluate(devices.item(0), XPathConstants.NODESET);
        assertEquals(2, device.getLength(), "2 device nodes expected");
    }

    @Test
    public void ShareUsageElement_RestrictedHeaders() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
            new ArrayList<>(Arrays.asList("x-forwarded-for", "forwarded-for")),
                new ArrayList<>(),
                new ArrayList<>());

        String useragent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0 Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:42.0) Gecko/20100101 Firefox/42.0.";
        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "x-forwarded-for", "5.6.7.8");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "forwarded-for", "2001::");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "user-agent", useragent);

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);
        // Act
        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        shareUsageElement.getSendDataFuture().get();

        ByteArrayOutputStream baos = connector.getBaos();
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        // Verify that there is only one clientIP element
        NodeList clientIP = (NodeList) xpath.compile("//ClientIP").evaluate(doc, XPathConstants.NODESET);
        assertEquals(1, clientIP.getLength(), "1 ClientIP node expected");

        // Make sure there is a header node
        NodeList device = (NodeList) xpath.compile("//header[@Name='user-agent']").evaluate(doc, XPathConstants.NODESET);
        assertEquals(1, device.getLength(), "1 user-agent header expected");
        // Make sure no x-forwarded-for
        NodeList xf = (NodeList) xpath.compile("//header[@Name='x-forwarded-for']").evaluate(doc, XPathConstants.NODESET);
        assertEquals(0, xf.getLength(), "0 x-forwarded-for header expected");
        // Make sure no forwarded-for
        NodeList f = (NodeList) xpath.compile("//header[@Name='forwarded-for']").evaluate(doc, XPathConstants.NODESET);
        assertEquals(0, f.getLength(), "0 forwarded-for header expected");
    }

    @Test
    public void ShareUsageElement_LowPercentage() throws Exception {
        // Arrange
        createShareUsage(
            0.001,
            100,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // Act
        int requiredEvents = 0;
        while (connector.getBaos().size()==0 && requiredEvents <= 1000000) {
            shareUsageElement.process(data);
            requiredEvents++;
        }
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        shareUsageElement.getSendDataFuture().get();

        logger.info("Required events was {}", requiredEvents);

        // Assert
        // On average, the number of required events should be around
        // 100,000. However, as it's chance based it can vary
        // significantly. We only want to catch any gross errors so just
        // make sure the value is of the expected order of magnitude.
        assertTrue(requiredEvents > 10000,
                "Expected the number of required events to be at least " +
                        "10,000, but was actually '" + requiredEvents + "'");
        assertTrue(requiredEvents < 1000000,
                "Expected the number of required events to be less than " +
                        "1,000,000, but was actually '" + requiredEvents + "'");
    }
    @Test
    public void ShareUsageElement_SendOnCleanup() throws Exception {
        // Arrange
        createShareUsage(
            1,
            2,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // Act
        shareUsageElement.process(data);

        // No data should be being sending yet.
        assertNull(shareUsageElement.getSendDataFuture());

        // Dispose of the element.
        shareUsageElement.close();

        // Assert
        // data should have been sent
        assertNotNull(shareUsageElement.getSendDataFuture());
        assertTrue(shareUsageElement.isClosed());

        ByteArrayOutputStream baos = connector.getBaos();
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        // Check that the expected values are populated.
        NodeList f = (NodeList) xpath.compile("//Device").evaluate(doc, XPathConstants.NODESET);
        assertEquals(1, f.getLength(), "1 device expected");

    }
    @Test
    public void ShareUsageElement_CancelOnServerError() throws Exception {
        logger.info("Test intentionally creates errors");
        LogbackHelper.intentionalErrorConfig();
        // Arrange
        createShareUsage(
            1,
            1,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        ((TestConnector)shareUsageElement.dataUploader).setResponseCode(500);
        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        shareUsageElement.getSendDataFuture().get();

        // Assert
        assertFalse(shareUsageElement.isCanceled());
        assertEquals(0, shareUsageElement.evidenceCollection.size());
    }

    @Test
    public void ShareUsageElement_IgnoreOnEvidence() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                Collections.singletonList(
                        new AbstractMap.SimpleEntry<>("header.User-Agent", "Azure Traffic Manager Endpoint Monitor")
                ));

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put("header.User-Agent", "Azure Traffic Manager Endpoint Monitor");

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // Act
        shareUsageElement.process(data);
        // Check that the consumer task did not start.
        assertNull(shareUsageElement.getSendDataFuture());

        assertEquals(0, connector.getBaos().size());
    }

    @Test  @Disabled // we now throw exceptions for invalid data
    public void ShareUsageBuilder_IgnoreData_InvalidFilter() throws IOException {
        logger.info("The following warning are part of the test:");
        for (String config : new String[]{"user-agent=iPhone", "user-agent,iPhone", "test,iPhone,block"}) {
            ILoggerFactory internalLoggerFactory = LoggerFactory.getILoggerFactory();
            TestLoggerFactory loggerFactory = new TestLoggerFactory(internalLoggerFactory);
            TestLogger logger = new TestLogger("test", LoggerFactory.getLogger("test"));

            ShareUsageBuilder builder = new ShareUsageBuilder(loggerFactory, logger);
            ShareUsageElement element = builder
                .setSharePercentage(1)
                .setMinimumEntriesPerMessage(1)
                .setRepeatEvidenceIntervalMinutes(1)
                .setIgnoreFlowDataEvidenceFilter(config)
                .build();
            assertNotNull(element);

            assertTrue(logger.warningsLogged.size() > 0);
            assertEquals(0, logger.errorsLogged.size());
        }

    }

    @Test
    public void ShareUsageBuilder_IgnoreData_ValidFilter() throws IOException {
        for (String config : new String[]{"user-agent:iPhone", "user-agent:iPhone,host:bacon.com", "user-agent:iPhone,host:bacon.com,license:ABCDEF"}) {
            ILoggerFactory internalLoggerFactory = LoggerFactory.getILoggerFactory();
            TestLoggerFactory loggerFactory = new TestLoggerFactory(internalLoggerFactory);
            TestLogger logger = new TestLogger("test", LoggerFactory.getLogger("test"));


            ShareUsageBuilder builder = new ShareUsageBuilder(loggerFactory, logger);
            ShareUsageElement element = builder
                .setSharePercentage(1)
                .setMinimumEntriesPerMessage(1)
                .setRepeatEvidenceIntervalMinutes(1)
                .setIgnoreFlowDataEvidenceFilter(config)
                .build();
            assertNotNull(element);

            assertEquals(0, logger.warningsLogged.size());
            assertEquals(0, logger.errorsLogged.size());
        }
    }



    /**
     * Check that the usage element can handle invalid xml chars.
     */

    @Test
    public void ShareUsageElement_BadSchema() throws Exception {
        // Arrange
        createShareUsage(1, 1, 1, new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();

        // Contains XML characters and illegal character.
        // (0x0018) - Cancel control character
        evidenceData.put(EVIDENCE_HEADER_USERAGENT_KEY, "<iPhone\u0018>");

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // Act
        shareUsageElement.process(data);
        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        shareUsageElement.getSendDataFuture().get();

        ByteArrayOutputStream baos = connector.getBaos();
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        // Check that the expected values are populated.
        NodeList f = (NodeList) xpath.compile("//header[@Name='user-agent']").evaluate(doc, XPathConstants.NODESET);
        assertEquals(1, f.getLength(), "1 user-agent header expected");
        assertEquals("<iPhone\uFFFD>", f.item(0).getTextContent());
        assertEquals("true", f.item(0).getAttributes().getNamedItem("replaced").getTextContent());
        NodeList b = (NodeList) xpath.compile("//BadSchema").evaluate(doc, XPathConstants.NODESET);
        assertEquals(0, b.getLength());
    }
    
/**
     * Test that the ShareUsageElement generates a session id if one is not
     * contained in the evidence and adds it to the results.
     */

    @Test
    public void ShareUsageElement_SessionIdAndSequence_None() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // Act
        sequenceElement.process(data);
        shareUsageElement.process(data);

        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        shareUsageElement.getSendDataFuture().get();

        ByteArrayOutputStream baos = connector.getBaos();
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        NodeList f = (NodeList) xpath.compile("//SessionId").evaluate(doc, XPathConstants.NODESET);
        assertEquals(1, f.getLength(), "Expecting 1 session ID");
        NodeList ff = (NodeList) xpath.compile("//Sequence").evaluate(doc, XPathConstants.NODESET);
        assertEquals("1", ff.item(0).getTextContent(), "Expecting Sequence ID 1");
    }
    
/**
     * Test that if a session id and sequence exists in the evidence the 
     * ShareUsageElement persists the session id and increments the 
     * sequence.
     */

    @Test
    public void ShareUsageElement_SessionIdAndSequence_Existing() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(Constants.EVIDENCE_SESSIONID, "abcdefg-hijklmn-opqrst-uvwxyz");
        evidenceData.put(Constants.EVIDENCE_SEQUENCE, 2);

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // Act
        sequenceElement.process(data);
        shareUsageElement.process(data);

        // Wait for the consumer task to finish.
        assertNotNull(shareUsageElement.getSendDataFuture());
        shareUsageElement.getSendDataFuture().get();

        ByteArrayOutputStream baos = connector.getBaos();
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        NodeList f = (NodeList) xpath.compile("//SessionId").evaluate(doc, XPathConstants.NODESET);
        assertEquals(1, f.getLength(), "Expecting 1 session ID");
        assertEquals("abcdefg-hijklmn-opqrst-uvwxyz", f.item(0).getTextContent(), "Expecting session ID to be ...");
        NodeList ff = (NodeList) xpath.compile("//Sequence").evaluate(doc, XPathConstants.NODESET);
        assertEquals("3", ff.item(0).getTextContent(), "Expecting Sequence ID 1");
    }

    /**
     * test for earlier bug where 100 out of 101 usages were shared (with min buffer of 50)
     * plus make sure to send buffer on close down
     */
    @Test
    public void CheckForOffByOneError() throws Exception {
        createShareUsage(
                1,
                10,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // collect multiple <Devices> documents
        connector.getBaos().write("<allDevices>".getBytes(StandardCharsets.UTF_8));
        // Act
        int count = 45;
        for (int i=0; i < count; i++) {
            shareUsageElement.process(data);
        }
        // Wait for the consumer task to finish.
        shareUsageElement.close();

        ByteArrayOutputStream baos = connector.getBaos();
        baos.write("</allDevices>".getBytes(StandardCharsets.UTF_8));
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        NodeList f = (NodeList) xpath.compile("//Device").evaluate(doc, XPathConstants.NODESET);
        assertEquals(count, f.getLength(), "Expecting devices");

    }
    @Test
    public void CheckForDrainQueueEvenIfError() throws Exception {
        logger.info("Test intentionally creates errors");
        LogbackHelper.intentionalErrorConfig();
        createShareUsage(
                1,
                10,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        // each send fails
        ((TestConnector)shareUsageElement.dataUploader).setResponseCode(500);

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // collect multiple <Devices> documents
        connector.getBaos().write("<allDevices>".getBytes(StandardCharsets.UTF_8));
        // Act
        int count = 45;
        for (int i=0; i < count; i++) {
            shareUsageElement.process(data);
        }
        // Wait for the consumer task to finish.
        shareUsageElement.close();

        ByteArrayOutputStream baos = connector.getBaos();
        baos.write("</allDevices>".getBytes(StandardCharsets.UTF_8));
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        NodeList f = (NodeList) xpath.compile("//Device").evaluate(doc, XPathConstants.NODESET);
        // this shows that sending is ignoring the failures
        assertEquals(count, f.getLength(), "Expecting devices");

    }
    @Test
    public void CheckForContinueAndResumeWhenQueueFull() throws Exception {
        logger.info("Test intentionally creates WARNs");
        createShareUsage(
                1,
                10,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        // add delay to the response so queue grows
        ((TestConnector)shareUsageElement.dataUploader).setResponseDelay(3000);

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        FlowData data = pipeline.createFlowData();
        data.addEvidence(evidenceData);

        // the output document will collect multiple <Devices> documents,
        // so we need to wrap them in a root element
        connector.getBaos().write("<allDevices>".getBytes(StandardCharsets.UTF_8));

        // Overwhelm the queue
        int count = 55;
        for (int i=0; i < count; i++) {
            shareUsageElement.process(data);
        }
        long amountOfLostData;
        assertTrue((amountOfLostData = shareUsageElement.lostData) > 0);
        // wait for queue to drain
        logger.info("Sleeping");
        Thread.sleep(5000);

        // send a new element which should reset the flag
        shareUsageElement.process(data);
        assertEquals(amountOfLostData, shareUsageElement.lostData);

        // overwhelm the queue again
        for (int i=0; i < count; i++) {
            shareUsageElement.process(data);
        }
        assertTrue(amountOfLostData < shareUsageElement.lostData);

        // Wait for the consumer task to finish.
        shareUsageElement.close();

        // get the data output
        ByteArrayOutputStream baos = connector.getBaos();
        baos.write("</allDevices>".getBytes(StandardCharsets.UTF_8));
        byte[] output = baos.toByteArray();

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(output));

        NodeList f = (NodeList) xpath.compile("//Device").evaluate(doc, XPathConstants.NODESET);
        // the count is what we put in minus what we have recorded as lost
        assertEquals(2 * count + 1 - shareUsageElement.lostData, f.getLength(), "Expecting devices");

    }
}

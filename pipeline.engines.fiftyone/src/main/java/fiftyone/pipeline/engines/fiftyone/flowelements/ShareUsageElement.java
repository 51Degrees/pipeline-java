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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.fiftyone.exceptions.HttpException;
import fiftyone.pipeline.engines.services.DataUploader;
import fiftyone.pipeline.engines.services.DataUploaderHttp;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.trackers.Tracker;
import org.slf4j.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.SHARE_USAGE_DEFAULT_HTTP_POST_TIMEOUT;
import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.SHARE_USAGE_MAX_EVIDENCE_LENGTH;

/**
 * Flow element that sends usage data to 51Degrees for analysis. 
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/usage-sharing-element.md">Specification</a>
 */
public class ShareUsageElement extends ShareUsageBase {
    // max time to wait to send data
    protected int httpSendTimeout = SHARE_USAGE_DEFAULT_HTTP_POST_TIMEOUT; // milliseconds
    protected Map<String, String> headers = new HashMap<>();
    protected HttpClient httpClient; // old style HttpClient
    protected DataUploader dataUploader = new DataUploaderHttp(shareUsageUrl, headers, httpSendTimeout);

    protected XMLOutputFactory xmlOutputFactory;

    /**
     * Constructor
     *
     * @param logger                        the logger to use
     * @param sharePercentage               the approximate proportion of requests to share.
     *                                      1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage      the minimum number of request entries per
     *                                      message sent to 51Degrees
     * @param maximumQueueSize              the maximum number of items to hold in the queue
     *                                      at one time. This must be larger than minimum
     *                                      entries
     * @param addTimeout                    the timeout in milliseconds to allow when attempting to
     *                                      add an item to the queue. If this timeout is exceeded
     *                                      then usage sharing will be disabled
     * @param takeTimeout                   the timeout in milliseconds to allow when attempting
     *                                      to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession                  set if the tracker should consider sessions in share
     *                                      usage
     * @param shareUsageUrl                 the URL to send data to
     * @param blockedHttpHeaders            a list of the names of the HTTP headers that
     *                                      share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter      the filter used to determine if an item
     *                                      of evidence should be ignored or not
     * @deprecated HttpClient no longer used, use other constructor
     */
    @Deprecated
    ShareUsageElement(
            Logger logger,
            HttpClient httpClient,
            double sharePercentage,
            int minimumEntriesPerMessage,
            int maximumQueueSize,
            int addTimeout,
            int takeTimeout,
            int repeatEvidenceIntervalMinutes,
            boolean trackSession,
            String shareUsageUrl,
            List<String> blockedHttpHeaders,
            List<String> includedQueryStringParameters,
            List<Map.Entry<String, String>> ignoreDataEvidenceFilter) {
        this(
                logger,
                httpClient,
                sharePercentage,
                minimumEntriesPerMessage,
                maximumQueueSize,
                addTimeout,
                takeTimeout,
                repeatEvidenceIntervalMinutes,
                trackSession,
                shareUsageUrl,
                blockedHttpHeaders,
                includedQueryStringParameters,
                ignoreDataEvidenceFilter,
                Constants.DEFAULT_SESSION_COOKIE_NAME);
    }

    /**
     * Constructor
     *
     * @param logger                        the logger to use
     * @param sharePercentage               the approximate proportion of requests to share.
     *                                      1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage      the minimum number of request entries per
     *                                      message sent to 51Degrees
     * @param maximumQueueSize              the maximum number of items to hold in the queue
     *                                      at one time. This must be larger than minimum
     *                                      entries
     * @param addTimeout                    the timeout in milliseconds to allow when attempting to
     *                                      add an item to the queue. If this timeout is exceeded
     *                                      then usage sharing will be disabled
     * @param takeTimeout                   the timeout in milliseconds to allow when attempting
     *                                      to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession                  set if the tracker should consider sessions in share
     *                                      usage
     * @param shareUsageUrl                 the URL to send data to
     * @param blockedHttpHeaders            a list of the names of the HTTP headers that
     *                                      share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter      the filter used to determine if an item
     *                                      of evidence should be ignored or not
     * @param sessionCookieName             the name of the cookie that contains the session id
     * @deprecated HttpClient no longer used, use other constructor
     */
    @Deprecated
    ShareUsageElement(
            Logger logger,
            HttpClient httpClient,
            double sharePercentage,
            int minimumEntriesPerMessage,
            int maximumQueueSize,
            int addTimeout,
            int takeTimeout,
            int repeatEvidenceIntervalMinutes,
            boolean trackSession,
            String shareUsageUrl,
            List<String> blockedHttpHeaders,
            List<String> includedQueryStringParameters,
            List<Map.Entry<String, String>> ignoreDataEvidenceFilter,
            String sessionCookieName) {
        this(
                logger,
                httpClient,
                sharePercentage,
                minimumEntriesPerMessage,
                maximumQueueSize,
                addTimeout,
                takeTimeout,
                repeatEvidenceIntervalMinutes,
                trackSession,
                shareUsageUrl,
                blockedHttpHeaders,
                includedQueryStringParameters,
                ignoreDataEvidenceFilter,
                sessionCookieName,
                null);
    }

    /**
     * Constructor
     *
     * @param logger                        the logger to use
     * @param sharePercentage               the approximate proportion of requests to share.
     *                                      1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage      the minimum number of request entries per
     *                                      message sent to 51Degrees
     * @param maximumQueueSize              the maximum number of items to hold in the queue
     *                                      at one time. This must be larger than minimum
     *                                      entries
     * @param addTimeout                    the timeout in milliseconds to allow when attempting to
     *                                      add an item to the queue. If this timeout is exceeded
     *                                      then usage sharing will be disabled
     * @param takeTimeout                   the timeout in milliseconds to allow when attempting
     *                                      to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession                  set if the tracker should consider sessions in share
     *                                      usage
     * @param shareUsageUrl                 the URL to send data to
     * @param blockedHttpHeaders            a list of the names of the HTTP headers that
     *                                      share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter      the filter used to determine if an item
     *                                      of evidence should be ignored or not
     * @param sessionCookieName             the name of the cookie that contains the session
     *                                      id
     * @param tracker                       the {@link Tracker} to use to determine if a given
     *                                      {@link FlowData} instance should be shared or not
     * @deprecated HttpClient no longer used, use other constructor
     */
    @Deprecated
    ShareUsageElement(
            Logger logger,
            HttpClient httpClient,
            double sharePercentage,
            int minimumEntriesPerMessage,
            int maximumQueueSize,
            int addTimeout,
            int takeTimeout,
            int repeatEvidenceIntervalMinutes,
            boolean trackSession,
            String shareUsageUrl,
            List<String> blockedHttpHeaders,
            List<String> includedQueryStringParameters,
            List<Map.Entry<String, String>> ignoreDataEvidenceFilter,
            String sessionCookieName,
            Tracker tracker) {
        this(
                logger,
                sharePercentage,
                minimumEntriesPerMessage,
                maximumQueueSize,
                addTimeout,
                takeTimeout,
                repeatEvidenceIntervalMinutes,
                trackSession,
                shareUsageUrl,
                blockedHttpHeaders,
                includedQueryStringParameters,
                ignoreDataEvidenceFilter,
                sessionCookieName,
                tracker);
        this.httpClient = httpClient;
    }

    /**
     * Constructor
     *
     * @param logger                        the logger to use
     * @param sharePercentage               the approximate proportion of requests to share.
     *                                      1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage      the minimum number of request entries per
     *                                      message sent to 51Degrees
     * @param maximumQueueSize              the maximum number of items to hold in the queue
     *                                      at one time. This must be larger than minimum
     *                                      entries
     * @param addTimeout                    the timeout in milliseconds to allow when attempting to
     *                                      add an item to the queue. If this timeout is exceeded
     *                                      then usage sharing will be disabled
     * @param takeTimeout                   the timeout in milliseconds to allow when attempting
     *                                      to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession                  set if the tracker should consider sessions in share
     *                                      usage
     * @param shareUsageUrl                 the URL to send data to
     * @param blockedHttpHeaders            a list of the names of the HTTP headers that
     *                                      share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter      the filter used to determine if an item
     *                                      of evidence should be ignored or not
     * @param sessionCookieName             the name of the cookie that contains the session
     *                                      id
     * @param tracker                       the {@link Tracker} to use to determine if a given
     *                                      {@link FlowData} instance should be shared or not
     */
    ShareUsageElement(
            Logger logger,
            double sharePercentage,
            int minimumEntriesPerMessage,
            int maximumQueueSize,
            int addTimeout,
            int takeTimeout,
            int repeatEvidenceIntervalMinutes,
            boolean trackSession,
            String shareUsageUrl,
            List<String> blockedHttpHeaders,
            List<String> includedQueryStringParameters,
            List<Map.Entry<String, String>> ignoreDataEvidenceFilter,
            String sessionCookieName,
            Tracker tracker) {
        super(
                logger,
                sharePercentage,
                minimumEntriesPerMessage,
                maximumQueueSize,
                addTimeout,
                takeTimeout,
                repeatEvidenceIntervalMinutes,
                trackSession,
                shareUsageUrl,
                blockedHttpHeaders,
                includedQueryStringParameters,
                ignoreDataEvidenceFilter,
                sessionCookieName,
                tracker);

        headers.put("Content-Type", "text/xml; charset=utf-8");
        // assume default GZIP connection
        headers.put("Content-Encoding", "gzip");

        xmlOutputFactory = XMLOutputFactory.newInstance();
        if (xmlOutputFactory.isPropertySupported("escapeCharacters")) {
            xmlOutputFactory.setProperty("escapeCharacters", true);
        }
    }

    /**
     * Take data from the queue until there are fewer entries than the minimum batch size
     */
    @Override
    protected void sendUsageData() {
        logger.debug(threadMarker, "Send Usage Data");

        List<ShareUsageData> allData = new ArrayList<>();
        // drain queue while minimum entries in it
        do {
            logger.debug(threadMarker, "Queue size is {}", evidenceCollection.size());
            try {
                allData.clear();
                // for historical reasons max entries set to 2*
                while (allData.size() < minEntriesPerMessage * 2) {
                    ShareUsageData currentData = evidenceCollection.poll(takeTimeout, TimeUnit.MILLISECONDS);
                    // no data available
                    if (currentData == null) {
                        break;
                    }
                    allData.add(currentData);
                }
                sendAsXML(allData);
            } catch (InterruptedException e) {
                logger.error("Interrupted exception caught while waiting on share usage queue");
            } catch (Exception e) {
                logger.error("Exception sending usage data", e);
            }
          // send in minEntries batches unless shutting down in which case drain the queue
        } while (evidenceCollection.size() >= minEntriesPerMessage ||
                executor.isShutdown() && evidenceCollection.size() > 0);
        logger.debug(threadMarker, "Stopping sending. Queue size is {}", evidenceCollection.size());
    }

    protected void sendAsXML(List<ShareUsageData> allData) throws Exception {
        logger.debug(threadMarker, "send {} usage elements", allData.size());

        if (Objects.nonNull(httpClient)) {
            legacySendAsXML(allData);
            return;
        }
        // get an output stream to send the usage
        try (OutputStream os = dataUploader.getOutputStream()) {
            // encode to the stream
            streamXml(allData, os);
        }

        // check for completion
        int code = dataUploader.getResponseCode();
        if (code != 200) {
            throw new Exception(String.format("Share Usage response code was %d ", code));
        }
        logger.debug(threadMarker, "Send elements done");
    }

    /**
     * Serialise the SharedUsageData to an outputStream
     * @throws XMLStreamException on error
     */
    private void streamXml(List<ShareUsageData> allData, OutputStream os) throws XMLStreamException {
        XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(os, "UTF-8");
        writer.writeStartElement("Devices");
        writer.writeAttribute("version", "1.1");
        for (ShareUsageData data : allData) {
            writeXmlData(writer, data);
        }
        writer.writeEndElement();
        writer.close();
    }

    /**
     * Output the data as XML
     *
     * @param writer an XMLWriter
     * @param data   the data to write
     */
    protected void writeXmlData(XMLStreamWriter writer, ShareUsageData data) throws XMLStreamException {
        writer.writeStartElement("Device");
        // --- write invariant data
        // The version number of the Pipeline API
        writeXmlElement(writer, "Version", coreVersion);
        // Write Pipeline information
        // The product name
        writeXmlElement(writer, "Product", "Pipeline");
        // The flow elements in the current pipeline
        for (String element : getFlowElements()) {
            writeXmlElement(writer, "FlowElement", element);
        }
        writeXmlElement(writer, "Language", "java");
        // The software language version
        writeXmlElement(writer, "LanguageVersion", languageVersion);
        // The IP of this server
        writeXmlElement(writer, "ServerIP", getHostAddress());
        // The OS name and version
        writeXmlElement(writer, "Platform", osVersion);

        // --- write variable data
        // The SessionID used to track a series of requests
        writeXmlElement(writer, "SessionId", data.sessionId);
        // The sequence number of the request in a series of requests.
        writeXmlElement(writer, "Sequence", String.valueOf(data.sequence));
        // The UTC date/time this entry was written
        writeXmlElement(writer, "DateSent", DATE_FMT.format(new Date()));
        // The client IP of the request
        writeXmlElement(writer, "ClientIP", data.clientIP);

        // Write all other evidence data that has been included.
        for (Map.Entry<String, Map<String, String>> category : data.evidenceData.entrySet()) {
            for (Map.Entry<String, String> entry : category.getValue().entrySet()) {
                ReplacedString replacedString = new ReplacedString(entry.getValue());
                if (category.getKey().length() > 0) {
                    writer.writeStartElement(category.getKey());
                    writer.writeAttribute("Name", entry.getKey());
                } else {
                    writer.writeStartElement(entry.getKey());
                }
                if (replacedString.isReplaced()) {
                    writer.writeAttribute("replaced", "true");
                }
                if (replacedString.isTruncated()) {
                    writer.writeAttribute("truncated", "true");
                }
                writer.writeCharacters(replacedString.toString());
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    protected void writeXmlElement(XMLStreamWriter writer,
                                   String elementName,
                                   String elementContent) throws XMLStreamException {
        if(elementContent != null)
        {
            writer.writeStartElement(elementName);
            writer.writeCharacters(elementContent);
            writer.writeEndElement();
        }
    }

    /**
     * replace characters that cause problems in XML with the "Replacement character"
     */
    public static class ReplacedString {
        // a set of valid XML character values (ignoring valid controls x09, x0a, x0d, x85)
        static final Set<Integer> VALID_XML_CHARS =
                Stream.of(  IntStream.range(0x20,0x7F),
                            IntStream.range(0xA0, 0x100))
                .flatMapToInt(i -> i)
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
        // an array describing whether a character value is valid
        static final Boolean[] IS_VALID_XML_CHAR = IntStream
                .range(0, 0x100)
                .mapToObj(VALID_XML_CHARS::contains)
                .toArray(Boolean[]::new);

        // maximum string length
        static final int MAX_LENGTH = SHARE_USAGE_MAX_EVIDENCE_LENGTH;

        private boolean replaced;
        private boolean truncated;
        private final StringBuilder builder;

        public ReplacedString(String text) {
            if (Objects.isNull(text) || text.length() == 0) {
                builder = new StringBuilder(0);
                // Using 'return' statement in a constructor throws compilation
                // error in Jdk 8.
                // return;
            } else {
                builder = new StringBuilder(Math.min(text.length(), MAX_LENGTH));
                truncated = text.length() > MAX_LENGTH;
                text.chars()
                        .limit(MAX_LENGTH)
                        .forEach(c -> {
                            if (c < IS_VALID_XML_CHAR.length && IS_VALID_XML_CHAR[c]) {
                                builder.append((char) c);
                            } else {
                                builder.append((char) 0xFFFD);
                                replaced = true;
                            }
                        });
            }
        }

        public boolean isReplaced() {
            return replaced;
        }

        public boolean isTruncated() {
            return truncated;
        }

        public String toString() {
            return builder.toString();
        }
    }

    /**
     * If there is an HttpClient we will use this as a legacy
     * @param allData data to send
     */
    protected void legacySendAsXML(List<ShareUsageData> allData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream os = new GZIPOutputStream(baos)) {
            // encode to the stream
            streamXml(allData, os);
        }

        HttpURLConnection connection = httpClient.connect(new URL(shareUsageUrl.trim()));
        String response = httpClient.postData(connection, headers, baos.toByteArray());
        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage() + "data: '" + response + "'";
        if (responseCode != 200) {
            throw new HttpException(responseCode, responseMessage);
        }
    }
}
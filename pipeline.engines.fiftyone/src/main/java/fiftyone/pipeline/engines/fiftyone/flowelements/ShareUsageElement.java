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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.fiftyone.exceptions.HttpException;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.trackers.Tracker;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Flow element that sends usage data to 51Degrees for analysis. The type and
 * quantity of data being sent can be customised using the options on the
 * constructor.
 * By default, data is queued until there are at least 50 items in memory. It is
 * then serialised to an XML file and sent to the specified URL.
 */
public class ShareUsageElement extends ShareUsageBase {

    private final HttpClient httpClient;

    private final Map<String, String> headers;

    /**
     * Constructor
     * @param logger the logger to use
     * @param sharePercentage the approximate proportion of requests to share.
     *                        1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage the minimum number of request entries per
     *                                 message sent to 51Degrees
     * @param maximumQueueSize the maximum number of items to hold in the queue
     *                         at one time. This must be larger than minimum
     *                         entries
     * @param addTimeout the timeout in milliseconds to allow when attempting to
     *                   add an item to the queue. If this timeout is exceeded
     *                   then usage sharing will be disabled
     * @param takeTimeout the timeout in milliseconds to allow when attempting
     *                    to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession set if the tracker should consider sessions in share
     *                     usage
     * @param shareUsageUrl the URL to send data to
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter the filter used to determine if an item
     *                                 of evidence should be ignored or not
     */
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
        List<Map.Entry<String, String>> ignoreDataEvidenceFilter) throws IOException {
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
     * @param logger the logger to use
     * @param sharePercentage the approximate proportion of requests to share.
     *                        1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage the minimum number of request entries per
     *                                 message sent to 51Degrees
     * @param maximumQueueSize the maximum number of items to hold in the queue
     *                         at one time. This must be larger than minimum
     *                         entries
     * @param addTimeout the timeout in milliseconds to allow when attempting to
     *                   add an item to the queue. If this timeout is exceeded
     *                   then usage sharing will be disabled
     * @param takeTimeout the timeout in milliseconds to allow when attempting
     *                    to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession set if the tracker should consider sessions in share
     *                     usage
     * @param shareUsageUrl the URL to send data to
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter the filter used to determine if an item
     *                                 of evidence should be ignored or not
     * @param sessionCookieName the name of the cookie that contains the session
     *                          id
     */
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
     * @param logger the logger to use
     * @param sharePercentage the approximate proportion of requests to share.
     *                        1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage the minimum number of request entries per
     *                                 message sent to 51Degrees
     * @param maximumQueueSize the maximum number of items to hold in the queue
     *                         at one time. This must be larger than minimum
     *                         entries
     * @param addTimeout the timeout in milliseconds to allow when attempting to
     *                   add an item to the queue. If this timeout is exceeded
     *                   then usage sharing will be disabled
     * @param takeTimeout the timeout in milliseconds to allow when attempting
     *                    to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession set if the tracker should consider sessions in share
     *                     usage
     * @param shareUsageUrl the URL to send data to
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter the filter used to determine if an item
     *                                 of evidence should be ignored or not
     * @param sessionCookieName the name of the cookie that contains the session
     *                          id
     * @param tracker the {@link Tracker} to use to determine if a given
     *                {@link FlowData} instance should be shared or not
     */
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
        this.httpClient = httpClient;
        headers = new HashMap<>();
        headers.put("Content-Type", "text/xml; charset=utf-8");
        headers.put("Content-Encoding", "gzip");

    }

    @Override
    protected void buildAndSendXml() throws HttpException {
        List<ShareUsageData> allData = new ArrayList<>();

        try {
            ShareUsageData currentData = evidenceCollection.poll(
                takeTimeout,
                TimeUnit.MILLISECONDS);
            while (currentData != null &&
                allData.size() < minEntriesPerMessage * 2) {
                allData.add(currentData);
                currentData = evidenceCollection.poll(
                    takeTimeout,
                    TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            // do nothing.
        }

        // Create the zip stream and XML writer.
        XmlBuilder xmlBuilder = new XmlBuilder();
        xmlBuilder.writeStartElement("Devices");
        for (ShareUsageData data : allData) {
            buildData(xmlBuilder, data);
        }
        xmlBuilder.writeEndElement("Devices");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        boolean zipFailed = false;
        try (GZIPOutputStream zipStream = new GZIPOutputStream(os)) {
            zipStream.write(xmlBuilder.toString().getBytes("UTF-8"));
            zipStream.flush();
        } catch (IOException e) {
            zipFailed = true;
            logger.error("An error occurred writing usage data to the stream. " +
                "Usage sharing will be canceled.", e);
            cancel();
        }
        if (zipFailed == false) {

            HttpURLConnection connection;
            try {
                connection = httpClient.connect(new URL(shareUsageUrl.trim()));
            } catch (MalformedURLException e) {
                throw new HttpException("The URL '" + shareUsageUrl +
                    "' was not properly formed.", e);
            } catch (IOException e) {
                throw new HttpException("There was an error connecting to " +
                    "the URL '" + shareUsageUrl + "'.", e);
            }

            int responseCode;
            String responseMessage;
            try {
                String response = httpClient.postData(
                    connection,
                    headers,
                    os.toByteArray());
                responseCode = connection.getResponseCode();
                responseMessage = connection.getResponseMessage() +
                    "data: '" + response + "'";
            } catch (IOException e) {
                throw new HttpException("There was an error getting a response" +
                    "from the URL '" + shareUsageUrl + "'.", e);
            }
            if (responseCode != 200) {
                throw new HttpException(
                    responseCode,
                    responseMessage);
            }
        }
    }
}

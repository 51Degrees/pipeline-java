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

import fiftyone.caching.LruPutCache;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.fiftyone.data.EvidenceKeyFilterShareUsage;
import fiftyone.pipeline.engines.fiftyone.exceptions.HttpException;
import fiftyone.pipeline.engines.fiftyone.trackers.ShareUsageTracker;
import fiftyone.pipeline.engines.trackers.Tracker;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR;
import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.LOST_DATA_RESET_DEFAULT;


/**
 * Abstract base class for ShareUsage elements. Contains common functionality
 * such as filtering the evidence and building the XML records.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/usage-sharing-element.md">Specification</a>
 */
public abstract class ShareUsageBase
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    /**
     * IP Addresses of local host device.
     */
    private static final InetAddress[] localHosts;
    protected static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    static {
        InetAddress[] localHosts1;
        try {
            localHosts1 = new InetAddress[]{
                InetAddress.getByName("127.0.0.1"),
                InetAddress.getByName("::1")
            };
        } catch (UnknownHostException e) {
            localHosts1 = new InetAddress[0];
        }
        localHosts = localHosts1;
    }

    static final Marker threadMarker = MarkerFactory.getMarker("Sending");

    /**
     * Executor service used to start data sending threads.
     */
    protected final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Queue used to store entries in memory prior to them being sent to
     * 51Degrees.
     */
    protected BlockingQueue<ShareUsageData> evidenceCollection;

    /**
     * Timeout to use when taking from the queue.
     */
    protected int takeTimeout;

    /**
     * The minimum number of request entries per message sent to 51Degrees.
     */
    protected final int minEntriesPerMessage;

    /**
     * The URL to send data to.
     */
    protected final String shareUsageUrl;

    /**
     * Evidence key filter including the session id.
     */
    private final EvidenceKeyFilter evidenceKeyFilter;

    /**
     * Evidence key filter excluding the session id
     */
    private final EvidenceKeyFilter evidenceKeyFilterExclSession;

    /**
     * The filter used to determine if an item of evidence should be ignored or
     * not.
     */
    private final List<Map.Entry<String, String>> ignoreDataEvidenceFilter;

    /**
     * The host address of the current machine.
     */
    private String hostAddress;

    /**
     * Empty list. This engine returns no properties.
     */
    private List<ElementPropertyMetaData> properties;

    /**
     * Future for the last thread attempting to send data (null if none yet),
     * thread may or may not have finished
     */
    private volatile Future<?> sendDataFuture = null;

    /**
     * Lock used to start a new thread for sending data
     */
    private final Object lock = new Object();

    /**
     * Timeout to use when adding to the queue.
     */
    private final int addTimeout;

    /**
     * Random number generator used when sharing only a percentage of data i.e.
     * when {@link #sharePercentage} < 1.
     */
    private final Random random = new Random();

    /**
     * The tracker to use to determine if a {@link FlowData} instance should be
     * shared or not.
     */
    private Tracker tracker;

    /**
     * The interval is a timespan which is used to determine if a piece of
     * repeated evidence should be considered new evidence to share. If the
     * evidence from a request matches that in the tracker but this interval has
     * elapsed then the tracker will track it as new evidence.
     */
    private final long intervalMillis;

    /**
     * The approximate proportion of requests to be shared.
     * 1 = 100%, 0.5 = 50%, etc.
     */
    private double sharePercentage = 1;

    /**
     * List of {@link FlowElement} in the pipeline. This is populated when the
     * {@link #getFlowElements()} is called.
     */
    private List<String> flowElements = null;

    /**
     * Version of the OS the pipeline is being run on, as reported by
     * System.getProperty("os.name").
     */
    protected final String osVersion;

    /**
     * The Java version the pipeline is being run on, as reported by
     * System.getProperty("java.version")
     */
    protected final String languageVersion;

    /**
     * The version of the pipeline package.
     */
    protected final String coreVersion;

    /**
     * The version of this engine package.
     */
    private String enginesVersion;

    /**
     * True if usage sharing has been canceled.
     */
    private boolean canceled = false;

    /**
     * Keep a count of data lost
     */
    protected long lostData;

    /**
     * lostData is rest when it exceeds this value
     */
    protected long lostDataReset = LOST_DATA_RESET_DEFAULT;

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
    protected ShareUsageBase(
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
        List<Map.Entry<String, String>> ignoreDataEvidenceFilter) {
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
            fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME);
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
    protected ShareUsageBase(
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
        String sessionCookieName) {
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
    protected ShareUsageBase(
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
        super(logger, null);

        if (minimumEntriesPerMessage > maximumQueueSize) {
            throw new IllegalArgumentException(
                "The minimum entries per message cannot be larger than " +
                    "the maximum size of the queue.");
        }

        // Make sure the cookie headers are ignored.
        if (!blockedHttpHeaders.contains(Constants.EVIDENCE_HTTPHEADER_COOKIE_SUFFIX)) {
            blockedHttpHeaders.add(Constants.EVIDENCE_HTTPHEADER_COOKIE_SUFFIX);
        }

        evidenceCollection = new LinkedBlockingQueue<>(maximumQueueSize);

        this.addTimeout = addTimeout;
        this.takeTimeout = takeTimeout;
        this.sharePercentage = sharePercentage;
        this.minEntriesPerMessage = minimumEntriesPerMessage;
        this.intervalMillis = (long) repeatEvidenceIntervalMinutes * 60 * 1000;
        this.shareUsageUrl = shareUsageUrl;

        // Some data is going to stay the same on all requests so we can
        // gather that now.
        languageVersion = System.getProperty("java.version");
        osVersion = System.getProperty("os.name");

        setEnginesVersion(getClass().getPackage().getImplementationVersion());
        coreVersion = Pipeline.class.getPackage().getImplementationVersion();

        includedQueryStringParameters.add(Constants.EVIDENCE_SESSIONID_SUFFIX);
        includedQueryStringParameters.add(Constants.EVIDENCE_SEQUENCE_SUFIX);

        evidenceKeyFilter = new EvidenceKeyFilterShareUsage(
            blockedHttpHeaders, includedQueryStringParameters, true, sessionCookieName);
        evidenceKeyFilterExclSession = new EvidenceKeyFilterShareUsage(
            blockedHttpHeaders, includedQueryStringParameters, false, sessionCookieName);

        this.ignoreDataEvidenceFilter = ignoreDataEvidenceFilter;

        this.tracker = tracker;
        // If no tracker was supplied then create the default one.
        if (tracker == null) {
            CacheConfiguration cacheConfiguration = new CacheConfiguration(
                new LruPutCache.Builder(),
                1000);
            this.tracker = new ShareUsageTracker(
                cacheConfiguration,
                    intervalMillis,
                new EvidenceKeyFilterShareUsage(
                    blockedHttpHeaders, includedQueryStringParameters, trackSession, sessionCookieName));
        }

        properties = new ArrayList<>();
    }

    /**
     * Return a list of {@link FlowElement} in the pipeline.
     * If the list is null then populate from the pipeline.
     * If there are multiple or no pipelines then log an error.
     * @return list of flow elements
     */
    @SuppressWarnings("rawtypes")
    protected List<String> getFlowElements() {
        if (flowElements == null) {
            Pipeline pipeline;
            if (getPipelines().size() == 1) {
                pipeline = getPipelines().get(0);
                List<String> list = new ArrayList<>();
                for ( FlowElement element : pipeline.getFlowElements()) {
                    list.add(element.getClass().getSimpleName());
                }
                flowElements = list;
            } else {
                // This element has somehow been registered to too
                // many (or zero) pipelines.
                // This means we cannot know the flow elements that
                // make up the pipeline so a warning is logged
                // but otherwise, the system can continue as normal.
                logger.warn("Share usage element registered " +
                    "to " + (getPipelines().size() > 0 ? "too many" : "no") +
                    " pipelines. Unable to send share usage information.");
                flowElements = new ArrayList<>();
            }
        }
        return flowElements;
    }

    @Override
    public String getElementDataKey() {
        return "shareusage";
    }

    /**
     * Get the IP address of the machine that this code is running on.
     * @return machine IP
     */
    protected String getHostAddress() {
        if (hostAddress == null) {
            String address = null;
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                address = socket.getLocalAddress().getHostAddress();
            } catch (UnknownHostException e) {
                logger.debug("The host was unknown", e);
            } catch (SocketException e) {
                logger.debug("There was a socket exception", e);
            }

            hostAddress = address == null ? "" : address;
        }
        return hostAddress;
    }

    @Override
    public void addPipeline(Pipeline pipeline) {
        if (getPipelines().size() > 0) {
            throw new RuntimeException("Cannot add ShareUsageElement to " +
                "multiple pipelines.");
        }
        super.addPipeline(pipeline);
    }

    @Override
    protected void processInternal(FlowData flowData){
        boolean ignoreData = false;
        Map<String, Object> evidence = flowData.getEvidence().asKeyMap();

        logger.debug("processInternal()");

        if (ignoreDataEvidenceFilter != null) {
            for (Map.Entry<String, String> entry : ignoreDataEvidenceFilter) {
                String key = entry.getKey();
                if (evidence.containsKey(key)) {
                    if (evidence.get(key).toString().equals(entry.getValue())) {
                        ignoreData = true;
                        break;
                    }
                }
            }
        }

        if (isCanceled() == false && ignoreData == false) {
            processData(flowData);
        }
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceKeyFilter;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return properties;
    }

    /**
     * Indicates whether share usage has been canceled as a result of an error.
     * @return true if share usage has been canceled
     */
    boolean isCanceled() {
        return canceled;
    }

    /**
     * Cancel the sending of usage data.
     */
    protected void cancel() {
        logger.warn("UsageData sending has been cancelled");
        canceled = true;
    }

    /**
     * Returns true if there is a thread attempting to send usage data.
     * @return true if data is being sent
     */
    boolean isRunning() {
        return sendDataFuture != null &&
            sendDataFuture.isDone() == false &&
            sendDataFuture.isCancelled() == false;
    }

    /**
     * Get the future for the thread currently attempting to send usage data.
     * @return future for data being sent, or null if no future has yet been created
     */
    Future<?> getSendDataFuture() {
        return sendDataFuture;
    }

    @Override
    public DataFactory<ElementData> getDataFactory() {
        return null;
    }

    @Override
    protected void managedResourcesCleanup() {
        trySendData();
        try {
            executor.shutdown();
            if (!executor.awaitTermination(Constants.SHARE_USAGE_DEFAULT_HTTP_POST_TIMEOUT, TimeUnit.MILLISECONDS)){
                logger.warn("Could not send final data on close down");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while awaiting close down");
        }
    }

    @Override
    protected void unmanagedResourcesCleanup() {

    }

    /**
     * Return true if the address is the localhost address.
     * @param address the address to check
     * @return true if localhost
     */
    @SuppressWarnings("unused")
    private boolean isLocalHost(String address) throws UnknownHostException {
        InetAddress other = InetAddress.getByName(address);
        for (InetAddress host : localHosts) {
            if (host.equals(other)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Process the supplied request data
     * @param data the {@link FlowData} instance that provides the evidence
     */
    private void processData(FlowData data) {
        logger.debug("processData()");
        if (random.nextDouble() <= sharePercentage) {
            // Check if the tracker will allow sharing of this data
            if (tracker.track(data)) {
                // Extract the data we want from the evidence and add
                // it to the collection.
                try {
                    if (evidenceCollection.offer(
                            getDataFromEvidence(data.getEvidence()),
                            addTimeout,
                            TimeUnit.MILLISECONDS)) {
                        logger.debug("Queued {} entries", evidenceCollection.size());
                        // If the collection has enough entries then start
                        // taking data from it to be sent.
                        if (evidenceCollection.size() >= minEntriesPerMessage) {
                            trySendData();
                        }
                    } else {
                        if (++lostData >= lostDataReset) {
                            logger.warn("Could not queue data for Share Usage. This could mean that" +
                                    " the queue is too small, that the proportion of data shared is" +
                                    " too high - or that the sending process has encountered" +
                                    " problems sending.");
                            lostData = 0;
                        }
                    }
                } catch (InterruptedException e) {
                    logger.warn("interrupted Exception while enqueueing ShareUsage data.");
                }
            }
        }
    }

    /**
     * Extract the desired data from the evidence.
     * In order to avoid problems with the evidence data being disposed before
     * it is sent, the data placed into a new object rather than being a
     * reference to the existing evidence instance.
     *
     * @param evidence an {@link Evidence} instance that contains the data to be extracted
     * @return a {@link ShareUsageData} instance populated with data from the evidence
     */
    private ShareUsageData getDataFromEvidence(Evidence evidence) {
        ShareUsageData shareUsageData = new ShareUsageData();

        for (Map.Entry<String, Object> entry : evidence.asKeyMap().entrySet()) {
            // Check if we can send this piece of evidence
            if (evidenceKeyFilterExclSession.include(entry.getKey()) == false) {
                continue;
            }

            switch (entry.getKey()) {
                case fiftyone.pipeline.core.Constants.EVIDENCE_CLIENTIP_KEY:
                    // The client IP is dealt with separately for backwards
                    // compatibility purposes.
                    shareUsageData.clientIP = entry.getValue().toString();
                    break;
                case Constants.EVIDENCE_SESSIONID:
                    // The SessionID is dealt with separately.
                    shareUsageData.sessionId = entry.getValue().toString();
                    break;
                case Constants.EVIDENCE_SEQUENCE:
                    // The Sequence is dealt with separately.
                    int sequence;
                    try {
                        sequence = Integer.parseInt(entry.getValue().toString(), 10);
                        shareUsageData.sequence = sequence;
                    } catch (NumberFormatException e) {
                        logger.error("The value '{}' could not be parsed to an integer.", entry.getValue());
                    }
                    break;
                default:
                    shareUsageData.tryAddToData(entry.getKey(), entry.getValue());
            }
        }
        return shareUsageData;
    }


    /**
     * Attempt to send the data to the remote service. This only happens if
     * there is not a task already running.
     */
    protected void trySendData() {
        logger.debug("trySendData");
        if (isCanceled() == false && isRunning() == false) {
            synchronized (lock) {
                if (isRunning() == false) {
                    logger.debug(threadMarker,"starting runnable");
                    sendDataFuture = executor.submit(
                            () -> {
                                logger.debug(threadMarker,"runnable is running");
                                try {
                                    sendUsageData();
                                    logger.debug(threadMarker, "Done sending");
                                } catch (Exception e) {
                                    logger.error("Share usage encountered an error.", e);
                                }
                            }
                    );
                }
            }
        }
    }

    protected abstract void sendUsageData() throws HttpException;

    /**
     * Get engine Version No.
     * @return engine version no
     */
    public String getEnginesVersion() {
        return enginesVersion;
    }

    /**
     * Set Engine Version Number.
     * @param enginesVersion version number string of the engine
     */
    public void setEnginesVersion(String enginesVersion) {
        this.enginesVersion = enginesVersion;
    }

    /**
     * Inner class that is used to store details of data in memory
     * prior to it being sent to 51Degrees.
     */
    protected static class ShareUsageData {
        public String sessionId;
        public int sequence;
        public String clientIP;
        public final Map<String, Map<String, String>> evidenceData = new HashMap<>();

        void tryAddToData(String key, Object value) {
            // Get the category and field names from the evidence key.
            String category = "";
            String field = key;

            int firstSeparator = key.indexOf(EVIDENCE_SEPERATOR);
            if (firstSeparator > 0) {
                category = key.substring(0, firstSeparator);
                field = key.substring(firstSeparator + 1);
            }

            // Add the evidence to the dictionary.
            Map<String, String> categoryDict = evidenceData.get(category);
            if (Objects.isNull(categoryDict)) {
                categoryDict = new HashMap<>();
                evidenceData.put(category, categoryDict);
            }
            categoryDict.put(field, value.toString());
        }

    }
}

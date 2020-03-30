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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR;
import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.SHARE_USAGE_MAX_EVIDENCE_LENGTH;

public abstract class ShareUsageBase
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    /**
     * IP Addresses of local host device.
     */
    private static final InetAddress[] localHosts;
    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss");

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

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    protected BlockingQueue<ShareUsageData> evidenceCollection;
    protected int takeTimeout;
    protected int minEntriesPerMessage = 50;
    protected String shareUsageUrl = "";
    private EvidenceKeyFilter evidenceKeyFilter;
    private EvidenceKeyFilter evidenceKeyFilterExclSession;
    private List<Map.Entry<String, String>> ignoreDataEvidenceFilter;
    private String hostAddress;
    private List<ElementPropertyMetaData> properties;
    private volatile Future sendDataFuture = null;
    private volatile Object lock = new Object();
    private int addTimeout;
    private Random random = new Random();
    private Tracker tracker;
    private long interval;

    private double sharePercentage = 1;

    private List<String> flowElements = null;
    private String osVersion = "";
    private String languageVersion = "";
    private String coreVersion = "";
    private String enginesVersion = "";
    private boolean canceled = false;

    /**
     * Set to true if the evidence within a flow data contains invalid XML
     * characters such as control characters.
     */
    private boolean flagBadSchema;

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
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 20);
        this.interval = calendar.getTimeInMillis();
        this.shareUsageUrl = shareUsageUrl;

        // Some data is going to stay the same on all requests so we can
        // gather that now.
        languageVersion = System.getProperty("java.version");
        osVersion = System.getProperty("os.name");

        enginesVersion = getClass().getPackage().getImplementationVersion();
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
                interval,
                new EvidenceKeyFilterShareUsage(
                    blockedHttpHeaders, includedQueryStringParameters, trackSession, sessionCookieName));
        }

        properties = new ArrayList<>();
    }

    private List<String> getFlowElements() {
        if (flowElements == null) {
            Pipeline pipeline = null;
            if (getPipelines().size() == 1) {
                pipeline = getPipelines().get(0);
                List<String> list = new ArrayList<>();
                for (FlowElement element : pipeline.getFlowElements()) {
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

    private String getHostAddress() {
        if (hostAddress == null) {
            String address = null;
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                hostAddress = socket.getLocalAddress().getHostAddress();
            } catch (UnknownHostException e) {
            } catch (SocketException e) {
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
    protected void processInternal(FlowData flowData) throws Exception {
        boolean ignoreData = false;
        Map<String, Object> evidence = flowData.getEvidence().asKeyMap();

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

    boolean isCanceled() {
        return canceled;
    }

    protected void cancel() {
        canceled = true;
    }

    boolean isRunning() {
        return sendDataFuture != null &&
            sendDataFuture.isDone() == false &&
            sendDataFuture.isCancelled() == false;
    }

    Future getSendDataFuture() {
        return sendDataFuture;
    }

    @Override
    public DataFactory<ElementData> getDataFactory() {
        return null;
    }

    @Override
    protected void managedResourcesCleanup() {
        trySendData();
        if (isRunning()) {
            sendDataFuture.cancel(false);
        }
        executor.shutdown();
    }

    @Override
    protected void unmanagedResourcesCleanup() {

    }

    private boolean isLocalHost(String address) {
        for (InetAddress host : localHosts) {
            if (host.equals(address)) {
                return true;
            }
        }
        return false;
    }

    private void processData(FlowData data) {
        if (random.nextDouble() <= sharePercentage) {
            // Check if the tracker will allow sharing of this data
            if (tracker.track(data)) {
                // Extract the data we want from the evidence and add
                // it to the collection.
                try {
                    if (evidenceCollection.offer(
                        getDataFromEvidence(data.getEvidence()),
                        addTimeout,
                        TimeUnit.MILLISECONDS) == true) {
                        // If the collection has enough entries then start
                        // taking data from it to be sent.
                        if (evidenceCollection.size() >= minEntriesPerMessage) {
                            trySendData();
                        }
                    } else {
                        cancel();
                        logger.error("Share usage was canceled after " +
                            "failing to add data to the collection. This " +
                            "may mean that the max collection size is too " +
                            "low for the amount of traffic / min devices to " +
                            "send, or that the 'send' thread has stopped " +
                            "taking data from the collection.");

                    }
                } catch (InterruptedException e) {
                    cancel();
                    logger.error("Share usage was canceled after " +
                        "failing to add data to the collection. This " +
                        "may mean that the max collection size is too " +
                        "low for the amount of traffic / min devices to " +
                        "send, or that the 'send' thread has stopped " +
                        "taking data from the collection.");
                }
            }
        }
    }

    private ShareUsageData getDataFromEvidence(Evidence evidence) {
        ShareUsageData data = new ShareUsageData();

        Map<String, Object> evidenceMap = evidence.asKeyMap();
        for (Map.Entry<String, Object> entry : evidenceMap.entrySet()) {
            // Check if we can send this piece of evidence
            boolean addToData = evidenceKeyFilterExclSession.include(entry.getKey());

            switch (entry.getKey()) {
                case fiftyone.pipeline.core.Constants.EVIDENCE_CLIENTIP_KEY:
                    // The client IP is dealt with separately for backwards
                    // compatibility purposes.
                    data.clientIP = entry.getValue().toString();
                    break;
                case Constants.EVIDENCE_SESSIONID:
                    // The SessionID is dealt with separately.
                    data.sessionId = entry.getValue().toString();
                    break;
                case Constants.EVIDENCE_SEQUENCE:
                    // The Sequence is dealt with separately.
                    int sequence = 0;
                    try {
                        sequence = Integer.parseInt(entry.getValue().toString(), 10);
                        data.sequence = sequence;
                    } catch (NumberFormatException e) {
                        logger.error("The value '" + entry.getValue().toString() +
                            "' could not be parsed to an integer.");
                    }
                    break;
                default:
                    if (addToData) {
                        tryAddToData(entry.getKey(), entry.getValue(), data);
                    }
            }
        }
        return data;
    }

    void tryAddToData(String key, Object value, ShareUsageData data) {
        // Get the category and field names from the evidence key.
        String category = "";
        String field = key;

        int firstSeperator = key.indexOf(EVIDENCE_SEPERATOR);
        if (firstSeperator > 0) {
            category = key.substring(0, firstSeperator);
            field = key.substring(firstSeperator + 1);
        }

        // Get the evidence value.
        String evidenceValue = value.toString();
        // If the value is longer than the permitted length
        // then truncate it.
        if (evidenceValue.length() > SHARE_USAGE_MAX_EVIDENCE_LENGTH) {
            evidenceValue = "[TRUNCATED BY USAGE SHARING] " +
                evidenceValue.substring(0, SHARE_USAGE_MAX_EVIDENCE_LENGTH);
        }

        // Add the evidence to the dictionary.
        Map<String, String> categoryDict;
        if (data.evidenceData.containsKey(category)) {
            categoryDict = data.evidenceData.get(category);
        } else {
            categoryDict = new HashMap<>();
            data.evidenceData.put(category, categoryDict);
        }
        categoryDict.put(field, evidenceValue);
    }

    protected void trySendData() {
        if (isCanceled() == false &&
            isRunning() == false) {
            synchronized (lock) {
                if (isRunning() == false) {
                    sendDataFuture = executor.submit(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    buildAndSendXml();
                                } catch (Exception e) {
                                    cancel();
                                    logger.error(
                                        "Share usage was canceled due to an error.",
                                        e);

                                }
                            }
                        }
                    );
                }
            }
        }
    }

    protected abstract void buildAndSendXml() throws HttpException;

    protected void buildData(XmlBuilder builder, ShareUsageData data) {
        builder.writeStartElement("Device");

        buildDeviceData(builder, data);

        builder.writeEndElement("Device");
    }

    protected void buildDeviceData(XmlBuilder builder, ShareUsageData data) {
        flagBadSchema = false;
        // The SessionID used to track a series of requests
        builder.writeElement("SessionId", data.sessionId);
        // The sequence number of the request in a series of requests.
        builder.writeElement("Sequence", String.valueOf(data.sequence));
        // The UTC date/time this entry was written
        builder.writeElement("DateSent", DATE_FMT.format(new Date()));
        // The version number of the Pipeline API
        builder.writeElement("Version", coreVersion);
        // Write Pipeline information
        writePipelineInfo(builder);
        builder.writeElement("Language", "java");
        // The software language version
        builder.writeElement("LanguageVersion", languageVersion);
        // The client IP of the request
        builder.writeElement("ClientIP", data.clientIP);
        // The IP of this server
        builder.writeElement("ServerIP", getHostAddress());
        // The OS name and version
        builder.writeElement("Platform", osVersion);

        // Write all other evidence data that has been included.
        for (Map.Entry<String, Map<String, String>> category : data.evidenceData.entrySet()) {
            for (Map.Entry<String, String> entry : category.getValue().entrySet()) {
                if (category.getKey().length() > 0) {
                    builder.writeStartElement(category.getKey(), new AbstractMap.SimpleEntry<>("Name", encodeInvalidXMLChars(entry.getKey())));
                    builder.writeCData(encodeInvalidXMLChars(entry.getValue()));
                    builder.writeEndElement(category.getKey());
                } else {
                    builder.writeElement(
                        encodeInvalidXMLChars(entry.getKey()),
                        encodeInvalidXMLChars(entry.getValue()));
                }
            }
        }
        if (flagBadSchema) {
            builder.writeElement("BadSchema", "true");
        }
    }

    /**
     * encodes any unusual characters into their hex representation
     */
    public String encodeInvalidXMLChars(String text) {
        // Validate characters in string. If not valid check chars
        // individually and build new string with encoded chars. Set
        // flagBadSchema to add "bad schema" element into usage data.
        if (XmlBuilder.verifyXmlChars(text)) {
            return text;
        }
        else {
            flagBadSchema = true;
            StringBuilder tmp = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (XmlBuilder.isValidChar(c)) {
                    tmp.append(c);
                }
                else {
                    tmp.append(XmlBuilder.escapeUnicode(c));
                }
            }
            return tmp.toString();
        }
    }

    /**
     * Method to write details about the pipeline.
     */
    protected void writePipelineInfo(XmlBuilder builder) {
        // The product name
        builder.writeElement("Product", "Pipeline");
        // The flow elements in the current pipeline
        for (String element : getFlowElements()) {
            builder.writeElement("FlowElement", element);
        }
    }


    /**
     * Inner class that is used to store details of data in memory
     * prior to it being sent to 51Degrees.
     */
    protected class ShareUsageData {
        public String sessionId;
        public int sequence;
        public String clientIP;
        public Map<String, Map<String, String>> evidenceData =
            new HashMap<>();
    }

}

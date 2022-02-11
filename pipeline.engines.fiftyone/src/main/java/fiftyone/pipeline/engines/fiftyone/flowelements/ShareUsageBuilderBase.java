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

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * Abstract base class for ShareUsageElement builders.
 * @param <T> element type
 */
public abstract class ShareUsageBuilderBase<T extends ShareUsageBase> {

    protected final ILoggerFactory loggerFactory;

    private final Logger logger;

    protected int repeatEvidenceInterval;
    protected double sharePercentage = 1;
    protected int minimumEntriesPerMessage = 50;
    // This value must be accessed through the corresponding getter/setter.
    private int maximumQueueSize = 0;
    protected int addTimeout = Constants.SHARE_USAGE_DEFAULT_ADD_TIMEOUT;
    protected int takeTimeout = Constants.SHARE_USAGE_DEFAULT_TAKE_TIMEOUT;
    protected String shareUsageUrl = Constants.SHARE_USAGE_DEFAULT_URL;
    protected String sessionCookieName = fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
    protected List<String> blockedHttpHeaders = new ArrayList<>();
    protected final List<String> includedQueryStringParameters = new ArrayList<>();
    protected final List<Map.Entry<String, String>> ignoreDataEvidenceFilter = new ArrayList<>();
    protected boolean trackSession;

    /**
     * Constructor
     * @param loggerFactory the {@link ILoggerFactory} to use when creating
     *                      loggers for a {@link ShareUsageBuilderBase}
     */
    public ShareUsageBuilderBase(ILoggerFactory loggerFactory) {
        this(
            loggerFactory,
            loggerFactory.getLogger(ShareUsageBuilderBase.class.getName()));
    }

    /**
     * Constructor
     * @param loggerFactory the {@link ILoggerFactory} to use when creating
     *                      loggers for a {@link ShareUsageBuilderBase}
     * @param logger the {@link Logger} to use for {@link ShareUsageElement}
     */
    public ShareUsageBuilderBase(ILoggerFactory loggerFactory, Logger logger) {
        this.logger = logger;
        this.loggerFactory = loggerFactory;
    }

    /**
     * By default query string and HTTP form parameters are not shared 
     * unless prefixed with '51D_'.
     * This setting allows you to share these parameters with 51Degrees
     * if needed.
     * @param queryStringParameterNames the (case insensitive) names of 
     *                                  the query string parameters
     *                                  to include
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setIncludedQueryStringParameters(
        List<String> queryStringParameterNames) {
        includedQueryStringParameters.addAll(queryStringParameterNames);
        return this;
    }

    /**
     * By default query string and HTTP form parameters are not shared 
     * unless prefixed with '51D_'.
     * This setting allows you to share these parameters with 51Degrees
     * if needed.
     * @param queryStringParameterNames a comma separated list of 
     *                                  (case insensitive) names of the
     *                                  query string parameters to include
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setIncludedQueryStringParameters(
        String queryStringParameterNames) {
        return setIncludedQueryStringParameters(
            Arrays.asList(queryStringParameterNames.split(",")));
    }

    /**
     * By default query string and HTTP form parameters are not shared 
     * unless prefixed with '51D_'.
     * This setting allows you to share these parameters with 51Degrees
     * if needed.
     * @param queryStringParameterName the (case insensitive) name of the 
     *                                 query string parameter to include
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setIncludedQueryStringParameter(
        String queryStringParameterName) {
        includedQueryStringParameters.add(queryStringParameterName);
        return this;
    }

    /**
     * By default, all HTTP headers (excluding a few such as 'cookies')
     * are shared. Individual headers can be excluded from sharing by 
     * adding them to this list.
     * @param blockedHeaders the (case insensitive) names of the headers to block
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setBlockedHttpHeaders(
        List<String> blockedHeaders) {
        blockedHttpHeaders = blockedHeaders;
        return this;
    }

    /**
     * By default, all HTTP headers (excluding a few such as 'cookies')
     * are shared. Individual headers can be excluded from sharing by 
     * adding them to this list.
     * @param blockedHeader the (case insensitive) name of the header to block
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setBlockedHttpHeader(String blockedHeader) {
        blockedHttpHeaders.add(blockedHeader);
        return this;
    }

    /**
     * This setting can be used to stop the usage sharing element 
     * from sharing anything about specific requests.
     * For example, if you wanted to stop sharing any details from requests
     * where the user-agent header was 'ABC', you would set this
     * to "header.user-agent:ABC"
     * @param evidenceFilter Comma separated string containing entries 
     *                       in the format [evidenceKey]:[evidenceValue].
     *                       Any requests with evidence matching these 
     *                       entries will not be shared.
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setIgnoreFlowDataEvidenceFilter(
        String evidenceFilter) {
        if (evidenceFilter != null &&
            evidenceFilter.isEmpty() == false) {
            for (String entryString : evidenceFilter.split(",")) {
                if (entryString.contains(":")) {
                    Map.Entry<String, String> entry =
                        new AbstractMap.SimpleEntry<>(
                            entryString.split(":")[0],
                            entryString.split(":")[1]);
                    ignoreDataEvidenceFilter.add(entry);
                } else {
                    logger.warn("Configuration for " +
                        "'IgnoreFlowDataEvidenceFilter' is invalid, " +
                        "ignoring: " + entryString);
                }
            }
        } else {
            logger.warn("Configuration for " +
                "'IgnoreFlowDataEvidenceFilter' is invalid.");
        }
        return this;
    }

    /**
     * Set the percentage of data that the {@link ShareUsageElement} should be
     * sharing.
     * @param sharePercentage the proportion of events sent to the pipeline that
     *                        should be shared to 51Degrees. 1 = 100%,
     *                        0.5 = 50%, etc.
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setSharePercentage(double sharePercentage) {
        this.sharePercentage = sharePercentage;
        return this;
    }

    /**
     * The usage element will group data into single requests before sending it.
     * This setting controls the minimum number of entries before data is sent.
     * If you are sharing large amounts of data, increasing this value is 
     * recommended in order to reduce the overhead of sending HTTP messages.
     * For example, the 51Degrees cloud service uses a value of 2500.
     * @param minimumEntriesPerMessage the minimum number of entries to be
     *                                 aggregated by the {@link ShareUsageElement}
     *                                 before they are sent to the remote
     *                                 service
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setMinimumEntriesPerMessage(int minimumEntriesPerMessage) {
        this.minimumEntriesPerMessage = minimumEntriesPerMessage;
        return this;
    }

    /**
     * Set the maximum number of entries to be stored in the queue to be sent.
     * This must be more than the minimum entries per message.        
     * By default, the value is calculated automatically based on the 
     * MinimumEntriesPerMessage setting.
     * @param size the size to set
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setMaximumQueueSize(int size) {
        maximumQueueSize = size;
        return this;
    }

    /**
     * Get the maximum number of entries to be stored in the queue to be sent. 
     * @return the maximum queue size
     * */
    public int getMaximumQueueSize() {
        int result = maximumQueueSize;
        if(result == 0) {
            result = Constants.SHARE_USAGE_DEFAULT_MAX_QUEUE_SIZE;
            int calc = minimumEntriesPerMessage * 10;
            if(calc > result) { result = calc; }            
        }
        return result;
    }

    /**
     * Set the timeout in milliseconds to allow when attempting to add an item
     * to the queue. If this timeout is exceeded then usage sharing will be
     * disabled.
     * @param milliseconds timeout to set
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setAddTimeout(int milliseconds) {
        addTimeout = milliseconds;
        return this;
    }

    /**
     * Set the timeout in milliseconds to allow when attempting to take an item
     * from the queue in order to send to the remote service.
     * @param milliseconds timeout to set
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setTakeTimeout(int milliseconds) {
        takeTimeout = milliseconds;
        return this;
    }

    /**
     * Set the URL to use when sharing usage data.
     * @param shareUsageUrl the URL to use when sharing usage data
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setShareUsageUrl(String shareUsageUrl) {
        this.shareUsageUrl = shareUsageUrl;
        return this;
    }

    /**
     * Set the name of the cookie that contains the session id.
     * This setting has no effect if TrackSession is false.
     * @param cookieName the name of the cookie that contains the session id
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setSessionCookieName(String cookieName) {
        this.sessionCookieName = cookieName;
        return this;
    }

    /**
     * If exactly the same evidence values are seen multiple times within this
     * time limit then they will only be shared once.
     * @param interval the interval in minutes
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setRepeatEvidenceIntervalMinutes(int interval) {
        this.repeatEvidenceInterval = interval;
        return this;
    }

    /**
     * If set to true, the configured session cookie will be used to
     * identify user sessions.
     * This will help to differentiate duplicate values that should not be 
     * shared.
     * @param track boolean value sets whether the usage element should track
     *              sessions
     * @return this builder
     */
    public ShareUsageBuilderBase<T> setTrackSession(boolean track) {
        this.trackSession = track;
        return this;
    }

    /**
     * Create the {@link ShareUsageElement}.
     * @return the newly created target instance
     * @throws IOException if there was an exception creating the element
     */
    public abstract T build() throws IOException;
}

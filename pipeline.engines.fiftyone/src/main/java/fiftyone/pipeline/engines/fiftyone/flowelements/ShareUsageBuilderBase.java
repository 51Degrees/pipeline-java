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

import fiftyone.pipeline.annotations.DefaultValue;
import fiftyone.pipeline.util.Check;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Abstract base class for ShareUsageElement builders.
 * @param <T> element type
 */
@SuppressWarnings({"UnusedReturnValue", "DefaultAnnotationParam"})
public abstract class ShareUsageBuilderBase<T extends ShareUsageBase> {

    protected final ILoggerFactory loggerFactory;

    protected final Logger logger;

    protected int repeatEvidenceInterval = Constants.SHARE_USAGE_DEFAULT_REPEAT_EVIDENCE_MINUTES;
    protected double sharePercentage = Constants.SHARE_USAGE_DEFAULT_SHARE_PERCENTAGE;
    protected int minimumEntriesPerMessage = Constants.SHARE_USAGE_DEFAULT_MIN_ENTRIES_PER_MESSAGE;
    // This value must be accessed through the corresponding getter/setter.
    protected int maximumQueueSize = Constants.SHARE_USAGE_DEFAULT_MAX_QUEUE_SIZE;
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
     * By default, query string parameters are not shared.
     * <p>
     * This setter replaces the current list, if any
     * <p>
     * This setting allows you to share these parameters with 51Degrees
     * if needed.
     * @param queryStringParameterNames the (case-insensitive) names of
     *                                  the query string parameters
     *                                  to include
     * @return this builder
     */

    @DefaultValue("No sharing")
    public ShareUsageBuilderBase<T> setIncludedQueryStringParameters(
        List<String> queryStringParameterNames) {
        includedQueryStringParameters.clear();
        includedQueryStringParameters.addAll(queryStringParameterNames);
        return this;
    }

    /**
     * By default, query string parameters are not shared.
     * <p>
     * This setter replaces the current list, if any
     * <p>
     * This setting allows you to share these parameters with 51Degrees
     * if needed.
     * @param queryStringParameterNames a comma separated list of
     *                                  (case-insensitive) names of the
     *                                  query string parameters to include
     * @return this builder
     */
    @DefaultValue("No sharing")
    public ShareUsageBuilderBase<T> setIncludedQueryStringParameters(
            String queryStringParameterNames) {
        includedQueryStringParameters.clear();
        includedQueryStringParameters.addAll(
                Arrays.asList(queryStringParameterNames.split("\\s*,\\s*")));
        return this;
    }

    /**
     * By default, query string parameters are not shared.
     * <p>
     * This setting adds the name of a parameter to share with 51Degrees
     * if needed.
     * @param queryStringParameterName the (case-insensitive) name of the
     *                                 query string parameter to include
     * @return this builder
     */
    @DefaultValue("No sharing")
    public ShareUsageBuilderBase<T> setIncludedQueryStringParameter(
        String queryStringParameterName) {
        includedQueryStringParameters.add(queryStringParameterName.trim());
        return this;
    }

    /**
     * By default, all HTTP headers (excluding a few such as 'cookies', if they
     * don't start with 51D_) are shared. Individual headers can be excluded from sharing by
     * adding them to this list.
     * <p>
     * This setter replaces the current list
     * @param blockedHeaders the (case-insensitive) names of the headers to block
     * @return this builder
     */
    @DefaultValue("All HTTP Headers are shared except cookies that do not start with 51D_")
    public ShareUsageBuilderBase<T> setBlockedHttpHeaders(List<String> blockedHeaders) {
        blockedHttpHeaders = blockedHeaders;
        return this;
    }

    /**
     * By default, all HTTP headers (excluding a few such as 'cookies', if they
     * don't start with 51D_) are shared. Individual headers can be excluded from sharing by
     * adding them to this list.
     * <p>
     * This setter adds to the blocked headers
     * @param blockedHeaders a comma separated list of
     *                                  (case-insensitive) names of the
     *                                  headers to include
     * @return this builder
     */
    @DefaultValue("All HTTP Headers are shared except cookies that do not start with 51D_")
    public ShareUsageBuilderBase<T> setBlockedHttpHeaders(String blockedHeaders) {
        blockedHttpHeaders.addAll(Arrays.asList(blockedHeaders.split("\\s*,\\s*")));
        return this;
    }

    /**
     * By default, all HTTP headers (excluding a few such as 'cookies', if they
     * don't start with 51D_) are shared. Individual headers can be excluded from sharing by
     * adding them to this list.
     * <p>
     * This setter allows you to add the name of a header to block.
     * @param blockedHeader the (case-insensitive) name of the header to block
     * @return this builder
     */
    @DefaultValue("All HTTP Headers are shared except cookies that do not start with 51D_")
    public ShareUsageBuilderBase<T> setBlockedHttpHeader(String blockedHeader) {
        blockedHttpHeaders.add(blockedHeader.trim());
        return this;
    }

    /**
     * This setter can be used to stop the usage sharing element
     * from sharing anything about specific requests. By default, no values
     * are suppressed.
     * <p>
     * This setter adds to the filter
     * <p>
     * For example, if you wanted to stop sharing any details from requests
     * where the user-agent header was 'ABC', you would set this
     * to "header.user-agent:ABC"
     * @param evidenceFilter Comma separated string containing entries 
     *                       in the format [evidenceKey]:[evidenceValue].
     *                       Any requests with evidence matching these 
     *                       entries will not be shared.
     * @return this builder
     */
    @DefaultValue("All values are shared")
    public ShareUsageBuilderBase<T> setIgnoreFlowDataEvidenceFilter(String evidenceFilter) {
        if (Check.isNullOrBlank(evidenceFilter)) {
            throw new IllegalArgumentException("Evidence filter must be non-null");
        }
        for (String entryString : evidenceFilter.split(",")) {
            String[] split = entryString.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Evidence filter must be of the form " +
                        "key:value[,key:value] but was \""+evidenceFilter+"\"");
            }
            ignoreDataEvidenceFilter.add(
                    new AbstractMap.SimpleEntry<>(split[0].trim(),split[1].trim()));
        }
        return this;
    }

    /**
     * Set the percentage of data that the {@link ShareUsageElement} should be
     * sharing.
     * <p>
     * Default is 100% represented as 1.0.
     * @param sharePercentage the proportion of events sent to the pipeline that
     *                        should be shared to 51Degrees. 1 = 100%,
     *                        0.5 = 50%, etc.
     * @return this builder
     */
    @DefaultValue(doubleValue = Constants.SHARE_USAGE_DEFAULT_SHARE_PERCENTAGE)
    public ShareUsageBuilderBase<T> setSharePercentage(double sharePercentage) {
        if (sharePercentage < 0 || sharePercentage > 1.0) {
            throw new IllegalArgumentException("Share percentage must be between 0 and 1 (" +
                    sharePercentage + ")");
        }
        this.sharePercentage = sharePercentage;
        return this;
    }

    /**
     * The usage element will group data into single requests before sending it.
     * This setting controls the minimum number of entries before data is sent.
     * If you are sharing large amounts of data, increasing this value is 
     * recommended in order to reduce the overhead of sending HTTP messages.
     * <p>
     * The default value is 50.
     * @param minimumEntriesPerMessage the minimum number of entries to be
     *                                 aggregated by the {@link ShareUsageElement}
     *                                 before they are sent to the remote
     *                                 service
     * @return this builder
     */
    @DefaultValue(intValue=Constants.SHARE_USAGE_DEFAULT_MIN_ENTRIES_PER_MESSAGE)
    public ShareUsageBuilderBase<T> setMinimumEntriesPerMessage(int minimumEntriesPerMessage) {
        if (minimumEntriesPerMessage <= 0) {
            throw new IllegalArgumentException("Minimum entries per message must be greater than 0");
        }
        this.minimumEntriesPerMessage = minimumEntriesPerMessage;
        return this;
    }

    /**
     * Set the maximum number of entries to be stored in the queue to be sent.
     * This must be more than the minimum entries per message. If the queue reaches this size and
     * a new item cannot be enqueued within the add timeout the item will be dropped.
     * <p>
     * By default, the value is 20* the default minimum entries per message, i.e. 1000 entries.
     * @param size the size to set
     * @return this builder
     */
    @DefaultValue(intValue = Constants.SHARE_USAGE_DEFAULT_MAX_QUEUE_SIZE)
    public ShareUsageBuilderBase<T> setMaximumQueueSize(int size) {
        if (size <= minimumEntriesPerMessage) {
            throw new IllegalArgumentException("Maximum queue size must be greater than " +
                    "the minimum entries per message, trying to set " + size +
                    " but minimum is " + minimumEntriesPerMessage);
        }
        maximumQueueSize = size;
        return this;
    }

    /**
     * Get the maximum number of entries to be stored in the queue to be sent. 
     * @return the maximum queue size
     * */
    public int getMaximumQueueSize() {
        return maximumQueueSize;
    }

    /**
     * Set the timeout in milliseconds to allow when attempting to add an item
     * to the queue.
     * @deprecated Use {@link #setAddTimeoutMillis}
     * @param milliseconds timeout to set
     * @return this builder
     */
    @Deprecated
    public ShareUsageBuilderBase<T> setAddTimeout(int milliseconds) {
        addTimeout = milliseconds;
        return this;
    }

    /**
     * Set the timeout in milliseconds to allow when attempting to add an item
     * to the queue.
     * <p>
     * Default is 0. i.e. if the item cannot be added then it is discarded.
     * @param milliseconds timeout to set
     * @return this builder
     */
    @DefaultValue(intValue = Constants.SHARE_USAGE_DEFAULT_ADD_TIMEOUT)
    public ShareUsageBuilderBase<T> setAddTimeoutMillis(int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("Timeout must be greater than 0");
        }
        addTimeout = milliseconds;
        return this;
    }

    /**
     * Set the timeout in milliseconds to allow when attempting to take an item
     * from the queue in order to send to the remote service.
     * @deprecated use {@link #setTakeTimeoutMillis(int)}
     * @param milliseconds timeout to set
     * @return this builder
     */
    @Deprecated
    public ShareUsageBuilderBase<T> setTakeTimeout(int milliseconds) {
        takeTimeout = milliseconds;
        return this;
    }
    /**
     * Set the timeout in milliseconds to allow when attempting to take an item
     * from the queue in order to send to the remote service.
     * <p>
     * Default value is 0, i.e. if there are no more items available, send straight away.
     * @param milliseconds timeout to set
     * @return this builder
     */
    @DefaultValue(intValue = Constants.SHARE_USAGE_DEFAULT_TAKE_TIMEOUT)
    public ShareUsageBuilderBase<T> setTakeTimeoutMillis(int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("Timeout must be greater than 0");
        }
        takeTimeout = milliseconds;
        return this;
    }

    /**
     * Set the URL to use when sharing usage data.
     * <p>
     * The default is to send to 51Degrees - see {@link Constants#SHARE_USAGE_DEFAULT_URL}
     * @param shareUsageUrl the URL to use when sharing usage data
     * @return this builder
     */
    @DefaultValue(value="Send to 51D - " + Constants.SHARE_USAGE_DEFAULT_URL)
    public ShareUsageBuilderBase<T> setShareUsageUrl(String shareUsageUrl) {
        try {
            URL url = new URL(shareUsageUrl);
            assert !url.toString().isEmpty();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        this.shareUsageUrl = shareUsageUrl;
        return this;
    }

    /**
     * Set the name of the cookie that contains the session id.
     * <p>
     * The default value is "JSESSIONID"
     * <p>
     * This setting has no effect if TrackSession is false.
     * @param cookieName the name of the cookie that contains the session id
     * @return this builder
     */
    @DefaultValue(value=fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME)
    public ShareUsageBuilderBase<T> setSessionCookieName(String cookieName) {
        // test that the cookie name is valid
        HttpCookie cookie = new HttpCookie(cookieName, "test");
        assert !cookie.getName().isEmpty();
        this.sessionCookieName = cookieName;
        return this;
    }

    /**
     * If exactly the same evidence values are seen multiple times within this
     * time limit then they will only be shared once.
     * <p>
     * The default value for this is 0 - meaning always share.
     * @param interval the interval in minutes
     * @return this builder
     */
    @DefaultValue(intValue=Constants.SHARE_USAGE_DEFAULT_REPEAT_EVIDENCE_MINUTES)
    public ShareUsageBuilderBase<T> setRepeatEvidenceIntervalMinutes(int interval) {
        this.repeatEvidenceInterval = interval;
        return this;
    }

    /**
     * If set to true, the configured session cookie will be used to
     * identify user sessions.
     * <p>
     * This will help to differentiate duplicate values that should not be 
     * shared.
     * <p>
     * Default value is false
     * @param track boolean value sets whether the usage element should track sessions
     * @return this builder
     */
    @DefaultValue(value="false")
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

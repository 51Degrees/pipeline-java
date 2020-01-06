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

public abstract class ShareUsageBuilderBase<T> {

    protected final ILoggerFactory loggerFactory;

    private final Logger logger;

    protected int repeatEvidenceInterval;
    protected double sharePercentage = 1;
    protected int minimumEntriesPerMessage = 50;
    protected int maximumQueueSize = Constants.SHARE_USAGE_DEFAULT_MAX_QUEUE_SIZE;
    protected int addTimeout = Constants.SHARE_USAGE_DEFAULT_ADD_TIMEOUT;
    protected int takeTimeout = Constants.SHARE_USAGE_DEFAULT_TAKE_TIMEOUT;
    protected String shareUsageUrl = Constants.SHARE_USAGE_DEFAULT_URL;
    protected String sessionCookieName = fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
    protected List<String> blockedHttpHeaders = new ArrayList<>();
    protected List<String> includedQueryStringParameters = new ArrayList<>();
    protected List<Map.Entry<String, String>> ignoreDataEvidenceFilter = new ArrayList<>();
    protected boolean trackSession;

    public ShareUsageBuilderBase(ILoggerFactory loggerFactory) {
        this(loggerFactory, loggerFactory.getLogger(ShareUsageBuilderBase.class.getName()));
    }

    public ShareUsageBuilderBase(ILoggerFactory loggerFactory, Logger logger) {
        this.logger = logger;
        this.loggerFactory = loggerFactory;
    }

    public ShareUsageBuilderBase<T> setIncludedQueryStringParameters(
        List<String> queryStringParameterNames) {
        for (String name : queryStringParameterNames) {
            includedQueryStringParameters.add(name);
        }
        return this;
    }

    public ShareUsageBuilderBase<T> setIncludedQueryStringParameters(
        String queryStringParameterNames) {
        return setIncludedQueryStringParameters(
            Arrays.asList(queryStringParameterNames.split(",")));
    }

    public ShareUsageBuilderBase<T> setIncludedQueryStringParameter(
        String queryStringParameterName) {
        includedQueryStringParameters.add(queryStringParameterName);
        return this;
    }

    public ShareUsageBuilderBase<T> setBlockedHttpHeaders(
        List<String> blockedHeaders) {
        blockedHttpHeaders = blockedHeaders;
        return this;
    }

    public ShareUsageBuilderBase<T> setBlockedHttpHeader(String blockedHeader) {
        blockedHttpHeaders.add(blockedHeader);
        return this;
    }

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

    public ShareUsageBuilderBase<T> setSharePercentage(double sharePercentage) {
        this.sharePercentage = sharePercentage;
        return this;
    }

    public ShareUsageBuilderBase<T> setMinimumEntriesPerMessage(int minimumEntriesPerMessage) {
        this.minimumEntriesPerMessage = minimumEntriesPerMessage;
        return this;
    }

    public ShareUsageBuilderBase<T> setMaximumQueueSize(int size) {
        maximumQueueSize = size;
        return this;
    }

    public ShareUsageBuilderBase<T> setAddTimeout(int milliseconds) {
        addTimeout = milliseconds;
        return this;
    }

    public ShareUsageBuilderBase<T> setTakeTimeout(int milliseconds) {
        takeTimeout = milliseconds;
        return this;
    }

    public ShareUsageBuilderBase<T> setShareUsageUrl(String shareUsageUrl) {
        this.shareUsageUrl = shareUsageUrl;
        return this;
    }

    public ShareUsageBuilderBase<T> setAspSessionCookieName(String cookieName) {
        this.sessionCookieName = cookieName;
        return this;
    }

    public ShareUsageBuilderBase<T> setRepeatEvidenceIntervalMinutes(int interval) {
        this.repeatEvidenceInterval = interval;
        return this;
    }

    public ShareUsageBuilderBase<T> setTrackSession(boolean track) {
        this.trackSession = track;
        return this;
    }

    public abstract T build() throws IOException;
}

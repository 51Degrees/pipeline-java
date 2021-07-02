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

import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.engines.services.HttpClient;
import java.io.IOException;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * Builder class that is used to create {@link ShareUsageElement}.
 */
@ElementBuilder
public class ShareUsageBuilder extends ShareUsageBuilderBase<ShareUsageElement> {

    private final HttpClient httpClient;

    /**
     * Constructor
     * @param loggerFactory the {@link ILoggerFactory} to use when creating
     *                      loggers for a {@link ShareUsageElement}
     * @param httpClient the {@link HttpClient} that {@link ShareUsageElement}
     *                   should use for sending data
     */
    public ShareUsageBuilder(
        ILoggerFactory loggerFactory,
        HttpClient httpClient) {
        super(loggerFactory);
        this.httpClient = httpClient;
    }

    /**
     * Constructor
     * @param loggerFactory the {@link ILoggerFactory} to use when creating
     *                      loggers for a {@link ShareUsageElement}
     * @param logger the {@link Logger} to use for {@link ShareUsageElement}
     * @param httpClient the {@link HttpClient} that {@link ShareUsageElement}
     *                   should use for sending data
     */
    public ShareUsageBuilder(
        ILoggerFactory loggerFactory,
        Logger logger,
        HttpClient httpClient) {
        super(loggerFactory, logger);
        this.httpClient = httpClient;
    }

    @Override
    public ShareUsageElement build() throws IOException {
        return new ShareUsageElement(
            loggerFactory.getLogger(ShareUsageElement.class.getName()),
            httpClient,
            sharePercentage,
            minimumEntriesPerMessage,
            getMaximumQueueSize(),
            addTimeout,
            takeTimeout,
            repeatEvidenceInterval,
            trackSession,
            shareUsageUrl,
            blockedHttpHeaders,
            includedQueryStringParameters,
            ignoreDataEvidenceFilter,
            sessionCookieName);
    }
}

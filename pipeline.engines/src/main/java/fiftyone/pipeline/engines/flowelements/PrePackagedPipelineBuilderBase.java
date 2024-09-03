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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.core.flowelements.PipelineBuilderBase;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Base class for pipeline builders that will produce a pipeline
 * with specific flow elements.
 * @param <TBuilder> the builder type
 */
public abstract class PrePackagedPipelineBuilderBase
    <TBuilder extends PrePackagedPipelineBuilderBase<TBuilder>>
    extends PipelineBuilderBase<TBuilder> {

    protected boolean lazyLoading = false;

    protected boolean resultsCache = false;

    protected long lazyLoadingTimeoutMillis = 1000;

    protected int resultsCacheSize = 1000;

    /**
     * Construct a new instance using the default {@link ILoggerFactory}
     * implementation returned by the {@link LoggerFactory#getILoggerFactory()}
     * method.
     */
    public PrePackagedPipelineBuilderBase() {
        super();
    }

    /**
     * Construct a new instance.
     * @param loggerFactory the {@link ILoggerFactory} used to create any
     *                      loggers required by instances being built by the
     *                      builder
     */
    public PrePackagedPipelineBuilderBase(ILoggerFactory loggerFactory) {
        super(loggerFactory);
    }

    /**
     * Enable lazy loading of results. Uses a default timeout of 5 seconds.
     * @return this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder useLazyLoading() {
        lazyLoading = true;
        return (TBuilder)this;
    }

    /**
     * Enable lazy loading of results.
     * @param timeoutMillis the timeout to use when attempting to access
     *                      lazy-loaded values. Default is 5 seconds
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder useLazyLoading(long timeoutMillis) {
        lazyLoading = true;
        this.lazyLoadingTimeoutMillis = timeoutMillis;
        return (TBuilder)this;
    }

    /**
     * Enable caching of results. Uses a default cache size of 1000.
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder useResultsCache() {
        resultsCache = true;
        return (TBuilder) this;
    }

    /**
     * Enable caching of results.
     * @param size the maximum number of results to hold in the device detection
     *             cache. Default is 1000
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder useResultsCache(int size) {
        resultsCache = true;
        resultsCacheSize = size;
        return (TBuilder) this;
    }
}
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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.core.flowelements.PipelineBuilderBase;
import org.slf4j.ILoggerFactory;

public abstract class PrePackagedPipelineBuilderBase
    <TBuilder extends PrePackagedPipelineBuilderBase<TBuilder>>
    extends PipelineBuilderBase<TBuilder> {

    protected boolean lazyLoading = false;

    protected boolean resultsCache = false;

    protected long lazyLoadingTimeoutMillis = 1000;

    protected int resultsCacheSize = 1000;

    public PrePackagedPipelineBuilderBase() {
        super();
    }

    public PrePackagedPipelineBuilderBase(ILoggerFactory loggerFactory) {
        super(loggerFactory);
    }

    public TBuilder useLazyLoading() {
        lazyLoading = true;
        return (TBuilder)this;
    }

    public TBuilder useLazyLoading(long timeoutMillis) {
        lazyLoading = true;
        this.lazyLoadingTimeoutMillis = timeoutMillis;
        return (TBuilder)this;
    }

    public TBuilder useResultsCache() {
        resultsCache = true;
        return (TBuilder) this;
    }

    public TBuilder useResultsCache(int size) {
        resultsCache = true;
        resultsCacheSize = size;
        return (TBuilder) this;
    }
}
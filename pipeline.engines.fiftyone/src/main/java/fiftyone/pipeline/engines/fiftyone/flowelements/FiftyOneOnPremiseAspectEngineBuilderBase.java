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

import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneDataFileDefault;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneUrlFormatter;
import fiftyone.pipeline.engines.flowelements.SingleFileAspectEngineBuilderBase;
import fiftyone.pipeline.engines.services.DataUpdateService;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class that exposes the common options that all 51Degrees
 * on-premise engine builders using a single data file should make use of.
 * @param <TBuilder> the specific builder type to use as the return type from
 *                  the fluent builder methods
 * @param <TEngine> the type of the engine that this builder will build
 */
public abstract class FiftyOneOnPremiseAspectEngineBuilderBase<
    TBuilder extends FiftyOneOnPremiseAspectEngineBuilderBase<TBuilder, TEngine>,
    TEngine extends FiftyOneAspectEngine> extends
    SingleFileAspectEngineBuilderBase<TBuilder, TEngine> {

    /**
     * Default constructor which uses the {@link ILoggerFactory} implementation
     * returned by {@link LoggerFactory#getILoggerFactory()}.
     */
    public FiftyOneOnPremiseAspectEngineBuilderBase() {
        this(LoggerFactory.getILoggerFactory());
    }

    /**
     * Construct a new instance using the {@link ILoggerFactory} supplied.
     * @param loggerFactory the logger factory to use
     */
    public FiftyOneOnPremiseAspectEngineBuilderBase(ILoggerFactory loggerFactory) {
        this(loggerFactory, null);
    }

    /**
     * Construct a new instance using the {@link ILoggerFactory} and
     * {@link DataUpdateService} supplied.
     * @param loggerFactory the logger factory to use
     * @param dataUpdateService the {@link DataUpdateService} to use when
     *                          automatic updates happen on the data file
     */
    public FiftyOneOnPremiseAspectEngineBuilderBase(
        ILoggerFactory loggerFactory,
        DataUpdateService dataUpdateService) {
        super(loggerFactory, dataUpdateService);
        setDataUpdateUrl("https://distributor.51degrees.com/api/v2/download");
        setDataUpdateUrlFormatter(new FiftyOneUrlFormatter());
    }

    @Override
    protected AspectEngineDataFile newAspectEngineDataFile() {
        return new FiftyOneDataFileDefault();
    }

    /**
     * Set the expected number of concurrent operations using the engine.
     * This sets the concurrency of the internal caches to avoid excessive
     * locking.
     * @param concurrency expected concurrent accesses
     * @return this builder
     */
    public abstract TBuilder setConcurrency(int concurrency);
}

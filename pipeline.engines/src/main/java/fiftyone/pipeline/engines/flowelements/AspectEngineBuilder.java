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

import fiftyone.pipeline.annotations.BuildArg;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.services.DataUpdateService;

import java.util.List;
import java.util.Set;

/**
 * Builder interface specific to {@link AspectEngine}s.
 */
public interface AspectEngineBuilder {

    /**
     * Set a list of properties to be included in the results of its processing.
     * If none are set, then all properties are included.
     *
     * @param properties names of properties to include in results
     * @return this builder
     */
    AspectEngineBuilder setProperties(List<String> properties);

    /**
     * Set a property to be included in the results of its processing.
     * If none are set, then all properties are included.
     *
     * @param property name of property to include in results
     * @return this builder
     */
    AspectEngineBuilder setProperty(String property);

    /**
     * Set a list of properties to be included in the results of its processing.
     * If none are set, then all properties are included.
     *
     * @param properties to include in results
     * @return this builder
     */
    AspectEngineBuilder setProperties(Set<AspectPropertyMetaData> properties);

    /**
     * Set a property to be included in the results of its processing.
     * If none are set, then all properties are included.
     *
     * @param property to include in results
     * @return this builder
     */
    AspectEngineBuilder setProperty(AspectPropertyMetaData property);

    /**
     * Set the cache that the engine should use to cache its results. This
     * is a cache configuration which is the build inside the engine to ensure
     * it is immutable. This is used cache {@link AspectData} keyed on the
     * evidence in {@link FlowData}, so should be configured to construct a key
     * using only the relevant pieces of evidence.
     *
     * @param config of cache to create
     * @return this builder
     */
    AspectEngineBuilder setCache(CacheConfiguration config);

    /**
     * Set the interval in milliseconds at which the file on disk is checked for
     * changes by the {@link DataUpdateService}. By default this is 30 minutes.
     *
     * @param interval check interval in ms
     * @return this builder
     */
    AspectEngineBuilder setPollingInterval(int interval);

    /**
     * Set the maximum time in milliseconds for the {@link DataUpdateService} to
     * add to scheduled update times for this Engine. Scheduling an update for a
     * random time within a defined window avoids the possibility of many
     * machines updating at the same time. By default this is 10 minutes.
     *
     * @param maxRandomisation max update randomisation in ms
     * @return this builder
     */
    AspectEngineBuilder setUpdateTimeRandomisation(int maxRandomisation);

    /**
     * Override the Engine's default data update URL. This must contain the
     * entire URL string needed including any license keys and other parameters
     * as this completely overrides any URL generation performed by the Engine.
     *
     * @param url full URL to use for data updates
     * @return this builder
     */
    AspectEngineBuilder setUpdateUrl(String url);

    /**
     * Build the {@link AspectEngine} that has been defined in this instance of
     * {@link AspectEngineBuilder} using the data file provided.
     *
     * @param dataFile path to the file to be used
     * @return constructed {@link AspectEngine}
     */
    AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> build(@BuildArg("dataFile") String dataFile);

    /**
     * Build the {@link AspectEngine} that has been defined in this instance of
     * {@link AspectEngineBuilder} using the data provided.
     *
     * @param data in memory data to be used
     * @return constructed {@link AspectEngine}
     */
    AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> build(byte[] data);
}

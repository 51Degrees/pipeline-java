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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.caching.FlowCache;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.services.DataUpdateService;

import java.util.concurrent.ExecutorService;

/**
 * AspectEngines are a subset of {@link FlowElement}s 
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#aspect-engine">Specification</a>
 * @param <TData> the type of aspect data that the flow element will write to
 * @param <TProperty> the type of meta data that the flow element will supply
 *                    about the properties it populates.
 */
public interface AspectEngine<
    TData extends AspectData,
    TProperty extends AspectPropertyMetaData>
    extends FlowElement<TData, TProperty> {

    /**
     * Set the results cache.
     * This is used to store the results of queries against the evidence that
     * was provided.
     * If the same evidence is provided again then the cached response is
     * returned without needing to call the engine itself.
     * @param cache the cache to use
     */
    void setCache(FlowCache cache);

    /**
     * Get the tier to which the current data source belongs.
     * For 51Degrees this will usually be one of:
     * Lite
     * Premium
     * Enterprise
     * @return data tier
     */
    String getDataSourceTier();

    /**
     * Configure lazy loading of results.
     * @param configuration the configuration to use
     */
    void setLazyLoading(LazyLoadingConfiguration configuration);

    /**
     * Get the lazy loading configuration used for loading of results.
     * @return lazy loading configuration
     */
    LazyLoadingConfiguration getLazyLoadingConfiguration();

    /**
     * Get the executor service to use when starting processing threads which
     * are lazily loaded.
     * @return an {@link ExecutorService}
     */
    ExecutorService getExecutor();
}

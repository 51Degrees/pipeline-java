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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.caching.FlowCache;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.services.DataUpdateService;

import java.util.concurrent.ExecutorService;

/**
 * AspectEngines are a subset of {@link FlowElement}s which follow a certain
 * defined structure. They have defined inputs, outputs and other common
 * methods/properties. They use certain pieces of evidence contained within the
 * {@link FlowData} (e.g. User-Agent) to determine the properties of an Aspect
 * (e.g. hardware device). By defining an AspectEngine, 51Degrees FlowElements
 * can easily share common functionality through base classes and convention.
 * <p>
 * Third parties can also benefit by extending {@link AspectEngineBase} to make
 * use of its generic methods.
 * <p>
 * A major defining feature of an AspectEngine is that it uses a data file which
 * will be kept up to date by the {@link DataUpdateService}.
 */
public interface AspectEngine<
    TData extends AspectData,
    TProperty extends AspectPropertyMetaData>
    extends FlowElement<TData, TProperty> {

    void setCache(FlowCache cache);

    String getDataSourceTier();

    void setLazyLoading(LazyLoadingConfiguration configuration);

    LazyLoadingConfiguration getLazyLoadingConfiguration();

    ExecutorService getExecutor();
}

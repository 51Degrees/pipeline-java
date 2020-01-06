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
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.engines.caching.FlowCache;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.ProcessCallable;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import fiftyone.pipeline.util.Types;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Base class for {@link AspectEngine}s to extend. This implements the data
 * update service and missing property service along with everything
 * already implemented by {@link FlowElementBase}.
 */
public abstract class AspectEngineBase<
    TData extends AspectData,
    TProperty extends AspectPropertyMetaData>
    extends FlowElementBase<TData, TProperty>
    implements AspectEngine<TData, TProperty> {

    private LazyLoadingConfiguration lazyLoadingConfiguration = null;

    private ExecutorService executor = null;

    protected MissingPropertyService missingPropertyService;

    protected FlowCache cache = null;

    /**
     * Construct a new instance
     */
    public AspectEngineBase(Logger logger, ElementDataFactory<TData> aspectDataFactory) {
        super(logger, aspectDataFactory);
    }

    @Override
    public TypedKey<TData> getTypedDataKey() {
        if (typedKey == null) {
            typedKey = new TypedKeyDefault<>(getElementDataKey(), Types.findSubClassParameterType(this, AspectEngineBase.class, 0));
        }
        return typedKey;

    }

    @Override
    public abstract List<TProperty> getProperties();

    @Override
    public abstract String getDataSourceTier();

    @Override
    public void setCache(FlowCache cache) {
        this.cache = cache;
        cache.setFlowElement(this);
    }

    @Override
    public void setLazyLoading(LazyLoadingConfiguration configuration) {
        this.lazyLoadingConfiguration = configuration;
        this.executor = lazyLoadingConfiguration.getExecutorService();
    }

    @Override
    public LazyLoadingConfiguration getLazyLoadingConfiguration() {
        return lazyLoadingConfiguration;
    }

    @Override
    public ExecutorService getExecutor() {
        return executor;
    }

    protected abstract void processEngine(FlowData flowData, TData aspectData) throws Exception;

    @Override
    protected final void processInternal(FlowData flowData) throws Exception {
        processWithCache(flowData);
    }

    private void processWithCache(final FlowData flowData) throws Exception {
        TData cacheResult = null;

        if (cache != null) {
            try {
                cacheResult = (TData) cache.get(flowData);
            } catch (ClassCastException e) {
                cacheResult = null;
            }
        }
        // If we don't have a result from the cache then
        // run through the normal processing.
        if (cacheResult == null) {
            // If the flow data already contains an entry for this
            // element's key then use it. Otherwise, create a new
            // aspect data instance and add it to the flow data.
            final TData aspectData =
                flowData.getOrAdd(getTypedDataKey(), getDataFactory());
            if (aspectData.getEngines().contains(this) == false) {
                ((AspectDataBase) aspectData).addEngine(this);
            }

            // Start the engine processing
            if (lazyLoadingConfiguration != null) {
                // If lazy loading is configured then create a task
                // to do the processing and assign the task to the
                // aspect data property.
                ((AspectDataBase) aspectData).addProcessCallable(
                    new ProcessCallable(this) {
                        @Override
                        public Void call() throws Exception {
                            processEngine(flowData, aspectData);
                            return null;
                        }
                    }
                );
            }
            else {
                // If not lazy loading, just start processing.
                processEngine(flowData, aspectData);
            }
            // If there is a cache then add the result
            // of processing to the cache.
            if (cache != null) {
                cache.put(flowData, flowData.getFromElement(this));
            }
        } else {
            // We have a result from the cache so add it
            // into the flow data.
            flowData.getOrAdd(getTypedDataKey(), new DataFactorySimple<>(cacheResult));
        }
    }

    @Override
    protected void managedResourcesCleanup() {
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException e) {
            }
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (executor.awaitTermination(
                    lazyLoadingConfiguration.getPropertyTimeoutMillis(),
                    TimeUnit.MILLISECONDS) == false) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }
}
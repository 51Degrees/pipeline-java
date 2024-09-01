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

import fiftyone.pipeline.annotations.DefaultValue;
import fiftyone.pipeline.engines.caching.FlowCacheDefault;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class that exposes the common options that all 51Degrees engine
 * builders should make use of.
 * @param <TBuilder> the specific builder type to use as the return type from
 *                  the fluent builder methods
 * @param <TEngine> the type of the engine that this builder will build
 */
@SuppressWarnings("rawtypes")
public abstract class AspectEngineBuilderBase<
    TBuilder extends AspectEngineBuilderBase<TBuilder, TEngine>,
    TEngine extends AspectEngine> {
    protected final ILoggerFactory loggerFactory;
    private final List<String> properties = new ArrayList<>();
    protected CacheConfiguration cacheConfig = null;
    private LazyLoadingConfiguration lazyLoadingConfig = null;

    /**
     * Default constructor which uses the {@link ILoggerFactory} implementation
     * returned by {@link LoggerFactory#getILoggerFactory()}.
     */
    public AspectEngineBuilderBase() {
        this(LoggerFactory.getILoggerFactory());
    }

    /**
     * Construct a new instance using the {@link ILoggerFactory} supplied.
     * @param loggerFactory the logger factory to use
     */
    public AspectEngineBuilderBase(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }

    /**
     * Ad a property to the list of properties if it does not already exist in
     * the list.
     * @param property to add
     */
    private void tryAddProperty(String property) {
        if (properties.contains(property) == false) {
            properties.add(property);
        }
    }

    /**
     * Get the list of properties that have been added to the builder.
     * Duplicate properties have been removed. This list represents the
     * properties which the user wants to retrieve from the engine. If the list
     * is empty, this means the user wants all properties available.
     *
     * @return list of required properties
     */
    protected List<String> getProperties() {
        return properties;
    }

    /**
     * Configure the properties that the engine will populate in the response.
     * By default all properties will be populated.
     * @param set The properties that we want the engine to populate
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setProperties(Set<AspectPropertyMetaData> set) {
        for (AspectPropertyMetaData property : set) {
            tryAddProperty(property.getName());
        }
        return (TBuilder) this;
    }

    /**
     * Add a property to the list of properties that the engine will populate in
     * the response.
     * <p>
     * By default, all properties will be populated.
     * @param s the property that we want the engine to populate
     * @return this builder
     */
    @DefaultValue("All properties")
    @SuppressWarnings("unchecked")
    public TBuilder setProperty(String s) {
        tryAddProperty(s);
        return (TBuilder) this;
    }

    /**
     * Configure the properties that the engine will populate in the response.
     * By default all properties will be populated.
     * @param properties The properties that we want the engine to populate
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setProperties(List<String> properties) {
        for (String property : properties) {
            tryAddProperty(property);
        }
        return (TBuilder) this;
    }

    /**
     * Configure the properties that the engine will populate in the response.
     * <p>
     * By default, all properties will be populated.
     * @param properties The properties that we want the engine to populate
     * @return this builder
     */
    @DefaultValue("All properties")
    @SuppressWarnings("unchecked")
    public TBuilder setProperties(String properties) {
        for (String property : properties.split("\\s,\\s")) {
            tryAddProperty(property);
        }
        return (TBuilder) this;
    }
    
    /**
     * Add a property to the list of properties that the engine will populate in
     * the response. By default all properties will be populated.
     * @param aspectProperty the property that we want the engine to populate
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setProperty(AspectPropertyMetaData aspectProperty) {
        tryAddProperty(aspectProperty.getName());
        return (TBuilder) this;
    }

    /**
     * Configure lazy loading of results.
     * @param configuration the configuration to use
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setLazyLoading(LazyLoadingConfiguration configuration) {
        this.lazyLoadingConfig = configuration;
        return (TBuilder)this;
    }

    /**
     * Configure the results cache that will be used by the Pipeline to cache
     * results from this engine.
     * @param cacheConfiguration the cache configuration to use
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setCache(CacheConfiguration cacheConfiguration) {
        this.cacheConfig = cacheConfiguration;
        return (TBuilder) this;
    }

    /**
     * Called by the {@link #buildEngine()} method to handle configuration of
     * the engine after it is built. Can be overridden by derived classes to add
     * additional configuration, but the base method should always be called.
     * @param engine the engine to configure
     * @throws Exception if an exception occurred which configuring the engine
     */
    protected void configureEngine(TEngine engine) throws Exception {
        if (cacheConfig != null) {
            engine.setCache(new FlowCacheDefault(cacheConfig));
        }
        if (lazyLoadingConfig != null) {
            engine.setLazyLoading(lazyLoadingConfig);
        }
    }

    /**
     * Called by the {@link #buildEngine()} method to handle anything that needs
     * doing before the engine is built. By default, nothing needs to be done.
     */
    protected void preCreateEngine() {

    }

    /**
     * Called by the {@link #buildEngine()} method to handle creation of
     * the engine instance.
     * @param properties the properties list to create the engine with
     * @return an {@link AspectEngine}
     * @throws Exception if the engine could not be created
     */
    protected abstract TEngine newEngine(List<String> properties) throws Exception;

    /**
     * Build an engine using the configured options. Derived classes should call
     * this method when building an engine to ensure it is configured correctly
     * all down the class hierarchy.
     * @return an {@link AspectEngine}
     * @throws Exception if the engine could not be created
     */
    protected TEngine buildEngine() throws Exception {
        preCreateEngine();
        TEngine engine = newEngine(getProperties());
        configureEngine(engine);
        return engine;
    }
}

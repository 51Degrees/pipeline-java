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

import fiftyone.pipeline.engines.caching.FlowCacheDefault;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AspectEngineBuilderBase<TBuilder extends AspectEngineBuilderBase<TBuilder, TEngine>, TEngine extends AspectEngine> {
    protected final ILoggerFactory loggerFactory;
    private List<String> properties = new ArrayList<>();
    private CacheConfiguration cacheConfig = null;
    private LazyLoadingConfiguration lazyLoadingConfig = null;

    public AspectEngineBuilderBase() {
        this(LoggerFactory.getILoggerFactory());
    }

    public AspectEngineBuilderBase(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }

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

    public TBuilder setProperties(Set<AspectPropertyMetaData> set) {
        for (AspectPropertyMetaData property : set) {
            tryAddProperty(property.getName());
        }
        return (TBuilder) this;
    }

    public TBuilder setProperty(String s) {
        tryAddProperty(s);
        return (TBuilder) this;
    }

    public TBuilder setProperty(AspectPropertyMetaData aspectProperty) {
        tryAddProperty(aspectProperty.getName());
        return (TBuilder) this;
    }

    public TBuilder setLazyLoading(LazyLoadingConfiguration configuration) {
        this.lazyLoadingConfig = configuration;
        return (TBuilder)this;
    }

    public TBuilder setCache(CacheConfiguration cacheConfiguration) {
        this.cacheConfig = cacheConfiguration;
        return (TBuilder) this;
    }

    protected void configureEngine(TEngine engine) throws Exception {
        if (cacheConfig != null) {
            engine.setCache(new FlowCacheDefault(cacheConfig));
        }
        if (lazyLoadingConfig != null) {
            engine.setLazyLoading(lazyLoadingConfig);
        }
    }

    /**
     * Called by the buildEngine() method to handle anything that needs doing
     * before the engine is built. By default, nothing needs to be done.
     */
    protected void preCreateEngine() {

    }

    /**
     * Called by the buildEngine() method to handle creation of
     * the engine instance.
     * @param properties
     * @return an AspectEngine
     */
    protected abstract TEngine newEngine(List<String> properties);

    /**
     * Build an engine using the configured options. Derived classes should call
     * this method when building an engine to ensure it is configured correctly
     * all down the class hierarchy.
     * @return an AspectEngine
     */
    protected TEngine buildEngine() throws Exception {
        preCreateEngine();
        TEngine engine = newEngine(getProperties());
        configureEngine(engine);
        return engine;
    }

}

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

package fiftyone.pipeline.engines.configuration;

import fiftyone.caching.CacheBuilder;
import fiftyone.caching.LruPutCache;

/**
 * Contains everything needed to build a cache.
 * Currently, a {@link CacheBuilder} and an integer size parameter.
 */
public class CacheConfiguration {

    private static final int defaultSize = 1000;

    private final CacheBuilder builder;

    private final int size;

    /**
     * Construct a new builder with the builder and size specified.
     * @param builder the builder to use to create caches
     * @param size the maximum size of the cache
     */
    public CacheConfiguration(CacheBuilder builder, int size) {
        this.builder = builder;
        this.size = size;
    }

    /**
     * Construct a new builder using the {@link LruPutCache} with the size
     * specified.
     * @param size maximum size of the cache
     */
    public CacheConfiguration(int size) {
        this(new LruPutCache.Builder(), defaultSize);
    }

    /**
     * Default constructor uses the {@link LruPutCache} with a size of 1000.
     */
    public CacheConfiguration() {
        this(new LruPutCache.Builder(), defaultSize);
    }

    /**
     * Get the builder to use when building a cache.
     * @return cache builder
     */
    public CacheBuilder getCacheBuilder() {
        return builder;
    }

    /**
     * Get the maximum size parameter to use when building a cache.
     * @return cache size
     */
    public int getSize() {
        return size;
    }
}

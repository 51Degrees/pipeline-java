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

package fiftyone.caching;

import java.io.IOException;

/**
 * Uses the {@link LruCacheBase} to implement the {@link LoadingCache} interface.
 * @param <K> the type of key
 * @param <V> the type of value
 */
public class LruLoadingCache<K, V>
    extends LruCacheBase<K, V>
    implements LoadingCache<K, V> {

    private ValueLoader<K, V> loader;

    /**
     * Constructs a new instance of the cache.
     * @param cacheSize the number of items to store in the cache
     */
    LruLoadingCache(int cacheSize) {
        super(cacheSize, Runtime.getRuntime().availableProcessors(), false);
    }

    /**
     * Constructs a new instance of the cache.
     * @param cacheSize the number of items to store in the cache
     * @param concurrency the expected number of concurrent requests to the
     *                    cache
     */
    LruLoadingCache(int cacheSize, int concurrency) {
        super(cacheSize, concurrency, false);
    }

    /**
     * Constructs a new instance of the cache.
     * @param cacheSize the number of items to store in the cache
     * @param loader the loader used to fetch items not already in the cache
     */
    public LruLoadingCache(int cacheSize, ValueLoader<K, V> loader) {
        this(cacheSize, loader, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Constructs a new instance of the cache.
     * @param cacheSize the number of items to store in the cache
     * @param concurrency the expected number of concurrent requests to the
     *                    cache
     * @param loader the loader used to fetch items not already in the cache
     */
    LruLoadingCache(int cacheSize, ValueLoader<K, V> loader, int concurrency) {
        super(cacheSize, concurrency, false);
        setCacheLoader(loader);
    }

    /**
     * Set the loader used to fetch items not in the cache.
     * @param loader the loader to use
     */
    public void setCacheLoader(ValueLoader<K, V> loader) {
        this.loader = loader;
    }

    /**
     * Retrieves the value for key requested. If the key does not exist
     * in the cache then the Fetch method of the cache's loader is used to
     * retrieve the value.
     *
     * @param key of the item required.
     * @return an instance of the value associated with the key
     * @throws IllegalStateException if there was a problem accessing data file
     */
    @Override
    public V get(K key) {
        try {
            return get(key, loader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Retrieves the value for key requested. If the key does not exist
     * in the cache then the Fetch method is used to retrieve the value
     * from another loader.
     *
     * @param key of the item required
     * @param loader to fetch the items from
     * @return an instance of the value associated with the key
     * @throws java.io.IOException if there was a problem accessing data file
     */
    public V get(K key, ValueLoader<K, V> loader) throws IOException {
        V result = super.get(key);

        if (result == null) {
            result = loader.load(key);
            super.add(key, result);
        }
        return result;
    }

    /**
     * Implementation of {@link CacheBuilder} for {@link LruLoadingCache} caches.
     */
    public static class Builder implements LoadingCacheBuilder {

        @Override
        public <K, V> Cache<K, V> build(Cache<K, V> c, int cacheSize) {
            return new LruLoadingCache<>(cacheSize);
        }
    }
}

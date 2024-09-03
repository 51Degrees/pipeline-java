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

/**
 * Uses the {@link LruCacheBase} to implement the {@link PutCache} interface.
 * @param <K> the type of key
 * @param <V> the type of value
 */
public class LruPutCache<K, V> extends LruCacheBase<K, V> implements PutCache<K, V> {

    /**
     * Constructs a new instance of the cache.
     * @param cacheSize the number of items to store in the cache
     * @param concurrency the expected number of concurrent requests to the
     *                    cache
     * @param updateExisting true if existing items should be replaced
     */
    LruPutCache(int cacheSize, int concurrency, boolean updateExisting) {
        super(cacheSize, concurrency, updateExisting);
    }

    @Override
    public void put(K key, V value) {
        super.add(key, value);
    }

    /**
     * Implementation of {@link CacheBuilder} for {@link LruPutCache} caches.
     */
    public static class Builder implements PutCacheBuilder {

        private boolean updateExisting = false;
        private int concurrency = Runtime.getRuntime().availableProcessors();

        @Override
        public <K, V> PutCache<K, V> build(Cache<K, V> c, int cacheSize) {
            return new LruPutCache<>(cacheSize, concurrency, updateExisting);
        }

        /**
         * Set the expected number of concurrent requests to the cache. This
         * will determine the number linked lists used in the cache structure.
         * For details see description of multiple linked lists in
         * {@link LruCacheBase}.
         * @param concurrency the expected number of concurrent requests to the
         * cache.
         * @return this builder
         */
        public Builder setConcurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        @Override
        public Builder setUpdateExisting(boolean update) {
            this.updateExisting = update;
            return this;
        }
    }
}

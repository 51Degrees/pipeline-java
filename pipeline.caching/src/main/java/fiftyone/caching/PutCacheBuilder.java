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

public interface PutCacheBuilder extends CacheBuilder {
    /**
     * Set whether or not an existing item in the cache should be updated
     * with the value given to the put method. By default this is false,
     * meaning that if put is called for a key which already exists in the
     * cache, the existing value is kept and there is no result to the put
     * method
     * @param update true if existing items should be updated by the put method
     * @return this builder
     */
    PutCacheBuilder setUpdateExisting(boolean update);

    @Override
    <K, V> PutCache<K, V> build(Cache<K, V> c, int cacheSize);
}

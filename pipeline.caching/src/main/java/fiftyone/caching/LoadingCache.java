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
 * Extension of general cache contract to provide for getting a value with a
 * particular value loaded. Primarily used to allow the value loader to be an
 * already instantiated value of the type V to avoid construction
 * costs of that value. (In other words the loader has the signature
 * "extends V implements IValueLoader").
 */
public interface LoadingCache<K, V> extends Cache<K, V> {

    /**
     * Get the value using the specified key and calling the specified loader if
     * needed.
     * @param key the key of the value to load
     * @param loader the loader to use when getting the value
     * @return the value from the cache, or loader if not available
     * @throws IOException if there was an error from the loader
     */
    V get(K key, ValueLoader<K, V> loader) throws IOException;
}

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

package fiftyone.pipeline.core.data;

import java.util.Map;

/**
 * Represents a collection of property values.
 */
public interface Data {
    /**
     * Get the value stored using the specified key.
     * @param key name of the property
     * @return value or null
     */
    Object get(String key);

    /**
     * Set the value stored using the specified key.
     * @param key name of the property
     * @param value value for the property
     */
    void put(String key, Object value);

    /**
     * Use the values in the specified map to populate this data instance.
     *
     * The data will not be cleared before the new values are added.
     * The new values will overwrite old values if any exist with the
     * same keys.
     * @param values the values to transfer to this data instance
     */
    void populateFromMap(Map<String, Object> values);

     /**
      * Get the data contained in this instance as a read only {@link Map}.
      * @return the data
      */
    Map<String, Object> asKeyMap();
}

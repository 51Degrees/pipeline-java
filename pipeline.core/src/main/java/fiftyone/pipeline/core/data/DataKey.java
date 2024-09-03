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

package fiftyone.pipeline.core.data;

import java.util.List;

/**
 * Data key class containing a number of objects which are used for equality.
 * When comparing {@link DataKey}s, the hash codes of each object in the key
 * are compared. Instances of this class are created using a
 * {@link DataKeyBuilder}.
 */
public class DataKey {

    private int hashCode;

    private final List<Object> keyValues;

    /**
     * Construct a new instance of {@link DataKey} using the objects provided.
     * @param keyValues list of objects which define the key
     */
    DataKey(List<Object> keyValues) {
        this.keyValues = keyValues;
        hashCode = 0;
        for (Object keyValue : keyValues) {
            if (keyValue != null) {
                hashCode ^= keyValue.hashCode();
            }
        }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof DataKey) {
            DataKey other = (DataKey) obj;

            // Check if the object passed in is a DataKey and contains the
            // same number of key fields.
            if (other.keyValues.size() == keyValues.size()) {
                result = true;
                int count = 0;
                // Check each key field in turn until the values fail to match
                // or we run out of key fields.
                while (result == true && count < keyValues.size()) {
                    Object thisValue = keyValues.get(count);
                    if (thisValue == null) {
                        result = other.keyValues.get(count) == null;
                    } else {
                        result = other.keyValues.contains(thisValue);
                    }
                    count++;
                }
            }
        }
        return result;
    }
}

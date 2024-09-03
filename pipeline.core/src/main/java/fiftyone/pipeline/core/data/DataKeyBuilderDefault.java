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

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

/**
 * The default implementation of {@link DataKeyBuilder}.
 */
public class DataKeyBuilderDefault implements DataKeyBuilder {

    private final List<Entry<Integer, Entry<String, Object>>> keys = new ArrayList<>();

    @Override
    public DataKeyBuilder add(int order, String keyName, Object keyValue) {
        keys.add(new SimpleEntry<Integer, Entry<String, Object>>(
            order,
            new SimpleEntry<>(keyName, keyValue)));
        return this;
    }

    @Override
    public DataKey build() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Entry<Integer, Entry<String, Object>> keyArray[] = new Entry[0];
        keyArray = keys.toArray(keyArray);
        Arrays.sort(keyArray, new KeyComparator());
        List<Object> keyList = new ArrayList<>(keyArray.length);
        for (Entry<Integer, Entry<String, Object>> key : keyArray) {
            keyList.add(key.getValue().getValue());
        }
        return new DataKey(keyList);
    }

    private static class KeyComparator
        implements Comparator<Entry<Integer, Entry<String, Object>>> {

        @Override
        public int compare(
            Entry<Integer, Entry<String, Object>> o1,
            Entry<Integer, Entry<String, Object>> o2) {

            int result = o1.getKey().compareTo(o2.getKey());
            if (result != 0) {
                return result;
            }

            result = o1.getValue().getKey().compareTo(o2.getValue().getKey());
            return result;
        }
    }
}

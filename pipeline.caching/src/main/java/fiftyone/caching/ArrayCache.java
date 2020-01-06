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

package fiftyone.caching;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;

public class ArrayCache<V extends Closeable> implements PutCache<Integer, V> {

    private final V[] array;

    public ArrayCache(int capacity, Class<V> type) {
        array = (V[]) Array.newInstance(type, capacity);
    }

    @Override
    public void put(Integer key, V value) {
        array[key] = value;
    }

    public long getCacheSize() {
        return array.length;
    }

    public long getCacheMisses() {
        return 0;
    }

    public long getCacheRequests() {
        return 0;
    }

    public double getPercentageMisses() {
        return 0;
    }

    public void resetCache() {
        for (int i = 0; i < array.length; i++) {
            array[i] = null;
        }
    }

    @Override
    public V get(Integer key) {
        return array[key];
    }

    @Override
    public void close() throws IOException {

    }
}

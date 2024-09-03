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

package fiftyone.pipeline.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Static helper methods for manipulating strings.
 */
public class StringManipulation {
    /**
     * Join a list of strings using the delimiter provided.
     * @param strings to join
     * @param delimiter to separate the strings
     * @return the joined string
     */
    public static String stringJoin(List<String> strings, String delimiter) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String string : strings) {
            if (first) {
                first = false;
            } else {
                builder.append(delimiter);
            }
            builder.append(string);
        }
        return builder.toString();
    }

    /**
     * Join a list of strings using the delimiter provided.
     * @param strings to join
     * @param delimiter to separate the strings
     * @return the joined string
     */
    public static String stringJoin(String[] strings, String delimiter) {
        return stringJoin(Arrays.asList(strings), delimiter);
    }

    /**
     * Join a list of strings returned by the {@link Object#toString()} method
     * using the delimiter provided.
     * @param objects to join the values of {@link Object#toString()}
     * @param delimiter to separate the strings
     * @return the joined string
     */
    public static String stringJoin(Iterable<?> objects, String delimiter) {
        List<String> strings = new ArrayList<>();
        for (Object object : objects) {
            strings.add(object.toString());
        }
        return stringJoin(strings, delimiter);
    }
}
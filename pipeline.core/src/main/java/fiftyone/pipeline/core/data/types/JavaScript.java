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

package fiftyone.pipeline.core.data.types;

import fiftyone.pipeline.core.data.ElementData;

/**
 * JavaScript type which can be returned as a value by an {@link ElementData}.
 * A value being of type JavaScript indicates that it is intended to be run on a
 * client browser.
 */
public class JavaScript implements Comparable<String> {

    private final String value;

    public JavaScript(String value) {
        this.value = value;
    }

    @Override
    public int compareTo(String o) {
        return value.compareTo(o);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JavaScript) {
            return value.equals(((JavaScript) obj).value);
        }
        if (obj instanceof String) {
            return value.equals(obj);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}

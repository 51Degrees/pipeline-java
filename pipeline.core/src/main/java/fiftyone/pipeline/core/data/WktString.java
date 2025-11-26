/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2025 51 Degrees Mobile Experts Limited, Davidson House,
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

import java.util.Objects;

/**
 * Well-known text representation of geometry.
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">
 * Well-known text representation of geometry</a>
 * </p>
 */
public class WktString {

    /**
     * Value that adheres to the OGC 06-103r4 standard.
     * <p>
     * See <a href="https://www.ogc.org/publications/standard/sfa/">
     * OGC Simple Feature Access</a>
     * </p>
     * <p>
     * Examples:
     * <ul>
     *     <li>{@code POINT(2 4)}</li>
     *     <li>{@code POLYGON((10 10,10 20,20 20,20 15,10 10))}</li>
     * </ul>
     * </p>
     */
    private final String value;

    /**
     * Construct a new instance of {@link WktString}.
     * @param value internal text value
     */
    public WktString(String value) {
        this.value = value;
    }

    /**
     * Get the WKT string value.
     * @return the WKT string value
     */
    public String getValue() {
        return value;
    }

    /**
     * Check if the specified value is equal to this instance.
     * @param other the value to check for equality
     * @return true if the values are equal, false otherwise
     */
    public boolean equals(WktString other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(value, other.value);
    }

    /**
     * Check if the specified value is equal to this instance.
     * @param other the value to check for equality
     * @return true if the values are equal, false otherwise
     */
    public boolean equals(String other) {
        return Objects.equals(value, other);
    }

    /**
     * Check if the specified value is equal to this instance.
     * @param obj the value to check for equality
     * @return true if the values are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof WktString) {
            return equals((WktString) obj);
        }
        if (obj instanceof String) {
            return equals((String) obj);
        }
        return false;
    }

    /**
     * Get a hash code for this instance.
     * <p>
     * The hash code is taken directly from the string representation
     * of this instance.
     * </p>
     * @return the hash code for this instance
     */
    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    /**
     * Get the string representation of this instance.
     * @return the string representation of this instance
     */
    @Override
    public String toString() {
        return value;
    }
}

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

/**
 * Return value of a 'try get' method.  This class contains an indicator as to
 * whether a valid value was returned, and the value if the indicator is true.
 * @param <T> the type of value contained in the result
 */
public class TryGetResult<T> {

    private T value = null;

    private boolean hasValue = false;

    /**
     * Get the value returned. The {@link #hasValue()} method should always be
     * called before fetching the value.
     * @return the returned value
     */
    public T getValue() {
        return value;
    }

    /**
     * Set the value which is to be returned. This also sets {@link #hasValue()}
     * to true.
     * @param value the value to set
     */
    public void setValue(T value) {
        this.value = value;
        this.hasValue = true;
    }

    /**
     * Returns true if a value has been set in this instance.
     * @return true if there is a value
     */
    public boolean hasValue() {
        return hasValue;
    }
}

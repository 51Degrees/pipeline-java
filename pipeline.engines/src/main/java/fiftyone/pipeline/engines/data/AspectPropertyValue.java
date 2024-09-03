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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.engines.exceptions.NoValueException;

/**
 * This interface can be used where engines have a property that may be
 * populated and may not.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/properties.md#null-values">Specification</a>
 * @param <T> the type of data stored within the instance
 */
public interface AspectPropertyValue<T> {

    /**
     * Indicates whether or not the instance contains a value for the property.
     * @return true if this instance contains a value, false otherwise
     */
    boolean hasValue();

    /**
     * Get the underlying value.
     * @return the underlying value
     * @throws NoValueException this exception will be thrown if the instance
     * does not contain a value
     */
    T getValue() throws NoValueException;

    /**
     * Set the underlying value.
     * @param value the value to set
     */
    void setValue(T value);

    /**
     * Get the message that will appear in the exception thrown if this instance
     * has no value.
     * @return message associated with the NoValueException
     */
    String getNoValueMessage();

    /**
     * Set the message that will appear in the exception thrown if this instance
     * has no value.
     * @param message associated with the NoValueException
     */
    void setNoValueMessage(String message);
}
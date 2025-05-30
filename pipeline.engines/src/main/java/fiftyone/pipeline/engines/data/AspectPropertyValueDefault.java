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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.engines.exceptions.NoValueException;

/**
 * This class can be used where engines have a property that may be
 * populated and may not.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/properties.md#null-values">Specification</a>
 * @param <T> the type of data stored within the instance
 */
public class AspectPropertyValueDefault<T> implements AspectPropertyValue<T> {

    boolean hasValue = false;
    private T value;
    private String noValueMessage = "The instance does not have a set value.";

    /**
     * Construct a new instance with no value set.
     */
    public AspectPropertyValueDefault() {

    }

    /**
     * Construct a new instance and set the value. This also result in the
     * 'hasValue' property being set to true.
     * @param value the value to set
     */
    public AspectPropertyValueDefault(T value) {
        setValue(value);
    }

    @Override
    public boolean hasValue() {
        return hasValue;
    }

    @Override
    public T getValue() throws NoValueException {
        if (hasValue == false) {
            throw new NoValueException(getNoValueMessage());
        }
        return value;
    }

    @Override
    public void setValue(T value) {
        this.hasValue = true;
        this.value = value;
    }

    @Override
    public String getNoValueMessage() {
        return noValueMessage;
    }

    @Override
    public void setNoValueMessage(String message) {
        this.noValueMessage = message;
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.hashCode() ;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj != null &&
            obj instanceof AspectPropertyValue &&
            this.value != null) {
            try {
                result = this.getValue()
                    .equals(((AspectPropertyValue<?>) obj).getValue());
            } catch (NoValueException e) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return value == null ? "NULL" : value.toString();
    }
}

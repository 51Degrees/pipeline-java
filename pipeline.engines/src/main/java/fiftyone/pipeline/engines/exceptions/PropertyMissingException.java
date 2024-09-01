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

package fiftyone.pipeline.engines.exceptions;

import fiftyone.pipeline.engines.services.MissingPropertyReason;

/**
 * A property missing exception is thrown by the missing property service if an
 * attempt is made to access a property that does not exist in the FlowData.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/exception-handling.md#property-missing-exception">Specification</a>
 */
public class PropertyMissingException extends RuntimeException {

    /**
     * Serializable class version number, which is used during deserialization.
     */
    private static final long serialVersionUID = -2395447752714222366L;

    private String propertyName = null;

    private MissingPropertyReason reason;

    public PropertyMissingException() {
        super();
    }

    /**
     * Constructs a new exception containing the property name, error, and
     * reason the property was not found.
     *
     * @param reason the reason for the missing property
     * @param propertyName of the missing property
     * @param message      why the property was not found
     */
    public PropertyMissingException(
        MissingPropertyReason reason,
        String propertyName,
        String message) {
        super(message);
        this.setReason(reason);
        this.propertyName = propertyName;
    }

    /**
     * Constructs a new exception containing the property name, error, and
     * reason the property was not found.
     *
     * @param reason the reason for the missing property
     * @param propertyName of the missing property
     * @param message      why the property was not found
     * @param cause        of the exception
     */
    public PropertyMissingException(
        MissingPropertyReason reason,
        String propertyName,
        String message,
        Throwable cause) {
        super(message, cause);
        this.setReason(reason);
        this.propertyName = propertyName;
    }

    /**
     * Get the name of the property that was missing.
     * @return property name
     */
    public String getPropertyName() {

        return propertyName;
    }

    /**
     * Get missing property reason.
     * @return reason for the missing property
     */
    public MissingPropertyReason getReason() {
        return reason;
    }

    /**
     * Set missing property reason.
     * @param reason the reason for the missing property
     */
    public void setReason(MissingPropertyReason reason) {
        this.reason = reason;
    }
}

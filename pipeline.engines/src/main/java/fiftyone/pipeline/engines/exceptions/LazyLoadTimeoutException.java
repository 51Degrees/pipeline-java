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

package fiftyone.pipeline.engines.exceptions;

import fiftyone.pipeline.core.data.ElementData;

/**
 * Timeout exception thrown if the lazy loading of a property value from an
 * {@link ElementData} times out.
 */
public class LazyLoadTimeoutException extends RuntimeException {

    /**
     * Serializable class version number, which is used during deserialization.
     */
    private static final long serialVersionUID = -263332472368855660L;

    /**
     * Construct a new instance
     * @param message the message to give to the exception
     * @param cause cause of the exception
     */
    public LazyLoadTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a new instance
     * @param message the message to give to the exception
     */
    public LazyLoadTimeoutException(String message) {
        super(message);
    }

    /**
     * Default constructor.
     */
    public LazyLoadTimeoutException() {
        super();
    }

}

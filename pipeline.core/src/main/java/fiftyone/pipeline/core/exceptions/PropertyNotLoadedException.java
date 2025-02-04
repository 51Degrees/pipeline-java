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

package fiftyone.pipeline.core.exceptions;

/**
 * Thrown to indicate that properties are not available yet but MAY(!) be re-requested later.
 */
public class PropertyNotLoadedException extends RuntimeException {

    /**
     * Serializable class version number, which is used during deserialization.
     */
    private static final long serialVersionUID = -8356317224796833123L;

    public PropertyNotLoadedException() {
        super();
    }

    public PropertyNotLoadedException(String message) {
        super(message);
    }

    public PropertyNotLoadedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyNotLoadedException(Throwable cause) {
        super(cause);
    }

    protected PropertyNotLoadedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

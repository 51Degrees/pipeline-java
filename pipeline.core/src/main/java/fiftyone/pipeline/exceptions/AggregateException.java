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

package fiftyone.pipeline.exceptions;

import java.util.Collection;

/**
 * Aggregation of multiple exceptions. All exceptions which caused an instance
 * of this are added to the {@link #suppressedExceptions} list and can be
 * retrieved via the {@link #getSuppressed()} method.
 */
public class AggregateException extends RuntimeException {

    /**
     * Serializable class version number, which is used during deserialization.
     */
    private static final long serialVersionUID = 4875303941913611736L;

    /**
     * Construct a new instance
     * @param message the message to give to the exception
     * @param causes multiple causes to be contained in the exception
     */
    public AggregateException(String message, Collection<Throwable> causes) {
        super(message);
        for (Throwable cause : causes) {
            super.addSuppressed(cause);
        }
    }

    /**
     * Construct a new instance with no message.
     * @param causes multiple causes to be contained in the exception
     */
    public AggregateException(Collection<Throwable> causes) {
        super();
        for (Throwable cause : causes) {
            super.addSuppressed(cause);
        }
    }
}

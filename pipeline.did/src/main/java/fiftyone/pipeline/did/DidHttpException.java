/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.did;

import java.io.IOException;

/**
 * The cloud answered a {@link DidClient} request with a status the client
 * does not map to a result, or with a body it could not read. Carries the
 * status and the body so the caller can log what the service said. An
 * {@link IOException} because, like {@link java.net.HttpRetryException}, it
 * is a failure of the exchange rather than of the caller's input.
 */
public class DidHttpException extends IOException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String body;

    /**
     * @param message    what went wrong
     * @param statusCode the HTTP status the service answered with
     * @param body       the body the service answered with
     */
    public DidHttpException(String message, int statusCode, String body) {
        super(message);
        this.statusCode = statusCode;
        this.body = body == null ? "" : body;
    }

    /** @return the HTTP status the service answered with */
    public int getStatusCode() {
        return statusCode;
    }

    /** @return the body the service answered with, never null */
    public String getBody() {
        return body;
    }
}

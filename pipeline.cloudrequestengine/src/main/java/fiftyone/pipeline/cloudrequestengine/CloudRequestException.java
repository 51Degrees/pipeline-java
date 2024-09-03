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

package fiftyone.pipeline.cloudrequestengine;

import java.util.List;
import java.util.Map;

import fiftyone.pipeline.core.exceptions.PipelineDataException;

/**
 * Exception that can be thrown by the Cloud Engines
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/exception-handling.md#cloud-request-exception">Specification</a>
 */
public class CloudRequestException extends PipelineDataException {
    /**
     * Serializable class version number, which is used during deserialization.
     */
    private static final long serialVersionUID = 2110016714691972118L;

    private int httpStatusCode;
    private Map<String, List<String>> responseHeaders;

    /** 
     * Get the HTTP status code from the response.
     * @return HTTP status code
     */
    public int getHttpStatusCode() { return httpStatusCode; }

    /** 
     * Get all HTTP headers that were present in the response.
     * @return collection of HTTP response headers
     */
    public Map<String, List<String>> getResponseHeaders() { return responseHeaders; }

    /**
     * Constructor.
     */
    public CloudRequestException() {
        super();
    }

    /**
     * Constructor
     * @param message the exception message
     */
    public CloudRequestException(String message) {
        super(message);
    }

    /**
     * Constructor
     * @param message the exception message
     * @param cause the exception which triggered this exception
     */
    public CloudRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     * @param cause the exception which triggered this exception
     */
    public CloudRequestException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor
     * @param message the exception message
     * @param httpStatusCode the status code returned in the HTTP response
     * @param responseHeaders the HTTP headers returned in the response 
     */
    public CloudRequestException(
        String message,
        int httpStatusCode,
        Map<String, List<String>> responseHeaders) {
        super(message);
        this.httpStatusCode = httpStatusCode;
        this.responseHeaders = responseHeaders;
    }
}

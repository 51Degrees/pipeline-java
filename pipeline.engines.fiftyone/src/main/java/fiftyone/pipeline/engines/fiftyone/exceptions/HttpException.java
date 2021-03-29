/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2019 51 Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY.
 *
 * This Original Work is licensed under the European Union Public Licence (EUPL) 
 * v.1.2 and is subject to its terms as set out below.
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

package fiftyone.pipeline.engines.fiftyone.exceptions;

import java.io.IOException;

/**
 * HTTP exception which can be thrown by a cloud aspect engine.
 */
public class HttpException extends IOException {

    /**
     * Serializable class version number, which is used during deserialization.
     */
    private static final long serialVersionUID = -8984394712231560002L;

    /**
     * Default constructor.
     */
    public HttpException() {
        super();
    }

    /**
     * Construct a new instance.
     * @param statusCode the status code of the HTTP request
     * @param message the message to give to the exception
     */
    public HttpException(int statusCode, String message) {
        this("HTTP response was " + statusCode + " " + message);
    }

    /**
     * Construct a new instance.
     * @param message the message to give to the exception
     */
    public HttpException(String message) {
        super(message);
    }

    /**
     * Construct a new instance.
     * @param message the message to give to the exception
     * @param cause the cause of the exception
     */
    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a new instance.
     * @param cause the cause of the exception
     */
    public HttpException(Throwable cause) {
        super(cause);
    }
}

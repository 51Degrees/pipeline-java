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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The one HTTP operation {@link DidClient} needs, so that a test can stand in
 * for the network and a caller can route the client's requests through an
 * HTTP stack of its own. The default implementation uses
 * {@link java.net.HttpURLConnection}.
 */
public interface HttpTransport {

    /**
     * Sends the request and returns whatever the server answered, whatever
     * the status. Only a failure to reach the server or read its answer is
     * an exception.
     *
     * @param request the request to send
     * @return the status and body the server answered with
     * @throws IOException if the server could not be reached or the answer
     *                     could not be read
     */
    Response send(Request request) throws IOException;

    /** An HTTP request: method, URL, headers and an optional body. */
    final class Request {

        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final byte[] body;

        /**
         * @param method  the HTTP method, {@code GET} or {@code POST}
         * @param url     the full URL to send to
         * @param headers the headers to send, copied
         * @param body    the body to send, or null for none
         */
        public Request(
                String method,
                String url,
                Map<String, String> headers,
                byte[] body) {
            this.method = Objects.requireNonNull(method, "method");
            this.url = Objects.requireNonNull(url, "url");
            this.headers = Collections.unmodifiableMap(
                new LinkedHashMap<String, String>(headers));
            this.body = body == null ? null : body.clone();
        }

        /** @return the HTTP method */
        public String getMethod() {
            return method;
        }

        /** @return the full URL */
        public String getUrl() {
            return url;
        }

        /** @return the headers, read only */
        public Map<String, String> getHeaders() {
            return headers;
        }

        /** @return a copy of the body, or null when there is none */
        public byte[] getBody() {
            return body == null ? null : body.clone();
        }
    }

    /** An HTTP response: the status code and the body as text. */
    final class Response {

        private final int statusCode;
        private final String body;

        /**
         * @param statusCode the HTTP status code
         * @param body       the body as text, empty when there was none
         */
        public Response(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
        }

        /** @return the HTTP status code */
        public int getStatusCode() {
            return statusCode;
        }

        /** @return the body as text, never null */
        public String getBody() {
            return body;
        }
    }
}

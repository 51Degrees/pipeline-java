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

package fiftyone.pipeline.web.services;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.javascriptbuilder.flowelements.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fiftyone.pipeline.core.Constants.EVIDENCE_HTTPHEADER_PREFIX;
import static fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Class that provides functionality for the 'Client side overrides' feature.
 * Client side overrides allow JavaScript running on the client device to
 * provide additional evidence in the form of cookies or query string parameters
 * to the pipeline on subsequent requests. This enables more detailed
 * information to be supplied to the application. (e.g. iPhone model for device
 * detection).
 */
public interface ClientsidePropertyServiceCore {

    /**
     * Add the JavaScript from the {@link FlowData} object to the
     * {@link HttpServletResponse}
     * @param request the {@link HttpServletRequest} containing the
     * {@link FlowData}
     * @param response the {@link HttpServletResponse} to add the JavaScript to
     * @throws IOException
     */
    void serveJavascript(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException;


    /**
     * Default implementation of the {@link ClientsidePropertyServiceCore}
     * interface.
     */
    class Default implements ClientsidePropertyServiceCore {

        /**
         * Character used by profile override logic to separate profile IDs.
         */
        protected static final char PROFILE_OVERRIDES_SPLITTER = '|';

        /**
         * Provider to get the {@link FlowData} from.
         */
        private final FlowDataProviderCore flowDataProviderCore;

        /**
         * The Pipeline in the server instance.
         */
        protected Pipeline pipeline;

        /**
         * A list of all the HTTP headers that are requested evidence for
         * elements that populate JavaScript properties.
         */
        private List<String> headersAffectingJavaScript;

        /**
         * The cache control values that will be set for the JavaScript.
         */
        private final List<String> cacheControl = Collections.singletonList(
            "max-age=0");

        /**
         * Create a new instance.
         * @param flowDataProviderCore the provider to the {@link FlowData} for
         *                             a request from
         * @param pipeline the Pipeline in the server instance
         */
        public Default(
            FlowDataProviderCore flowDataProviderCore,
            Pipeline pipeline) {
            this.flowDataProviderCore = flowDataProviderCore;
            this.pipeline = pipeline;
            if (pipeline != null) {
                init();
            }
        }

        /**
         * Initialise the service.
         */
        protected void init() {
            headersAffectingJavaScript = new ArrayList<>();
            // Get evidence filters for all elements that have
            // JavaScript properties.
            List<EvidenceKeyFilter> filters = new ArrayList<>();
            for (FlowElement element : pipeline.getFlowElements()) {
                boolean hasJavaScript = false;
                for (Object propertyObject : element.getProperties()) {
                    ElementPropertyMetaData property =
                        (ElementPropertyMetaData)propertyObject;
                    if (property.getType().equals(JavaScript.class)) {
                        hasJavaScript = true;
                        break;
                    }
                }
                if (hasJavaScript) {
                    filters.add(element.getEvidenceKeyFilter());
                }
            }

            for (EvidenceKeyFilter filter : filters) {
                // If the filter is a white list or derived type then
                // get all HTTP header evidence keys from white list
                // and add them to the headers that could affect the
                // generated JavaScript.
                if (filter.getClass().equals(EvidenceKeyFilterWhitelist.class)) {
                    EvidenceKeyFilterWhitelist whitelist = (EvidenceKeyFilterWhitelist) filter;
                    for (String key : whitelist.getWhitelist().keySet()) {
                        if (key.startsWith(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR)) {
                            headersAffectingJavaScript.add(key.substring(key.indexOf(EVIDENCE_SEPERATOR) + 1));
                        }
                    }
                }
            }
        }

        @Override
        public void serveJavascript(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
            // Get the hash code.
            FlowData flowData = flowDataProviderCore.getFlowData(request);
            // TODO: Should this use a Guid version of the hash code to
            // allow a larger key space or is int sufficient?
            int hash = flowData.generateKey(pipeline.getEvidenceKeyFilter()).hashCode();

            String ifNoneMatch = request.getHeader("If-None-Match");
            if (ifNoneMatch != null &&
                Integer.toString(hash).equals(request.getHeader("If-None-Match"))) {
                // The response hasn't changed so respond with a 304.
                response.setStatus(304);
            } else {
                JavaScriptBuilderElement builder = flowData.getPipeline().getElement(JavaScriptBuilderElement.class);
                JavaScriptBuilderData builderData = flowData.getFromElement(builder);

                // Otherwise, return the minified script to the client.
                response.getWriter().write(builderData.getJavaScript());

                setHeaders(response, hash, builderData.getJavaScript().length());
            }

        }

        /**
         * Set various HTTP headers on the JavaScript response.
         * @param response the {@link HttpServletResponse} to add the response
         *                 headers to
         * @param hash the hash to use for the ETag
         * @param contentLength the length of the returned content. This is used
         *                      for the 'Content-Length' header
         */
        private void setHeaders(
            HttpServletResponse response,
            int hash,
            int contentLength) {
            response.setContentType("application/x-javascript");
            response.setContentLength(contentLength);
            response.setStatus(200);
            response.setHeader("Cache-Control", stringJoin(cacheControl, ","));
            response.setHeader("Vary", stringJoin(headersAffectingJavaScript, ","));
            response.setHeader("ETag", Integer.toString(hash));
        }
    }
}
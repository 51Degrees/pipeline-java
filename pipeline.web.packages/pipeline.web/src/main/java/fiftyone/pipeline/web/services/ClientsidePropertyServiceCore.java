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

package fiftyone.pipeline.web.services;

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.javascriptbuilder.flowelements.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fiftyone.pipeline.core.Constants.EVIDENCE_HTTPHEADER_PREFIX;
import static fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;

import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Class that provides functionality for the 'Client side overrides' feature.
 * Client side overrides allow JavaScript running on the client device to
 * provide additional evidence in the form of cookies or query string parameters
 * to the pipeline on subsequent requests. This enables more detailed
 * information to be supplied to the application. (e.g. iPhone model for device
 * detection).
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/web-integration.md#client-side-features">Specification</a>
 */
public interface ClientsidePropertyServiceCore {

    /**
     * Add the JavaScript from the {@link FlowData} object to the
     * {@link HttpServletResponse}
     * @param request the {@link HttpServletRequest} containing the
     * {@link FlowData}
     * @param response the {@link HttpServletResponse} to add the JavaScript to
     * @throws IOException if there was a failure reading or writing to the
     * request or response
     */
    void serveJavascript(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException;

    /**
     * Add the JSON from the {@link FlowData} object to the
     * {@link HttpServletResponse}
     * @param request the {@link HttpServletRequest} containing the
     * {@link FlowData}
     * @param response the {@link HttpServletResponse} to add the JSON to
     * @throws IOException if there was a failure reading or writing to the
     * request or response
     */
    void serveJson(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException;

    /**
     * Default implementation of the {@link ClientsidePropertyServiceCore}
     * interface.
     */
    class Default implements ClientsidePropertyServiceCore {

        private enum ContentTypes {
            JavaScript,
            Json
        }

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
        private final List<String> cacheControl = Arrays.asList(
            "private",
            "max-age=1800");

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
        @SuppressWarnings("rawtypes")
        protected void init() {
            headersAffectingJavaScript = new ArrayList<>();
            // Get evidence filters for all elements.
            List<EvidenceKeyFilter> filters = new ArrayList<>();
            for (FlowElement element : pipeline.getFlowElements()) {
                filters.add(element.getEvidenceKeyFilter());
            }

            for (EvidenceKeyFilter filter : filters) {
                // If the filter is a white list or derived type then
                // get all HTTP header evidence keys from white list
                // and add them to the headers that could affect the
                // generated JavaScript.
                if (filter.getClass().equals(EvidenceKeyFilterWhitelist.class)) {
                    EvidenceKeyFilterWhitelist whitelist = (EvidenceKeyFilterWhitelist) filter;
                    for (String key : whitelist.getWhitelist().keySet()) {
                        if (key.startsWith(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR) &&
                            hasControlChar(key) == false) {
                            String header = key.substring(key.indexOf(EVIDENCE_SEPERATOR) + 1);
                            if(headersAffectingJavaScript.contains(header) == false){
                                headersAffectingJavaScript.add(header);
                            }
                        }
                    }
                }
            }
        }

        private boolean hasControlChar(String str) {
            for (char chr : str.toCharArray()) {
                if(chr <= 31) return true;
            }
            return false;
        }

        @Override
        public void serveJavascript(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
            serveContent(request, response, ContentTypes.JavaScript);
        }

        @Override
        public void serveJson(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
            serveContent(request, response, ContentTypes.Json);
        }

        public void serveContent(
            HttpServletRequest request,
            HttpServletResponse response,
            ContentTypes contentType) throws IOException {
            // Get the hash code.
            FlowData flowData = flowDataProviderCore.getFlowData(request);            
            int hash = flowData.generateKey(pipeline.getEvidenceKeyFilter()).hashCode();

            String ifNoneMatch = request.getHeader("If-None-Match");
            if (ifNoneMatch != null &&
                Integer.toString(hash).equals(request.getHeader("If-None-Match"))) {
                // The response hasn't changed so respond with a 304.
                response.setStatus(304);
            } else {
                // Otherwise, return the requested content to the client.
                String content = null;
                switch (contentType) {
                    case JavaScript:
                        JavaScriptBuilderElement jsElement = flowData.getPipeline().getElement(JavaScriptBuilderElement.class);
                        if (jsElement == null) {
                            throw new PipelineConfigurationException(
                                "Client-side JavaScript has been requested from " +
                                    "the Pipeline. However, the JavaScriptBuilderElement " +
                                    "is not present. To resolve this error, " +
                                    "either disable client-side evidence or ensure " +
                                    "the JavaScriptBuilderElement is added to " +
                                    "your Pipeline.");
                        }
                        JavaScriptBuilderData jsData = flowData.getFromElement(jsElement);
                        content = jsData == null ? null : jsData.getJavaScript();
                        break;
                    case Json:
                        JsonBuilderElement jsonElement = flowData.getPipeline().getElement(JsonBuilderElement.class);
                        if (jsonElement == null) {
                            throw new PipelineConfigurationException(
                                "JSON data has been requested from the Pipeline. " +
                                    "However, the JsonBuilderElement is not " +
                                    "present. To resolve this error, either " +
                                    "disable client-side evidence or ensure " +
                                    "the JsonBuilderElement is added to your " +
                                    "Pipeline."
                            );
                        }
                        JsonBuilderData jsonData = flowData.getFromElement(jsonElement);
                        content = jsonData == null ? null : jsonData.getJson();
                        break;
                    default:
                        break;
                }

                setHeaders(
                    response,
                    hash,
                    content == null ? 0 : content.length(),
                    contentType == ContentTypes.JavaScript ? "x-javascript" : "json");

                response.getWriter().write(content);
            }

        }

        /**
         * Set various HTTP headers on the JavaScript response.
         * @param response the {@link HttpServletResponse} to add the response
         *                 headers to
         * @param hash the hash to use for the ETag
         * @param contentLength the length of the returned content. This is used
         *                      for the 'Content-Length' header
         * @param contentType the type of content
         */
        private void setHeaders(
            HttpServletResponse response,
            int hash,
            int contentLength,
            String contentType) {
            response.setContentType("application/" + contentType);
            response.setContentLength(contentLength);
            response.setStatus(200);
            response.setHeader("Cache-Control", stringJoin(cacheControl, ","));
            response.setHeader("Vary", stringJoin(headersAffectingJavaScript, ","));
            response.setHeader("ETag", Integer.toString(hash));
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
    }
}
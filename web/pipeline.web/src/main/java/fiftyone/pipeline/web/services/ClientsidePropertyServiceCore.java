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
import fiftyone.pipeline.web.shared.data.JavaScriptData;
import fiftyone.pipeline.web.shared.flowelements.JavaScriptBundlerElement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fiftyone.pipeline.core.Constants.EVIDENCE_HTTPHEADER_PREFIX;
import static fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR;
import static fiftyone.pipeline.util.StringManipulation.stringJoin;

public interface ClientsidePropertyServiceCore {

    void serveJavascript(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException;


        class Default implements ClientsidePropertyServiceCore {
        protected static final char PROFILE_OVERRIDES_SPLITTER = '|';

        private FlowDataProviderCore flowDataProviderCore;

        protected Pipeline pipeline;

        private List<String> headersAffectingJavaScript;

        private List<String> cacheControl = Arrays.asList(
            "max-age=0");

        public Default(
            FlowDataProviderCore flowDataProviderCore,
            Pipeline pipeline) {
            this.flowDataProviderCore = flowDataProviderCore;
            this.pipeline = pipeline;
            if (pipeline != null) {
                init();
            }
        }

        protected void init() {
            headersAffectingJavaScript = new ArrayList<>();
            // Get evidence filters for all elements that have
            // JavaScript properties.
            List<EvidenceKeyFilter> filters = new ArrayList<>();
            for (FlowElement element : pipeline.getFlowElements()) {
                boolean hasJavaScript = false;
                for (ElementPropertyMetaData property : (List<ElementPropertyMetaData>) element.getProperties()) {
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
                JavaScriptBundlerElement bundler = flowData.getPipeline().getElement(JavaScriptBundlerElement.class);
                JavaScriptData bundlerData = flowData.getFromElement(bundler);

                // Otherwise, return the minified script to the client.
                response.getWriter().write(bundlerData.getJavaScript());

                setHeaders(response, hash, bundlerData.getJavaScript().length());
            }

        }

        private void setHeaders(HttpServletResponse response, int hash, int contentLength) {
            response.setContentType("application/x-javascript");
            response.setContentLength(contentLength);
            response.setStatus(200);
            response.setHeader("Cache-Control", stringJoin(cacheControl, ","));
            response.setHeader("Vary", stringJoin(headersAffectingJavaScript, ","));
            response.setHeader("ETag", Integer.toString(hash));
        }
    }
}
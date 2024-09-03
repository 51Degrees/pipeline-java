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

import fiftyone.pipeline.core.data.Evidence;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;

import static fiftyone.pipeline.core.Constants.*;

/**
 * Service used to populate the {@link Evidence} from a
 * {@link HttpServletRequest} ready for it to be processed by the Pipeline.
 * The Pipeline's {@link EvidenceKeyFilter} is used to determine what should be
 * collected from the request.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/web-integration.md#populating-evidence">Specification</a>
 */
public interface WebRequestEvidenceServiceCore {

    /**
     * Collect all the evidence needed from the request and add to the
     * {@link FlowData} instance.
     * @param flowData to get the {@link FlowData#getEvidenceKeyFilter()} from
     *                 and add the evidence to
     * @param request the {@link HttpServletRequest} to get the evidence from
     */
    void addEvidenceFromRequest(FlowData flowData, HttpServletRequest request);

    /**
     * Default implementation of the {@link WebRequestEvidenceServiceCore}
     * service.
     */
    class Default implements WebRequestEvidenceServiceCore {

        @Override
        public void addEvidenceFromRequest(
            FlowData flowData,
            HttpServletRequest request) {

            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    String evidenceKey = EVIDENCE_HTTPHEADER_PREFIX +
                        EVIDENCE_SEPERATOR +
                        headerName;
                    checkAndAdd(flowData, evidenceKey, headerValue);
                }

            }
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    String evidenceKey = EVIDENCE_COOKIE_PREFIX +
                        EVIDENCE_SEPERATOR +
                        cookie.getName();
                    checkAndAdd(flowData, evidenceKey, cookie.getValue());
                }
            }
            Enumeration<String> parameterNames = request.getParameterNames();
            if (parameterNames != null) {
                while (parameterNames.hasMoreElements()) {
                    String parameterName = parameterNames.nextElement();
                    String evidenceKey = EVIDENCE_QUERY_PREFIX +
                        EVIDENCE_SEPERATOR +
                        parameterName;
                    checkAndAdd(
                        flowData,
                        evidenceKey,
                        request.getParameter(parameterName));
                }
            }

            HttpSession session = request.getSession();
            if (session != null) {
                checkAndAdd(flowData, EVIDENCE_SESSION_KEY, session);
                Enumeration<String> sessionNames = session.getAttributeNames();
                while (sessionNames.hasMoreElements()) {
                    String sessionName = sessionNames.nextElement();
                    String sessionKey = EVIDENCE_SESSION_PREFIX +
                        EVIDENCE_SEPERATOR +
                        sessionName;
                    checkAndAdd(
                        flowData,
                        sessionKey,
                        session.getAttribute(sessionName));
                }
            }

            checkAndAdd(flowData, EVIDENCE_CLIENTIP_KEY, request.getRemoteAddr());
            
            checkAndAdd(flowData, EVIDENCE_PROTOCOL, request.getScheme());
        }

        /**
         * Check if the {@link FlowData} requires this item of evidence, and add
         * it if it does.
         * @param flowData to check and add to
         * @param key the key to check and add
         * @param value the value to add if the check passes
         */
        protected void checkAndAdd(FlowData flowData, String key, Object value) {
            if (flowData.getEvidenceKeyFilter().include(key)) {
                flowData.addEvidence(key, value);
            }
        }
    }
}

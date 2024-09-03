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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;

import javax.servlet.http.HttpServletResponse;

import static fiftyone.pipeline.engines.fiftyone.data.SetHeadersData.*;
import static fiftyone.pipeline.engines.fiftyone.flowelements.SetHeadersElement.*;

import java.util.Map;

/**
 * Service to set HTTP headers in the response
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/web-integration.md#setting-response-headers">Specification</a>
 */
public interface UACHServiceCore {

    /**
     * Set UACH response header in web response (sets Accept-CH header in response).
     * @param flowData a FlowData object
     * @param response http response
     */
    void setResponseHeaders(
    	FlowData flowData, HttpServletResponse response);
    
    /**
     * Default implementation of the {@link UACHServiceCore} service.
     */
    class Default implements UACHServiceCore {
        
		@SuppressWarnings("unchecked")
		@Override
		public void setResponseHeaders(
			FlowData flowData, HttpServletResponse response) {
			
			ElementData setHeaderData = flowData.get(SET_HEADER_ELEMENT_DATAKEY);
			if (setHeaderData != null) {
				Map<String, String> responseHeaders =
					(Map<String, String>)
						setHeaderData.get(RESPONSE_HEADER_PROPERTY_NAME);
				if (responseHeaders != null) {
					responseHeaders.forEach((k, v) -> {
						if(response.getHeader(k) != null) {
							v = response.getHeader(k) + ", " + v;
						}
						    response.setHeader(k, v);
					});
				}
			}
		}
    }
}

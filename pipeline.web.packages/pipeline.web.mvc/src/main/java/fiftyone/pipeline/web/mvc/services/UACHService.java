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

package fiftyone.pipeline.web.mvc.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.web.mvc.components.FlowDataProvider;
import fiftyone.pipeline.web.services.UACHServiceCore;

/**
 * Spring framework service for the {@link UACHServiceCore}.
 */
@Service
public interface UACHService extends UACHServiceCore {

	public void setResponseHeaders(
		HttpServletRequest request, HttpServletResponse response);
	
    @Service
    class Default extends UACHServiceCore.Default implements UACHService {
    	// Internal FlowData provider 
    	private final FlowDataProvider flowDataProvider;
    	
    	@Autowired
		public Default(FlowDataProvider flowDataProvider) {
			this.flowDataProvider = flowDataProvider;
		}
    	
    	@Override
    	public void setResponseHeaders(
    		HttpServletRequest request, HttpServletResponse response) {
    		FlowData flowData = flowDataProvider.getFlowData(request);
    		setResponseHeaders(flowData, response);
    	}

    }
}

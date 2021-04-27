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

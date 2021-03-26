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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.NoValueException;

import java.util.Arrays;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import static fiftyone.pipeline.web.Constants.*;

/**
 * Service to set UACH request headers in the response
 */
public interface UACHServiceCore {

    /**
     * Set UACH response header in web response (sets Accept-CH header in response).  
     * @param response: A Response object
     * @return A response object with Accept-CH header set in response if its value is not null
     */	
    void setResponseHeader(HttpServletResponse response);
    
    /**
     * Get response header value using set header properties from FlowData
     * @param A processed FlowData {@link FlowData} object containing setheader properties from
     * @return A concatenated string to be set in response header for UACH
     */	
    String getResponseHeaderValue(FlowData flowData);

    /**
     * Get property map from FlowData
     * @param FlowData {@link FlowData} object
     * @return HashMap containing device element properties from flowdata  
     */	
    Map<String, Object> getPropertyMap(FlowData flowData);
    
    /**
     * Default implementation of the {@link UACHServiceCore} service.
     */
    class Default implements UACHServiceCore {
        
        /**
         * Response header value containing UACH.
         */
        protected String responseHeaderValue;
        
        /**
         * Create a new instance.
         */
        public Default() {
            this.responseHeaderValue = "";
        }
        
        /**
         * Set UACH response header in web response (sets Accept-CH header in response).  
         * @param response: A Response object
         * @return A response object with Accept-CH header set in response if its value is not null
         */	
		@Override
		public void setResponseHeader(HttpServletResponse response) {
		if(responseHeaderValue.length()>0) {
			responseHeaderValue = responseHeaderValue.replace(",", ", ");
            response.setHeader(ACCEPTCH_HEADER, responseHeaderValue);
		}
        responseHeaderValue = "";
	  } 

	    /**
	     * Get response header value using set header properties from FlowData
	     * @param A processed FlowData {@link FlowData} object containing setheader properties from
	     * @return A concatenated string to be set in response header for UACH
	     */	
		@Override
		public String getResponseHeaderValue(FlowData flowData) {
			Map<String, Object> propertyMap = getPropertyMap(flowData);		
			try {				
				responseHeaderValue = checkSetHeaderPropertyValue(propertyMap, ACCEPTCH_BROWSER);
				responseHeaderValue = responseHeaderValue + checkSetHeaderPropertyValue(propertyMap, ACCEPTCH_PLATFORM);
				responseHeaderValue = responseHeaderValue + checkSetHeaderPropertyValue(propertyMap, ACCEPTCH_HARDWARE);
	            
			} catch (NoValueException e) {
				e.printStackTrace();
			} 
			
			return responseHeaderValue;
		}
		
        /**
         * Get and validate SetHeader property value.
         * @param propertyMap map contains UACH properties built from {@link FlowData}
         * @param key for SetHeaderAcceptCH property
         * @return SetHeaderAcceptCH property
         */
		@SuppressWarnings("unchecked")
		public String checkSetHeaderPropertyValue(Map<String, Object> propertyMap, String propertyKey) throws NoValueException {
			String value = "";
			if(propertyMap.containsKey(propertyKey)) {
				   AspectPropertyValue<String> property = (AspectPropertyValue<String>) propertyMap.get(propertyKey);
				   if(property.hasValue() && !Arrays.asList(EXCLUDED_VALUES_UACH).contains(property.getValue())) {
					   if(responseHeaderValue.length()>0) {
						   value = "," + property.getValue();
					   }
					   else {
						   value = property.getValue();
					   }
				   }
				   else {
					   value = "";
				   }
				}
			return value;
		}

	    /**
	     * Get property map from FlowData
	     * @param FlowData {@link FlowData} object
	     * @return HashMap containing device element properties from flowdata  
	     */
		@Override
		public Map<String, Object> getPropertyMap(FlowData flowData) {
			ElementData elementData = flowData.get(ELEMENT_DATAKEY);
			Map<String, Object> propertyMap = elementData.asKeyMap();			
			return propertyMap;
		}
    }
}

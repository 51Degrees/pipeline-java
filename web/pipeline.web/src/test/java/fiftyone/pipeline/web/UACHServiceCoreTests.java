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

package fiftyone.pipeline.web;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.web.services.UACHServiceCore;
import static fiftyone.pipeline.web.Constants.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class UACHServiceCoreTests {

    private HttpServletResponse response;
    private UACHServiceCore uachServiceCore;
    private FlowData flowData;
    private String responseHeaderValue;
    private Map<String, Object> device;
    private String expectedValue;
    
    /** Each parameter should be placed as an argument here
     * Every time runner triggers, it will pass the arguments
     * from parameters we defined in primeNumbers() method
     */
    public UACHServiceCoreTests(Map<String, Object> device, String expectedValue) {
       this.device = device;
       this.expectedValue = expectedValue;
    }
    
	@Before
    public void init() throws IOException, ServletException {
	    
        // Configure mocks
        response = mock(HttpServletResponse.class);
        uachServiceCore = spy(new UACHServiceCore.Default());
        flowData = mock(FlowData.class);
        
        doAnswer(invocationOnMock -> {
            return this.device;
        }).when(uachServiceCore).getPropertyMap(any(FlowData.class));
        
        doAnswer(invocationOnMock -> {
        	responseHeaderValue = invocationOnMock.getArgument(1);
            assertNotNull(responseHeaderValue);
            return null;
        }).when(response).setHeader(eq(ACCEPTCH_HEADER), anyString());   
        
	}
	
	@SuppressWarnings("rawtypes")
	@Parameterized.Parameters
    public static Collection input() {
		
		AspectPropertyValue<String> unknownValue = new AspectPropertyValueDefault<>("Unknown");
		AspectPropertyValue<String> nullValue = new AspectPropertyValueDefault<>("null");
		AspectPropertyValue<String> testValue = new AspectPropertyValueDefault<>("test");
		AspectPropertyValue<String> browserValue = new AspectPropertyValueDefault<>("SEC-CH-UA,SEC-CH-UA-Full-Version");
		AspectPropertyValue<String> platformValue = new AspectPropertyValueDefault<>("SEC-CH-UA-Platform,SEC-CH-UA-Platform-Version");
		AspectPropertyValue<String> hardwareValue = new AspectPropertyValueDefault<>("SEC-CH-UA-Model,SEC-CH-UA-Mobile,SEC-CH-UA-Arch");
		
        Map<String, Object> map1 = new HashMap<>();
        map1.put(ACCEPTCH_BROWSER, browserValue);
         
        Map<String, Object> map2 = new HashMap<>();
        map2.put(ACCEPTCH_BROWSER, browserValue);
        map2.put(ACCEPTCH_PLATFORM, platformValue);
        
        Map<String, Object> map3 = new HashMap<>();
        map3.put(ACCEPTCH_BROWSER, browserValue);
        map3.put(ACCEPTCH_PLATFORM, unknownValue);
        map3.put(ACCEPTCH_HARDWARE, testValue);
        
        Map<String, Object> map4 = new HashMap<>();
        map4.put(ACCEPTCH_BROWSER, nullValue);
        map4.put(ACCEPTCH_PLATFORM, testValue);
        map4.put(ACCEPTCH_HARDWARE, hardwareValue);

        return Arrays.asList(new Object[][] {
            { map1, "SEC-CH-UA, SEC-CH-UA-Full-Version" },
            { map2, "SEC-CH-UA, SEC-CH-UA-Full-Version, SEC-CH-UA-Platform, SEC-CH-UA-Platform-Version" },
            { map3, "SEC-CH-UA, SEC-CH-UA-Full-Version" },
            { map4, "SEC-CH-UA-Model, SEC-CH-UA-Mobile, SEC-CH-UA-Arch"}
        });
	}
	
    /**
     * Check that the SetResponseHeaders correctly construct response header value 
     * and adds it to the response header.
     * @throws ServletException
     * @throws IOException
     */   
    @Test
    public void UACHServiceCore_GetResponseHeaderValue() throws ServletException, IOException {
           
        uachServiceCore.getResponseHeaderValue(flowData);
        uachServiceCore.setResponseHeader(response);
        verify(response, times(1)).setHeader(
                eq(ACCEPTCH_HEADER),
                any(String.class));
        assertEquals(responseHeaderValue, this.expectedValue);
    }
    
}

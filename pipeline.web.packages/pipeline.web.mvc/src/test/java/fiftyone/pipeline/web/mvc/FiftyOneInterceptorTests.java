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

package fiftyone.pipeline.web.mvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.web.mvc.components.FiftyOneInterceptor;
import fiftyone.pipeline.web.mvc.components.FlowDataProvider;
import fiftyone.pipeline.web.mvc.configuration.FiftyOneInterceptorConfig;
import fiftyone.pipeline.web.mvc.services.ClientsidePropertyService;
import fiftyone.pipeline.web.mvc.services.FiftyOneJSService;
import fiftyone.pipeline.web.mvc.services.PipelineResultService;
import fiftyone.pipeline.web.mvc.services.UACHService;

public class FiftyOneInterceptorTests {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Object handler;
    
    private FiftyOneInterceptor interceptor;
    private PipelineResultService resultService;
    private FlowData flowData;
    
    @Before
    public void init() throws IOException {
        FiftyOneInterceptorConfig config = 
            mock(FiftyOneInterceptorConfig.class);
        when(config.getDataFilePath())
            .thenReturn("src/test/resources/Test.xml");
        
        FlowDataProvider flowDataProvider = mock(FlowDataProvider.class);
        ClientsidePropertyService clientsidePropertyService = 
            mock(ClientsidePropertyService.class);
        
        FiftyOneJSService fiftyOneJsService = mock(FiftyOneJSService.class);
        when(fiftyOneJsService.serveJS(
                any(HttpServletRequest.class), 
                any(HttpServletResponse.class)))
            .thenReturn(true);
        when(fiftyOneJsService.serveJson(
                any(HttpServletRequest.class), 
                any(HttpServletResponse.class)))
            .thenReturn(true);
        
        UACHService uachService = mock(UACHService.class);
        
        resultService = mock(PipelineResultService.class);
        flowData = mock(FlowData.class);
        
        // Mock request, response and handler object
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        handler = mock(Object.class);
        
        when(flowDataProvider.getFlowData(any(HttpServletRequest.class)))
            .thenReturn(flowData);
        
        // Create the interceptor object for testing
        interceptor = new FiftyOneInterceptor(
                config, 
                resultService, 
                flowDataProvider, 
                clientsidePropertyService, 
                fiftyOneJsService,
                uachService);
    }
    
    /**
     * Check that 'resultService.process()' is called during
     * interceptor's preHandle process
     * @throws Exception
     */
    @Test
    public void FiftyOneInterceptor_PreHandle() throws Exception {
        interceptor.preHandle(request, response, handler);
        verify(resultService, times(1))
            .process(any(HttpServletRequest.class));
    }
    
    /**
     * Check that 'close()' is called on the FlowData instance in the 
     * interceptor's afterCompletion processed
     * @throws Exception
     */
    @Test
    public void FiftyOneInterceptor_AfterCompletion() throws Exception {
        interceptor.afterCompletion(
                request, 
                response, 
                handler, 
                new Exception());
        verify(flowData, times(1))
            .close();
    }
}

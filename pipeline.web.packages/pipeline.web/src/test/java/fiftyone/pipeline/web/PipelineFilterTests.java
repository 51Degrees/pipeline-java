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

package fiftyone.pipeline.web;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static fiftyone.pipeline.engines.fiftyone.data.SetHeadersData.*;
import static fiftyone.pipeline.engines.fiftyone.flowelements.SetHeadersElement.*;
import static fiftyone.pipeline.web.Constants.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PipelineFilterTests {

    private FilterConfig config;
    private ServletContext context;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private FlowData flowData;
    private ElementData elementData;
    
	@Before
    public void init() throws IOException, ServletException {
        config = mock(FilterConfig.class);
        when(config.getInitParameter(anyString())).thenAnswer((Answer<String>) invocationOnMock -> {
            switch ((String)invocationOnMock.getArgument(0)) {
                case "config-file":
                    return "filename";
                default:
                    return null;
            }
        });
        context = mock(ServletContext.class);
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("config.xml").getFile());
        when(context.getRealPath(anyString())).thenReturn(file.getAbsolutePath());

        when(config.getServletContext()).thenReturn(context);

        // Configure mock  request and response.
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        doNothing().when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        when(request.getAttribute(eq(HTTPCONTEXT_FLOWDATA)))
            .thenAnswer((Answer<FlowData>) invocationOnMock -> flowData);
        when(request.getScheme()).thenReturn("");
        when(request.getLocalAddr()).thenReturn("");
        when(request.getRequestURL()).thenReturn(new StringBuffer(""));
        when(request.getRequestURI()).thenReturn("");
        
        Map<String, Object> setHeader = new HashMap<String, Object>();
        setHeader.put("Accept-CH", "TestAcceptCHs");
        setHeader.put("Critical-CH", "TestCriticalCHs");
        
        doAnswer(invocationOnMock -> {
            // Don't store the real data as we wont use it, and we want to verify
            // calls.
            FlowData realData = invocationOnMock.getArgument(1);
            assertNotNull(realData);
            flowData = mock(FlowData.class);
            elementData = mock(ElementData.class);
            when(elementData.get(RESPONSE_HEADER_PROPERTY_NAME)).thenReturn(setHeader);
            when(flowData.get(SET_HEADER_ELEMENT_DATAKEY)).thenReturn(elementData);
            return null;
        }).when(request).setAttribute(eq(HTTPCONTEXT_FLOWDATA), any(Object.class));       
	}

    /**
     * Check that the filter can be initialised.
     * @throws ServletException
     */
    @Test
    public void PipelineFilter_Initialise() throws ServletException {
        PipelineFilter filter = new PipelineFilter();

        filter.init(config);
    }

    /**
     * Check that the filter can process a request, and that it correctly adds
     * a processed FlowData to the request.
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void PipelineFilter_Process() throws ServletException, IOException {
        PipelineFilter filter = new PipelineFilter();

        filter.init(config);
        filter.doFilter(request, response, chain);
        verify(request, times(1)).setAttribute(
            eq(HTTPCONTEXT_FLOWDATA),
            any(FlowData.class));
        verify(response, times(1)).setHeader(
        	eq("Accept-CH"),
            any(String.class));
        verify(response, times(1)).setHeader(
            eq("Critical-CH"),
            any(String.class));
    }

    /**
     * Check that the filter correctly disposes of the FlowData after processing.
     * @throws Exception
     */
    @Test
    public void PipelineFilter_CloseFlowData() throws Exception {
        PipelineFilter filter = new PipelineFilter();

        filter.init(config);
        filter.doFilter(request, response, chain);
        verify(flowData, times(1)).close();
    }
}

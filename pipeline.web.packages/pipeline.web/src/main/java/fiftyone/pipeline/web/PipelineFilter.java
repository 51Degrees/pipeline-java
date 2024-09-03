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

import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.DataUpdateService;
import fiftyone.pipeline.engines.services.DataUpdateServiceDefault;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import fiftyone.pipeline.web.services.*;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;

import static fiftyone.pipeline.web.Constants.DEFAULT_CLIENTSIDE_ENABLED;
import static fiftyone.pipeline.web.Constants.DEFAULT_CONFIG_FILE;

/**
 * Servlet filter used to intercept HTTP requests and process them using the
 * 51Degrees Pipeline.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/web-integration.md">Specification</a>
 */
public class PipelineFilter implements Filter {
    private Pipeline pipeline;

    private WebRequestEvidenceServiceCore evidenceService;

    private PipelineResultServiceCore resultService;

    private FlowDataProviderCore flowDataProviderCore;

    private ClientsidePropertyServiceCore clientsidePropertyServiceCore;

    private FiftyOneJSServiceCore jsService;

    private UACHServiceCore uachServiceCore;
    
    FilterConfig config;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
        String configFileName = filterConfig.getInitParameter("config-file");
        if (configFileName == null || configFileName.isEmpty()) {
            configFileName = DEFAULT_CONFIG_FILE;
        }
        boolean clientsideEnabled;
        String clientsideEnabledString = filterConfig.getInitParameter("clientside-properties-enabled");
        if (clientsideEnabledString == null) {
            clientsideEnabled = DEFAULT_CLIENTSIDE_ENABLED;
        }
        else {
            clientsideEnabled = Boolean.parseBoolean(clientsideEnabledString);
        }
        
        ServletContext context = config.getServletContext();

        File configFile = new File(context.getRealPath(configFileName));
        PipelineBuilder builder = new PipelineBuilder()
            .addService(new DataUpdateServiceDefault(
                LoggerFactory.getLogger(DataUpdateService.class.getSimpleName()),
                new HttpClientDefault()));

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PipelineOptions.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            // Bind the configuration to a pipeline options instance
            PipelineOptions options = (PipelineOptions) unmarshaller.unmarshal(configFile);

            pipeline = StartupHelpers.buildFromConfiguration(builder, options, 
                clientsideEnabled, context.getContextPath());
        } catch (Exception e) {
            throw new ServletException(e);
        }

        evidenceService = new WebRequestEvidenceServiceCore.Default();
        resultService = new PipelineResultServiceCore.Default(evidenceService, pipeline);
        flowDataProviderCore = new FlowDataProviderCore.Default();
        uachServiceCore = new UACHServiceCore.Default();
        clientsidePropertyServiceCore = new ClientsidePropertyServiceCore.Default(
            flowDataProviderCore,
            pipeline);
        jsService = new FiftyOneJSServiceCore.Default(
            clientsidePropertyServiceCore,
            clientsideEnabled);
    }

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        // Populate the request properties and store against the
        // HttpContext.
        resultService.process((HttpServletRequest)request);
        
        // Get FlowData from the request.
        FlowData flowData = flowDataProviderCore.getFlowData((HttpServletRequest)request);
        
        // Set UACH response headers.
        uachServiceCore.setResponseHeaders(
        	flowData, (HttpServletResponse)response);
        
        // If 51Degrees JavaScript or JSON is being requested then serve it.
        // Otherwise continue down the filter Pipeline.
        if (jsService.serveJS((HttpServletRequest)request, (HttpServletResponse) response) == false &&
            jsService.serveJson((HttpServletRequest)request, (HttpServletResponse) response) == false) {
            chain.doFilter(request, response);//sends request to next resource
        }

        // The rest of the filters in the chain have now been called. So it is
        // time to dispose of the FlowData instance.
        try {
            flowDataProviderCore.getFlowData((HttpServletRequest) request).close();
        } catch (Exception e) {
            throw new ServletException("FlowData could not be disposed of.", e);
        }
    }

    @Override
    public void destroy() {
        try {
            pipeline.close();
        } catch (Exception e) {
            // Nothing to be done here as everything is closing anyway.
        }
    }
}

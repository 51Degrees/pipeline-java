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

package fiftyone.pipeline.web.mvc.components;

import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.DataUpdateService;
import fiftyone.pipeline.engines.services.DataUpdateServiceDefault;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import fiftyone.pipeline.web.Constants;
import fiftyone.pipeline.web.StartupHelpers;
import fiftyone.pipeline.web.mvc.configuration.FiftyOneInterceptorConfig;
import fiftyone.pipeline.web.mvc.services.ClientsidePropertyService;
import fiftyone.pipeline.web.mvc.services.FiftyOneJSService;
import fiftyone.pipeline.web.mvc.services.PipelineResultService;
import fiftyone.pipeline.web.mvc.services.UACHService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import java.io.File;

import static fiftyone.pipeline.web.Constants.CORE_JS_NAME;
import static fiftyone.pipeline.web.Constants.CORE_JSON_NAME;

/**
 * The 51Degrees middleware component.
 * This carries out the processing on the device making the request using the
 * Pipeline which has been constructed, and intercepts requests for the
 * 51Degrees JavaScript.
 */
@Component
public class FiftyOneInterceptor extends HandlerInterceptorAdapter {
    public Pipeline pipeline;

    private final PipelineResultService resultService;

    private final FlowDataProvider flowDataProvider;

    private final ClientsidePropertyService clientsidePropertyService;

    private final FiftyOneJSService fiftyOneJsService;
    
    private final UACHService uachService;

    @Autowired
    public FiftyOneInterceptor(
        FiftyOneInterceptorConfig config,
        PipelineResultService resultService,
        FlowDataProvider flowDataProvider,
        ClientsidePropertyService clientsidePropertyService,
        FiftyOneJSService fiftyOneJsService,
        UACHService uachService) throws RuntimeException {
        super();
        this.resultService = resultService;
        this.clientsidePropertyService = clientsidePropertyService;
        this.flowDataProvider = flowDataProvider;
        this.fiftyOneJsService = fiftyOneJsService;
        this.uachService = uachService;

        String configFileName = config.getDataFilePath();

        if (configFileName == null || configFileName.isEmpty()) {
            configFileName = Constants.DEFAULT_CONFIG_FILE;
        }

        File configFile = new File(configFileName);
        PipelineBuilder builder = new PipelineBuilder()
            .addService(new DataUpdateServiceDefault(
                LoggerFactory.getLogger(DataUpdateService.class.getSimpleName()),
                new HttpClientDefault()));
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PipelineOptions.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            // Bind the configuration to a pipeline options instance
            PipelineOptions options = (PipelineOptions) unmarshaller.unmarshal(configFile);
            pipeline = StartupHelpers.buildFromConfiguration(
                    builder, 
                    options, 
                    config.getClientsidePropertiesEnabled());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        resultService.setPipeline(pipeline);
        clientsidePropertyService.setPipeline(pipeline);
        fiftyOneJsService.enable(config.getClientsidePropertiesEnabled());
    }

    public static void enableClientsideProperties(ViewControllerRegistry viewControllerRegistry) {
        viewControllerRegistry.addViewController("/" + CORE_JS_NAME);
        viewControllerRegistry.addViewController("/" + CORE_JSON_NAME);
    }

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler) throws Exception {
        resultService.process(request);
        
        // Set the Client Hints request headers
        uachService.setResponseHeaders(request, response);
        
        if (fiftyOneJsService.serveJS(request,  response) == false &&
                fiftyOneJsService.serveJson(request, response) == false) {
            return super.preHandle(request, response, handler);
        }
        return false;
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception ex) throws Exception {
        try {
            flowDataProvider.getFlowData(request).close();
        } catch (Exception e) {
            throw new Exception("FlowData could not be disposed of.", e);
        }
    }

    public ClientsidePropertyService getClientsidePropertyService() {
        return clientsidePropertyService;
    }
}

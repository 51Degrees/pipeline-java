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

import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.web.mvc.components.FlowDataProvider;
import fiftyone.pipeline.web.services.ClientsidePropertyServiceCore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring framework service for the {@link ClientsidePropertyServiceCore}.
 */
@Service
public interface ClientsidePropertyService extends ClientsidePropertyServiceCore {
    void setPipeline(Pipeline pipeline);
    void serveJavascript(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException;
    void serveJson(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException;

    @Service
    class Default extends ClientsidePropertyServiceCore.Default implements ClientsidePropertyService {
        @Autowired
        public Default(FlowDataProvider flowDataProvider) {
            super(flowDataProvider, null);
        }

        @Override
        public void setPipeline(Pipeline pipeline) {
            this.pipeline = pipeline;
            init();
        }
    }
}

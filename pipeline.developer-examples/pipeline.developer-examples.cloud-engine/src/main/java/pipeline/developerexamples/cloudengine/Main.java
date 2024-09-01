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

package pipeline.developerexamples.cloudengine;

import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import pipeline.developerexamples.cloudengine.flowelements.SimpleCloudEngine;
import pipeline.developerexamples.cloudengine.flowelements.SimpleCloudEngineBuilder;

public class Main {
    private static final ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    private static final HttpClient httpClient = new HttpClientDefault();

    public static class Example {
        public void run() throws Exception {
//! [usage]
            CloudRequestEngine cloudRequestEngine =
                new CloudRequestEngineBuilder(loggerFactory, httpClient)
                    .setEndpoint("http://51degrees.com/starsign/api/")
                    .setResourceKey("cloudexample")
                    .build();

            SimpleCloudEngine ageElement =
                new SimpleCloudEngineBuilder(
                    loggerFactory)
                    .build();

            Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(cloudRequestEngine)
                .addFlowElement(ageElement)
                .build();

            String dob = "18/12/1992";

            try (FlowData flowData = pipeline.createFlowData()) {
	            flowData
	                .addEvidence("cookie.date-of-birth", dob)
	                .process();
	
	            System.out.println("With a date of birth of " +
	                dob +
	                ", your star sign is " +
	                flowData.getFromElement(ageElement).getStarSign() + ".");
            }
//! [usage]
        }
    }

    public static void main(String[] args) throws Exception {
        new Example().run();
    }
}

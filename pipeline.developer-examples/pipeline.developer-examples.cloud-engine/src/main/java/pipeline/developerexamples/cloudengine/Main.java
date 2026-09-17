/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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

    /**
     * The star sign service this example was written for, and its
     * resource key, used when nothing else is set.
     */
    private static final String STAR_SIGN_ENDPOINT =
        "https://51degrees.com/starsign/api/";
    private static final String STAR_SIGN_RESOURCE_KEY = "cloudexample";

    /**
     * The 51Degrees cloud service, used when a 51Degrees resource key is
     * set and no other endpoint is given.
     */
    private static final String FIFTYONE_CLOUD_ENDPOINT =
        "https://cloud.51degrees.com/api/v4/";

    /**
     * The cloud endpoint, taken from FOD_CLOUD_API_URL when that is set,
     * which is the variable every 51Degrees cloud example honours and the
     * one the cloud request engine builder reads by itself when no
     * endpoint is given. A host other than cloud.51degrees.com would be
     * used to (a) use an on premise web server, or (b) use a privately
     * hosted version of the 51Degrees cloud for performance reasons,
     * which is the private hosting option of the cloud service. Both run
     * the same service, so the example works unchanged. Normalised to
     * end in one slash so the builder appends its three paths to it
     * directly.
     */
    static String endpoint() {
        String value = env("FOD_CLOUD_API_URL");
        if (value == null) {
            // A resource key and an endpoint travel together, because a
            // key is only known to the service that issued it. So the
            // 51Degrees cloud is used whenever a 51Degrees resource key
            // was found, and the star sign service, with the star sign
            // key, only when there was none.
            return configuredResourceKey() == null
                ? STAR_SIGN_ENDPOINT
                : FIFTYONE_CLOUD_ENDPOINT;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "/";
    }

    /**
     * The names a 51Degrees resource key is read from, in the order they
     * are tried. Each is read as an environment variable first and then
     * as a system property, so a developer can export one and a build
     * can pass one on the command line with -D.
     */
    static final String[] RESOURCE_KEY_NAMES = {
        "_51DEGREES_RESOURCE_KEY",
        "RESOURCE_KEY",
        "_51DEGREES_RESOURCE_KEY_BESPOKE",
        "TestResourceKey",
        "SUPER_RESOURCE_KEY",
    };

    /**
     * The 51Degrees resource key that was found, or null when none of the
     * names above is set anywhere.
     */
    static String configuredResourceKey() {
        for (String name : RESOURCE_KEY_NAMES) {
            String value = env(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * The resource key the example runs with, which is a 51Degrees key
     * where one was found and the key of the star sign service where
     * none was.
     */
    static String resourceKey() {
        String value = configuredResourceKey();
        return value == null ? STAR_SIGN_RESOURCE_KEY : value;
    }

    static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(name);
        }
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public static class Example {
        public void run() throws Exception {
//! [usage]
            // The endpoint comes from FOD_CLOUD_API_URL when it is set, so
            // the example can be pointed at an on premise web server or a
            // privately hosted version of the 51Degrees cloud, and
            // otherwise at the star sign service it was written for.
            CloudRequestEngine cloudRequestEngine =
                new CloudRequestEngineBuilder(loggerFactory, httpClient)
                    .setEndpoint(endpoint())
                    .setResourceKey(resourceKey())
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
	
                String starSign =
                    flowData.getFromElement(ageElement).getStarSign();
                if (starSign == null) {
                    // The service answered but offers no star sign
                    // product, which is what the 51Degrees cloud says,
                    // because star signs are only served by the example
                    // service. The connection, the property and evidence
                    // key negotiation and the request itself all worked.
                    System.out.println("The cloud service at " + endpoint()
                        + " does not offer the star sign product, so no "
                        + "star sign is available for a date of birth of "
                        + dob + ".");
                } else {
                    System.out.println("With a date of birth of " + dob
                        + ", your star sign is " + starSign + ".");
                }
            }
//! [usage]
        }
    }

    public static void main(String[] args) throws Exception {
        new Example().run();
    }
}

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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Calls the live 51Degrees cloud service through the cloud request
 * engine, end to end.
 * <p>
 * Every other test in this module answers the engine from a mock, which
 * checks the request the engine builds and the way it reads a reply, and
 * proves nothing about the service on the other end. A field the service
 * stops writing, a header it starts refusing or an endpoint that moves
 * all leave those tests green. This one makes the three calls the engine
 * really makes, which are accessible properties and evidence keys when it
 * is built, and the JSON request when it processes, so a change in the
 * service is seen here.
 * <p>
 * {@code FOD_CLOUD_API_URL} points the engine at another host, which the
 * builder reads for itself.
 */
public class CloudRequestEngineLiveTests {

    /**
     * A real browser User-Agent, so the service has something to detect
     * rather than a string it will report as unknown.
     */
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        + "(KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36";

    @Test
    public void build_Process_ReadsDeviceDataFromTheLiveService()
            throws Exception {
        String resourceKey =
            LiveCloudKeys.find(LiveCloudKeys.RESOURCE_KEY_NAMES);
        if (resourceKey == null) {
            fail(LiveCloudKeys.noResourceKey());
        }

        CloudRequestEngineBuilder builder = new CloudRequestEngineBuilder(
            LoggerFactory.getILoggerFactory(), new HttpClientDefault())
            .setResourceKey(resourceKey);
        String licenceKey =
            LiveCloudKeys.find(LiveCloudKeys.LICENCE_KEY_NAMES);
        if (licenceKey != null) {
            builder.setLicenseKey(licenceKey);
        }

        // Building asks the service for its accessible properties and its
        // evidence keys, so a service that cannot be reached, or a key it
        // refuses, fails here.
        CloudRequestEngine engine = builder.build();

        Map<String, AccessiblePropertyMetaData.ProductMetaData> products =
            engine.getPublicProperties();
        assertNotNull(products,
            "The live cloud service returned no accessible properties at "
            + "all, which is the service or the key rather than this "
            + "package.");
        assertFalse(products.isEmpty(),
            "The live cloud service listed no products for this resource "
            + "key. A key carries the products its account holds, so this "
            + "is an entitlement problem and not a fault in the service. "
            + "Create a key at https://configure.51degrees.com.");
        assertTrue(products.containsKey("device"),
            "The live cloud service listed the products "
            + String.join(", ", products.keySet()) + " for this resource "
            + "key, and none of them is the device product this test asks "
            + "for. That is the key not carrying the device product rather "
            + "than a fault in the service.");

        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(engine)
                .build()) {
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("header.user-agent", USER_AGENT).process();

                CloudRequestData requestData = data.getFromElement(engine);
                assertNotNull(requestData,
                    "The cloud request engine put no data in the flow, so "
                    + "the request to the live cloud service produced "
                    + "nothing to read.");
                String json = requestData.getJsonResponse();
                assertNotNull(json,
                    "The live cloud service returned no JSON body for a "
                    + "request carrying a User-Agent.");

                JSONObject answer = new JSONObject(json);
                assertTrue(answer.has("device"),
                    "The live cloud service answered with " + json
                    + ", which carries no device element. The resource key "
                    + "lists the device product, so this is the service "
                    + "rather than entitlement.");
                JSONObject device = answer.getJSONObject("device");
                assertTrue(device.length() > 0,
                    "The live cloud service returned an empty device "
                    + "element for a real browser User-Agent.");
            }
        }
    }
}

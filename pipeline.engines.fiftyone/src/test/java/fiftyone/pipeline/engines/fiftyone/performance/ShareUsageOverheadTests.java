/*
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2022 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 *  (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 *  If a copy of the EUPL was not distributed with this file, You can obtain
 *  one at https://opensource.org/licenses/EUPL-1.2.
 *
 *  The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 *  amended by the European Commission) shall be deemed incompatible for
 *  the purposes of the Work and the provisions of the compatibility
 *  clause in Article 5 of the EUPL shall not apply.
 *
 *   If using the Work as, or as part of, a network application, by
 *   including the attribution notice(s) required under Article 5 of the EUPL
 *   in the end user terms of the application under an appropriate heading,
 *   such notice(s) shall fulfill the requirements of that article.
 */

package fiftyone.pipeline.engines.fiftyone.performance;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShareUsageOverheadTests {
    static Logger logger = LoggerFactory.getLogger("testLogger");
    private Pipeline pipeline;
    final private double maxOverheadPerCall = 0.25;
    private AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void Initialise() throws Exception {
        ILoggerFactory loggerFactory = mock(ILoggerFactory.class);
        Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(false);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);

        HttpClient httpClient = mock(HttpClient.class);
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(httpClient.connect(any(URL.class))).thenReturn(connection);
        when(httpClient.postData(any(HttpURLConnection.class), any(Map.class), any(byte[].class)))
            .thenReturn(null);
        when(httpClient.getResponseString(any(HttpURLConnection.class))).thenReturn(null);
        when(connection.getResponseCode()).thenReturn(200);
        when(connection.getResponseMessage()).thenReturn("");

        engine = new EmptyEngineBuilder(loggerFactory)
            .setProcessCost(0)
            .build();

        ShareUsageElement shareUsage =
            new ShareUsageBuilder(loggerFactory)
            .build();

        pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(engine)
            .addFlowElement(shareUsage)
            .build();
    }

    private double getTime(int iterations, int headers, Pipeline pipeline) {
        List<FlowData> data = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            FlowData flowData = pipeline.createFlowData();
            for (int j = 0; j < headers; j++) {
                flowData.addEvidence("header." + Integer.toString(j), j);
            }
            flowData.addEvidence("header.user-agent", "Mozilla/5.0 (iPad; U; CPU OS 3_2_1 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Mobile/7B405");
            data.add(flowData);
        }
        long start = System.currentTimeMillis();
        for (FlowData entry : data) {
            entry.process();
        }
        long end = System.currentTimeMillis();
        
        // It is not efficient to use try-with-resources
        // in this scenario, so loop through and perform
        // manual close on each FlowData
        data.forEach((f) -> { 
            try {
                f.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return ((double)end - (double)start) / (double)iterations;
    }

    @Test @Disabled
    public void ShareUsageOverhead_SingleEvidence() {
        int iterations = 10000;
        List<FlowData> data = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            FlowData flowData = pipeline.createFlowData();
            flowData.addEvidence("header.user-agent", "Mozilla/5.0 (iPad; U; CPU OS 3_2_1 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Mobile/7B405");
            data.add(flowData);
        }
        long start = System.currentTimeMillis();
        for (FlowData entry : data) {
            entry.process();
        }
        long end = System.currentTimeMillis();
        
        // It is not efficient to use try-with-resources
        // in this scenario, so loop through and perform
        // manual close on each FlowData
        data.forEach((f) -> { 
            try {
                f.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        double msOverheadPerCall = (double)(end - start) / iterations;
        logger.info("Overhead was {} millis", msOverheadPerCall);
        assertTrue("Pipeline with share usage overhead per Process call was " +
                        msOverheadPerCall + "ms. Maximum permitted is " + maxOverheadPerCall,
                    msOverheadPerCall < maxOverheadPerCall);
    }

    @Test @Disabled
    public void ShareUsageOverhead_ThousandEvidence() {
        int iterations = 1000;
        int evidenceCount = 1000;
        List<FlowData> data = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            FlowData flowData = pipeline.createFlowData();
            for (int j = 0; j < evidenceCount; j++) {
                flowData.addEvidence("header." + j, j);
            }
            flowData.addEvidence("header.user-agent", "Mozilla/5.0 (iPad; U; CPU OS 3_2_1 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Mobile/7B405");
            data.add(flowData);
        }
        long start = System.currentTimeMillis();
        for (FlowData entry : data) {
            entry.process();
        }
        long end = System.currentTimeMillis();
        
        // It is not efficient to use try-with-resources
        // in this scenario, so loop through and perform
        // manual close on each FlowData
        data.forEach((f) -> { 
            try {
                f.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        double msOverheadPerCall = (double)(end - start) / iterations;
        logger.info("Overhead was {} millis", msOverheadPerCall);
        assertTrue(
                "Pipeline with share usage overhead per Process call was " +
                        msOverheadPerCall + "ms. Maximum permitted is 10ms",
                msOverheadPerCall < 10);
    }
}

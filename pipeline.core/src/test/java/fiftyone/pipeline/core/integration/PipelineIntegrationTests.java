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

package fiftyone.pipeline.core.integration;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByFiveElement;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByTenElement;
import fiftyone.pipeline.core.testclasses.flowelements.StopElement;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class PipelineIntegrationTests {

    private static ILoggerFactory loggerFactory;

    @BeforeClass
    public static void setup() {
        loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString()))
            .thenReturn(mock(Logger.class));
    }

    @Test
    public void PipelineIntegration_SingleElement() throws Exception {
        MultiplyByFiveElement fiveElement = new MultiplyByFiveElement(mock(Logger.class));
        Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(fiveElement)
            .build();

        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence(fiveElement.evidenceKeys.get(0), 2);
    
            flowData.process();
    
            assertEquals(10, flowData.getFromElement(fiveElement).getResult());
        }
    }

    @Test
    public void PipelineIntegration_MultipleElements() throws Exception {
        MultiplyByFiveElement fiveElement = new MultiplyByFiveElement(mock(Logger.class));
        MultiplyByTenElement tenElement = new MultiplyByTenElement(mock(Logger.class));
        Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(fiveElement)
            .addFlowElement(tenElement)
            .build();

        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence(fiveElement.evidenceKeys.get(0), 2);

            flowData.process();

            assertEquals(10, flowData.getFromElement(fiveElement).getResult());
            assertEquals(20, flowData.getFromElement(tenElement).getResult());
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void PipelineIntegration_MultipleElementsParallel() throws Exception {
        MultiplyByFiveElement fiveElement = new MultiplyByFiveElement(mock(Logger.class));
        MultiplyByTenElement tenElement = new MultiplyByTenElement(mock(Logger.class));
        Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElementsParallel(new FlowElement[]{fiveElement, tenElement})
            .build();

        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence(fiveElement.evidenceKeys.get(0), 2);
    
            flowData.process();
    
            assertEquals(10, flowData.getFromElement(fiveElement).getResult());
            assertEquals(20, flowData.getFromElement(tenElement).getResult());
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void PipelineIntegration_StopFlag() throws Exception {
        // Configure the pipeline
        StopElement stopElement = new StopElement(mock(Logger.class));
        FlowElement testElement = mock(FlowElement.class);
        when(testElement.getElementDataKey()).thenReturn("test");
        when(testElement.getProperties()).thenReturn(Collections.emptyList());
        Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(stopElement)
            .addFlowElement(testElement)
            .build();

        // Create and process flow data
        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.process();
    
            // Check that the stop flag is set
            assertTrue(flowData.isStopped());
            // Check that the second element was never processed
            verify(testElement, never()).process(any(FlowData.class));
        }
    }
}

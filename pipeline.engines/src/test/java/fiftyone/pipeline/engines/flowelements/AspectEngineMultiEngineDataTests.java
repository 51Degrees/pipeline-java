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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngine;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import fiftyone.pipeline.engines.testhelpers.data.EmptyEngineData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AspectEngineMultiEngineDataTests {

    private EmptyEngine engine;
    private TestLoggerFactory loggerFactory;
    private Pipeline pipeline;

    @Before
    public void Init() {
        ILoggerFactory internalLoggerFactory = mock(ILoggerFactory.class);
        when(internalLoggerFactory.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLoggerFactory);
    }

    @After
    public void Cleanup() throws Exception {
        // Check that no errors or warnings were logged.
        for (TestLogger logger : loggerFactory.loggers) {
            logger.assertMaxErrors(0);
            logger.assertMaxWarnings(0);
        }

        engine.close();
        pipeline.close();
    }

    private void buildEngine() throws Exception {
        EmptyEngineBuilder builder = new EmptyEngineBuilder(loggerFactory);
        engine = builder.build();

        pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(engine)
            .build();
    }

    @Test
    public void MultiEngineData_SimpleTest() throws Exception {
        buildEngine();

        try (FlowData data = pipeline.createFlowData()) {
            EmptyEngineData engineData = data.getOrAdd(
                engine.getTypedDataKey(),
                engine.getDataFactory());
            engineData.setValueOne(0);
            engineData.setValueTwo(50);
            data.process();
    
            EmptyEngineData result = data.get(EmptyEngineData.class);
            assertEquals(1, result.getValueOne());
            assertEquals(2, result.getValueTwo());
        }
    }

    @Test
    public void MultiEngineData_LazyLoadingTest() throws Exception {
        buildEngine();

        try (FlowData data = pipeline.createFlowData()) {
            EmptyEngineData engineData = data.getOrAdd(
                engine.getTypedDataKey(),
                engine.getDataFactory());
            engineData.setValueOne(0);
            engineData.setValueTwo(50);
    
            data.process();
    
            EmptyEngineData result = data.get(EmptyEngineData.class);
            assertEquals(1, result.getValueOne());
            assertEquals(2, result.getValueTwo());
        }
    }
}

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
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.engines.caching.FlowCache;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngine;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import fiftyone.pipeline.engines.testhelpers.data.EmptyEngineData;
import fiftyone.pipeline.engines.testhelpers.data.MockFlowData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class AspectEngineCachedTests {

    private EmptyEngine engine;

    private TestLoggerFactory loggerFactory;

    private FlowCache cache;

    @Before
    public void Init() throws Exception {
        cache = mock(FlowCache.class);
        ILoggerFactory internalLoggerFactory = mock(ILoggerFactory.class);
        when(internalLoggerFactory.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLoggerFactory);

        engine = new EmptyEngineBuilder(loggerFactory).build();
        engine.setCache(cache);
    }

    @After
    public void Cleanup() throws Exception {
        for (TestLogger logger : loggerFactory.loggers) {
            logger.assertMaxErrors(0);
            logger.assertMaxWarnings(0);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void FlowElementCached_Process_CheckCachePut() throws Exception {
        // Arrange
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("user-agent", "1234");
        final FlowData data = MockFlowData.createFromEvidence(evidence, false);
        EmptyEngineData aspectData = new EmptyEngineData(
            mock(Logger.class),
            data,
            engine,
            MissingPropertyServiceDefault.getInstance());
        when(data.getOrAdd(
            any(TypedKey.class),
            any(FlowElement.DataFactory.class)))
            .thenReturn(aspectData);
        when(data.getFromElement(eq(engine))).thenReturn(aspectData);

        when(cache.get(any(FlowData.class))).thenReturn(null);

        // Act
        engine.process(data);

        // Assert
        // Verify that the cache was checked to see if there was an
        // existing result for this key.
        verify(cache, times(1)).get(argThat(new ArgumentMatcher<FlowData>() {
            @Override
            public boolean matches(FlowData flowData) {
                return flowData.equals(data);
            }
        }));
        // Verify that the result was added to the cache.
        verify(cache, times(1)).put(
            argThat(new ArgumentMatcher<FlowData>() {
                @Override
                public boolean matches(FlowData flowData) {
                    return flowData.equals(data);
                }
            }),
            any(EmptyEngineData.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void FlowElementCached_Process_CheckCacheGet() throws Exception {
        // Arrange
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("user-agent", "1234");
        final FlowData data = MockFlowData.createFromEvidence(evidence, false);
        final EmptyEngineData cachedData = new EmptyEngineData(
            mock(Logger.class),
            data,
            engine,
            mock(MissingPropertyService.class));
        cachedData.setValueOne(2);
        when(cache.get(any(FlowData.class))).thenReturn(cachedData);

        // Act
        engine.process(data);

        // Assert
        // Verify that the cached result was added to the flow data.
        verify(data, times(1)).getOrAdd(
            any(TypedKey.class),
            argThat(new ArgumentMatcher<FlowElement.DataFactory<? extends ElementData>>() {
                @Override
                public boolean matches(FlowElement.DataFactory<? extends ElementData> dataFactory) {
                    return dataFactory.create(data).equals(cachedData);
                }
            })
        );
        // Verify that the cache was checked once.
        verify(cache, times(1)).get(
            argThat(new ArgumentMatcher<FlowData>() {
                @Override
                public boolean matches(FlowData flowData) {
                    return flowData.equals(data);
                }
            })
        );
        // Verify that the Put method of the cache was not called.
        verify(cache, never()).put(any(FlowData.class), any(ElementData.class));
    }
}

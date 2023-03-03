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

package fiftyone.pipeline.engines.performance;

import fiftyone.caching.LruPutCache;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.caching.FlowCacheDefault;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngine;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static fiftyone.pipeline.util.StringManipulation.stringJoin;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class PipelineOverheadTests {
    static Logger logger = LoggerFactory.getLogger("testLogger");
    private Pipeline pipeline;
    private EmptyEngine engine;

    @Before
    public void Initialise() throws Exception {
        ILoggerFactory loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(mock(Logger.class));
        PipelineBuilder builder = new PipelineBuilder(loggerFactory);
        engine = new EmptyEngineBuilder(loggerFactory).build();

        pipeline = builder.addFlowElement(engine)
            .build();
    }

    @Test
    public void PipelineOverhead_NoCache() {
        int iterations = 10_000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            pipeline.createFlowData()
                .process();
        }
        long end = System.currentTimeMillis();

        double msOverheadPerCall =
            (double)(end - start) / iterations;
        logger.info("Average was {} millis", msOverheadPerCall);
        assertTrue("Pipeline overhead per Process call was " +
                msOverheadPerCall + "ms. Maximum permitted is 0.1ms",
            msOverheadPerCall < 0.1);
    }

    @Test
    public void PipelineOverhead_Cache() {
        CacheConfiguration cacheConfig = new CacheConfiguration(
            new LruPutCache.Builder(),
            100);
        engine.setCache(new FlowCacheDefault(cacheConfig));
        // Set process cost to 0.2 sec. Therefore the test cannot be passed
        // unless the cache is mitigating this cost as it should do.
        engine.setProcessCost(200);

        int iterations = 10_000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            pipeline.createFlowData()
                .addEvidence("test.value", 10)
                .process();
        }
        long end = System.currentTimeMillis();

        double msOverheadPerCall =
            (double)(end - start) / iterations;
        logger.info("Average was {} millis", msOverheadPerCall);
        assertTrue(
            "Pipeline overhead per Process call was " +
                msOverheadPerCall + "ms. Maximum permitted is 0.1ms",
            msOverheadPerCall < 0.1);
    }

    @Test
    public void PipelineOverhead_Concurrency() throws InterruptedException, ExecutionException {
        final int iterations = 10_000;
        int threads = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
        List<Callable<Long>> callables = new ArrayList<>();

        // Create the threads.
        // Each will create a FlowData instance and process it.
        for (int i = 0; i < threads; i++) {
            callables.add(
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        long start = System.currentTimeMillis();
                        for (int j = 0; j < iterations; j++) {
                            pipeline.createFlowData()
                                .process();
                        }
                        return System.currentTimeMillis() - start;
                    }
                }
            );
        }
        // Start all tasks together
        ExecutorService service = Executors.newFixedThreadPool(threads);
        List<Future<Long>> results = service.invokeAll(callables);

        // Wait for tasks to complete.
        // Calculate the time per call from the task results.
        List<String> times = new ArrayList<>();
        int overran = 0;
        for (Future<Long> result : results) {
            Double time = (double) result.get() / iterations;
            if (time >= 0.1) {
                overran++;
            }
            times.add(time.toString());
        }
        logger.info("Times were {}", stringJoin(times, ","));
        assertTrue(
            "Pipeline overhead per Process call was too high for " +
                overran + " out of " + threads + "threads. Maximum permitted " +
                "is 0.1. Actual results: " + stringJoin(times, ","),
            overran == 0);
    }
}

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

package fiftyone.pipeline.engines.performance;

import fiftyone.caching.LruPutCache;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.caching.FlowCacheDefault;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngine;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static fiftyone.pipeline.util.StringManipulation.stringJoin;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PipelineOverheadTests {
    static Logger logger = LoggerFactory.getLogger("testLogger");
    private Pipeline pipeline;
    private EmptyEngine engine;
    final private double maxOverheadPerCallMillis = 0.1;

    static class TestCallable implements Callable<Long> {
        final int iterations;
        final Pipeline pipeline;
        final Map<String, Object> evidence;

        TestCallable(Pipeline pipeline, int iterations) {
            this(pipeline, iterations, null);
        }
        TestCallable(Pipeline pipeline, int iterations, Map<String, Object> evidence) {
            this.pipeline = pipeline;
            this.iterations = iterations;
            this.evidence = evidence;
        }

        @Override
        public Long call() throws Exception {
            long start = System.currentTimeMillis();
            for (int j = 0; j < iterations; j++) {
                try (FlowData flowData = pipeline.createFlowData()) {
                    if (Objects.nonNull(this.evidence)) {
                        flowData.addEvidence(this.evidence);
                    }
                    flowData.process();
                }
            }
            return System.currentTimeMillis() - start;
        }
    }


    @Before
    public void Initialise() throws Exception {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        PipelineBuilder builder = new PipelineBuilder(loggerFactory);
        engine = new EmptyEngineBuilder(loggerFactory)
                .build();

        pipeline = builder.addFlowElement(engine)
            .build();
    }

    @After
    public void tearDown() throws Exception {
        pipeline.close();
        engine.close();
    }

    @Test
    public void PipelineOverhead_NoCache() throws Exception {
        int iterations = 10_000;
        long result = new TestCallable(pipeline, iterations).call();
        double msOverheadPerCall = (double) result / iterations;
        logger.info("Process cost {}", engine.getProcessCost());
        logger.info("Average was {} millis", msOverheadPerCall);
        assertTrue("Pipeline overhead per Process call was " +
                msOverheadPerCall + "ms. Maximum permitted is " + maxOverheadPerCallMillis,
            msOverheadPerCall < maxOverheadPerCallMillis);
    }

    @Test
    public void PipelineOverhead_Cache() throws Exception {
        CacheConfiguration cacheConfig = new CacheConfiguration(
            new LruPutCache.Builder(),100);
        engine.setCache(new FlowCacheDefault(cacheConfig));
        // Set process cost to 0.2 sec. Therefore, the test cannot pass
        // unless the cache is mitigating this cost as it should do.
        engine.setProcessCost(200);
        int iterations = 10_000;
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("test.value", 10);
        long result = new TestCallable(pipeline, iterations, evidence).call();

        double msOverheadPerCall = (double)(result) / iterations;
        logger.info("Process cost {}", engine.getProcessCost());
        logger.info("Average was {} millis", msOverheadPerCall);
        assertTrue(
            "Pipeline overhead per Process call was " +
                msOverheadPerCall + "ms. Maximum permitted is " + maxOverheadPerCallMillis,
            msOverheadPerCall < maxOverheadPerCallMillis);
    }

    @Test
    public void PipelineOverhead_Concurrency() throws InterruptedException, ExecutionException {
        // use half the available processors, we don't know what the rest are doing
        int threads = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
        final int iterations = 800_000;
        List<Callable<Long>> callables = new ArrayList<>();

        // Create the threads.
        // Each will create a FlowData instance and process it.
        for (int i = 0; i < threads; i++) {
            callables.add(new TestCallable(pipeline, iterations));
        }
        // Start all tasks together
        ExecutorService service = Executors.newFixedThreadPool(threads);
        List<Future<Long>> results = service.invokeAll(callables);

        // Wait for tasks to complete.
        // Calculate the time per call from the task results.
        List<String> times = new ArrayList<>();
        int overran = 0;
        for (Future<Long> result : results) {
            double time = (double) result.get() / iterations;
            if (time >= maxOverheadPerCallMillis) {
                overran++;
            }
            times.add(Double.toString(time));
        }
        logger.info("Process cost {}", engine.getProcessCost());
        logger.info("Times were {}", stringJoin(times, ","));
        assertEquals(
            "Pipeline overhead per Process call was too high for " +
                overran + " out of " + threads + "threads. Maximum permitted " +
                "is "+ maxOverheadPerCallMillis +". Actual results: " + stringJoin(times, ","),
            0, overran);
    }
}

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
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.engines.configuration.ExecutorServiceFactory;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.exceptions.LazyLoadTimeoutException;
import fiftyone.pipeline.engines.testhelpers.data.EmptyEngineData;
import fiftyone.pipeline.engines.testhelpers.data.MockFlowData;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngine;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import fiftyone.pipeline.exceptions.AggregateException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unused", "unchecked"})
public class AspectEngineLazyLoadTests {
    private EmptyEngine engine;

    private TestLoggerFactory loggerFactory;

    private int timeoutMillis = 1000;

    private ExecutorService executor;

    @Before
    public void Init() throws Exception {
        executor = Executors.newSingleThreadExecutor();
        ILoggerFactory internalLoggerFactory = mock(ILoggerFactory.class);
        when(internalLoggerFactory.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLoggerFactory);
        engine = new EmptyEngineBuilder(loggerFactory)
            .setLazyLoading(new LazyLoadingConfiguration(
                timeoutMillis,
                new ExecutorServiceFactory() {
                    @Override
                    public ExecutorService create() {
                        return executor;
                    }
                }))
            .build();
    }

    @After
    public void Cleanup() {
        // Check that no errors or warnings were logged.
        for (TestLogger logger : loggerFactory.loggers) {
            logger.assertMaxErrors(0);
            logger.assertMaxWarnings(0);
        }
        executor.shutdownNow();
    }

    @Test
    public void AspectEngineLazyLoad_Process() throws Exception {
        // Arrange
        // Set the process time to half of the configured timeout
        engine.setProcessCost(timeoutMillis / 2);

        // Act
        Map<String, Object> evidence =  new HashMap<>();
        evidence.put("user-agent", "1234");

        final FlowData mockData = MockFlowData.createFromEvidence(evidence, false);
        // Use the mock flow data to populate this variable with the
        // engine data from the call to process.
        final EmptyEngineData[] engineData = {null};
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FlowElement.DataFactory<EmptyEngineData> factory =
                    invocationOnMock.getArgument(1);
                engineData[0] = factory.create(mockData);
                return engineData[0];
            }
        }).when(mockData).getOrAdd(
            any(TypedKey.class),
            any(FlowElement.DataFactory.class));

        // Process the data.
        engine.process(mockData);
        // Check that the task is not complete when process returns.
        boolean processReturnedBeforeTaskComplete =
            engineData[0].getProcessFuture().isDone() == false;

        // Assert
        assertEquals(1, engineData[0].getValueOne());
        // Check that the task is now complete (because the code to get
        // the property value will wait until it is complete)
        boolean valueReturnedAfterTaskComplete =
            engineData[0].getProcessFuture().isDone() == true;
        assertTrue(processReturnedBeforeTaskComplete);
        assertTrue(valueReturnedAfterTaskComplete);
    }

    @Test(expected = LazyLoadTimeoutException.class)
    public void AspectEngineLazyLoad_PropertyTimeout() throws Exception {
        // Arrange
        // Set the process time to double the configured timeout
        engine.setProcessCost(timeoutMillis * 2);

        // Act
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("user-agent", "1234");

        // Use the mock flow data to populate this variable with the
        // engine data from the call to process.
        final FlowData mockData = MockFlowData.createFromEvidence(evidence, false);
        final EmptyEngineData[] engineData = {null};
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FlowElement.DataFactory<EmptyEngineData> factory =
                    invocationOnMock.getArgument(1);
                engineData[0] = factory.create(mockData);
                return engineData[0];
            }
        }).when(mockData).getOrAdd(
            any(TypedKey.class),
            any(FlowElement.DataFactory.class));

        // Process the data
        engine.process(mockData);
        // Attempt to get the value. This should cause the timeout
        // to be triggered.
        int result = engineData[0].getValueOne();

        // No asserts needed. Just the ExpectedException attribute
        // on the method.
    }

    @Test(expected = CancellationException.class)
    public void AspectEngineLazyLoad_ProcessCancelled() throws Exception {
        // Arrange
        // Set the process time to half the configured timeout
        engine.setProcessCost(timeoutMillis/ 2);

        // Act
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("user-agent", "1234");
        // Use the mock flow data to populate this variable with the
        // engine data from the call to process.
        final FlowData mockData = MockFlowData.createFromEvidence(evidence, false);
        final EmptyEngineData[] engineData = {null};
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FlowElement.DataFactory<EmptyEngineData> factory =
                    invocationOnMock.getArgument(1);
                engineData[0] = factory.create(mockData);
                return engineData[0];
            }
        }).when(mockData).getOrAdd(
            any(TypedKey.class),
            any(FlowElement.DataFactory.class));

        // Process the data
        engine.process(mockData);
        // Trigger a cancellation.
        mockData.stop();
        engineData[0].getProcessFuture().cancel(true);

        // Attempt to get the value.
        int result = engineData[0].getValueOne();
    }

    @Test
    public void AspectEngineLazyLoad_ProcessErrored() throws Exception {
        // Arrange
        // Set the engine to throw an exception while processing
        String exceptionMessage = "an exception message";
        engine.setException(new Exception(exceptionMessage));
        // Act
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("user-agent", "1234");
        // Use the mock flow data to populate this variable with the
        // engine data from the call to process.
        final FlowData mockData = MockFlowData.createFromEvidence(evidence, false);
        final EmptyEngineData[] engineData = {null};
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FlowElement.DataFactory<EmptyEngineData> factory =
                    invocationOnMock.getArgument(1);
                engineData[0] = factory.create(mockData);
                return engineData[0];
            }
        }).when(mockData).getOrAdd(
            any(TypedKey.class),
            any(FlowElement.DataFactory.class));

        // Process the data
        engine.process(mockData);

        // Attempt to get the value.
        try {
            int result = engineData[0].getValueOne();
        }
        catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
            assertTrue(e.getCause() != null);
            assertEquals("java.lang.Exception: " + exceptionMessage, e.getCause().getMessage());
        }
    }

    @Test
    public void AspectEngineLazyLoad_ProcessMultipleErrored() throws Exception {
        // Arrange
        // Set the engine to throw an exception while processing
        String exceptionMessage = "an exception message";
        EmptyEngine engine2 = new EmptyEngineBuilder(loggerFactory)
            .setLazyLoading(new LazyLoadingConfiguration(
                timeoutMillis,
                new ExecutorServiceFactory() {
                    @Override
                    public ExecutorService create() {
                        return executor;
                    }
                }))
            .build();
        engine.setException(new Exception(exceptionMessage));
        engine2.setException(new Exception(exceptionMessage));
        // Act
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("user-agent", "1234");
        // Use the mock flow data to populate this variable with the
        // engine data from the call to process.
        final FlowData mockData = MockFlowData.createFromEvidence(evidence, false);
        final EmptyEngineData[] engineData = {null};
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FlowElement.DataFactory<EmptyEngineData> factory =
                    invocationOnMock.getArgument(1);
                if (engineData[0] == null) {
                    engineData[0] = factory.create(mockData);
                }
                return engineData[0];
            }
        }).when(mockData).getOrAdd(
            any(TypedKey.class),
            any(FlowElement.DataFactory.class));

        // Process the data twice
        engine.process(mockData);
        engine2.process(mockData);

        // Attempt to get the value.
        try {
            int result = engineData[0].getValueOne();
        }
        catch (Exception e) {
            assertTrue(e instanceof AggregateException);
            assertEquals(2, e.getSuppressed().length);
            assertEquals("java.lang.Exception: " + exceptionMessage, e.getSuppressed()[0].getMessage());
            assertEquals("java.lang.Exception: " + exceptionMessage, e.getSuppressed()[0].getMessage());
        }
    }
}

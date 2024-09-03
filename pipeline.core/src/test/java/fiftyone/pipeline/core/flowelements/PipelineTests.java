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

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.factories.FlowDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.core.services.PipelineService;
import fiftyone.pipeline.core.testclasses.data.TestElementData;
import fiftyone.pipeline.exceptions.AggregateException;
import fiftyone.pipeline.core.testclasses.services.TestCloseableService;
import fiftyone.pipeline.core.testclasses.services.TestAutoCloseableService;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("rawtypes")
public class PipelineTests {

    private static FlowElement getMockFlowElement() {
        FlowElement element = mock(FlowElement.class);
        when(element.getElementDataKey()).thenReturn("test");
        when(element.getProperties()).thenReturn(Collections.emptyList());
        return element;
    }

    @Test
    public void Pipeline_Process_SequenceOfTwo() throws Exception {
        // Arrange
        FlowElement element1 = getMockFlowElement();
        FlowElement element2 = getMockFlowElement();

        // Configure the elements
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                TestElementData tempData = ((FlowData) invocationOnMock.getArgument(0))
                    .getOrAdd(
                        "element1",
                        new FlowElement.DataFactory<TestElementData>() {
                            @Override
                            public TestElementData create(FlowData flowData) {
                                return new TestElementData(mock(Logger.class), flowData);
                            }
                        });
                tempData.put("key", "done");
                return null;
            }
        }).when(element1).process(any(FlowData.class));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                TestElementData tempData = ((FlowData) invocationOnMock.getArgument(0))
                    .getOrAdd(
                        "element2",
                        new FlowElement.DataFactory<TestElementData>() {
                            @Override
                            public TestElementData create(FlowData flowData) {
                                return new TestElementData(mock(Logger.class), flowData);
                            }
                        });
                tempData.put("key", "done");
                return null;
            }
        }).when(element2).process(any(FlowData.class));

        // Create the pipeline
        PipelineInternal pipeline = createPipeline(
            false,
            false,
            new FlowElement[]{element1, element2});
        // Don't create the flow data via the pipeline as we just want
        // to test Process.
        FlowData data = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)));

        // Act
        data.process();

        // Assert
        assertTrue("Expected no errors",
            data.getErrors() == null || data.getErrors().size() == 0);
        // Check that the resulting data has the expected values
        assertTrue("data from element 1 is missing in the result",
            data.getDataKeys().contains("element1"));
        assertTrue("data from element 2 is missing in the result", data.getDataKeys().contains("element2"));
        assertEquals("done", data.get("element1").get("key").toString());
        assertEquals("done", data.get("element2").get("key").toString());
        // Check that element 1 was called before element 2.
        verify(element2, times(1)).process(argThat(new ArgumentMatcher<FlowData>() {
            @Override
            public boolean matches(FlowData flowData) {
                return flowData.getDataKeys().contains("element1");
            }
        }));
    }

    @Test
    public void Pipeline_Dispose() throws Exception {
        // Arrange
        FlowElement element1 = getMockFlowElement();
        FlowElement element2 = getMockFlowElement();
        
        //  Services
        PipelineService service1 = mock(TestCloseableService.class);
        PipelineService service2 = mock(TestAutoCloseableService.class);

        // Create the pipeline
        try(PipelineInternal pipeline = createPipeline(
            true,
            false,
            new FlowElement[]{element1, element2})) {
        
        	// Add services
        	pipeline.addService(service1);
        	pipeline.addService(service2);
        }

        // Assert
        verify(element1, times(1)).close();
        verify(element2, times(1)).close();
        verify((TestCloseableService)service1, times(1)).close();
        verify((TestAutoCloseableService)service2, times(1)).close();
    }
    
    @Test
    public void Pipeline_AddService() throws Exception {
    	try (Pipeline pipeline = createPipeline(false, false, new FlowElement[] {})) {
    		// Test 1 service
    		PipelineService service1 = new PipelineService() {};
    		pipeline.addService(service1);
    		List<PipelineService> services = pipeline.getServices();
    		assertEquals(1, services.size());
    		assertTrue(services.contains(service1));
    	
    		// Test 2 service
    		PipelineService service2 = new PipelineService() {};
    		pipeline.addService(service2);
    		assertEquals(2, services.size());
    		assertTrue(services.contains(service1));
    		assertTrue(services.contains(service2));
    	}
    }
    
    @Test
    public void Pipline_AddServices() throws Exception {
    	try (Pipeline pipeline = createPipeline(false, false, new FlowElement[] {})) {
    		List<PipelineService> testServices = new ArrayList<>();
    		testServices.add(new PipelineService() {});
    		testServices.add(new PipelineService() {});
    		pipeline.addServices(testServices);
    		
    		List<PipelineService> services = pipeline.getServices();
    		assertEquals(2, services.size());
    		testServices.forEach((s) -> {
    			assertTrue(services.contains(s));
    		});
    	}
    }

    @Test
    public void Pipeline_GetKeys() {
        FlowElement element1 = getMockFlowElement();
        FlowElement element2 = getMockFlowElement();
        // Configure the elements to return one key each
        when(element1.getEvidenceKeyFilter()).thenReturn(
            new EvidenceKeyFilterWhitelist(Arrays.asList("key1")));
        when(element2.getEvidenceKeyFilter()).thenReturn(
            new EvidenceKeyFilterWhitelist(Arrays.asList("key2")));

        // Create the pipeline
        PipelineInternal pipeline = createPipeline(
            false,
            false,
            new FlowElement[]{element1, element2});

        // Get the keys from the pipeline
        EvidenceKeyFilter result = pipeline.getEvidenceKeyFilter();

        // Check that the result is as expected.
        assertTrue(result.include("key1"));
        assertTrue(result.include("key2"));
        assertFalse(result.include("key3"));
    }

    @Test
    public void Pipeline_ExceptionDuringProcessingDontSuppress() {
        FlowElement element1 = getMockFlowElement();
        List<FlowError> errors = Arrays.asList(
            (FlowError) new FlowError.Default(new Exception("Test"), element1));
        FlowData data = mock(FlowData.class);

        // Configure the flow data to return errors.
        when(data.getErrors()).thenReturn(errors);

        // Create the pipeline
        PipelineInternal pipeline = createPipeline(
            false,
            true,
            new FlowElement[]{element1});

        // Start processing
        pipeline.process(data);
    }

    @Test
    public void Pipeline_ExceptionDuringProcessingSuppress() {
        FlowElement element1 = getMockFlowElement();
        List<FlowError> errors = Arrays.asList(
            (FlowError) new FlowError.Default(new Exception("Test"), element1));
        FlowData data = mock(FlowData.class);

        // Configure the flow data to return errors.
        when(data.getErrors()).thenReturn(errors);

        // Create the pipeline
        PipelineInternal pipeline = createPipeline(
            false,
            false,
            new FlowElement[]{element1});

        // Start processing
        Exception exception = null;
        try {
            pipeline.process(data);
        } catch (Exception e) {
            exception = e;
        }

        // Check that the correct exception was thrown.
        assertNotNull(
            "The exception did not bubble up to be thrown by the process" +
                "method.",
            exception);
        assertTrue(
            "An exception of type '" +
                exception.getClass().getSimpleName() + "' was thrown, the type" +
                "the type should have been '" +
                RuntimeException.class.getSimpleName() + "'.",
            exception instanceof AggregateException);
        assertEquals(
            "The incorrect number of inner exceptions were added.",
            1,
            exception.getSuppressed().length);
        assertEquals(
            "The correct exception message was not thrown.",
            "Test",
            exception.getSuppressed()[0].getMessage());
    }

    @Test
    public void Pipeline_ExceptionDuringProcessingAdd() throws Exception {
        FlowElement element1 = getMockFlowElement();
        final FlowElement element2 = getMockFlowElement();
        FlowData data = mock(FlowData.class);

        // Configure element 2 to throw an exception.

        doThrow(new Exception("Test")).when(element2).process(any(FlowData.class));

        // Create the pipeline
        PipelineInternal pipeline = createPipeline(
            false,
            true,
            new FlowElement[]{element1, element2});

        // Start processing
        pipeline.process(data);

        // Check that add error was called with the expected exception
        // and flow element.
        verify(data, times(1)).addError(
            argThat(new ArgumentMatcher<Throwable>() {
                @Override
                public boolean matches(Throwable throwable) {
                    return throwable.getMessage().equals("Test");
                }
            }),
            eq(element2));
    }


    @Test
    @SuppressWarnings("unused")
    public void Pipeline_GetPropertyMetaData() {
        FlowElement element1 = getMockFlowElement();
        FlowElement element2 = getMockFlowElement();
        when(element1.getProperties()).thenReturn(Arrays.asList(
            new ElementPropertyMetaDataDefault("testproperty", element1, "", String.class, true),
            new ElementPropertyMetaDataDefault("anotherproperty", element1, "", String.class, true)));
        when(element2.getProperties()).thenReturn(Arrays.asList(
            new ElementPropertyMetaDataDefault("testproperty2", element2, "", String.class, true),
            new ElementPropertyMetaDataDefault("anotherproperty2", element2, "", String.class, true)
        ));
        FlowData data = mock(FlowData.class);

        // Create the pipeline
        PipelineInternal pipeline = createPipeline(
            false,
            true,
            new FlowElement[]{element1, element2});

        // Get the requested property meta data
        ElementPropertyMetaData metadata = pipeline.getMetaDataForProperty("testproperty");

        assertNotNull(metadata);
        assertEquals("testproperty", metadata.getName());
    }


    @SuppressWarnings("unused")
    @Test(expected = PipelineDataException.class)
    public void Pipeline_GetPropertyMetaData_None() {
        FlowElement element1 = getMockFlowElement();
        FlowElement element2 = getMockFlowElement();
        FlowData data = mock(FlowData.class);

        // Create the pipeline
        PipelineInternal pipeline = createPipeline(
            false,
            true,
            new FlowElement[]{element1, element2});

        // Get the requested property meta data
        ElementPropertyMetaData metadata = pipeline.getMetaDataForProperty("noproperty");
    }

    @SuppressWarnings("unused")
    @Test(expected = PipelineDataException.class)
    public void Pipeline_GetPropertyMetaData_Multiple() {
        FlowElement element1 = getMockFlowElement();
        when(element1.getProperties()).thenReturn(Arrays.asList(
            new ElementPropertyMetaDataDefault("testproperty", element1, "", String.class, true)
        ));
        FlowElement element2 = getMockFlowElement();
        when(element2.getProperties()).thenReturn(Arrays.asList(
            new ElementPropertyMetaDataDefault("testproperty", element2, "", String.class, true)
        ));
        FlowData data = mock(FlowData.class);

        // Create the pipeline
        PipelineInternal pipeline = createPipeline(
            false,
            true,
            new FlowElement[]{element1, element2});

        // Get the requested property meta data
        ElementPropertyMetaData metaData = pipeline.getMetaDataForProperty("testproperty");
    }

    @Test
    public void Pipeline_GetPropertyMetaData_Concurrent() 
        throws InterruptedException {
        FlowElement element1 = getMockFlowElement();
        when(element1.getProperties()).thenReturn(Arrays.asList(
            new ElementPropertyMetaDataDefault("testproperty", element1, "", String.class, true)
        ));

        int repeatLimit = 100;
        // This test can just happen to work correctly by chance so
        // we repeat it 100 times in order to try and make sure
        // we eliminate the element of chance.
        for (int repeatCount = 0; repeatCount < repeatLimit; repeatCount++)
        {
            // Create the pipeline
            PipelineInternal pipeline = createPipeline(
                false,
                true,
                new FlowElement[]{element1});

            // Get the requested property meta data on two
            // threads simultaneously.
            int threadCount = 2;
            ExecutorService service = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            for (int i = 0; i < threadCount; i++)
            {
                service.execute(() -> {
                    try {
                        pipeline.getMetaDataForProperty("testproperty");
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
        }
    }


    private PipelineInternal createPipeline(
        boolean autoDispose,
        boolean suppressExceptions,
        FlowElement[] flowElements) {
        return new PipelineDefault(
            mock(Logger.class),
            Arrays.asList(flowElements),
            new FlowDataFactory() {
                @Override
                public FlowData create(Pipeline pipeline) {
                    return new FlowDataDefault(
                        mock(Logger.class),
                        pipeline,
                        new EvidenceDefault(mock(Logger.class)));
                }
            },
            autoDispose,
            suppressExceptions);
    }
}

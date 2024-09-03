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

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.testclasses.data.TestElementData;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.mockito.Mockito.*;

@SuppressWarnings("rawtypes")
public class ParallelElementsTests {

    private ParallelElements parallelElements;

    private PipelineInternal pipeline;

    public ParallelElementsTests() {
        pipeline = mock(PipelineInternal.class);
    }

    @Test
    public void ParallelElements_ThreeElements_ValidateParallel() throws Exception {
        assumeFalse(
            "This test cannot be run on a machine with less that 4 processing cores",
            Runtime.getRuntime().availableProcessors() < 4);

        when(pipeline.isConcurrent()).thenReturn(true);
        // Arrange
        FlowElement element1 = mock(FlowElement.class);
        FlowElement element2 = mock(FlowElement.class);
        FlowElement element3 = mock(FlowElement.class);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                TestElementData tempData = ((FlowData) invocationOnMock.getArgument(0))
                    .getOrAdd("element1", new FlowElement.DataFactory<TestElementData>() {
                        @Override
                        public TestElementData create(FlowData flowData) {
                            return new TestElementData(mock(Logger.class), flowData);
                        }
                    });
                tempData.put("start", new Date());
                Date end = new Date(System.currentTimeMillis() + 1000);
                while (end.after(new Date())) {
                }
                tempData.put("end", new Date());
                return null;
            }
        }).when(element1).process(any(FlowData.class));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                TestElementData tempData = ((FlowData) invocationOnMock.getArgument(0))
                    .getOrAdd("element2", new FlowElement.DataFactory<TestElementData>() {
                        @Override
                        public TestElementData create(FlowData flowData) {
                            return new TestElementData(mock(Logger.class), flowData);
                        }
                    });
                tempData.put("start", new Date());
                Date end = new Date(System.currentTimeMillis() + 1000);
                while (end.after(new Date())) {
                }
                tempData.put("end", new Date());
                return null;
            }
        }).when(element2).process(any(FlowData.class));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                TestElementData tempData = ((FlowData) invocationOnMock.getArgument(0))
                    .getOrAdd("element3", new FlowElement.DataFactory<TestElementData>() {
                        @Override
                        public TestElementData create(FlowData flowData) {
                            return new TestElementData(mock(Logger.class), flowData);
                        }
                    });
                tempData.put("start", new Date());
                Date end = new Date(System.currentTimeMillis() + 1000);
                while (end.after(new Date())) {
                }
                tempData.put("end", new Date());
                return null;
            }
        }).when(element3).process(any(FlowData.class));

        parallelElements = new ParallelElements(
            mock(Logger.class),
            Arrays.asList(element1,
                element2,
                element3));
        FlowData data = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)));
        data.process();

        // Act
        parallelElements.process(data);

        List<Date> startTimes = new ArrayList<>();
        startTimes.add((Date) data.get("element1").get("start"));
        startTimes.add((Date) data.get("element2").get("start"));
        startTimes.add((Date) data.get("element3").get("start"));
        List<Date> endTimes = new ArrayList<>();
        endTimes.add((Date) data.get("element1").get("end"));
        endTimes.add((Date) data.get("element2").get("end"));
        endTimes.add((Date) data.get("element3").get("end"));

        // Assert
        assertTrue("Expected no errors",
            data.getErrors() == null || data.getErrors().size() == 0);
        for (Date start : startTimes) {
            for (Date end : endTimes) {
                assertTrue(
                    "The elements were not processed sequentially. Times were: " +
                        startTimes.get(0) + " => " + endTimes.get(0) + "\n" +
                        startTimes.get(1) + " => " + endTimes.get(1) + "\n" +
                        startTimes.get(2) + " => " + endTimes.get(2),
                    start.before(end));
            }
        }
    }

    @Test
    public void ParallelElements_GetKeys() {
        FlowElement element1 = mock(FlowElement.class);
        FlowElement element2 = mock(FlowElement.class);
        // Configure the elements to return one key each
        when(element1.getEvidenceKeyFilter()).thenReturn(
            new EvidenceKeyFilterWhitelist(Arrays.asList("key1")));
        when(element2.getEvidenceKeyFilter()).thenReturn(
            new EvidenceKeyFilterWhitelist(Arrays.asList("key2")));

        parallelElements = new ParallelElements(
            mock(Logger.class),
            Arrays.asList(
                element1,
                element2));
        // Get the keys.
        EvidenceKeyFilter result = parallelElements.getEvidenceKeyFilter();

        // Check that the result is as expected.
        assertTrue(result.include("key1"));
        assertTrue(result.include("key2"));
        assertFalse(result.include("key3"));
    }

    @Test
    public void ParallelElements_ExceptionDuringProcessing() throws Exception {
        final FlowElement element1 = mock(FlowElement.class);
        final FlowElement element2 = mock(FlowElement.class);
        FlowData data = mock(FlowData.class);

        // Configure element 2 to throw an exception.
        doThrow(new Exception("TEST")).when(element2).process(any(FlowData.class));

        parallelElements = new ParallelElements(
            mock(Logger.class),
            Arrays.asList(
                element1,
                element2));

        // Start processing
        parallelElements.process(data);

        // Check that add error was called with the expected exception
        // and flow element.
        verify(data, times(1)).addError(
            argThat(new ArgumentMatcher<FlowError>() {
                @Override
                public boolean matches(FlowError flowError) {
                    return flowError.getFlowElement().equals(element2) &&
                        flowError.getThrowable().getMessage().equals("TEST");
                }
            })
        );
    }

    @Test
    public void ParallelElements_Dispose() throws Exception {
        // Arrange
        FlowElement element1 = mock(FlowElement.class);
        FlowElement element2 = mock(FlowElement.class);

        // Create the instance
        parallelElements = new ParallelElements(
            mock(Logger.class),
            Arrays.asList(
                element1,
                element2));

        // Act
        parallelElements.close();

        // Assert
        verify(element1, times(1)).close();
        verify(element2, times(1)).close();
    }
}

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
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.core.testclasses.data.MapElementData;
import fiftyone.pipeline.core.testclasses.data.TestElementData;
import fiftyone.pipeline.core.testclasses.flowelements.TestElement;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.*;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("rawtypes")
public class FlowDataTests {

    private static final String INT_PROPERTY = "intvalue";
    private static final String STRING_PROPERTY = "stringvalue";
    private static final String NULL_STRING_PROPERTY = "nullstringvalue";
    private static final String LIST_PROPERTY = "listvalue";
    private static final String DUPLICATE_PROPERTY = "duplicate";
    private static final Integer INT_PROPERTY_VALUE = 5;
    private static final String STRING_PROPERTY_VALUE = "test";
    private PipelineInternal pipeline;
    private FlowDataDefault flowData;
    private boolean flowDataClosed;
    private List<String> LIST_PROPERTY_VALUE =
        Arrays.asList("test", "abc");

    @Before
    public void Init() {
        pipeline = mock(PipelineInternal.class);
        flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)));
        flowDataClosed = false;
    }
    
    @After
    public void TearDown() throws Exception {
        CloseFlowData();
    }
    
    /**
     * This method should be called to properly close 
     * the global flowData object.
     * @throws Exception
     */
    private void CloseFlowData() throws Exception {
        if (flowDataClosed == false) {
            flowData.close();
            flowDataClosed = true;
        }
    }

    @Test
    public void FlowData_Process() {
        flowData.process();
        verify(pipeline).process(flowData);
    }

    @Test(expected = Exception.class)
    public void FlowData_ProcessAlreadyDone() {
        flowData.process();
        flowData.process();
    }

    @Test
    public void FlowData_AddEvidence() {
        String key = "key";
        flowData.addEvidence(key, "value");
        Object result = flowData.getEvidence().get(key);

        assertEquals("value", result);
    }

    @Test
    public void FlowData_AddEvidenceMap() {
        String key1 = "key1";
        String key2 = "key2";
        Map<String, Object> evidence = new HashMap<>();

        evidence.put(key1, "value1");
        evidence.put(key2, "value2");
        flowData.addEvidence(evidence);
        Object result1 = flowData.getEvidence().get(key1);
        Object result2 = flowData.getEvidence().get(key2);

        assertEquals("value1", result1);
        assertEquals("value2", result2);
    }

    @Test
    public void FlowData_AddDataString_GetDataString() {
        flowData.process();
        String key = "key";
        ElementData data = flowData.getOrAdd(
            key,
            (FlowElement.DataFactory<ElementData>) flowData -> new TestElementData(mock(Logger.class), flowData));

        Object result = flowData.get(key);

        assertEquals(data, result);
    }

    @Test
    public void FlowData_AddDataString_GetDataTypedKey() {
        flowData.process();
        String key = "key";
        TypedKey<TestElementData> typedKey = new TypedKeyDefault<>(key);
        ElementData data = flowData.getOrAdd(
            key,
            (FlowElement.DataFactory<ElementData>) flowData -> new TestElementData(mock(Logger.class), flowData));

        Object result = flowData.get(typedKey);

        assertEquals(data, result);
    }

    @Test
    public void FlowData_AddDataString_GetDataElement() {
        flowData.process();

        TestElement element = new TestElement(
            mock(Logger.class),
            (flowData, flowElement) -> new TestElementData(
                mock(Logger.class),
                flowData));
        ElementData data = flowData.getOrAdd(
            element.getElementDataKey(),
            element.getDataFactory());

        Object result = flowData.getFromElement(element);

        assertEquals(data, result);
    }

    @Test
    public void FlowData_AddDataTypedKey_GetDataString() {
        flowData.process();
        String key = "key";
        TypedKey<TestElementData> typedKey = new TypedKeyDefault<>(key);
        TestElementData data = flowData.getOrAdd(
            typedKey,
            flowData -> new TestElementData(mock(Logger.class), flowData));

        Object result = flowData.get(key);

        assertEquals(data, result);
    }

    @Test
    public void FlowData_AddDataTypedKey_GetDataTypedKey() {
        flowData.process();
        String key = "key";
        TypedKey<TestElementData> typedKey = new TypedKeyDefault<>(key);
        TestElementData data = flowData.getOrAdd(
            typedKey,
            flowData -> new TestElementData(mock(Logger.class), flowData));

        Object result = flowData.get(typedKey);

        assertEquals(data, result);
    }

    @Test
    public void FlowData_AddDataTypedKey_GetDataElement() {
        flowData.process();
        TestElement element = new TestElement(
            mock(Logger.class),
            (flowData, flowElement) -> new TestElementData(
                mock(Logger.class),
                flowData)
        );
        TypedKey<TestElementData> typedKey = new TypedKeyDefault<>(element.getElementDataKey());
        TestElementData data = flowData.getOrAdd(
            typedKey,
            element.getDataFactory());


        Object result = flowData.getFromElement(element);

        assertEquals(data, result);
    }

    @Test
    public void FlowData_GetDataAsMap() {
        String key = "key";
        TestElementData data = flowData.getOrAdd(
            key,
            flowData -> new TestElementData(mock(Logger.class), flowData));

        Map<String, Object> result = flowData.elementDataAsMap();

        assertEquals(1, result.size());
        assertEquals(data, result.get(key));
    }

    @Test
    public void FlowData_GetDataAsEnumerable() {
        flowData.process();
        String key = "key";
        TestElementData data = flowData.getOrAdd(
            key,
            flowData -> new TestElementData(mock(Logger.class), flowData));

        Iterable<ElementData> result = flowData.elementDataAsIterable();

        int count = 0;
        for (ElementData elementData : result) {
            count++;
            assertEquals(data, elementData);
        }
        assertEquals(1, count);
    }

    @Test
    public void FlowData_UpdateDataAsMap() {
        flowData.process();
        String key = "key";
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        Map<String, Object> dataAsMap = flowData.elementDataAsMap();

        dataAsMap.put(key, data);
        Object result = flowData.get(key);

        assertEquals(data, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void FlowData_GetWithNullStringKey() {
        flowData.process();
        flowData.get((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void FlowData_GetWithNullTypedKey() {
        flowData.process();
        flowData.get((TypedKey<TestElementData>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void FlowData_GetWithNullElement() {
        flowData.process();
        flowData.getFromElement(null);
    }

    @Test(expected = Exception.class)
    public void FlowData_GetBeforeProcess_String() {
        flowData.get("key");
    }

    @Test(expected = Exception.class)
    public void FlowData_GetBeforeProcess_TypedKey() {
        flowData.get(new TypedKeyDefault<TestElementData>("key"));
    }

    @Test(expected = Exception.class)
    @SuppressWarnings("unchecked")
    public void FlowData_GetBeforeProcess_FlowElement() {
        flowData.getFromElement(
            new TestElement(mock(Logger.class), mock(ElementDataFactory.class)));
    }

    @Test(expected = NoSuchElementException.class)
    public void FlowData_GetNotPresent_String() {
        flowData.process();
        Object result = flowData.get("key");
        assertNull(result);
    }

    @Test(expected = NoSuchElementException.class)
    public void FlowData_GetNotPresent_TypedKey() {
        flowData.process();
        Object result = flowData.get(new TypedKeyDefault<TestElementData>("key"));
        assertNull(result);
    }

    @Test(expected = NoSuchElementException.class)
    @SuppressWarnings("unchecked")
    public void FlowData_GetNotPresent_Element() {
        flowData.process();
        Object result = flowData.getFromElement(
            new TestElement(mock(Logger.class), mock(ElementDataFactory.class))
        );
        assertNull(result);
    }

    @Test(expected = IllegalStateException.class)
    public void FlowData_PipelineDisposed() {
        when(pipeline.isClosed()).thenReturn(true);
        FlowData flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)));

        flowData.process();
    }

    @Test
    public void FlowData_TryGetEvidence() {
        String key = "key";
        flowData.addEvidence(key, "value");
        TryGetResult<String> result = flowData.tryGetEvidence(key, String.class);

        assertTrue(result.hasValue());
        assertEquals("value", result.getValue());
    }

    @Test
    public void FlowData_TryGetEvidence_InvalidKey() {
        String key = "key";
        flowData.addEvidence(key, "value");
        TryGetResult<String> result = flowData.tryGetEvidence("key2", String.class);

        assertFalse(result.hasValue());
    }

    @Test
    public void FlowData_TryGetEvidence_InvalidCast() {
        String key = "key";
        flowData.addEvidence(key, "value");
        TryGetResult<Integer> result = flowData.tryGetEvidence("key", Integer.class);

        assertFalse(result.hasValue());
    }

    @Test
    public void FlowData_GetAs_Int() throws Exception {
        configureMultiElementValues();
        try (FlowData flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)))) {
            flowData.process();
    
            assertEquals(INT_PROPERTY_VALUE, flowData.getAs(INT_PROPERTY, Integer.class));
            assertEquals(INT_PROPERTY_VALUE, flowData.getAsInt(INT_PROPERTY));
        }
    }

    @Test
    public void FlowData_GetAs_String() throws Exception {
        configureMultiElementValues();
        try (FlowData flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)))) {
            flowData.process();

            assertEquals(STRING_PROPERTY_VALUE, flowData.getAs(STRING_PROPERTY, String.class));
            assertEquals(STRING_PROPERTY_VALUE, flowData.getAsString(STRING_PROPERTY));
        }
    }

    @Test
    public void FlowData_GetAs_StringNull() throws Exception {
        configureMultiElementValues();
        try (FlowData flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)))) {
            flowData.process();

            assertNull(flowData.getAs(NULL_STRING_PROPERTY, String.class));
            assertNull(flowData.getAsString(NULL_STRING_PROPERTY));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void FlowData_GetAs_List() throws Exception {
        configureMultiElementValues();
        try (FlowData flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)))) {
            flowData.process();
    
            List<String> result = flowData.getAs(LIST_PROPERTY, List.class);
            assertEquals(LIST_PROPERTY_VALUE.size(), result.size());
            for (int i = 0; i < result.size(); i++) {
                assertEquals(LIST_PROPERTY_VALUE.get(i), result.get(i));
            }
        }
    }

    @Test(expected = PipelineDataException.class)
    public void FlowData_GetAs_NotProcessed() throws Exception {
        configureMultiElementValues();
        try (FlowData flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)))) {

            flowData.getAs(STRING_PROPERTY, String.class);
        }
    }

    @Test(expected = PipelineDataException.class)
    public void FlowData_GetAs_NoProperty() throws Exception {
        configureMultiElementValues();
        try (FlowData flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)))) {
            flowData.process();

            flowData.getAs("not a property", String.class);
        }
    }

    @Test(expected = PipelineDataException.class)
    public void FlowData_GetAs_MultipleProperties() throws Exception {
        configureMultiElementValues();
        try (FlowData flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)))) {
            flowData.process();

            flowData.getAs(DUPLICATE_PROPERTY, String.class);
        }
    }

    @Test(expected = ClassCastException.class)
    public void FlowData_GetAs_WrongType() throws Exception {
        configureMultiElementValues();
        try (FlowData flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)))) {
            flowData.process();

            flowData.getAs(STRING_PROPERTY, int.class);
        }
    }

    private void configureMultiElementValues() {
        FlowElement element1 = mock(FlowElement.class);
        when(element1.getElementDataKey()).thenReturn("element1");
        final List<ElementPropertyMetaData> metaData1 = Arrays.asList(
            new ElementPropertyMetaData[]{
                new ElementPropertyMetaDataDefault(INT_PROPERTY, element1, INT_PROPERTY, int.class, true),
                new ElementPropertyMetaDataDefault(NULL_STRING_PROPERTY, element1, NULL_STRING_PROPERTY, String.class, true),
                new ElementPropertyMetaDataDefault(LIST_PROPERTY, element1, LIST_PROPERTY, LIST_PROPERTY_VALUE.getClass(), true),
                new ElementPropertyMetaDataDefault(DUPLICATE_PROPERTY, element1, DUPLICATE_PROPERTY, String.class, true),

            }
        );
        when(element1.getProperties()).thenReturn(metaData1);

        FlowElement element2 = mock(FlowElement.class);
        when(element2.getElementDataKey()).thenReturn("element2");
        final List<ElementPropertyMetaData> metaData2 = Arrays.asList(
            new ElementPropertyMetaData[]{
                new ElementPropertyMetaDataDefault(STRING_PROPERTY, element2, STRING_PROPERTY, STRING_PROPERTY_VALUE.getClass(), true),
                new ElementPropertyMetaDataDefault(DUPLICATE_PROPERTY, element2, DUPLICATE_PROPERTY, String.class, true),
            }
        );
        when(element2.getProperties()).thenReturn(metaData2);


        final MapElementData elementData1 = new MapElementData(mock(Logger.class), flowData);
        elementData1.put(INT_PROPERTY, INT_PROPERTY_VALUE);
        elementData1.put(NULL_STRING_PROPERTY, null);
        elementData1.put(LIST_PROPERTY, LIST_PROPERTY_VALUE);
        final MapElementData elementData2 = new MapElementData(mock(Logger.class), flowData);
        elementData2.put(STRING_PROPERTY, STRING_PROPERTY_VALUE);

        List<FlowElement> elements = Arrays.asList(element1, element2);
        when(pipeline.getFlowElements()).thenReturn(elements);

        doAnswer(invocationOnMock -> {
            ((FlowData) invocationOnMock.getArgument(0)).getOrAdd("element1",
                (FlowElement.DataFactory<ElementData>) flowData -> elementData1);
            ((FlowData) invocationOnMock.getArgument(0)).getOrAdd("element2",
                (FlowElement.DataFactory<ElementData>) flowData -> elementData2);
            return null;
        }).when(pipeline).process(any(FlowData.class));

        when(pipeline.getMetaDataForProperty(any(String.class))).thenAnswer(invocationOnMock -> {
            List<ElementPropertyMetaData> matches = new ArrayList<>();
            for (ElementPropertyMetaData metaData : metaData1) {
                if (metaData.getName().equals(invocationOnMock.getArgument(0))) {
                    matches.add(metaData);
                }
            }
            for (ElementPropertyMetaData metaData : metaData2) {
                if (metaData.getName().equals(invocationOnMock.getArgument(0))) {
                    matches.add(metaData);
                }
            }
            if (matches.size() == 0 || matches.size() > 1) {
                throw new PipelineDataException();
            }
            return matches.get(0);
        });
    }


    /**
     * Set up the pipeline and flow data with an element which contains
     * properties which can used to test the GetWhere method.
     */
    private void configureGetWhere() {
        // Mock the element
        FlowElement element1 = mock(FlowElement.class);
        when(element1.getElementDataKey()).thenReturn("element1");
        // Set up the properties
        when(element1.getProperties()).thenReturn(Arrays.asList(
            new ElementPropertyMetaDataDefault("available", element1, "category", String.class, true),
            new ElementPropertyMetaDataDefault("anotheravailable", element1, "category", String.class, true),
            new ElementPropertyMetaDataDefault("unavaiable", element1, "category", String.class, false),
            new ElementPropertyMetaDataDefault("differentcategory", element1, "another category", String.class, true),
            new ElementPropertyMetaDataDefault("nocategory", element1, "", String.class, true)));
        // Set up the values for the available properties
        final MapElementData elementData1 = new MapElementData(mock(Logger.class), flowData);
        elementData1.put("available", "a value");
        elementData1.put("anotheravailable", "a value");
        elementData1.put("differentcategory", "a value");
        elementData1.put("nocategory", "a value");
        // Set up the process method to add the values to the flow data
        doAnswer(invocationOnMock -> {
            ((FlowData) invocationOnMock.getArgument(0)).getOrAdd("element1",
                (FlowElement.DataFactory<ElementData>) flowData -> elementData1);
            return null;
        }).when(pipeline).process(any(FlowData.class));
        // Set up the element in the pipeline
        when(pipeline.getFlowElements()).thenReturn(Arrays.asList(element1));
    }

    /**
     * Test that when calling the GetWhere method, filtering on properties
     * which have 'Available' set to true, a valid set of properties and
     * values are returned. Also check that the values returned are
     * correct.
     */
    @Test
    public void FlowData_GetWhere_Available() {
        configureGetWhere();
        flowData.process();
        Map<String, String> values = flowData
            .getWhere(property -> property.isAvailable());
        for (Map.Entry<String, String> value : values.entrySet()) {
            assertNotNull(value);
            assertNotNull(value.getKey());
            assertTrue(value.getKey().startsWith("element1."));
            assertNotNull(value.getValue());
            assertEquals(
                flowData.get("element1").get(value.getKey().split("\\.")[1]),
                value.getValue());
        }
        assertEquals(4, values.size());
    }

    /**
     * Test that when calling the GetWhere method, filtering on properties
     * which have 'Category' set to 'category', only the properties in
     * that category are returned. Also check that the values returned are
     * correct.
     */
    @Test
    public void FlowData_GetWhere_Category() {
        configureGetWhere();
        flowData.process();
        Map<String, String> values = flowData
            .getWhere(property -> property.getCategory().equals("category"));
        for (Map.Entry<String, String> value : values.entrySet()) {
            assertNotNull(value);
            assertNotNull(value.getKey());
            assertTrue(
                value.getKey().equals("element1.available") ||
                value.getKey().equals("element1.anotheravailable"));
            assertNotNull(value.getValue());
            assertEquals(
                flowData.get("element1").get(value.getKey().split("\\.")[1]),
                value.getValue());
        }

        assertEquals(2, values.size());
    }

    /**
     * Test that when calling the GetWhere method, filtering on all
     * properties so that unavailable properties are also included, the
     * unavailable property is not returned and does not throw an
     * exception. Also check that the values returned are correct.
     */
    @Test
    public void FlowData_GetWhere_UnavailableExcluded() {
        configureGetWhere();
        flowData.process();

        Map<String, String> values = flowData.getWhere(new PropertyMatcher() {
            @Override
            public boolean isMatch(ElementPropertyMetaData property) {
                return true;
            }
        });
        for (Map.Entry<String, String> value : values.entrySet()) {
            assertNotNull(value);
            assertNotNull(value.getKey());
            assertTrue(value.getKey().startsWith("element1."));
            assertFalse(value.getKey().equals("element1.unavailable"));
            assertNotNull(value.getValue());
            assertEquals(flowData.get("element1").get(value.getKey().split("\\.")[1]), value.getValue());
        }

        assertEquals(4, values.size());
    }

    private interface CloseableData extends ElementData, AutoCloseable {

    }

    /**
     * Test that when closing the FlowData instance, an AutoCloseable
     * ElementData is closed.
     * @throws Exception
     */
    @Test
    public void FlowData_Close() throws Exception {
        flowData.process();

        CloseableData data = mock(CloseableData.class);

        flowData.getOrAdd(
            "test",
            (FlowElement.DataFactory<ElementData>) flowData -> data);
        CloseFlowData();
        verify(data, times(1)).close();
    }

    /**
     * Test that when closing the FlowData instance that an ElementData which is
     * not AutoCloseable does not throw an exception.
     * @throws Exception
     */
    @Test
    public void FlowData_CloseNotCloseable() throws Exception {
        flowData.process();

        ElementData data = mock(ElementData.class);

        flowData.getOrAdd("test", flowData -> data);
        CloseFlowData();
    }
}

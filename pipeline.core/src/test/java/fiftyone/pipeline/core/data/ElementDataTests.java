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

package fiftyone.pipeline.core.data;

import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.testclasses.data.TestElementData;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ElementDataTests {

    Pipeline pipeline = mock(Pipeline.class);
    FlowData flowData = mock(FlowData.class);

    @Test
    public void ElementData_String() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        String key = "key";
        data.put(key, "value");
        Object result = data.get(key);

        assertEquals("value", result);
    }

    @Test
    public void ElementData_SimpleValueType() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        String key = "key";
        data.put(key, 1);
        Object result = data.get(key);

        assertEquals(1, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ElementData_ComplexValueType() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        String key = "key";
        data.put(key, new AbstractMap.SimpleEntry<>("test", 1));
        Map.Entry<String, Integer> result = (Map.Entry<String, Integer>) data.get(key);

        assertEquals("test", result.getKey());
        assertEquals((Integer) 1, result.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ElementData_ComplexReferenceType() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        String key = "key";
        data.put(key, Arrays.asList("a", "b"));
        List<String> result = (List<String>) data.get(key);

        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    public void ElementData_CaseInsensitiveKey() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        String key = "key";
        data.put(key, "value");
        Object result = data.get(key);
        Object otherResult = data.get("Key");
        assertEquals("value", result);
        assertEquals("value", otherResult);
    }

    @Test
    public void ElementData_CaseInsensitiveKeySet() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        String key = "key";
        data.put(key, "value");
        data.put("Key", "otherValue");
        Object result = data.get(key);
        assertEquals("otherValue", result);
    }

    @Test
    public void ElementData_CaseSensitiveKey() {
        TestElementData data = new TestElementData(
            mock(Logger.class),
            flowData,
            new HashMap<String, Object>());

        String key = "key";
        data.put(key, "value");
        Object result = data.get(key);
        Object otherResult = data.get("Key");

        assertEquals("value", result);
        assertNull(otherResult);
    }

    @Test
    public void ElementData_CaseSensitiveKeySet() {
        TestElementData data = new TestElementData(
            mock(Logger.class),
            flowData,
            new HashMap<String, Object>());

        String key = "key";
        data.put(key, "value");
        data.put("Key", "otherValue");
        Object result = data.get(key);
        assertEquals("value", result);
    }

    @Test
    public void ElementData_AsElementData() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        String key = "key";
        data.put(key, "value");
        ElementData dataAsInterface = data;
        Object result = dataAsInterface.get(key);

        assertEquals("value", result);
    }

    @Test
    public void ElementData_AsMap() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        String key = "key";
        data.put(key, "value");
        Map<String, Object> dataAsMap = data.asKeyMap();

        Object result = dataAsMap.get(key);

        assertEquals("value", result);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void ElementData_ModifyAsMap() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        Map<String, Object> dataAsMap = data.asKeyMap();
        String key = "key";
        dataAsMap.put(key, "value");
    }

    @Test
    public void ElementData_NoData() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        String key = "key";

        Object result = data.get(key);

        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ElementData_NullKey() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        String key = null;

        data.get(key);
    }

    /**
     * Test that the populate from dictionary function works as
     * expected.
     */
    @Test
    public void ElementData_PopulateFromMap_SingleValue() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        Map<String, Object> newData = new HashMap<>();
        newData.put("key", "value");
        data.populateFromMap(newData);

        assertEquals("value", data.get("key"));
    }

    /**
     * Test that the populate from dictionary function works as
     * expected.
     */
    @Test
    public void ElementData_PopulateFromMap_SingleValueWithProperty() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        Map<String, Object> newData = new HashMap<>();
        newData.put("result", "value");
        data.populateFromMap(newData);

        assertEquals("value", data.getResult());
    }

    /**
     * Test that the populate from dictionary function works as
     * expected.
     */
    @Test
    public void ElementData_PopulateFromMap_TwoValues() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        Map<String, Object> newData = new HashMap<>();
        newData.put("key1", "value1");
        newData.put("key2", "value2");
        data.populateFromMap(newData);

        assertEquals("value1", data.get("key1"));
        assertEquals("value2", data.get("key2"));
    }

    /**
     * Test that the populate from dictionary function works as
     * expected.
     */
    @Test
    public void ElementData_PopulateFromMap_Overwrite() {
        TestElementData data = new TestElementData(mock(Logger.class), flowData);
        Map<String, Object> newData = new HashMap<>();
        data.put("key1", "valueA");
        newData.put("key1", "valueB");
        data.populateFromMap(newData);

        assertEquals("valueB", data.get("key1"));
    }
}
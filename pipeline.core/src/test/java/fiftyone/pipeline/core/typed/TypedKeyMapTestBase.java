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

package fiftyone.pipeline.core.typed;

import fiftyone.pipeline.core.data.TryGetResult;

import java.util.*;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class TypedKeyMapTestBase {

    TypedKeyMap map;


    protected void TypedMap_AddRetrieve() {
        String dataToStore = "testdata";
        TypedKey<String> key = new TypedKeyDefault<>("datakey", String.class);
        map.put(key, dataToStore);

        String result = map.get(key);

        assertEquals(dataToStore, result);
    }

    protected void TypedMap_ComplexValueType() {
        Map.Entry<Integer, String> dataToStore =
            new AbstractMap.SimpleEntry<>(1, "testdata");
        TypedKey<Map.Entry<Integer, String>> key =
            new TypedKeyDefault<>("datakey", Map.Entry.class);
        map.put(key, dataToStore);

        Map.Entry<Integer, String> result = map.get(key);

        assertTrue(result.getKey().equals(dataToStore.getKey()) &&
            result.getValue().equals(dataToStore.getValue()));
    }

    protected void TypedMap_ComplexReferenceType() {
        List<String> dataToStore = Arrays.asList("a", "b", "c");
        TypedKey<List<String>> key = new TypedKeyDefault<>("datakey", List.class);
        map.put(key, dataToStore);

        List<String> result = map.get(key);

        assertEquals(3, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    protected void TypedMap_MultipleDataObjects() {
        Map.Entry<Integer, String> dataToStore1 =
            new AbstractMap.SimpleEntry<>(1, "testdata");
        TypedKey<Map.Entry<Integer, String>> key1 = new TypedKeyDefault<>("datakey1", Map.Entry.class);
        map.put(key1, dataToStore1);
        String dataToStore2 = "testdata";
        TypedKey<String> key2 = new TypedKeyDefault<>("datakey2", String.class);
        map.put(key2, dataToStore2);

        Map.Entry<Integer, String> result1 = map.get(key1);
        String result2 = map.get(key2);

        assertTrue(result1.getKey().equals(dataToStore1.getKey()) &&
            result1.getValue().equals(dataToStore1.getValue()));
        assertEquals(dataToStore2, result2);
    }

    protected void TypedMap_Overwrite() {
        Map.Entry<Integer, String> dataToStore1 =
            new AbstractMap.SimpleEntry<>(1, "testdata");
        TypedKey<Map.Entry<Integer, String>> key1 = new TypedKeyDefault<>("datakey", Map.Entry.class);
        map.put(key1, dataToStore1);
        String dataToStore2 = "testdata";
        TypedKey<String> key2 = new TypedKeyDefault<>("datakey", String.class);
        map.put(key2, dataToStore2);

        String result = map.get(key2);

        assertEquals(dataToStore2, result);
    }

    protected void TypedMap_NoData() {
        TypedKey<String> key = new TypedKeyDefault<>("datakey", String.class);

        String result = map.get(key);
    }


    protected void TypedMap_WrongKeyType() {
        Map.Entry<Integer, String> dataToStore1 =
            new AbstractMap.SimpleEntry<>(1, "testdata");
        TypedKey<Map.Entry<Integer, String>> key1 = new TypedKeyDefault<>("datakey", Map.Entry.class);
        map.put(key1, dataToStore1);
        TypedKey<String> key2 = new TypedKeyDefault<>("datakey", String.class);

        String result = map.get(key2);
    }

    protected void TypedMap_NullValue() {
        List<String> dataToStore = null;
        TypedKey<List<String>> key = new TypedKeyDefault<>("datakey", List.class);
        map.put(key, dataToStore);

        List<String> result = map.get(key);

        assertNull(result);
    }

    protected void TypedMap_GetByType() {
        String dataToStore = "TEST";
        TypedKey<String> key = new TypedKeyDefault<>("datakey", String.class);
        map.put(key, dataToStore);

        String result = map.get(String.class);

        assertEquals("TEST", result);
    }

    protected void TypedMap_GetByTypeNoMatch() {
        String dataToStore = "TEST";
        TypedKey<String> key = new TypedKeyDefault<>("datakey", String.class);
        map.put(key, dataToStore);

        int result = map.get(int.class);
    }


    protected void TypedMap_GetByTypeMultiMatch() {
        String dataToStore = "TEST";
        TypedKey<String> key1 = new TypedKeyDefault<>("datakey1", String.class);
        TypedKey<String> key2 = new TypedKeyDefault<>("datakey2", String.class);
        map.put(key1, dataToStore);
        map.put(key2, dataToStore);

        String result = map.get(String.class);
    }

    @SuppressWarnings("unchecked")
    protected void TypedMap_GetByTypeInterface() {
        ArrayList<String> dataToStore = new ArrayList<>(Arrays.asList("TEST"));
        TypedKey<List<String>> key = new TypedKeyDefault<>("datakey", List.class);
        map.put(key, dataToStore);

        List<String> result = map.get(List.class);

        assertEquals(1, result.size());
    }


    protected void TypedMap_TryGetValue_GoodKey_String() {
        String dataToStore = "TEST";
        TypedKey<String> key1 = new TypedKeyDefault<>("datakey1", String.class);
        map.put(key1, dataToStore);

        TryGetResult<String> result = map.tryGet(key1);

        assertTrue(result.hasValue());
        assertEquals(dataToStore, result.getValue());
    }

    protected void TypedMap_TryGetValue_BadKeyName_String() {
        String dataToStore = "TEST";
        TypedKey<String> key1 = new TypedKeyDefault<>("datakey1", String.class);
        TypedKey<String> key2 = new TypedKeyDefault<>("datakey2", String.class);
        map.put(key1, dataToStore);

        TryGetResult<String> result = map.tryGet(key2);

        assertFalse(result.hasValue());
        assertNull(result.getValue());
    }

    protected void TypedMap_TryGetValue_BadKeyType_String() {
        String dataToStore = "TEST";
        TypedKey<String> key1 = new TypedKeyDefault<>("datakey1", String.class);
        TypedKey<Integer> key2 = new TypedKeyDefault<>("datakey1", Integer.class);
        map.put(key1, dataToStore);

        TryGetResult<Integer> result = map.tryGet(key2);

        assertFalse(result.hasValue());
        assertNull(result.getValue());
    }


    protected void TypedMap_TryGetValue_GoodKey_ComplexType() {
        List<String> dataToStore = Arrays.asList("TEST");
        TypedKey<List<String>> key1 = new TypedKeyDefault<>("datakey1", List.class);
        map.put(key1, dataToStore);

        TryGetResult<List<String>> result = map.tryGet(key1);

        assertTrue(result.hasValue());
        assertEquals(dataToStore.get(0), result.getValue().get(0));
    }

    protected void TypedMap_TryGetValue_GoodKeyInterface_ComplexType() {
        ArrayList<String> dataToStore = new ArrayList<>(Arrays.asList("TEST"));
        TypedKey<ArrayList<String>> key1 = new TypedKeyDefault<>("datakey1", ArrayList.class);
        TypedKey<List<String>> key2 = new TypedKeyDefault<>("datakey1", List.class);
        map.put(key1, dataToStore);

        TryGetResult<List<String>> result = map.tryGet(key2);

        assertTrue(result.hasValue());
        assertEquals(dataToStore.get(0), result.getValue().get(0));
    }

    protected void TypedMap_TryGetValue_BadKeyName_ComplexType() {
        List<String> dataToStore = Arrays.asList("TEST");
        TypedKey<List<String>> key1 = new TypedKeyDefault<>("datakey1", List.class);
        TypedKey<List<String>> key2 = new TypedKeyDefault<>("datakey2", List.class);
        map.put(key1, dataToStore);

        TryGetResult<List<String>> result = map.tryGet(key2);

        assertFalse(result.hasValue());
        assertNull(result.getValue());
    }

    protected void TypedMap_TryGetValue_BadKeyType_ComplexType() {
        List<String> dataToStore = Arrays.asList("TEST");
        TypedKey<List<String>> key1 = new TypedKeyDefault<>("datakey1", List.class);
        TypedKey<Map<String, Integer>> key2 = new TypedKeyDefault<>("datakey1", Map.class);
        map.put(key1, dataToStore);

        TryGetResult<Map<String, Integer>> result = map.tryGet(key2);

        assertFalse(result.hasValue());
        assertNull(result.getValue());
    }
}

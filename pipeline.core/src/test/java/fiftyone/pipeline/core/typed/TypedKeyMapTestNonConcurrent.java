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

import fiftyone.pipeline.core.exceptions.PipelineDataException;
import org.junit.Before;
import org.junit.Test;

import java.util.NoSuchElementException;

public class TypedKeyMapTestNonConcurrent extends TypedKeyMapTestBase {

    @Before
    public void Init() {
        map = new TypedKeyMapBuilder(false).build();
    }

    @Test
    public void TypedMap_NonConcurrent_AddRetrieve() {
        TypedMap_AddRetrieve();
    }

    @Test
    public void TypedMap_NonConcurrent_ComplexValueType() {
        TypedMap_ComplexValueType();
    }

    @Test
    public void TypedMap_NonConcurrent_ComplexReferenceType() {
        TypedMap_ComplexReferenceType();
    }

    @Test
    public void TypedMap_NonConcurrent_MultipleDataObjects() {
        TypedMap_MultipleDataObjects();
    }

    @Test
    public void TypedMap_NonConcurrent_Overwrite() {
        TypedMap_Overwrite();
    }

    @Test(expected = NoSuchElementException.class)
    public void TypedMap_NonConcurrent_NoData() {
        TypedMap_NoData();
    }

    @Test(expected = ClassCastException.class)
    public void TypedMap_NonConcurrent_WrongKeyType() {
        TypedMap_WrongKeyType();
    }

    @Test
    public void TypedMap_NonConcurrent_NullValue() {
        TypedMap_NullValue();
    }

    @Test
    public void TypedMap_NonConcurrent_GetByType() {
        TypedMap_GetByType();
    }

    @Test(expected = PipelineDataException.class)
    public void TypedMap_NonConcurrent_GetByTypeNoMatch() {
        TypedMap_GetByTypeNoMatch();
    }

    @Test(expected = PipelineDataException.class)
    public void TypedMap_NonConcurrent_GetByTypeMultiMatch() {
        TypedMap_GetByTypeMultiMatch();
    }

    @Test
    public void TypedMap_NonConcurrent_GetByTypeInterface() {
        TypedMap_GetByTypeInterface();
    }

    @Test
    public void TypedMap_NonConcurrent_TryGetValue_GoodKey_String() {
        TypedMap_TryGetValue_GoodKey_String();
    }

    @Test
    public void TypedMap_NonConcurrent_TryGetValue_BadKeyName_String() {
        TypedMap_TryGetValue_BadKeyName_String();
    }

    @Test
    public void TypedMap_NonConcurrent_TryGetValue_BadKeyType_String() {
        TypedMap_TryGetValue_BadKeyType_String();
    }

    @Test
    public void TypedMap_NonConcurrent_TryGetValue_GoodKey_ComplexType() {
        TypedMap_TryGetValue_GoodKey_ComplexType();
    }

    @Test
    public void TypedMap_NonConcurrent_TryGetValue_GoodKeyInterface_ComplexType() {
        TypedMap_TryGetValue_GoodKeyInterface_ComplexType();
    }

    @Test
    public void TypedMap_NonConcurrent_TryGetValue_BadKeyName_ComplexType() {
        TypedMap_TryGetValue_BadKeyName_ComplexType();
    }

    @Test
    public void TypedMap_NonConcurrent_TryGetValue_BadKeyType_ComplexType() {
        TypedMap_TryGetValue_BadKeyType_ComplexType();
    }
}

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

import org.junit.Test;

import static org.junit.Assert.*;

public class DataKeyTests {

    @Test
    public void DataKey_Equality_Strings() {
        DataKey key = new DataKeyBuilderDefault().add(1, "abc", "123").build();
        DataKey key2 = new DataKeyBuilderDefault().add(1, "abc", "123").build();

        assertEquals(key.hashCode(), key2.hashCode());
        assertTrue(key.equals(key2));
    }

    @Test
    public void DataKey_Equality_Integers() {
        DataKey key = new DataKeyBuilderDefault().add(1, "abc", 123).build();
        DataKey key2 = new DataKeyBuilderDefault().add(1, "abc", 123).build();

        assertEquals(key.hashCode(), key2.hashCode());
        assertTrue(key.equals(key2));
    }

    @Test
    public void DataKey_Equality_ReferenceType() {
        DataKey key = new DataKeyBuilderDefault()
            .add(1, "abc", new EqualityTest("123")).build();
        DataKey key2 = new DataKeyBuilderDefault()
            .add(1, "abc", new EqualityTest("123")).build();

        assertEquals(key.hashCode(), key2.hashCode());
        assertTrue(key.equals(key2));
    }

    @Test
    public void DataKey_Equality_MixedTypes() {
        DataKey key = new DataKeyBuilderDefault().add(1, "abc", 123)
            .add(1, "abc", "123").build();
        DataKey key2 = new DataKeyBuilderDefault().add(1, "abc", 123)
            .add(1, "abc", "123").build();

        assertEquals(key.hashCode(), key2.hashCode());
        assertTrue(key.equals(key2));
    }

    @Test
    public void DataKey_Equality_DifferentValues() {
        DataKey key = new DataKeyBuilderDefault().add(1, "abc", 123).build();
        DataKey key2 = new DataKeyBuilderDefault().add(1, "abc", 12).build();

        assertFalse(key.equals(key2));
    }

    @Test
    public void DataKey_Equality_ReferenceTypeNoMatch() {
        DataKey key = new DataKeyBuilderDefault()
            .add(1, "abc", new EqualityTest("123")).build();
        DataKey key2 = new DataKeyBuilderDefault()
            .add(1, "abc", new EqualityTest("12")).build();

        assertFalse(key.equals(key2));
    }

    @Test
    public void DataKey_Equality_DespiteDifferentKeyNames() {
        DataKey key = new DataKeyBuilderDefault()
            .add(1, "abc", 123).build();
        DataKey key2 = new DataKeyBuilderDefault()
            .add(1, "ab", 123).build();

        assertTrue(key.equals(key2));
    }

    @Test
    public void DataKey_Equality_DespiteOrderOfAdding() {
        DataKey key = new DataKeyBuilderDefault()
            .add(1, "abc", 123).add(2, "xyz", 789).build();
        DataKey key2 = new DataKeyBuilderDefault()
            .add(2, "xyz", 789).add(1, "abc", 123).build();

        assertTrue(key.equals(key2));
    }

    @Test
    public void DataKey_Equality_DespiteSameOrderValueAndOrderOfAdding() {
        DataKey key = new DataKeyBuilderDefault()
            .add(1, "abc", 123).add(1, "xyz", 789).build();
        DataKey key2 = new DataKeyBuilderDefault()
            .add(1, "xyz", 789).add(1, "abc", 123).build();

        assertTrue(key.equals(key2));
    }

    @Test
    public void DataKey_NullValue_GetHashCode() {
        DataKey key = new DataKeyBuilderDefault().add(1, "abc", null).build();
        key.hashCode();
    }

    @Test
    public void DataKey_NullValue_Equality() {
        DataKey key1 = new DataKeyBuilderDefault().add(1, "abc", null).build();
        DataKey key2 = new DataKeyBuilderDefault().add(1, "abc", null).build();
        assertTrue(key1.equals(key2));
    }

    @Test
    public void DataKey_NullValue_Inequality() {
        DataKey key1 = new DataKeyBuilderDefault().add(1, "abc", null).build();
        DataKey key2 = new DataKeyBuilderDefault().add(1, "abc", "value").build();
        assertFalse(key1.equals(key2));
    }

    private class EqualityTest {
        public String value;

        public EqualityTest(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            EqualityTest other = (EqualityTest) obj;
            return other != null && value == other.value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}

/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2025 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.engines.exceptions.NoValueException;
import org.junit.Test;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class AspectPropertyValueTests {

    @Test
    public void AspectPropertyValue_ValueConstructor() throws NoValueException {
        AspectPropertyValue<Integer> value = new AspectPropertyValueDefault<>(1);
        assertEquals(1, (int) value.getValue());
        assertTrue(value.hasValue());
    }

    @Test(expected = NoValueException.class)
    public void AspectPropertyValue_DefaultConstructor() throws NoValueException {
        AspectPropertyValue<Integer> value = new AspectPropertyValueDefault<>();
        assertFalse(value.hasValue());
        Integer result = value.getValue();
    }

    @Test
    public void AspectPropertyValue_DefaultConstructor_SetValue() throws NoValueException {
        AspectPropertyValue<Integer> value = new AspectPropertyValueDefault<>();
        value.setValue(1);
        assertEquals(1, (int) value.getValue());
        assertTrue(value.hasValue());
    }

    @Test
    public void AspectPropertyValue_CustomErrorMessage() {
        AspectPropertyValue<Integer> value = new AspectPropertyValueDefault<>();
        value.setNoValueMessage("CUSTOM MESSAGE");
        assertFalse(value.hasValue());
        try {
            Object result = value.getValue();
            fail("Expected NoValueException to be thrown");
        } catch (NoValueException ex) {
            assertEquals("CUSTOM MESSAGE", ex.getMessage());
        }
    }
}

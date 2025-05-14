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

package fiftyone.pipeline.core.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WeightedValueTests {
    private final static int MAX_RAW_WEIGHTING = 0xFFFF;
    private final static float WEIGHTING_DELTA = 1e-6f;

    @Test
    public void WeightedValue_GetWeighting_1() {
        IWeightedValue<String> value = new WeightedValue<>(
                65535, "the only value");
        assertEquals("the only value", value.getValue());
        assertEquals(1, value.getWeighting(), WEIGHTING_DELTA);
    }

    @Test
    public void WeightedValue_GetWeighting_05() {
        IWeightedValue<?>[] values =
        new IWeightedValue[]{
                new WeightedValue<>(MAX_RAW_WEIGHTING / 2 + 1, 5),
                new WeightedValue<>(MAX_RAW_WEIGHTING / 2, 13),
        };
        assertEquals(5, values[0].getValue());
        assertEquals(13, values[1].getValue());
        assertEquals(0.5f + 0.5f / MAX_RAW_WEIGHTING, values[0].getWeighting(), WEIGHTING_DELTA);
        assertEquals(0.5f - 0.5f / MAX_RAW_WEIGHTING, values[1].getWeighting(), WEIGHTING_DELTA);
    }
}

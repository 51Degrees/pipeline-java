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

/**
 * The value with associated weighting.
 *
 * @param <TValue> the type of value stored inside
 */
public interface IWeightedValue<TValue> {
    /**
     * <p>"Integer" weight factor.</p>
     * <p>Should be within `ushort` range (0~65535).</p>
     *
     * @return "Integer" weight factor.
     */
    int getRawWeighting();

    /**
     * A specific value stored within.
     * @return a specific value stored within.
     */
    TValue getValue();

    /**
     * Recalculates {@link IWeightedValue#getRawWeighting}
     * into a floating point value in range (0~1).
     * @return Weighting as (0~1) multiplier.
     */
    default float getWeighting() {
        return getRawWeighting() / (float)65535;
    }
}

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

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EvidenceKeyFilterAggregatorTests {

    private EvidenceKeyFilterAggregator filter;

    @Test
    public void EvidenceKeyFilterAggregator_Include_Whitelists() {
        EvidenceKeyFilter filter1 = new EvidenceKeyFilterWhitelist(
            Arrays.asList("key1", "key2"));
        EvidenceKeyFilter filter2 = new EvidenceKeyFilterWhitelist(
            Arrays.asList("key3", "key4"));
        filter = new EvidenceKeyFilterAggregator();
        filter.addFilter(filter1);
        filter.addFilter(filter2);

        assertTrue(filter.include("key1"));
        assertTrue(filter.include("key2"));
        assertTrue(filter.include("key3"));
        assertTrue(filter.include("key4"));
        assertFalse(filter.include("key5"));
    }

    @Test
    public void EvidenceKeyFilterAggregator_Include_Aggregated() {
        EvidenceKeyFilter filter1 = new EvidenceKeyFilterWhitelist(
            Arrays.asList("key1", "key2"));
        EvidenceKeyFilter filter2 = new EvidenceKeyFilterWhitelist(
            Arrays.asList("key3", "key4"));
        EvidenceKeyFilterAggregator aggFilter = new EvidenceKeyFilterAggregator();
        aggFilter.addFilter(filter1);
        aggFilter.addFilter(filter2);

        EvidenceKeyFilter filter3 = new EvidenceKeyFilterWhitelist(
            Arrays.asList("key5", "key6"));
        filter = new EvidenceKeyFilterAggregator();
        filter.addFilter(aggFilter);
        filter.addFilter(filter3);

        assertTrue(filter.include("key1"));
        assertTrue(filter.include("key2"));
        assertTrue(filter.include("key3"));
        assertTrue(filter.include("key4"));
        assertTrue(filter.include("key5"));
        assertTrue(filter.include("key6"));
        assertFalse(filter.include("key7"));
    }
    
    /**
     * Check that aggregated case-sensitive and case-insensitive filters
     * still work as expected.
     */
    @Test
    public void EvidenceKeyFilterAggregator_CaseSensitivity() {
        EvidenceKeyFilter filter1 = new EvidenceKeyFilterWhitelist(
            Arrays.asList("key1"), String.CASE_INSENSITIVE_ORDER);
        EvidenceKeyFilter filter2 = new EvidenceKeyFilterWhitelist(
            Arrays.asList("key2"));

        EvidenceKeyFilterAggregator filter = new EvidenceKeyFilterAggregator();
        filter.addFilter(filter1);
        filter.addFilter(filter2);

        assertTrue(filter.include("key1"));
        assertTrue(filter.include("KEY1"));
        assertTrue(filter.include("key2"));
        assertFalse(filter.include("KEY2"));
    }
}

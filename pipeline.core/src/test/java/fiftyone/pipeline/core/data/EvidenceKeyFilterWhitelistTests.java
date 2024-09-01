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
import java.util.Map;

import static org.junit.Assert.*;

public class EvidenceKeyFilterWhitelistTests {

    private EvidenceKeyFilterWhitelist filter;

    @Test
    public void EvidenceKeyFilterWhitelist_Include_CheckKeys() {
        filter = new EvidenceKeyFilterWhitelist(
            Arrays.asList("key1", "key2"));

        assertTrue(filter.include("key1"));
        assertTrue(filter.include("key2"));
        assertFalse(filter.include("key3"));
    }

    @Test
    public void EvidenceKeyFilterWhitelist_List() {
        filter = new EvidenceKeyFilterWhitelist(
            Arrays.asList("key1", "key2"));

        Map<String, Integer> result = filter.getWhitelist();

        assertEquals(2, result.size());
        assertTrue(result.containsKey("key1"));
        assertTrue(result.containsKey("key2"));
    }
}

/*
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2023 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 *  (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 *  If a copy of the EUPL was not distributed with this file, You can obtain
 *  one at https://opensource.org/licenses/EUPL-1.2.
 *
 *  The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 *  amended by the European Commission) shall be deemed incompatible for
 *  the purposes of the Work and the provisions of the compatibility
 *  clause in Article 5 of the EUPL shall not apply.
 *
 *   If using the Work as, or as part of, a network application, by
 *   including the attribution notice(s) required under Article 5 of the EUPL
 *   in the end user terms of the application under an appropriate heading,
 *   such notice(s) shall fulfill the requirements of that article.
 */

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.engines.fiftyone.exceptions.HttpException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ShareUsageBuilderBaseTest {

    ShareUsageBuilderBase<ShareUsageBase> base;

    @Before
    public void setUp(){
        base = new ShareUsageBuilderBase<ShareUsageBase>(null, null) {
            @Override
            public ShareUsageBase build() throws IOException {
                return null;
            }
        };
    }
    @Test
    public void setIncludedQueryStringParameters() {
        base.setIncludedQueryStringParameters("a, b, c");
        assertEquals(3, base.includedQueryStringParameters.size());
        assertTrue(base.includedQueryStringParameters.contains("a"));
        assertTrue(base.includedQueryStringParameters.contains("b"));
        assertTrue(base.includedQueryStringParameters.contains("c"));
    }

    @Test
    public void setIncludedQueryStringParameter() {
        base.setIncludedQueryStringParameter("  a  ");
        assertEquals(1, base.includedQueryStringParameters.size());
        assertTrue(base.includedQueryStringParameters.contains("a"));

        base.setIncludedQueryStringParameter("  b ");
        assertEquals(2, base.includedQueryStringParameters.size());
        assertTrue(base.includedQueryStringParameters.contains("a"));
        assertTrue(base.includedQueryStringParameters.contains("b"));
    }

    @Test
    public void setBlockedHttpHeaders() {
        base.setBlockedHttpHeaders("a, b, c");
        assertEquals(3, base.blockedHttpHeaders.size());
        assertTrue(base.blockedHttpHeaders.contains("a"));
        assertTrue(base.blockedHttpHeaders.contains("b"));
        assertTrue(base.blockedHttpHeaders.contains("c"));
    }

    @Test
    public void setBlockedHttpHeader() {
        base.setBlockedHttpHeader("  a  ");
        assertEquals(1, base.blockedHttpHeaders.size());
        assertTrue(base.blockedHttpHeaders.contains("a"));

        base.setBlockedHttpHeader("  b ");
        assertEquals(2, base.blockedHttpHeaders.size());
        assertTrue(base.blockedHttpHeaders.contains("a"));
        assertTrue(base.blockedHttpHeaders.contains("b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setIgnoreFlowDataEvidenceFilter1() {
        base.setIgnoreFlowDataEvidenceFilter("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setIgnoreFlowDataEvidenceFilter2() {
        base.setIgnoreFlowDataEvidenceFilter("hello");
    }
    @Test(expected = IllegalArgumentException.class)
    public void setIgnoreFlowDataEvidenceFilter2a() {
        base.setIgnoreFlowDataEvidenceFilter("hello, one: two");
    }
    @Test(expected = IllegalArgumentException.class)
    public void setIgnoreFlowDataEvidenceFilter2b() {
        base.setIgnoreFlowDataEvidenceFilter("hello : world, one");
    }
    @Test
    public void setIgnoreFlowDataEvidenceFilter3() {
        base.setIgnoreFlowDataEvidenceFilter(" hello : world , twice : over");
        assertEquals(2, base.ignoreDataEvidenceFilter.size());
        assertEquals("hello", base.ignoreDataEvidenceFilter.get(0).getKey());
        assertEquals("world", base.ignoreDataEvidenceFilter.get(0).getValue());
        assertEquals("twice", base.ignoreDataEvidenceFilter.get(1).getKey());
        assertEquals("over", base.ignoreDataEvidenceFilter.get(1).getValue());
    }

    @Test
    public void setSharePercentage() {
    }

    @Test
    public void setMinimumEntriesPerMessage() {
    }

    @Test
    public void setMaximumQueueSize() {
    }

    @Test
    public void getMaximumQueueSize() {
    }

    @Test
    public void setAddTimeout() {
    }

    @Test
    public void setTakeTimeout() {
    }

    @Test
    public void setShareUsageUrl() {
    }

    @Test
    public void setSessionCookieName() {
    }

    @Test
    public void setRepeatEvidenceIntervalMinutes() {
    }

    @Test
    public void setTrackSession() {
    }

    @Test
    public void build() {
    }
}
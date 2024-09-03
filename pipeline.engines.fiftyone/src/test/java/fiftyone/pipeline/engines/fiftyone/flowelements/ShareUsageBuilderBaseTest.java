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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

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
        base.includedQueryStringParameters.addAll(Arrays.asList("g", "h", "i"));
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
    public void testSetIncludedQueryStringParameters() {
        base.includedQueryStringParameters.addAll(Arrays.asList("g", "h", "i"));
        base.setIncludedQueryStringParameters(Arrays.asList("a", "b", "c"));
        assertEquals(3, base.includedQueryStringParameters.size());
        assertTrue(base.includedQueryStringParameters.contains("a"));
        assertTrue(base.includedQueryStringParameters.contains("b"));
        assertTrue(base.includedQueryStringParameters.contains("c"));
    }


    @Test
    public void setBlockedHttpHeaders() {
        base.setBlockedHttpHeaders("g, h, i");
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



    @Test
    public void testSetBlockedHttpHeaders() {
        base.setBlockedHttpHeaders(Arrays.asList("g", "h", "i"));
        base.setBlockedHttpHeaders(Arrays.asList("a", "b", "c"));
        assertEquals(3, base.blockedHttpHeaders.size());
        assertTrue(base.blockedHttpHeaders.contains("a"));
        assertTrue(base.blockedHttpHeaders.contains("b"));
        assertTrue(base.blockedHttpHeaders.contains("c"));

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
    public void setSharePercentage1() {
        base.setSharePercentage(0.1);
        assertEquals(0.1, base.sharePercentage, 0.00001);
    }
    @Test(expected = IllegalArgumentException.class)
    public void setSharePercentage2() {
        base.setSharePercentage(-0.1);
    }
    @Test(expected = IllegalArgumentException.class)
    public void setSharePercentage3() {
        base.setSharePercentage(1.1);
    }

    @Test
    public void setMinimumEntriesPerMessage1() {
        base.setMinimumEntriesPerMessage(50);
        assertEquals(50, base.minimumEntriesPerMessage);
    }
    @Test(expected = IllegalArgumentException.class)
    public void setMinimumEntriesPerMessage2() {
        base.setMinimumEntriesPerMessage(0);
    }
    @Test(expected = IllegalArgumentException.class)
    public void setMinimumEntriesPerMessage3() {
        base.setMinimumEntriesPerMessage(-1);
    }
    @Test
    public void setMaximumQueueSize() {
        base.setMaximumQueueSize(200);
        assertEquals(200, base.maximumQueueSize);
    }
    @Test(expected = IllegalArgumentException.class)
    public void setMaximumQueueSize1() {
        base.setMaximumQueueSize(0);
    }
    @Test(expected = IllegalArgumentException.class)
    public void setMaximumQueueSize2() {
        base.setMaximumQueueSize(-1);
    }

    @Test
    public void getMaximumQueueSize() {
        base.setMaximumQueueSize(200);
        assertEquals(200, base.getMaximumQueueSize());
    }

    @Test
    public void setAddTimeoutMillis1() {
        base.setAddTimeoutMillis(1000);
        assertEquals(1000, base.addTimeout);
    }
    @Test(expected = IllegalArgumentException.class)
    public void setAddTimeoutMillis2() {
        base.setAddTimeoutMillis(-1);
    }

    @Test
    public void setTakeTimeoutMillis1() {
        base.setTakeTimeoutMillis(1000);
        assertEquals(1000, base.takeTimeout);
    }
    @Test(expected = IllegalArgumentException.class)
    public void setTakeTimeoutMillis2() {
        base.setTakeTimeoutMillis(-1);
    }

    @Test
    public void setShareUsageUrl() {
        base.setShareUsageUrl("https://dog-food.com");
        assertEquals(base.shareUsageUrl, "https://dog-food.com");

    }
    @Test(expected = IllegalArgumentException.class)
    public void setShareUsageUrl1() {
        base.setShareUsageUrl("dog food");
    }

    @Test
    public void setSessionCookieName() {
        base.setSessionCookieName("test");
        assertEquals("test", base.sessionCookieName);
    }
    @Test (expected = IllegalArgumentException.class)
    public void setSessionCookieName1() {
        // illegal name
        base.setSessionCookieName("te st");
        assertEquals("test", base.sessionCookieName);
    }

    @Test
    public void setRepeatEvidenceIntervalMinutes() {
        base.setRepeatEvidenceIntervalMinutes(1);
        assertEquals(1, base.repeatEvidenceInterval);
    }
    @Test (expected = IllegalArgumentException.class)
    public void setRepeatEvidenceIntervalMinutes1() {
        base.setRepeatEvidenceIntervalMinutes(-1);
    }

    @Test
    public void setTrackSession() {
        base.setTrackSession(true);
        assertTrue(base.trackSession);
    }
}
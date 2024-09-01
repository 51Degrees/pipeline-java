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

package fiftyone.pipeline.engines.trackers;

import fiftyone.caching.LruPutCache;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.testhelpers.data.MockFlowData;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class TrackerBaseTests {

    @Test
    public void TrackerBase_Track() throws IOException {
        CacheConfiguration cacheConfig = new CacheConfiguration(
            new LruPutCache.Builder(),
            100);
        TestTracker tracker = new TestTracker(cacheConfig, 1);

        Map<String, Object> map = new HashMap<>();
        map.put("test.field1", "1.2.3.4");
        map.put("test.field2", "abcd");
        FlowData data1 = MockFlowData.createFromEvidence(map, true);
        FlowData data2 = MockFlowData.createFromEvidence(map, true);

        assertTrue(tracker.track(data1));
        assertFalse(tracker.track(data2));
        tracker.close();
    }

    public class TestTracker extends TrackerBase<TestTracker.TrackerCount> {
        private int limit;
        private EvidenceKeyFilter filter;

        TestTracker(
            CacheConfiguration configuration,
            int trackerLimit) {
            super(configuration);
            limit = trackerLimit;
            filter = mock(EvidenceKeyFilter.class);
            // No need to setup the detail of the filter as the
            // GenerateKey method will be mocked anyway.
        }

        @Override
        protected EvidenceKeyFilter getFilter() {
            return filter;
        }

        @Override
        protected boolean match(FlowData data, TrackerCount value) {
            value.count++;
            return value.count <= limit;
        }

        @Override
        protected TrackerCount newValue(FlowData data) {
            return new TrackerCount(1);
        }

        class TrackerCount {
            int count;

            TrackerCount(int count) {
                this.count = count;
            }
        }
    }
}

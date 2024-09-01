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

package fiftyone.pipeline.engines.fiftyone.trackers;

import fiftyone.caching.LruPutCache;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.fiftyone.data.EvidenceKeyFilterShareUsage;
import fiftyone.pipeline.engines.testhelpers.data.MockFlowData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fiftyone.pipeline.core.Constants.*;
import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class ShareUsageTrackerTests {

    private long interval;

    private EvidenceKeyFilter evidenceKeyFilter;

    private ShareUsageTracker shareUsageTracker;
    private String sessionCookieName;
    private List<String> blockedHttpHeaders;
    private List<String> includedQueryStringParameters;
    private Map<String, Object> evidenceData;
    private FlowData data;

    @BeforeEach
    public void Init() {
        blockedHttpHeaders = new ArrayList<>();
        includedQueryStringParameters = new ArrayList<>();
        sessionCookieName = DEFAULT_SESSION_COOKIE_NAME;

        interval = 50;
        evidenceKeyFilter = new EvidenceKeyFilterShareUsage(
            blockedHttpHeaders,
            includedQueryStringParameters,
            true,
            sessionCookieName);
        CacheConfiguration cacheConfig = new CacheConfiguration(
            new LruPutCache.Builder(),
            1000);
        shareUsageTracker = new ShareUsageTracker(
            cacheConfig,
            interval,
            evidenceKeyFilter);

        evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "user-agent", "iPhone");

        DataKeyBuilder dataKeyBuilder = new DataKeyBuilderDefault();
        for (Map.Entry<String, Object> evidence : evidenceData.entrySet()) {
            dataKeyBuilder.add(100, evidence.getKey(), evidence.getValue());
        }

        data = MockFlowData.createFromEvidence(evidenceData, false);
        when(data.generateKey(any(EvidenceKeyFilter.class))).thenReturn(dataKeyBuilder.build());
    }

    @Test
    public void ShareUsageTracker_RepeatEvidence_BeforeSessionTimeout() {
        int trackedEvents = 0;
        for (int i = 0; i < 2; i++) {
            if (shareUsageTracker.track(data)) {
                trackedEvents++;
            }
        }

        assertEquals(1, trackedEvents);
    }

    @Test
    public void ShareUsageTracker_RepeatEvidence_AfterSessionTimeout() throws InterruptedException {
        int trackedEvents = 0;
        for (int i = 0; i < 2; i++) {
            if (shareUsageTracker.track(data)) {
                trackedEvents++;
            }

            // Wait some time equal to the interval to elapse.
            Thread.sleep(interval);
        }

        assertEquals(2, trackedEvents);
    }

    @Test
    public void ShareUsageTracker_Session_Track() throws InterruptedException {
        shareUsageTracker = new ShareUsageTracker(
            new CacheConfiguration(new LruPutCache.Builder(), 1000),
            interval,
            evidenceKeyFilter);

        int trackedEvents = 0;
        for (int i = 0; i < 2; i++) {
            final Map<String, Object> evidenceData = new HashMap<>();
            evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "user-agent", "iPone");
            evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + DEFAULT_SESSION_COOKIE_NAME, i);
            FlowData data = MockFlowData.createFromEvidence(evidenceData, true);
            when(data.generateKey(any(EvidenceKeyFilter.class))).then(
                new Answer<DataKey>() {
                    @Override
                    public DataKey answer(InvocationOnMock invocationOnMock) throws Throwable {
                        return generateKey(
                            (EvidenceKeyFilter) invocationOnMock.getArgument(0),
                            evidenceData);
                    }
                }
            );
            Thread.sleep(interval);
            if (shareUsageTracker.track(data)) {
                trackedEvents++;
            }
        }

        assertEquals(2, trackedEvents);
    }

    @Test
    public void ShareUsageTracker_Session_DoNotTrack() {
        EvidenceKeyFilter evidenceKeyFilter = new EvidenceKeyFilterShareUsage(
            blockedHttpHeaders,
            includedQueryStringParameters,
            false,
            sessionCookieName);
        ShareUsageTracker shareUsageTracker = new ShareUsageTracker(
            new CacheConfiguration(new LruPutCache.Builder(), 1000),
            interval,
            evidenceKeyFilter);

        int trackedEvents = 0;
        for (int i = 0; i < 2; i++) {
            final Map<String, Object> evidenceData = new HashMap<>();
            evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "user-agent", "iPhone");
            evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + DEFAULT_SESSION_COOKIE_NAME, i);
            FlowData data = MockFlowData.createFromEvidence(evidenceData, true);
            when(data.generateKey(any(EvidenceKeyFilter.class))).then(
                new Answer<DataKey>() {
                    @Override
                    public DataKey answer(InvocationOnMock invocationOnMock) throws Throwable {
                        return generateKey(
                            (EvidenceKeyFilter) invocationOnMock.getArgument(0),
                            evidenceData);
                    }
                }
            );
            if (shareUsageTracker.track(data)) {
                trackedEvents++;
            }

        }

        assertEquals(1, trackedEvents);
    }

    public DataKey generateKey(
        EvidenceKeyFilter filter,
        Map<String, Object> evidence) {
        DataKeyBuilder result = new DataKeyBuilderDefault();
        for (Map.Entry<String, Object> entry : evidence.entrySet()) {
            if (filter.include(entry.getKey())) {
                result.add(100, entry.getKey(), entry.getValue());
            }
        }
        return result.build();
    }
}

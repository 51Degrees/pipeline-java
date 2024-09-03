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

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.trackers.TrackerBase;

import java.util.Date;

/**
 * A tracker used by share usage to attempt to avoid repeatedly sending data
 * relating to the same user session.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/usage-sharing-element.md#session-tracking">Specification</a>
 */
public class ShareUsageTracker extends TrackerBase<Date> {

    /**
     * The filter that defines which evidence keys to use in the tracker.
     */
    private final EvidenceKeyFilter filter;

    /**
     * The interval in milliseconds.
     */
    private final long interval;

    /**
     * Construct a new instance.
     * @param configuration the cache configuration to use when building the
     *                      internal cache used by this tracker
     * @param intervalMillis interval between matched in millis
     * @param filter the {@link EvidenceKeyFilter} that defines the evidence
     *               values to use when creating a key from an {@link FlowData}
     */
    public ShareUsageTracker(
        CacheConfiguration configuration,
        long intervalMillis,
        EvidenceKeyFilter filter) {
        super(configuration);
        this.interval = intervalMillis;
        this.filter = filter;
    }

    @Override
    protected Date newValue(FlowData data) {
        // Stored meta-data values for this tracker are always true.
        return new Date();
    }

    @Override
    protected boolean match(FlowData data, Date value) {
        Date now = new Date();
        return value.getTime() <= (now.getTime() - interval);
    }

    @Override
    protected EvidenceKeyFilter getFilter() {
        return filter;
    }
}

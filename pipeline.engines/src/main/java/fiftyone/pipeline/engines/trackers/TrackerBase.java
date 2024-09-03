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

import fiftyone.pipeline.core.data.DataKey;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.caching.DataKeyedCacheBase;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;

/**
 * The abstract base class for trackers.
 * A tracker is a data structure that stores meta-data relating to a key derived
 * from a given {@link FlowData} instance.
 * The details of key creation and the specifics of the meta-data are
 * determined by the tracker implementation.
 * The key will always be a {@link FlowData} instance as defined
 * by {@link DataKeyedCacheBase}.
 * The meta-data can be any type and is specified using the generic type
 * parameter T.
 *
 * As an example, a tracker could create a key using the source IP address from
 * the {@link FlowData} evidence and use the associated meta-data to store a
 * count of the number of times a given source IP has been seen.
 * @param <T> the type of the meta-data object that the tracker stores with each
 * key value.
 */
public abstract class TrackerBase<T>
    extends DataKeyedCacheBase<T>
    implements Tracker {

    /**
     * Construct a new instance
     * @param config the cache configuration to use when building the cache that
     *              is used internally by the tracker
     */
    public TrackerBase(CacheConfiguration config) {
        super(config);
    }

    @Override
    public boolean track(FlowData flowData) {
        boolean result = true;
        T value = get(flowData);
        if (value == null) {
            // If the tracker does not already have a matching item
            // then create one and store it.
            put(flowData, newValue(flowData));
        } else {
            // If the tracker does already have a matching item then
            // call Match to update it based on the new data.

            result = match(flowData, value);
        }

        return result;
    }

    /**
     * Create a tracker value instance.
     * Called when a new key is added to the tracker.
     * The {@link FlowData} that is being added to the tracker.
     * @param data the {@link FlowData} that is being added to the tracker
     * @return a new meta-data instance to be stored with the {@link DataKey}
     * created from the flow data instance
     */
    protected abstract T newValue(FlowData data);

    /**
     * Update the tracker value with relevant details.
     * Called when a tracked item matches an instance already in the tracker.
     * @param data the {@link FlowData} that has matched an existing entry in
     *             the tracker
     * @param value the meta-data instance that the tracker holds for the key
     *              generated from the data
     * @return true if the tracker's logic allows further processing of this
     * flow data instance, false otherwise
     */
    protected abstract boolean match(FlowData data, T value);
}

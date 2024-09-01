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

import fiftyone.pipeline.core.data.FlowData;

/**
 * Represents a 'tracker'.
 * A tracker is a data structure that stores meta-data relating to a key derived
 * from a given {@link FlowData} instance.
 * This meta-data is used to determine if the {@link FlowData} should continue
 * to be processed or not.
 *
 * The details of key creation and the specifics of the meta-data are determined
 * by the tracker implementation.
 *
 * As an example, a tracker could create a key using the source IP address from
 * the {@link FlowData} evidence and use the associated meta-data to store a
 * count of the number of times a given source IP has been seen.
 */
public interface Tracker {

    /**
     * Track the specified {@link FlowData} instance.
     * If the key created from the data does not yet exist in the tracker then
     * it will be added. If it does already exist then the meta-data will be
     * updated according to the tracker implementation's logic.
     * @param flowData the {@link FlowData} to track
     * @return true if further processing is allowed by the tracker's logic,
     * false otherwise
     */
    boolean track(FlowData flowData);
}

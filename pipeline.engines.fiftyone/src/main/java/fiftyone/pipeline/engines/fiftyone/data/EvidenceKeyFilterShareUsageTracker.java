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

package fiftyone.pipeline.engines.fiftyone.data;

import fiftyone.pipeline.engines.fiftyone.trackers.ShareUsageTracker;

import java.util.List;

import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.*;

/**
 * Wrapper for EvidenceKeyFilter for Share Usage, to be used with the
 * {@link ShareUsageTracker} to excluded specific evidence keys from the filter.
 */
public class EvidenceKeyFilterShareUsageTracker extends EvidenceKeyFilterShareUsage {
    /**
     * Constructor.
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParams a list of the names of query string
     *                                  parameters that share usage should send
     *                                  to 51Degrees
     * @param includeSession if true then the session cookie will be included in
     *                       the filter
     * @param sessionCookieName the name of the cookie that contains the session
     *                          id
     */
    public EvidenceKeyFilterShareUsageTracker(
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParams,
        boolean includeSession,
        String sessionCookieName) {
        super(
            blockedHttpHeaders,
            includedQueryStringParams,
            includeSession,
            sessionCookieName);
    }

    @Override
    public boolean include(String key) {
        if(key.equalsIgnoreCase(EVIDENCE_SESSIONID) ||
            key.equalsIgnoreCase(EVIDENCE_SEQUENCE)) {
            return false;
        }

        return super.include(key);
    }
}

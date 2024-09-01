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

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement;
import fiftyone.pipeline.engines.fiftyone.trackers.ShareUsageTracker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fiftyone.pipeline.core.Constants.*;

/**
 * This filter is used by the {@link ShareUsageElement}.
 * It will include anything that is:
 * 1) An HTTP header that is not blocked by the constructor parameter.
 * 2) A cookie that starts with {@link Constants#FIFTYONE_COOKIE_PREFIX} or is
 *     the session cookie (if configured in the constructor).
 * 3) An query string parameters that have been configured to be shared
 *     using the constructor parameter.
 * 4) Not a header, cookie or query string parameter.
 *
 * As this filter is generally inclusive, it will often cause far more
 * evidence to be passed into a pipeline than the engine-specific
 * filters, which tend to be based on a white list
 */
public class EvidenceKeyFilterShareUsage implements EvidenceKeyFilter {

    /**
     * If true then the session cookie will be included in the filter.
     * The session cookie is used by the {@link ShareUsageTracker} but we do not
     * actually want to share it.
     */
    private final boolean includeSession;

    /**
     * The cookie name being used to store the Java session id.
     */
    private final String sessionCookieName;

    /**
     * The content of HTTP headers in this array will not be included in the
     * request information sent to 51degrees. Any header names added here are
     * hard-coded to be blocked regardless of the settings passed to the
     * constructor.
     */
    private final Set<String> blockedHttpHeaders = new HashSet<>();

    /**
     * Query string parameters will not be shared by default. Any query string
     * parameters to be shared must be added to this collection.
     */
    private final Set<String> includedQueryStringParams = new HashSet<>();

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
    public EvidenceKeyFilterShareUsage(
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParams,
        boolean includeSession,
        String sessionCookieName) {
        this.blockedHttpHeaders.add("cookies");
        this.includeSession = includeSession;
        this.sessionCookieName = sessionCookieName;
        for (String header : blockedHttpHeaders) {
            String lowerHeader = header.toLowerCase();
            if (!this.blockedHttpHeaders.contains(lowerHeader)) {
                this.blockedHttpHeaders.add(lowerHeader);
            }
        }
        for (String parameter : includedQueryStringParams) {
            String lowerParameter = parameter.toLowerCase();
            if (!this.includedQueryStringParams.contains(lowerParameter)) {
                this.includedQueryStringParams.add(lowerParameter);
            }
        }
    }

    @Override
    public boolean include(String key) {
        boolean result;
        String[] parts = key.toLowerCase().split("\\.");
        if (parts.length == 2) {
            switch (parts[0]) {
                case EVIDENCE_HTTPHEADER_PREFIX:
                    // Add the header to the list if the header name does not
                    // appear in the list of blocked headers.
                    result = blockedHttpHeaders.contains(parts[1]) == false;
                    break;
                case EVIDENCE_COOKIE_PREFIX:
                    // Only add cookies that start with the 51Degrees cookie
                    // prefix.
                    result = parts[1].startsWith(Constants.FIFTYONE_COOKIE_PREFIX) ||
                        (includeSession && parts[1].equals(sessionCookieName));
                    break;
                case EVIDENCE_SESSION_PREFIX:
                    // Only add session values that start with the 51Degrees cookie
                    // prefix.
                    result = parts[1].startsWith(Constants.FIFTYONE_COOKIE_PREFIX);
                    break;
                case EVIDENCE_QUERY_PREFIX:
                    // Only include query string parameters that have been
                    // specified in the constructor.
                    result = includedQueryStringParams.contains(parts[1]);
                    break;
                default:
                    // Add anything that is not a cookie or a header.
                    result = true;
                    break;
            }
        } else {
            result = true;
        }

        return result;
    }

    @Override
    public Integer order(String key) {
        return 100;
    }
}

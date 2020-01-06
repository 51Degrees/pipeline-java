/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2019 51 Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY.
 *
 * This Original Work is licensed under the European Union Public Licence (EUPL) 
 * v.1.2 and is subject to its terms as set out below.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fiftyone.pipeline.core.Constants.*;

public class EvidenceKeyFilterShareUsage implements EvidenceKeyFilter {

    private boolean includeSession;

    private String sessionCookieName;

    private Set<String> blockedHttpHeaders = new HashSet<>();

    private Set<String> includedQueryStringParams = new HashSet<>();

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
        boolean result = false;
        String[] parts = key.toLowerCase().split("\\.");
        if (parts.length == 2) {
            if (parts[0].equals(EVIDENCE_HTTPHEADER_PREFIX)) {
                // Add the header to the list if the header name does not
                // appear in the list of blocked headers.
                result = blockedHttpHeaders.contains(parts[1]) == false;
            } else if (parts[0].equals(EVIDENCE_COOKIE_PREFIX)) {
                // Only add cookies that start with the 51Degrees cookie
                // prefix.
                result = parts[1].startsWith(Constants.FIFTYONE_COOKIE_PREFIX) ||
                    (includeSession && parts[1].equals(sessionCookieName));
            } else if (parts[0].equals(EVIDENCE_SESSION_PREFIX)) {
                // Only add session values that start with the 51Degrees cookie
                // prefix.
                result = parts[1].startsWith(Constants.FIFTYONE_COOKIE_PREFIX);
            } else if (parts[0].equals(EVIDENCE_QUERY_PREFIX)) {
                // Only include query string parameters that have been
                // specified in the constructor.
                result = includedQueryStringParams.contains(parts[1]);
            } else {
                // Add anything that is not a cookie or a header.
                result = true;
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

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

package fiftyone.pipeline.core;

public class Constants {
    // Evidence Separator
    public static final String EVIDENCE_SEPERATOR = ".";

    // Evidence Prefixes
    public static final String EVIDENCE_HTTPHEADER_PREFIX = "header";
    public static final String EVIDENCE_COOKIE_PREFIX = "cookie";
    public static final String EVIDENCE_SESSION_PREFIX = "session";
    public static final String EVIDENCE_QUERY_PREFIX = "query";
    public static final String EVIDENCE_SERVER_PREFIX = "server";

    // Evidence Suffixes
    public static final String EVIDENCE_USERAGENT = "user-agent";
    public static final String EVIDENCE_WEB_CONTEXT_ROOT_SUFFIX = "contextroot";

    // Evidence Keys
    public static final String EVIDENCE_CLIENTIP_KEY = EVIDENCE_SERVER_PREFIX + EVIDENCE_SEPERATOR + "client-ip";

    public static final String EVIDENCE_QUERY_USERAGENT_KEY = EVIDENCE_QUERY_PREFIX + EVIDENCE_SEPERATOR + EVIDENCE_USERAGENT;

    public static final String EVIDENCE_HEADER_USERAGENT_KEY = EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + EVIDENCE_USERAGENT;
    
    public static final String EVIDENCE_WEB_CONTEXT_ROOT = EVIDENCE_SERVER_PREFIX + EVIDENCE_SEPERATOR + EVIDENCE_WEB_CONTEXT_ROOT_SUFFIX;
    
    public static final String EVIDENCE_PROTOCOL = EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "protocol";

    public static final String EVIDENCE_SESSION_KEY = EVIDENCE_SESSION_PREFIX + EVIDENCE_SEPERATOR + "session";    
}

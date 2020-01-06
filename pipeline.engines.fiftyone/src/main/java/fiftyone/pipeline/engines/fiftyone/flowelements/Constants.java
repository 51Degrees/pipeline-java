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

package fiftyone.pipeline.engines.fiftyone.flowelements;

public class Constants {

    public static final int SHARE_USAGE_MAX_EVIDENCE_LENGTH = 10000;
    public static final String EVIDENCE_SESSIONID_SUFFIX = "session-id";
    public static final String EVIDENCE_SESSIONID =
        fiftyone.pipeline.core.Constants.EVIDENCE_QUERY_PREFIX +
            fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR +
            EVIDENCE_SESSIONID_SUFFIX;
    public static final String EVIDENCE_SEQUENCE_SUFIX = "sequence";
    public static final String EVIDENCE_SEQUENCE =
        fiftyone.pipeline.core.Constants.EVIDENCE_QUERY_PREFIX +
            fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR +
            EVIDENCE_SEQUENCE_SUFIX;
    static final int SHARE_USAGE_DEFAULT_MAX_QUEUE_SIZE = 1000;
    static final int SHARE_USAGE_DEFAULT_ADD_TIMEOUT = 5;
    static final int SHARE_USAGE_DEFAULT_TAKE_TIMEOUT = 100;
    static final String SHARE_USAGE_DEFAULT_URL = "https://devices-v4.51degrees.com/new.ashx";
    static final String EVIDENCE_HTTPHEADER_COOKIE_SUFFIX = "cookie";

}

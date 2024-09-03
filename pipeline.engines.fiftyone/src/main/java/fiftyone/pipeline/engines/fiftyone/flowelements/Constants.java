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

package fiftyone.pipeline.engines.fiftyone.flowelements;

/**
 * Constants used by 51Degrees aspect engines.
 */
public class Constants {

    /**
     * The maximum length of a piece of evidence's value which can be added to
     * the usage data being sent.
     */
    public static final int SHARE_USAGE_MAX_EVIDENCE_LENGTH = 512;

    /**
     * Session id evidence suffix. This is prefixed by the query constant.
     */
    public static final String EVIDENCE_SESSIONID_SUFFIX = "session-id";

    /**
     * Session id evidence constant.
     */
    public static final String EVIDENCE_SESSIONID =
        fiftyone.pipeline.core.Constants.EVIDENCE_QUERY_PREFIX +
            fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR +
            EVIDENCE_SESSIONID_SUFFIX;

    /**
     * Sequence evidence suffix. This is prefixed by the query constant.
     */
    public static final String EVIDENCE_SEQUENCE_SUFIX = "sequence";

    /**
     * Sequence evidence constant used to track the order of requests.
     */
    public static final String EVIDENCE_SEQUENCE =
        fiftyone.pipeline.core.Constants.EVIDENCE_QUERY_PREFIX +
            fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR +
            EVIDENCE_SEQUENCE_SUFIX;
    public static final int SHARE_USAGE_DEFAULT_REPEAT_EVIDENCE_MINUTES = 20;
    public static final double SHARE_USAGE_DEFAULT_SHARE_PERCENTAGE = 1.0;

    /**
     * The minimum number of entries to be sent when sharing data
     */
    static final int SHARE_USAGE_DEFAULT_MIN_ENTRIES_PER_MESSAGE = 50;
     /**
     * The default maximum size of the usage data queue which is stored before
     * sending to the remote service.
     */
    static final int SHARE_USAGE_DEFAULT_MAX_QUEUE_SIZE = 1000;

    /**
     * The default timeout in milliseconds to use when adding usage data to the
     * queue.
     */
    static final int SHARE_USAGE_DEFAULT_ADD_TIMEOUT = 0;

    /**
     * The default timeout in milliseconds to use when taking usage data from
     * the queue.
     */
    static final int SHARE_USAGE_DEFAULT_TAKE_TIMEOUT = 0;

    /**
     * The default timeout in milliseconds to use when making an HTTP connection
     * to share usage data
     */
    static final int SHARE_USAGE_DEFAULT_HTTP_POST_TIMEOUT = 5000;

    /**
     * Default lost data count for share usage before it wraps
     */
    static final int LOST_DATA_RESET_DEFAULT = 5000000;
    /**
     * The default URL to send usage data to.
     */
    static final String SHARE_USAGE_DEFAULT_URL = "https://devices-v4.51degrees.com/new.ashx";

    /**
     * Constant Added to blocked HTTP Headers so that the cookie header is
     * blocked by default.
     */
    static final String EVIDENCE_HTTPHEADER_COOKIE_SUFFIX = "cookie";

}

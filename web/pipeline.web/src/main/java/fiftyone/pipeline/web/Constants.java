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

package fiftyone.pipeline.web;

public class Constants {
    public static final String CORE_JS_NAME = "51Degrees.core.js";

    public static final String HTTPCONTEXT_FIFTYONE =
        "fiftyonedegrees";

    public static final String HTTPCONTEXT_FLOWDATA =
        HTTPCONTEXT_FIFTYONE + ".flowdata";

    public static final String PIPELINE_OPTIONS = "PipelineOptions";

    public static final String ClientSidePropertyCopyright =
        "// Copyright 51Degrees Mobile Experts Limited";

    public static final String DEFAULT_CONFIG_FILE = "/WEB-INF/51Degrees.xml";

    public static final String DEFAULT_URL_PATTERNS = "/*";

    public static final boolean DEFAULT_CLIENTSIDE_ENABLED = true;
}

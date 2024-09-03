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

package fiftyone.pipeline.web;

public class Constants {

    /**
     * The name of the core JavaScript served to the client.
     */
    public static final String CORE_JS_NAME = "51Degrees.core.js";
    
    /**
     * The name of the JSON served to the client.
     */
    public static final String CORE_JSON_NAME = "51Degrees.core.json";

    /**
     * Key prefix used for 51Degrees data stored in the HTTP context.
     */
    public static final String HTTPCONTEXT_FIFTYONE =
        "fiftyonedegrees";

    /**
     * Key used to store the FlowData object in the HTTP context.
     */
    public static final String HTTPCONTEXT_FLOWDATA =
        HTTPCONTEXT_FIFTYONE + ".flowdata";

    /**
     * The name used in the configuration options for the Pipeline's
     * configuration element.
     */
    public static final String PIPELINE_OPTIONS = "PipelineOptions";

    /**
     * The copyright message to add to all javascript. This message can not be
     * altered by 3rd parties.
     */
    public static final String ClientSidePropertyCopyright =
        "// Copyright 51Degrees Mobile Experts Limited";

    /**
     * The default path in a .war package to the 51Degrees configuration file.
     */
    public static final String DEFAULT_CONFIG_FILE = "/WEB-INF/51Degrees.xml";

    /**
     * The default URL matching pattern to apply Pipeline processing to,
     */
    public static final String DEFAULT_URL_PATTERNS = "/*";

    /**
     * Default value for the client-side enabled option.
     */
    public static final boolean DEFAULT_CLIENTSIDE_ENABLED = true;

}

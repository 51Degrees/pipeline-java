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

package fiftyone.pipeline.cloudrequestengine;

public class Constants {
    
    public static final String OriginHeaderName = "Origin";

    public class Messages {
        public static final String EvidenceConflict =
            "'%s:%s' evidence conflicts with %s";
        public static final String MessageNoDataInResponse =
            "No data in response from cloud service at '%s'";
        public static final String MessageErrorCodeReturned =
            "Cloud service at '%s' returned status code '%d' with content: %s";

        public static final String ExceptionCloudErrorsMultiple =
            "Multiple errors returned from 51Degrees cloud service. See inner " +
            "exceptions for details.";

        public static final String ExceptionCloudError =
            "Error returned from 51Degrees cloud service: %s";
        
        public static final String ExceptionFailedToLoadProperties = 
                "Failed to load aspect properties for element '%s'. This is "
                + "because your resource key does not include access to any "
                + "properties under '%s'. For more details on resource keys, "
                + "see our explainer: "
                + "https://51degrees.com/documentation/_info__resource_keys.html";
        
        public static final String ProcessCloudEngineNotImplemented = 
                  "This method should be overridden in the class derived from "
                + "CloudAspectEngineBase class. The implementation should use "
                + "the 'json' parameter to populate the 'aspectData' accordingly. " 
                + "This method will be called by the CloudAspectEngine.ProcessEngine() " 
                + "method after it has successfully retrieved the JsonResponse from "
                +  "the CloudRequestEngine.";

    }
}

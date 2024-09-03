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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import org.slf4j.Logger;

public class CloudRequestDataInternal extends AspectDataBase {
    private static final String JSON_RESPONSE_KEY = "json-response";
    private static final String PROCESS_STARTED_KEY = "process-started";

    public String getJsonResponse() {
    	if(super.get(JSON_RESPONSE_KEY) != null)
            return super.get(JSON_RESPONSE_KEY).toString();
		return null;
    }


    public CloudRequestDataInternal(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
        super(logger, flowData, engine);
    }

    public void setJsonResponse(String value) {
        super.put(JSON_RESPONSE_KEY, value);
    }


    /**
     * Flag to confirm that the CloudRequestEngine has started processing.
     * @return true if the engine has started processing
     */
    public Boolean getProcessStarted() {
    	if(super.get(PROCESS_STARTED_KEY) != null)
    		return Boolean.valueOf(super.get(PROCESS_STARTED_KEY).toString());
		return false;
    }
    
    public void setProcessStarted(Boolean value) {
        super.put(PROCESS_STARTED_KEY, value);
    }

}

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

package pipeline.developerexamples.cloudengine.flowelements;

import pipeline.developerexamples.cloudengine.data.StarSignData;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudAspectEngineBase;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

//! [class]
//! [constructor]
public class SimpleCloudEngine extends CloudAspectEngineBase<StarSignData> {
    private List<AspectPropertyMetaData> aspectProperties;
    private String dataSourceTier;

    public SimpleCloudEngine(
        Logger logger,
        ElementDataFactory<StarSignData> dataFactory) {
        super(logger, dataFactory);
        // Create an empty list
        aspectProperties = new ArrayList<AspectPropertyMetaData>();
    }
//! [constructor]

    @Override
    public List<AspectPropertyMetaData> getProperties() {
        return aspectProperties;
    }

    @Override
    public String getDataSourceTier() {
        return dataSourceTier;
    }

    @Override
    public String getElementDataKey() {
        return "starsign";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        // This engine needs no evidence.
        // It works from the cloud request data.
        return new EvidenceKeyFilterWhitelist(new ArrayList<String>());
    }

    private boolean checkedForCloudEngine = false;
    private CloudRequestEngine cloudRequestEngine = null;

    @Override
    protected void processEngine(FlowData data, StarSignData aspectData) {
        // Cast aspectData to StarSignDataDefault, so the 'setter' is available.
        StarSignDataInternal starSignData = (StarSignDataInternal)aspectData;

        if (checkedForCloudEngine == false) {
            cloudRequestEngine = getRequestEngine().getInstance();
            checkedForCloudEngine = true;
        }

        CloudRequestData requestData = data.getFromElement(cloudRequestEngine);
        String json = requestData.getJsonResponse();

        // Extract data from json to the aspectData instance.
        JSONObject jsonObj = new JSONObject(json);
        JSONObject deviceObj = jsonObj.getJSONObject("starsign");

        starSignData.setStarSign(deviceObj.getString("starsign"));
    }

    @Override
    protected void unmanagedResourcesCleanup() {
        // Nothing to clean up here.
    }
}
//! [class]

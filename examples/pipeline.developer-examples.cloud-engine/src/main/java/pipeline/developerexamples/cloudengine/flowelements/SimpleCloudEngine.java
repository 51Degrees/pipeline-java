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

package pipeline.developerexamples.cloudengine.flowelements;

import pipeline.developerexamples.cloudengine.data.StarSignData;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngineBase;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//! [class]
//! [constructor]
public class SimpleCloudEngine extends CloudAspectEngineBase<StarSignData, AspectPropertyMetaData> {
    private List<AspectPropertyMetaData> aspectProperties;
    private String dataSourceTier;
    private CloudRequestEngine engine;

    public SimpleCloudEngine(
        Logger logger,
        ElementDataFactory<StarSignData> dataFactory,
        CloudRequestEngine engine) {
        super(logger, dataFactory);
        this.engine = engine;
        if (this.engine != null) {
            if (loadAspectProperties(engine) == false) {
                logger.error("Failed to load aspect properties");
            }
        }
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
            cloudRequestEngine = data.getPipeline().getElement(CloudRequestEngine.class);
            checkedForCloudEngine = true;
        }

        if (cloudRequestEngine == null) {
            throw new PipelineConfigurationException(
                "The '" + getClass().getName() + "' requires a " +
                    "'CloudRequestEngine' before it in the Pipeline. This " +
                    "engine will be unable to produce results until this is " +
                    "corrected.");
        }
        else {
            CloudRequestData requestData = data.getFromElement(cloudRequestEngine);
            String json = "";
            json = requestData.getJsonResponse();

            // Extract data from json to the aspectData instance.
            JSONObject jsonObj = new JSONObject(json);
            JSONObject deviceObj = jsonObj.getJSONObject("starsign");

            starSignData.setStarSign(deviceObj.getString("starsign"));
        }
    }

    @Override
    protected void unmanagedResourcesCleanup() {
        // Nothing to clean up here.
    }

//! [loadaspectproperties]
    private boolean loadAspectProperties(CloudRequestEngine engine) {
        Map<String, AccessiblePropertyMetaData.ProductMetaData> map =
            engine.getPublicProperties();

        if (map != null &&
            map.size() > 0 &&
            map.containsKey(getElementDataKey())) {
            aspectProperties = new ArrayList<>();
            dataSourceTier = map.get(getElementDataKey()).dataTier;

            for (AccessiblePropertyMetaData.PropertyMetaData item :
                map.get(getElementDataKey()).properties) {
                AspectPropertyMetaData property = new AspectPropertyMetaDataDefault(
                    item.name,
                    this,
                    item.category,
                    item.getPropertyType(),
                    new ArrayList<String>(),
                    true);
                aspectProperties.add(property);
            }
            return true;
        }
        else {
            logger.error("Aspect properties could not be loaded for" +
                " the cloud engine", this);
            return false;
        }
    }
//! [loadaspectproperties]
}
//! [class]

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;

import java.util.Map;

/**
 * Engine that makes requests to the 51Degrees cloud service based on the
 * details passed at creation and the evidence in the FlowData instance. The
 * unprocessed JSON response is stored in the FlowData for other engines to make
 * use of.
 */
public interface CloudRequestEngine
    extends AspectEngine<CloudRequestData, AspectPropertyMetaData> {

    /**
     * A collection of the properties that the cloud service can populate in the
     * JSON response. Keyed on property name.
     * @return public properties
     */
    Map<String, AccessiblePropertyMetaData.ProductMetaData> getPublicProperties();
}

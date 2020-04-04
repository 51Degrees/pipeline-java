package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;

import java.util.Map;

public interface CloudRequestEngine extends AspectEngine<CloudRequestData, AspectPropertyMetaData> {

    Map<String, AccessiblePropertyMetaData.ProductMetaData> getPublicProperties();
}

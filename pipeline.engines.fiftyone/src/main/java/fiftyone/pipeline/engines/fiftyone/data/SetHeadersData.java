package fiftyone.pipeline.engines.fiftyone.data;

import java.util.HashMap;
import java.util.Map;

import fiftyone.pipeline.engines.fiftyone.flowelements.SetHeadersElement;
import org.slf4j.Logger;

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.FlowData;

//! [class]
//! [constructor]
/**
 * Data containing the result of {@link SetHeadersElement}.
 */
public class SetHeadersData extends ElementDataBase {
	public static final String RESPONSE_HEADER_PROPERTY_NAME =
		"ResponseHeaderDictionary";

	public SetHeadersData(Logger logger, FlowData flowData) {
		super(logger, flowData);
	}
	
	public SetHeadersData(
		Logger logger,
		FlowData flowData,
		Map<String, Object> data) {
		super(logger, flowData, data);
	}
//! [constructor]

	/**
	 * Get the response headers to be set.
	 * @return table of response headers
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, String> getResponseHeaderDictionary() {
		return getAs(RESPONSE_HEADER_PROPERTY_NAME, HashMap.class);
	}
	
	public void setResponseHeaderDictionary(HashMap<String, String> responseHeaders) {
		put(RESPONSE_HEADER_PROPERTY_NAME, responseHeaders);
	}
}
//! [class]
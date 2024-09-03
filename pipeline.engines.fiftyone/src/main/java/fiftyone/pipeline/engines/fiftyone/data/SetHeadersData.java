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
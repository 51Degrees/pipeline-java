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

package fiftyone.pipeline.setheader.flowelements;

import java.util.HashMap;

import org.slf4j.Logger;

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.setheader.data.SetHeaderData;
import static fiftyone.pipeline.setheader.Constants.*;

//! [class]
public class SetHeaderDataInternal extends ElementDataBase implements SetHeaderData {

	public SetHeaderDataInternal(Logger logger, FlowData flowData) {
		super(logger, flowData);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public HashMap<String, String> getResponseHeaderDictionary() {
		return getAs(RESPONSE_HEADER_PROPERTY_NAME, HashMap.class);
	}
	
	void setResponseHeaderDictionary(HashMap<String, String> responseHeaders) {
		put(RESPONSE_HEADER_PROPERTY_NAME, responseHeaders);
	}
}
//! [class]
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.NoValueException;
import fiftyone.pipeline.setheader.data.SetHeaderData;
import static fiftyone.pipeline.setheader.Constants.*;

//! [class]
//! [constructor]
public class SetHeaderElement
	extends FlowElementBase<SetHeaderData, ElementPropertyMetaData>
	implements SetHeader {
	private final List<ElementPropertyMetaData> properties;

	public SetHeaderElement(
		Logger logger,
		ElementDataFactory<SetHeaderData> elementDataFactory) {
		super(logger, elementDataFactory);
		properties = Collections.singletonList(
			(ElementPropertyMetaData)new ElementPropertyMetaDataDefault(
				RESPONSE_HEADER_PROPERTY_NAME,
				this,
				"",
				HashMap.class,
				true));
	}
//! [constructor]
	
	@Override
	protected void processInternal(FlowData data) throws Exception {
		SetHeaderDataInternal elementData =
			(SetHeaderDataInternal)data.getOrAdd(
				getElementDataKey(),
				getDataFactory());
		
		HashMap<String, String> responseHeaders =
			buildResponseHeaders(data);
		elementData.setResponseHeaderDictionary(responseHeaders);
	}
	
	@Override
	public String getElementDataKey() {
		return SET_HEADER_ELEMENT_DATAKEY;
	}
	
	@SuppressWarnings("serial")
	@Override
	public EvidenceKeyFilter getEvidenceKeyFilter() {
		// We don't need any evidence for this element
		return new EvidenceKeyFilterWhitelist(new ArrayList<String>(){},
                String.CASE_INSENSITIVE_ORDER);
	}

	@Override
	public List<ElementPropertyMetaData> getProperties() {
		return properties;
	}
	
	@Override
	protected void managedResourcesCleanup() {
		// Nothing to clean up here
	}
	
	@Override
	protected void unmanagedResourcesCleanup() {
		// Nothing to clean up here
	}
	
	/**
	 * Build the response headers from the SetHeader properties in the FlowData.
	 * @param data flowData object
	 * @return constructed response headers
	 */
	@SuppressWarnings("unchecked")
	private HashMap<String, String> buildResponseHeaders(FlowData data) {
		HashMap<String, String> responseHeaders =
				new HashMap<String, String>();
		ElementData deviceData = data.get(DEVICE_ELEMENT_DATAKEY);
		if (deviceData != null) {
			deviceData.asKeyMap().forEach((k, v) -> {
				try {
					if (k.startsWith(SET_HEADER_PREFIX) &&
						v != null &&
						((AspectPropertyValue<String>)v).hasValue() &&
						!((AspectPropertyValue<String>)v)
							.getValue().equals("Unknown")) {
						String responseHeader = getResponseHeader(k);
						String responseHeaderValue;
						String propertyValue =
							((AspectPropertyValue<String>) v).getValue();
						if ((responseHeaderValue =
							responseHeaders.get(responseHeader)) != null) {
								responseHeaderValue += "," + propertyValue;
								responseHeaders.put(
										responseHeader, responseHeaderValue);
						}
						else {
							responseHeaders.put(responseHeader, propertyValue);
						}
					}
				}
				catch (NoValueException e) {
					// We should never access a value if hasValue is false
					// Log it.
					data.addError(e, this);
				}
			});
		}
		return responseHeaders;
	}
	
	/**
	 * Extract the response header name from the SetHeader property.
	 * @param key a SetHeader property name
	 * @return extracted response header name
	 */
	private String getResponseHeader(String key) {
		for (int i = new String(SET_HEADER_PREFIX).length() + 1;
			i < key.length();
			i++) {
			if (key.charAt(i) <= 'Z' && key.charAt(i) >= 'A') {
				return key.substring(i).trim();
			}
		}
		return null;
	}
}
//! [class]

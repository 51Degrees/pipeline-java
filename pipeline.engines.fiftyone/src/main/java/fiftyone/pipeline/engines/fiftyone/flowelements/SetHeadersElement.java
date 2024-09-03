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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.NoValueException;
import fiftyone.pipeline.engines.fiftyone.data.SetHeadersData;
import fiftyone.pipeline.engines.fiftyone.exceptions.Messages;

import static fiftyone.pipeline.engines.fiftyone.data.SetHeadersData.*;

//! [class]
//! [constructor]
/**
 * SetHeadersElement constructs the header values to be set in the HTTP response.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/set-headers-element.md">Specification</a>
 */
public class SetHeadersElement
	extends FlowElementBase<SetHeadersData, ElementPropertyMetaData> {
	/**
	 * The element data key.
	 */
	public static final String SET_HEADER_ELEMENT_DATAKEY = "set-headers";
	
	/**
	 * The prefix of the SetHeader* properties of the Device Detection engine.
	 */
	public static final String SET_HEADER_PREFIX = "SetHeader";
	
	/**
	 * Lower case prefix of the SetHeader* properties.
	 */
	public static final String SET_HEADER_PREFIX_LOWER_CASE = "setheader";
	
	private final EvidenceKeyFilter evidenceKeyFilter;
	private final List<ElementPropertyMetaData> properties;
	private final Hashtable<Pipeline, PipelineConfig> pipelineConfigs;

	public SetHeadersElement(
		Logger logger,
		ElementDataFactory<SetHeadersData> elementDataFactory) {
		super(logger, elementDataFactory);
		evidenceKeyFilter =
			new EvidenceKeyFilterWhitelist(new ArrayList<String>());
		properties = Collections.singletonList(
			(ElementPropertyMetaData)new ElementPropertyMetaDataDefault(
				RESPONSE_HEADER_PROPERTY_NAME,
				this,
				"",
				HashMap.class,
				true));
		pipelineConfigs = new Hashtable<Pipeline, PipelineConfig>();
	}
//! [constructor]
	
	@Override
	protected void processInternal(FlowData data) throws Exception {
		if (data == null) {
			throw new NullPointerException(FlowData.class.getName());
		}
		
		PipelineConfig config;
		Pipeline pipeline = data.getPipeline();
		if ((config = pipelineConfigs.get(pipeline)) == null) {
			config = populateConfig(pipeline);
			config = pipelineConfigs.getOrDefault(pipeline, config);
		}
		
		SetHeadersData elementData = data.getOrAdd(
			getElementDataKey(),
			getDataFactory());
		
		HashMap<String, String> responseHeaders =
			buildResponseHeaders(data, config);
		elementData.setResponseHeaderDictionary(responseHeaders);
	}
	
	@Override
	public String getElementDataKey() {
		return SET_HEADER_ELEMENT_DATAKEY;
	}
	
	@Override
	public EvidenceKeyFilter getEvidenceKeyFilter() {
		return evidenceKeyFilter;
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
	private HashMap<String, String> buildResponseHeaders(
		FlowData data, PipelineConfig config) {
		HashMap<String, String> responseHeaders =
				new HashMap<String, String>();
		
		config.setHeaderProperties.forEach((k, v) -> {
			ElementData elementData =
				data.get(v.propertyMetaData.getElement().getElementDataKey());
			String headerValue = getHeaderValue(elementData.get(k));
			
			if (!headerValue.isEmpty() && !headerValue.equals("Unknown")) {
				String responseHeaderValue;
				if ((responseHeaderValue =
					responseHeaders.get(v.responseHeaderName)) != null) {
					responseHeaderValue += "," + headerValue;
					responseHeaders.put(
						v.responseHeaderName, responseHeaderValue);
				}
				else {
					responseHeaders.put(
						v.responseHeaderName,  headerValue);
				}
			}
			
		});
		return responseHeaders;
	}
	
	/**
	 * Get the header to be set from the 'SetHeader' property name.
	 * @param propertyValue
	 * @return header to set
	 */
	private String getHeaderValue(Object propertyValue) {
		String result = "";
		if (propertyValue instanceof AspectPropertyValue &&
			((AspectPropertyValue<?>)propertyValue).hasValue()) {
			try {
				Object value = ((AspectPropertyValue<?>) propertyValue).getValue();
				if (value instanceof String) {
					result = (String)value;
				}
			} catch (NoValueException e) {
				// Should never happen as we already check has Value
				e.printStackTrace();
			}
		}
		else if (propertyValue instanceof String){
			result = (String)propertyValue;
		}
		return result;
	}
	
	/**
	 * Populate the pipeline configuration object with all available 'SetHeader'
	 * properties.
	 * @param pipeline
	 * @return pipeline configuration object.
	 */
	private PipelineConfig populateConfig(Pipeline pipeline) {
		PipelineConfig config = new PipelineConfig();
		pipeline.getElementAvailableProperties().forEach((k, v) -> {
			v.forEach((p, m) -> {
				if (p.toLowerCase()
					.startsWith(SET_HEADER_PREFIX_LOWER_CASE)) {
					PropertyDetails details = new PropertyDetails();
					details.propertyMetaData = m;
					details.responseHeaderName = getResponseHeader(p);
					config.setHeaderProperties.put(p, details);
				}
			});
		});
		return config;
	}
	
	/**
	 * Extract the response header name from the SetHeader property.
	 * @param key a SetHeader property name
	 * @return extracted response header name
	 */
	private String getResponseHeader(String key) {
		if (!key.startsWith(SET_HEADER_PREFIX)) {
			throw new IllegalArgumentException(
				String.format(Messages.EXCEPTION_SET_HEADERS_NOT_SET_HEADER, key));
		}
		if (key.length() <= SET_HEADER_PREFIX.length() + 1) {
			throw new IllegalArgumentException(
				String.format(Messages.EXCEPTION_SET_HEADERS_WRONG_FORMAT, key));
		}
		for (int i = new String(SET_HEADER_PREFIX).length() + 1;
			i < key.length();
			i++) {
			if (Character.isUpperCase(key.charAt(i))) {
				return key.substring(i).trim();
			}
		}
		// Should never get to this stage if the format is correct
		throw new IllegalArgumentException(
				String.format(Messages.EXCEPTION_SET_HEADERS_WRONG_FORMAT, key));
	}
	
	/**
	 * Internal class to hold 'SetHeader' properties available per pipeline.
	 */
	protected class PipelineConfig {
		public Map<String, PropertyDetails> setHeaderProperties =
			new HashMap<String, PropertyDetails>();
	}
	
	/**
	 * Internal class to hold a list of property metadata and its response
	 * header to set.
	 */
	protected class PropertyDetails {
		public ElementPropertyMetaData propertyMetaData;
		public String responseHeaderName;
	}
}
//! [class]

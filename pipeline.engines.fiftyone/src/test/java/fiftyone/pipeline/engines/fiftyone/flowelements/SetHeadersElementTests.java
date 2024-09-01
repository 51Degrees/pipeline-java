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

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.fiftyone.data.SetHeadersData;
import fiftyone.pipeline.exceptions.AggregateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for SetHeadersElement
 */
public class SetHeadersElementTests {
	private SetHeadersElement element;
	private ActivePropertySourceElement sourceElement;
	private Pipeline pipeline;
	private TestLoggerFactory loggerFactory;
	
	public SetHeadersElementTests() {
		ILoggerFactory internalLogger = mock(ILoggerFactory.class);
		when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
		loggerFactory = new TestLoggerFactory(internalLogger);
	}
	
	/**
	 * Check that SetHeaderElement handles both 'String' and
	 * 'AspectPropertyValue' correctly. 
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void setHeadersElement(boolean valueIsAPV) throws Exception {
		String valueStr = "UA-Platform";
		Object value = valueStr;
		if (valueIsAPV) {
			value = new AspectPropertyValueDefault<String>(valueStr);
		}
		
		Map<String, Object> propertyNameValues =
			new HashMap<String, Object>();
		propertyNameValues.put("SetHeaderBrowserAccept-CH", value);
		createPipeline(propertyNameValues);
		FlowData data = pipeline.createFlowData();
		data.process();
		
		// Verify the output
		SetHeadersData typedOutput = getFromFlowData(data);
		assertEquals(1,  typedOutput.getResponseHeaderDictionary().size());
		assertTrue(typedOutput.getResponseHeaderDictionary().containsKey("Accept-CH"));
		assertEquals("UA-Platform", typedOutput.getResponseHeaderDictionary().get("Accept-CH"));
	}
	
	/**
	 * Check that SetHeadersElement returns an empty table if the value of
	 * 'SetHeaderBrowserAccept-CH' is invalid.
	 */
	@ParameterizedTest
	@MethodSource("createArgumentsInvalidPropertyValues")
	public void setHeadersElement_InvalidPropertyValues(Object sourcePropertyValue) throws Exception {
		Map<String, Object> propertyNameValues = new HashMap<String, Object>();
		propertyNameValues.put("SetHeaderBrowserAccept-CH", sourcePropertyValue);
		createPipeline(propertyNameValues);
		FlowData data = pipeline.createFlowData();
		data.process();
		
		// Verify the output
		SetHeadersData typedOutput = getFromFlowData(data);
		assertEquals(0, typedOutput.getResponseHeaderDictionary().size());
	}
	
	/**
	 * Method to create test input for InvalidPropertyValues test.
	 */
	private static Stream<Arguments> createArgumentsInvalidPropertyValues() {
		return Stream.of(
			Arguments.of(new Object[] {null}),
			Arguments.of(123),
			Arguments.of("Unknown"));
	}
	
	/**
	 * Check that SetHeadersElement returns an empty table if the value of
	 * AspectPropertyValue for 'SetHeaderBrowserAccept-CH' property is invalid.
	 */
	@ParameterizedTest
	@MethodSource("createArgumentsAPVInvalidPropertyValues")
	public void setHeadersElement_APV_InvalidPropertyValues(
		boolean hasValue, String sourcePropertyValue) throws Exception {
		AspectPropertyValue<String> value =
			new AspectPropertyValueDefault<String>();
		if (hasValue) {
			value.setValue(sourcePropertyValue);
		}
		
		Map<String, Object> propertyNameValues = new HashMap<String, Object>();
		propertyNameValues.put("SetHeaderBrowserAccept-CH", value);
		createPipeline(propertyNameValues);
		FlowData data = pipeline.createFlowData();
		data.process();
		
		// Verify the output
		SetHeadersData typedOutput = getFromFlowData(data);
		assertEquals(0, typedOutput.getResponseHeaderDictionary().size());
	}
	
	/**
	 * Method to create test input for APV_InvalidPropertyValues test.
	 */
	private static Stream<Arguments> createArgumentsAPVInvalidPropertyValues() {
		return Stream.of(
			Arguments.of(false, null),
			Arguments.of(true, null));
	}
	
	/**
	 * Check that 'SetHeadersElement' throws exception if the 'SetHeader'
	 * properties are in incorrect format.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"SetHeader", "SetHeaderBrowser"})
	public void setHeadersElement_InvalidPropertyNames(
		String sourcePropertyName) throws Exception {
		Map<String, Object> propertyNameValues = new HashMap<String, Object>();
		propertyNameValues.put(sourcePropertyName, "TEST");
		createPipeline(propertyNameValues);
		FlowData data = pipeline.createFlowData();
		assertThrows(AggregateException.class, () -> {
			data.process();
		});
	}
	
	/**
	 * Check that 'SetHeadersElement' correctly handle multiple 'SetHeader'
	 * properties with different header value to set.
	 */
	@Test
	public void setHeadersElement_MultipleProperties() throws Exception {
		Map<String, Object> propertyNameValues = new HashMap<String, Object>();
		propertyNameValues.put("SetHeaderBrowserAccept-CH", "Sec-CH-UA");
		propertyNameValues.put("SetHeaderHardwareCritical-CH",
			"Sec-CH-UA-Model,Sec-CH-UA-Mobile");
		createPipeline(propertyNameValues);
		FlowData data = pipeline.createFlowData();
		data.process();
		
		// Verify the output
		SetHeadersData typedOutput = getFromFlowData(data);
		assertEquals(2, typedOutput.getResponseHeaderDictionary().size());
		assertTrue(typedOutput.getResponseHeaderDictionary().containsKey("Accept-CH"));
		assertTrue(typedOutput.getResponseHeaderDictionary().containsKey("Critical-CH"));
		assertEquals("Sec-CH-UA", typedOutput.getResponseHeaderDictionary().get("Accept-CH"));
		assertEquals("Sec-CH-UA-Model,Sec-CH-UA-Mobile",
			typedOutput.getResponseHeaderDictionary().get("Critical-CH"));
	}

	/**
	 * Check that 'SetHeadersElement' correctly construct response header
	 * if multiple 'SetHeader' properties of a response header presents.
	 */
	@Test
	public void setHeadersElement_MultipleProperties_SameHeader() throws Exception {
		Map<String, Object> propertyNameValues = new HashMap<String, Object>();
		propertyNameValues.put("SetHeaderBrowserAccept-CH", "Sec-CH-UA");
		propertyNameValues.put("SetHeaderHardwareAccept-CH", "Sec-CH-UA-Model,Sec-CH-UA-Mobile");
		createPipeline(propertyNameValues);
		FlowData data = pipeline.createFlowData();
		data.process();
		
		// Verify the output
		SetHeadersData typedOutput = getFromFlowData(data);
		assertEquals(1, typedOutput.getResponseHeaderDictionary().size());
		assertTrue(typedOutput.getResponseHeaderDictionary().containsKey("Accept-CH"));
		String acceptCHValue = typedOutput.getResponseHeaderDictionary().get("Accept-CH");
		assertTrue(acceptCHValue.contains("Sec-CH-UA"));
		assertTrue(acceptCHValue.contains("Sec-CH-UA-Model,Sec-CH-UA-Mobile"));
		assertTrue(acceptCHValue.matches("^Sec-CH-[a-zA-Z-]+,Sec-CH-[a-zA-Z-]+,Sec-CH-[a-zA-Z-]+$"));
	}
	
	private void createPipeline(
		Map<String, Object> propertyNameValues) throws Exception {
		sourceElement = new ActivePropertySourceElement(
			loggerFactory.getLogger(ActivePropertySourceElement.class.getName()),
			new ElementDataFactory<SetHeadersSourceData>() {
                @Override
                public SetHeadersSourceData create(
                    FlowData flowData,
                    FlowElement<SetHeadersSourceData, ?> flowElement) {
                    return new SetHeadersSourceData(
                        loggerFactory.getLogger(
                            SetHeadersSourceData.class.getName()),
                        flowData);
                }
            },
			propertyNameValues);
		
		element = new SetHeadersElement(
			loggerFactory.getLogger(SetHeadersElement.class.getName()),
			new ElementDataFactory<SetHeadersData>() {
                @Override
                public SetHeadersData create(
                    FlowData flowData,
                    FlowElement<SetHeadersData, ?> flowElement) {
                    return new SetHeadersData(
                        loggerFactory.getLogger(
                            SetHeadersData.class.getName()),
                        flowData);
                }
            });
		pipeline = new PipelineBuilder(loggerFactory)
			.addFlowElement(sourceElement)
			.addFlowElement(element)
			.build();
	}
	
	private SetHeadersData getFromFlowData(FlowData data) {
		Map<String, Object> output = data.elementDataAsMap();
		Object elementOutput = output.get(element.getElementDataKey());
		assertNotNull(elementOutput);
		assertTrue(elementOutput instanceof SetHeadersData);
		SetHeadersData typedOutput = (SetHeadersData)elementOutput;
		assertNotNull(typedOutput.getResponseHeaderDictionary());
		return typedOutput;
	}
	
	private class SetHeadersSourceData extends ElementDataBase {
		public SetHeadersSourceData(Logger logger, FlowData flowData) {
			super(logger, flowData);
		}
	}
	
	private class ActivePropertySourceElement
		extends FlowElementBase<SetHeadersSourceData, ElementPropertyMetaData> {
		Map<String, Object> propertyNameValuesToReturn;
		String elementDataKey = "setheadersourceelement";
		EvidenceKeyFilter evidenceKeyFilter;
		
		public ActivePropertySourceElement(
			Logger logger,
			ElementDataFactory<SetHeadersSourceData> elementDataFactory,
			Map<String, Object> propertyNameValuesToReturn) {
			super(logger, elementDataFactory);
			this.propertyNameValuesToReturn = propertyNameValuesToReturn;
			evidenceKeyFilter =
				new EvidenceKeyFilterWhitelist(new ArrayList<String>());
		}

		@Override
		protected void processInternal(FlowData data) throws Exception {
			ElementData sourceData =
				data.getOrAdd(elementDataKey, getDataFactory());
			sourceData.populateFromMap(propertyNameValuesToReturn);
		}

		@Override
		public String getElementDataKey() {
			return elementDataKey;
		}

		@Override
		public EvidenceKeyFilter getEvidenceKeyFilter() {
			return evidenceKeyFilter;
		}

		@Override
		public List<ElementPropertyMetaData> getProperties() {
			List<ElementPropertyMetaData> properties =
				new ArrayList<ElementPropertyMetaData>();
			propertyNameValuesToReturn.forEach((k, v) -> {
				properties.add(
					new ElementPropertyMetaDataDefault(
						k, this, "", Object.class, true));
			});
			return properties;
		}

		@Override
		protected void managedResourcesCleanup() {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void unmanagedResourcesCleanup() {
			// TODO Auto-generated method stub
			
		}
		
	}
}

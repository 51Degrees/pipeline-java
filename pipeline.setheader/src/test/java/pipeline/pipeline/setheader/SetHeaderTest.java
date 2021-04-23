package pipeline.pipeline.setheader;

import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement.DataFactory;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.setheader.data.SetHeaderData;
import fiftyone.pipeline.setheader.flowelements.SetHeaderDataInternal;
import fiftyone.pipeline.setheader.flowelements.SetHeaderElement;
import fiftyone.pipeline.setheader.flowelements.SetHeaderElementBuilder;

import static fiftyone.pipeline.setheader.Constants.*;

public class SetHeaderTest {
	private FlowData flowData;
	private Pipeline pipeline;
	private TestLoggerFactory loggerFactory;
	
	public SetHeaderTest() {
		ILoggerFactory internalLogger = mock(ILoggerFactory.class);
		when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
		loggerFactory = new TestLoggerFactory(internalLogger);
	}
	
	@BeforeEach
	public void init() {
		flowData = mock(FlowData.class);
		pipeline = mock(Pipeline.class);
		when(flowData.getPipeline()).thenReturn(pipeline);
	}
	
	/**
	 * Check that when valid SetHeader properties are available, the
	 * SetHeaderElement correctly construct the Response Header.
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void SetHeader_ValidSetHeaders() throws Exception {
		ElementData elementData = mock(ElementData.class);
		Map<String, Object> validSetHeaders =
			new HashMap<String, Object>();
		
		SetHeaderElement setHeaderElement =
				new SetHeaderElementBuilder(loggerFactory).build();
		
		// Set the mock data for device engine.
		validSetHeaders.put(
			"SetHeaderBrowserAccept-CH",
			new AspectPropertyValueDefault<String>("TestBrowserAccept-CH"));
		validSetHeaders.put(
			"SetHeaderPlatformAccept-CH",
			new AspectPropertyValueDefault<String>("TestPlatformAccept-CH"));
		validSetHeaders.put(
			"SetHeaderHardwareAccept-CH",
			new AspectPropertyValueDefault<String>("TestHardwareAccept-CH"));
		validSetHeaders.put(
			"SetHeaderBrowserCritical-CH",
			new AspectPropertyValueDefault<String>("TestBrowserCritical-CH"));
		validSetHeaders.put(
			"SetHeaderPlatformCritical-CH",
			new AspectPropertyValueDefault<String>("TestPlatformCritical-CH"));
		validSetHeaders.put(
			"SetHeaderUnknownCritical-CH",
			new AspectPropertyValueDefault<String>("Unknown"));
		validSetHeaders.put(
			"SetHeaderNullCritical-CH",
			new AspectPropertyValueDefault<String>());
		when(elementData.asKeyMap()).thenReturn(validSetHeaders);
		
		// Set expected returned value for flowData
		Logger testLogger = loggerFactory.getLogger(
				SetHeaderDataInternal.class.getName());
		SetHeaderData setHeaderData = new SetHeaderDataInternal(
			testLogger, flowData);
		when(flowData.getOrAdd(eq(SET_HEADER_ELEMENT_DATAKEY),
			any(DataFactory.class))).thenReturn(setHeaderData);
		when(flowData.get(DEVICE_ELEMENT_DATAKEY)).thenReturn(elementData);
		
		// Process the flow data with setHeaderElement
		setHeaderElement.process(flowData);
		
		// Check that the returned type is a HashMap
		assertTrue(
			setHeaderData.get(RESPONSE_HEADER_PROPERTY_NAME) instanceof HashMap);
		
		// Get the response header property from the flow data
		HashMap<String, String> responseHeaderDict = 
			(HashMap<String, String>)setHeaderData.get(
				RESPONSE_HEADER_PROPERTY_NAME);
		String acceptCHValue = responseHeaderDict.get("Accept-CH");
		String criticalCHValue = responseHeaderDict.get("Critical-CH"); 
		assertTrue(acceptCHValue.matches("^[a-zA-Z-]+,[a-zA-Z-]+,[a-zA-Z-]+$"));
		assertTrue(criticalCHValue.matches("^[a-zA-Z-]+,[a-zA-Z-]+$"));
		assertTrue(acceptCHValue.contains("TestBrowserAccept-CH"));
		assertTrue(acceptCHValue.contains("TestPlatformAccept-CH"));
		assertTrue(acceptCHValue.contains("TestHardwareAccept-CH"));
		assertTrue(criticalCHValue.contains("TestBrowserCritical-CH"));
		assertTrue(criticalCHValue.contains("TestPlatformCritical-CH"));
		assertFalse(criticalCHValue.contains("Unknown"));
	}
	
}

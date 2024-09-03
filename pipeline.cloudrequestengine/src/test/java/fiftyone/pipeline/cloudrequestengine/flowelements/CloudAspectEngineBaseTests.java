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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import static fiftyone.pipeline.cloudrequestengine.Constants.Messages.ExceptionFailedToLoadProperties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiftyone.common.testhelpers.TestLoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.AspectEngineBase;

public class CloudAspectEngineBaseTests {
    public class TestData extends AspectDataBase {
        public TestData(
            Logger logger,
            FlowData flowData,
            AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
            super(logger, flowData, engine);
        }
    }

    private class TestInstance extends CloudAspectEngineBase<TestData> {
        public TestInstance() {
            super(loggerFactory.getLogger("TestInstance"), new ElementDataFactory<TestData>() {
                @Override
                public TestData create(FlowData flowData, FlowElement<TestData, ?> flowElement) {
                    return new TestData(null, flowData, (AspectEngine<TestData, ?>) flowElement);
                }
            });
        }

        @Override
        public String getElementDataKey() {
            return "test";
        }

        private final EvidenceKeyFilter evidenceKeyFilter = new EvidenceKeyFilterWhitelist(new ArrayList<String>());

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return evidenceKeyFilter;
        }

        @Override
        protected void unmanagedResourcesCleanup() {

        }

        @Override
        protected void processCloudEngine(FlowData data, TestData aspectData, String json) {
            if (json == null || json.isEmpty())  
            {
                fail("'json' value should not be null or empty if " +
                        "this method is called");                
            }        
        }
    }

    private class TestRequestEngine extends AspectEngineBase<CloudRequestData, AspectPropertyMetaData>
        implements CloudRequestEngine {

        public TestRequestEngine() {
            super(loggerFactory.getLogger("TestRequestEngine"), new ElementDataFactory<CloudRequestData>() {
                @Override
                public CloudRequestData create(FlowData flowData, FlowElement<CloudRequestData, ?> flowElement) {
                    return new CloudRequestData(loggerFactory.getLogger("TestRequestEngine"), flowData, (AspectEngine<CloudRequestData, ?>) flowElement);
                }
            });
        }

        private Map<String, AccessiblePropertyMetaData.ProductMetaData> publicProperties;
        @Override
        public Map<String, AccessiblePropertyMetaData.ProductMetaData> getPublicProperties() {
            return publicProperties;
        }

        public void setPublicProperties(Map<String, AccessiblePropertyMetaData.ProductMetaData> properties) {
            this.publicProperties = properties;
        }

        @Override
        public String getDataSourceTier() {
            return "";
        }

        @Override
        public String getElementDataKey() {
            return "cloud";
        }

        private final EvidenceKeyFilter evidenceKeyFilter = new EvidenceKeyFilterWhitelist(new ArrayList<String>());
        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return evidenceKeyFilter;
        }

        private final List<AspectPropertyMetaData> properties = new ArrayList<>();
        @Override
        public List<AspectPropertyMetaData> getProperties() {
            return properties;
        }

        @Override
        protected void unmanagedResourcesCleanup() {

        }

        @Override
        protected void processEngine(FlowData data, CloudRequestData aspectData) {

        }

    }

    private TestInstance engine;
    private TestRequestEngine requestEngine;
    private Pipeline pipeline;

    private Map<String, AccessiblePropertyMetaData.ProductMetaData> propertiesReturnedByRequestEngine;
    private TestLoggerFactory loggerFactory;
    private int maxWarnings;
    private int maxErrors;

    @BeforeEach
    public void init() {
        propertiesReturnedByRequestEngine = new HashMap<>();
        loggerFactory = new TestLoggerFactory(null);
        maxWarnings = 0;
        maxErrors = 0;
    }

    @AfterEach
    public void cleanUp() {
        loggerFactory.assertMaxWarnings(maxWarnings);
        loggerFactory.assertMaxErrors(maxErrors);
    }
    
    private <T extends ElementPropertyMetaData> boolean propertiesContainName(
            List<T> properties,
            String name) {
        for (T property : properties) {
            if (property.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Test the LoadProperties method of the CloudAspectEngine which
     * retrieves property meta-data from the cloud request engine.
     * @throws Exception 
     */
    @Test
    public void loadProperties() throws Exception {
        List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevendor", "String", null, null));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevariants", "Array", null, null));
        AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);

        createPipeline();

        assertEquals(3, engine.getProperties().size());
        assertTrue(propertiesContainName(engine.getProperties(), "ismobile"));
        assertTrue(propertiesContainName(engine.getProperties(), "hardwarevendor"));
        assertTrue(propertiesContainName(engine.getProperties(), "hardwarevariants"));
    }
    
    /**
    * Test that an exception is thrown by the Properties auto property if 
    * the cloud request engine returns no properties.
    * @throws Exception 
    */
    @Test
    public void loadProperties_noProperties() throws Exception {
        try {
            createPipeline();
            fail("RuntimeException should be thrown");
        } catch (RuntimeException ex){
            assertEquals(
                String.format(
                    ExceptionFailedToLoadProperties,
                    "test",
                    "test"),
                ex.getMessage()
            );
        }
        maxErrors = 1;
    }
    
    /**
     * Test that an exception is thrown by the Properties auto property if
     * the cloud engine only returns properties for other engines.
     * @throws Exception 
     */
    @Test
    public void loadProperties_wrongProperties() throws Exception {
        List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevendor", "String", null, null));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevariants", "Array", null, null));
        AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test2", devicePropertyData);

        try {
            createPipeline();
            fail("RuntimeException should be thrown");
        } catch (RuntimeException ex){
            assertEquals(
                String.format(
                    ExceptionFailedToLoadProperties,
                    "test",
                    "test"),
                ex.getMessage()
            );
        }

        maxErrors = 1;
    }

    /**
     * Test loading sub-property meta data where a cloud aspect engine 
     * has nested properties. E.g. the cloud property keyed engine.
     * @throws Exception 
     */
    @Test
    public void loadProperties_subProperties() throws Exception {
        List<AccessiblePropertyMetaData.PropertyMetaData> subProperties = new ArrayList<>();
        subProperties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null));
        subProperties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevendor", "String", null, null));
        subProperties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevariants", "Array", null, null));

        List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("devices", "Array", null, subProperties));

        AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);

        createPipeline();

        assertEquals(1, engine.getProperties().size());
        assertEquals("devices", engine.getProperties().get(0).getName());
        assertEquals(3, engine.getProperties().get(0).getItemProperties().size());
        assertTrue(propertiesContainName(engine.getProperties().get(0).getItemProperties(), "ismobile"));
        assertTrue(propertiesContainName(engine.getProperties().get(0).getItemProperties(), "hardwarevendor"));
        assertTrue(propertiesContainName(engine.getProperties().get(0).getItemProperties(), "hardwarevariants"));
    }
    
    /**
     * Test loading delayed evidence property meta data.
     * @throws Exception 
     */
    @Test
    public void loadProperties_delayedProperties() throws Exception {
        List<String> evidenceProperties = new ArrayList<>();
        evidenceProperties.add("javascript");
        
        List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("javascript", "JavaScript", null, null, true, null));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevendor", "String", null, null, null, evidenceProperties));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevariants", "Array", null, null, null, evidenceProperties));
        AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);

        createPipeline();

        assertEquals(3, engine.getProperties().size());
        assertTrue(propertiesContainName(engine.getProperties(), "javascript"));
        assertTrue(propertiesContainName(engine.getProperties(), "hardwarevendor"));
        assertTrue(propertiesContainName(engine.getProperties(), "hardwarevariants"));
        assertTrue(engine.getProperty("javascript").getDelayExecution());
        assertEquals(1, engine.getProperty("hardwarevendor").getEvidenceProperties().size());
        assertEquals(1, engine.getProperty("hardwarevariants").getEvidenceProperties().size());
    }

    /*
     * Test that when processing the cloud aspect engine, the
     * ProcessCloudEngine Method is called when the JSON response is
     * populated.
     */
    @Test
    public void Process_CloudResponse() {
        // Setup properties.
    	List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();;
    	properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null, true, null));
    	AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestInstance mockTestInstance = Mockito.spy(new TestInstance());
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestRequestEngine mockRequestEngine = Mockito.spy(new TestRequestEngine()); 
        mockRequestEngine.publicProperties = propertiesReturnedByRequestEngine;
        Answer<Object> responseStringAnswer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            	CloudRequestData aspectData = (CloudRequestData)invocationOnMock.getArgument(1);
            	aspectData.setJsonResponse("{ \"response\": true }");
            	aspectData.setProcessStarted(true);
				return null;
            }
        };
        doAnswer(responseStringAnswer)
        	.when(mockRequestEngine)
        		.processEngine(any(FlowData.class), any(CloudRequestData.class));
       
        try {
			pipeline = new PipelineBuilder(loggerFactory)
			        .addFlowElement(mockRequestEngine)
			        .addFlowElement(mockTestInstance)
			        .build();
			FlowData flowData = pipeline.createFlowData();
			flowData.addEvidence("query.user-agent", "iPhone");
			flowData.process();
			
		} catch (Exception e) {
			fail("An exception has occurred here.");
		}
 
        // Verify the ProcessCloudEngine method was called
        verify(mockTestInstance, times(1)) 
        .processCloudEngine(any(FlowData.class), any(TestData.class), any(String.class));
    }
 
    /*
     * Test that when processing the cloud aspect engine, the
     * ProcessCloudMethod is not called when the JSON response is not
     * populated.
     */
    @Test
    public void Process_NoCloudResponse() {
        // Setup properties.
    	List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();;
    	properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null, true, null));
    	AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestInstance mockTestInstance = Mockito.spy(new TestInstance());
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestRequestEngine mockRequestEngine = Mockito.spy(new TestRequestEngine()); 
        mockRequestEngine.publicProperties = propertiesReturnedByRequestEngine;
        Answer<Object> responseStringAnswer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            	CloudRequestData aspectData = (CloudRequestData)invocationOnMock.getArgument(1);
            	aspectData.setProcessStarted(true);
				return null;
            }
        };
        doAnswer(responseStringAnswer)
        	.when(mockRequestEngine)
        		.processEngine(any(FlowData.class), any(CloudRequestData.class));

        try {
			pipeline = new PipelineBuilder(loggerFactory)
			        .addFlowElement(mockRequestEngine)
			        .addFlowElement(mockTestInstance)
			        .build();
			FlowData flowData = pipeline.createFlowData();
			flowData.addEvidence("query.user-agent", "iPhone");
			flowData.process();
			
		} catch (Exception e) {
			fail("An exception has occurred here.");
		}
 
        // Verify the ProcessCloudEngine method was called
        verify(mockTestInstance,times(0))
            .processCloudEngine(
                any(FlowData.class),
                any(TestData.class),
                any(String.class));

        maxWarnings = 1;
    }
    
    /*
     * Test that the expected exception is thrown when the
     * CloudRequestEngine and a CloudAspectEngine have been added to the
     * Pipeline in the wrong order.
     */
    @Test
    public void CloudEngines_WrongOrder() {
        // Setup properties.
    	List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();;
    	properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null, true, null));
    	AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestInstance mockTestInstance = Mockito.spy(new TestInstance());
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestRequestEngine mockRequestEngine = Mockito.spy(new TestRequestEngine()); 
        mockRequestEngine.publicProperties = propertiesReturnedByRequestEngine;
        Answer<Object> responseStringAnswer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            	CloudRequestData aspectData = (CloudRequestData)invocationOnMock.getArgument(1);
            	aspectData.setJsonResponse("{ \"response\": true }");
            	aspectData.setProcessStarted(true);
				return null;
            }
        };
        doAnswer(responseStringAnswer)
        	.when(mockRequestEngine)
        		.processEngine(any(FlowData.class), any(CloudRequestData.class));
       
        try {
			pipeline = new PipelineBuilder(loggerFactory)
			        .addFlowElement(mockTestInstance)
			        .addFlowElement(mockRequestEngine)
			        .build();
			FlowData flowData = pipeline.createFlowData();
			flowData.addEvidence("query.user-agent", "iPhone");
			flowData.process();
			fail("Expected exception was not thrown");
			
		} catch (Exception ex) {
			Throwable[] exceptions= ex.getSuppressed();
		    assertTrue(exceptions.length > 0);
		    assertTrue(exceptions[0] instanceof PipelineConfigurationException);
		    assertTrue(exceptions[0].getMessage().contains("requires a 'CloudRequestEngine'before"
		    		+ " it in the Pipeline. This engine will be unable to produce"
		    		+ " results until this is corrected"));		    
		}
        maxErrors = 1;
    }

    /**
     * Test that the expected exception is thrown when the 
     * CloudRequestEngine has not been added to the Pipeline but a
     * CloudAspectEngine has.
     */
    public void CloudEngines_NoRequestEngine() {
    	TestInstance engine = new TestInstance();
		try {
			pipeline = new PipelineBuilder(loggerFactory)
			        .addFlowElement(engine)
			        .build();
			fail("Expected exception was not thrown");
		} catch (Exception e) {
			assertTrue(e instanceof PipelineConfigurationException);
		}
    }
    
	private void createPipeline() throws Exception {
        engine = new TestInstance();
        requestEngine = new TestRequestEngine();
        requestEngine.setPublicProperties(propertiesReturnedByRequestEngine);
        pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(requestEngine)
            .addFlowElement(engine)
            .build();
    }
}

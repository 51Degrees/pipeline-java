package fiftyone.pipeline.cloudrequestengine.flowelements;

import static fiftyone.pipeline.cloudrequestengine.Constants.Messages.ExceptionFailedToLoadProperties;
import static org.junit.jupiter.api.Assertions.*;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.AspectEngineBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudAspectEngineBaseTests {
    private class TestData extends AspectDataBase {
        public TestData(
            Logger logger,
            FlowData flowData,
            AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
            super(logger, flowData, engine);
        }
    }

    private class TestInstance extends CloudAspectEngineBase<TestData> {
        public TestInstance() {
            super(LoggerFactory.getLogger("TestInstance"), new ElementDataFactory<TestData>() {
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
        protected void processEngine(FlowData data, TestData aspectData) {
        }
    }

    private class TestRequestEngine extends AspectEngineBase<CloudRequestData, AspectPropertyMetaData>
        implements CloudRequestEngine {

        public TestRequestEngine() {
            super(LoggerFactory.getLogger("TestRequestEngine"), new ElementDataFactory<CloudRequestData>() {
                @Override
                public CloudRequestData create(FlowData flowData, FlowElement<CloudRequestData, ?> flowElement) {
                    return new CloudRequestData(null, flowData, (AspectEngine<CloudRequestData, ?>) flowElement);
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

    @BeforeEach
    public void init() {
        propertiesReturnedByRequestEngine = new HashMap<>();
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

    private void createPipeline() throws Exception {
        engine = new TestInstance();
        requestEngine = new TestRequestEngine();
        requestEngine.setPublicProperties(propertiesReturnedByRequestEngine);
        pipeline = new PipelineBuilder(LoggerFactory.getILoggerFactory())
            .addFlowElement(requestEngine)
            .addFlowElement(engine)
            .build();
    }
}

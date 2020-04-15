package fiftyone.pipeline.cloudrequestengine;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudAspectEngineBase;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.AspectEngineBase;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CloudAspectEngineBaseTests {
    private class TestData extends AspectDataBase {
        public TestData(
            Logger logger,
            FlowData flowData,
            AspectEngine engine) {
            super(logger, flowData, engine);
        }
    }

    private class TestInstance extends CloudAspectEngineBase<TestData> {
        public TestInstance() {
            super(LoggerFactory.getLogger("TestInstance"), new ElementDataFactory<TestData>() {
                @Override
                public TestData create(FlowData flowData, FlowElement<TestData, ?> flowElement) {
                    return new TestData(null, flowData, (AspectEngine) flowElement);
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
                    return new CloudRequestData(null, flowData, (AspectEngine) flowElement);
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

    @Before
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

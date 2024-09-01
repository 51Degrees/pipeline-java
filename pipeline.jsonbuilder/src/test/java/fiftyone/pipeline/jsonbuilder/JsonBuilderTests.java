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

package fiftyone.pipeline.jsonbuilder;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import fiftyone.pipeline.engines.testhelpers.flowelements.*;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;

import java.util.*;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

@SuppressWarnings("unused")
public class JsonBuilderTests {
    private TestLoggerFactory loggerFactory;
    private EmptyEngine testEngine;
    private JsonBuilderElement jsonBuilderElement;
    private ElementData elementDataMock;
    private FlowData flowData;
    private JsonBuilderData result;
    private Pipeline pipeline;
    
    public JsonBuilderTests() {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }
    
    @BeforeEach
    public void Init() {
        flowData = mock(FlowData.class);
        
        testEngine = mock(EmptyEngine.class, new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new Exception("The method '" +
                invocationOnMock.getMethod().getName() + "' should not have been called.");
            }});
            
        List<AspectPropertyMetaData> properties = new ArrayList<>();
        properties.add(new AspectPropertyMetaDataDefault("property", testEngine, "", String.class, new ArrayList<String>(), true));
        properties.add(new AspectPropertyMetaDataDefault("jsproperty", testEngine, "", String.class, new ArrayList<String>(), true));

        doReturn(properties).when(testEngine).getProperties();
        doReturn("test").when(testEngine).getElementDataKey();
        
        jsonBuilderElement = new JsonBuilderElementBuilder(loggerFactory)
            .build();

        elementDataMock = mock(ElementData.class);
        
        Map<String,Object> data = new HashMap<>();
        data.put("property", "thisIsAValue" );
        data.put("jsproperty", "var = 'some js code';");
        
        doReturn(data).when(elementDataMock).asKeyMap();

        pipeline = mock(Pipeline.class);
        //when(pipeline.hashCode()).thenReturn(1);
        Map<String, Map<String, ElementPropertyMetaData>> propertyMetaData =
            new HashMap<>();
        when(pipeline.getElementAvailableProperties()).thenReturn(propertyMetaData);
    }

    /**
     * Check that the JSON produced by the JsonBuilder is valid.
     * @throws Exception
     */
    @Test
    public void JsonBuilder_ValidJson() throws Exception {
        String json = testIteration(1);

        assertTrue(isExpectedJson(json));
    }

    /**
     * Check that the JSON element removes JavaScript properties from the
     * response after max number of iterations has been reached.
     * @throws Exception
     */
    @Test
    public void JsonBuilder_MaxIterations() throws Exception {
        for (int i = 0; true; i++)
        {
            Map<String, Object> jsProperties = new HashMap<>();
            jsProperties.put(
                "test.jsproperty",
                new AspectPropertyValueDefault<JavaScript>(
                    new JavaScript("var = 'some js code';")));
            String json = testIteration(i, null, jsProperties);
            boolean result = containsJavaScriptProperties(json);

            if (i >= Constants.MAX_JAVASCRIPT_ITERATIONS) {
                assertFalse(result);
                break;
            }
            else {
                assertTrue(result);
            }
        }
    }

    /**
     * Check that entries will not appear in the output for blacklisted elements.
     * @throws Exception
     */
    @Test
    public void JsonBuilder_ElementBlacklist() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("test", elementDataMock);
        map.put("cloud-response", elementDataMock);
        map.put("json-builder", elementDataMock);
        String json = testIteration(1, map);

        assertTrue(isExpectedJson(json));
        JSONObject obj = new JSONObject(json);
        assertEquals(
            1,
            obj.length(),
            "There should only be the 'test' key at the top level as " +
                "the other elements should have been ignored. Complete JSON: " +
                System.lineSeparator() + json);
    }

    /**
     * Data class used in the nested properties test.
     */
    private class NestedData extends ElementDataBase {
        public NestedData(Logger logger, FlowData data, String value1, int value2) {
            this(logger, data);
            setValue1(value1);
            setValue2(value2);
        }

        public NestedData(Logger logger, FlowData data) {
            super(logger, data);
        }

        public String getValue1() {
            return (String)get("Value1");
        }
        public void setValue1(String value) {
            put("Value1", value);
        }
        public int getValue2() {
            return (int)get("Value2");
        }
        public void setValue2(int value) {
            put("Value2", value);
        }
    }

    /**
     * Check that nested properties are serialised as expected
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void JsonBuilder_NestedProperties() throws Exception {

        Map<String, Object> map = new HashMap<>();
        map.put("property", new ArrayList<NestedData>());
        ((List<NestedData>)map.get("property")).add(new NestedData(
            loggerFactory.getLogger(NestedData.class.getSimpleName()),
            flowData,
            "abc",
            123));
        ((List<NestedData>)map.get("property")).add(new NestedData(
            loggerFactory.getLogger(NestedData.class.getSimpleName()),
            flowData,
            "xyz",
            789));
        when(elementDataMock.asKeyMap()).thenReturn(map);

        // Configure the property meta-data as needed for
        // this test.
        Map<String, Map<String, ElementPropertyMetaData>> propertyMetaData =
            new HashMap<>();
        Map<String, ElementPropertyMetaData> testElementMetaData = new HashMap<>();
        List<ElementPropertyMetaData> nestedMetaData = Arrays.asList(
            (ElementPropertyMetaData)new ElementPropertyMetaDataDefault("value1", testEngine, "value1", String.class, true),
            new ElementPropertyMetaDataDefault("value2", testEngine, "value2", int.class, true));
        ElementPropertyMetaData p1 = new ElementPropertyMetaDataDefault(
            "properrty",
            testEngine,
            "property",
            List.class,
            true,
            nestedMetaData,
            false,
            null);
        testElementMetaData.put("property", p1);
        propertyMetaData.put("test", testElementMetaData);
        when(pipeline.getElementAvailableProperties()).thenReturn(propertyMetaData);

        String json = testIteration(1);

        JSONObject obj = new JSONObject(json);
        assertTrue(
            obj.length() == 1 &&
            obj.has("test"),
            "There should only be the 'test' key at the top level. " +
                "Complete JSON: " + System.lineSeparator() + json);
        JSONObject element = obj.getJSONObject("test");

        assertTrue(
            element.length() == 1 &&
            element.names().get(0).equals("property"),"There should only be one property, named 'property'," +
                " under the 'test' element. " +
                "Complete JSON: " + System.lineSeparator() + json);

        assertEquals(
            2,
            element.getJSONArray("property").length(),
            "There should be two properties, 'value1' and 'value2', " +
                "under the 'test.property[0]' entry. " +
                "Complete JSON: " + System.lineSeparator() + json);

        assertTrue(element.getJSONArray("property").getJSONObject(0).has("value1") &&
        element.getJSONArray("property").getJSONObject(1).has("value2"));

        assertTrue(
            element.getJSONArray("property").getJSONObject(1).length() == 2 &&
            element.getJSONArray("property").getJSONObject(1).has("value1") &&
            element.getJSONArray("property").getJSONObject(1).has("value2"),
            "There should be two properties, 'value1' and 'value2', " +
                "under the 'test.property[0]' entry. " +
                "Complete JSON: " + System.lineSeparator() + json);
    }

    private class DelayExecutionArgs {
        public boolean delayExecution;
        public boolean propertyValueNull;
    }

    private static Stream<Arguments> getDelayArgs() {
        return Stream.of(
            Arguments.of(true, true),
            Arguments.of(false, false),
            Arguments.of(true, false),
            Arguments.of(false, true));
    }

    /**
     * Check that delayed execution and evidence properties values are populated
     * correctly
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("getDelayArgs")
    public void JsonBuilder_DelayedExecution(
        boolean delayExecution,
        boolean propertyValueNull) throws Exception {
        // todo...
        // Configure the property meta-data as needed for
        // this test.
        Map<String, Map<String, ElementPropertyMetaData>> propertyMetaData = new HashMap<>();
        Map<String, ElementPropertyMetaData> testElementMetaData = new HashMap<>();
        ElementPropertyMetaData p1 = new ElementPropertyMetaDataDefault(
            "property",
            testEngine,
            "property",
            String.class,
            true,
            null,
            false,
            Arrays.asList("jsproperty"));
        testElementMetaData.put("property", p1);
        ElementPropertyMetaData p2 = new ElementPropertyMetaDataDefault(
            "jsproperty",
            testEngine,
            "jsproperty",
            JavaScript.class,
            true,
            null,
            delayExecution,
            null);
        testElementMetaData.put("jsproperty", p2);
        propertyMetaData.put("test", testElementMetaData);
        when(pipeline.getElementAvailableProperties()).thenReturn(propertyMetaData);

        // Run the test
        String json = testIteration(1);

        // Verify that the *delayexecution and *evidenceproperties
        // values are populated as expected.
        JSONObject obj = new JSONObject(json);
        JSONObject results = obj.getJSONObject("test");

        boolean hasEvidenceProperties = results.toString().contains("propertyevidenceproperties");
        boolean hasDelayedExecution = results.toString().contains("jspropertydelayexecution");

        assertEquals(
            delayExecution,
            hasEvidenceProperties,
            "The JSON data does " + (delayExecution ? "not" : "") +
                "contain a 'propertyevidenceproperties' property. " +
                "Complete JSON: " + json);
        assertEquals(
            delayExecution,
            hasDelayedExecution,
            "The JSON data does " + (delayExecution ? "not" : "") +
                "contain a 'jspropertydelayexecution' property. " +
                "Complete JSON: " + json);
    }

    /**
     * Check that delayed execution and evidence properties values are populated
     * correctly when a property has multiple evidence properties
     * @throws Exception
     */ 
    @Test
    public void JsonBuilder_MultipleEvidenceProperties() throws Exception {
        // Configure the property meta-data as needed for
        // this test.
        // property is populated by 2 JavaScript properties:
        // jsproperty and jsproperty2.
        // jsproperty has delayed execution true and
        // jsproperty2 does not.
        Map<String, Map<String, ElementPropertyMetaData>> propertyMetaData = new HashMap<>();
        Map<String, ElementPropertyMetaData> testElementMetaData = new HashMap<>();
        ElementPropertyMetaData p1 = new ElementPropertyMetaDataDefault(
            "property",
            testEngine,
            "property",
            String.class,
            true,
            null,
            false,
            Arrays.asList("jsproperty", "jsproperty2"));
        testElementMetaData.put("property", p1);
        ElementPropertyMetaData p2 = new ElementPropertyMetaDataDefault(
            "jsproperty",
            testEngine,
            "jsproperty",
            JavaScript.class,
            true,
            null,
            true,
            null);
        testElementMetaData.put("jsproperty", p2);
        ElementPropertyMetaData p3 = new ElementPropertyMetaDataDefault(
            "jsproperty2",
            testEngine,
            "jsproperty2",
            JavaScript.class,
            true,
            null,
            false,
            null);
        testElementMetaData.put("jsproperty2", p2);
        propertyMetaData.put("test", testElementMetaData);
        when(pipeline.getElementAvailableProperties()).thenReturn(propertyMetaData);

        Map<String, Object> map = new HashMap<>();
        map.put("property", "thisIsAValue");
        map.put("jsproperty", "var = 'some js code';");
        map.put("jsproperty2", "var = 'some js code';");
        when(elementDataMock.asKeyMap()).thenReturn(map);

        // Run the test
        String json = testIteration(1);

        // Verify that the *delayexecution and *evidenceproperties
        // values are populated as expected.
        JSONObject obj = new JSONObject(json);
        JSONObject results = obj.getJSONObject("test");

        assertTrue(
            results.toString().contains("propertyevidenceproperties"),
            "Expected the JSON to contain a 'propertyevidenceproperties' " +
                "item. Complete JSON: " + json);
        final JSONArray[] pep = new JSONArray[1];
        results.names().forEach(name -> {
            if (name.toString().contains("propertyevidenceproperties")) {
                pep[0] = results.getJSONArray(name.toString());
            }
        });
        assertTrue(
            pep[0].toList().contains("test.jsproperty"),
            "Expected the JSON to contain a 'propertyevidenceproperties' " +
                "item where the value is an array with one item, 'test.jsproperty'." +
                "Complete JSON: " + json);
        assertTrue(
            results.toString().contains("jspropertydelayexecution"),
            "Expected the JSON to contain a 'jspropertydelayexecution' " +
                "item. Complete JSON: " + json);
        assertFalse(
            results.toString().contains("jsproperty2delayexecution"),
            "Expected the JSON not to contain a 'jsproperty2delayexecution' " +
                "item. Complete JSON: " + json);
    }

    /**
     *  Check that the JSON produced by the JsonBuilder is correct when lazy
     *  loading is enabled.
     * @throws Exception
     */
    @Test
    public void JsonBuilder_LazyLoading() throws Exception {
        EmptyEngine engine = new EmptyEngineBuilder(loggerFactory)
            .setLazyLoading(new LazyLoadingConfiguration(1000))
            .setProcessCost(500)
            .build();
        JsonBuilderElement jsonBuilder = new JsonBuilderElementBuilder(loggerFactory)
            .build();
        SequenceElement sequenceElement = new SequenceElementBuilder(loggerFactory)
            .build();
        Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(sequenceElement)
            .addFlowElement(engine)
            .addFlowElement(jsonBuilder)
            .build();

        try (FlowData flowData = pipeline.createFlowData()) {

            flowData.process();
    
            JsonBuilderData jsonResult = flowData.get(JsonBuilderData.class);
            assertNotNull(jsonResult);
            assertNotNull(jsonResult.getJson());
    
            JSONObject jsonData = new JSONObject(jsonResult.getJson());
            assertEquals(1, jsonData.getJSONObject("empty-aspect").getLong("valueone"));
            assertEquals(2, jsonData.getJSONObject("empty-aspect").getLong("valuetwo"));
        }
    }

    private String testIteration(int iteration) throws Exception {
        return testIteration(iteration, null);
    }

    private String testIteration(
        int iteration,
        Map<String, Object> data) throws Exception {
        return testIteration(iteration, data, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String testIteration(
        int iteration,
        Map<String, Object> data,
        Map<String, Object> jsProperties) throws Exception {
        if(data == null) {
            data = new HashMap<>();
            data.put("test", elementDataMock);
        }
        if(jsProperties == null) {
            jsProperties = new HashMap<>();
        }
        Map<String, String> jsPropertyNames = new HashMap<>();
        for (Map.Entry<String, Object> entry : jsProperties.entrySet()) {
            jsPropertyNames.put(entry.getKey(), entry.getValue().toString());
        }

        FlowData flowData = mock(FlowData.class);
        MissingPropertyService missingPropertyService = mock(MissingPropertyService.class);

        when(flowData.elementDataAsMap()).thenReturn(data);
        TryGetResult<Object> session = new TryGetResult<>();
        session.setValue("somesessionid");
        TryGetResult<Integer> sequence = new TryGetResult<>();
        sequence.setValue(iteration);
        when(flowData.tryGetEvidence(eq("query.session-id"), any(Class.class))).thenReturn(session);
        when(flowData.tryGetEvidence(eq("query.sequence"), any(Class.class))).thenReturn(sequence);
        when(flowData.getWhere(any(PropertyMatcher.class))).thenReturn(jsPropertyNames);

        final JsonBuilderData[] result = {null};
        when(flowData.getOrAdd(anyString(), any(FlowElement.DataFactory.class)))
            .thenAnswer((Answer<ElementData>) invocationOnMock -> {
                FlowElement.DataFactory f = invocationOnMock.getArgument(1);
                result[0] = (JsonBuilderData)f.create(flowData);
                return result[0];
            });
        when(flowData.getPipeline()).thenReturn(pipeline);

        jsonBuilderElement.process(flowData);

        String json = result[0].getJson().toString();

        return json;
    }

    private boolean isExpectedJson(String json) {
        JSONObject obj = new JSONObject(json);
        JSONObject results = obj.getJSONObject("test");

        for (Object result : results.names()) {
            String name = result.toString();
            if (name.contains("property") && results.get(name).toString().contains("thisIsAValue")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsJavaScriptProperties(String json) {
        JSONObject obj = new JSONObject(json);

        for (Object name : obj.names()) {
            if (name.toString().contains("javascriptProperties") ||
                obj.get(name.toString()).toString().contains("javascriptProperties")) {
                return true;
            }
        }
        return false;
    }

    public JsonBuilderData getResult() {
        return result;
    }

    public void setResult(JsonBuilderData result) {
        this.result = result;
    }
}

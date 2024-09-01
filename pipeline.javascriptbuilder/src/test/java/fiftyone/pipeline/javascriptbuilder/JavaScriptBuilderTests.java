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

package fiftyone.pipeline.javascriptbuilder;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.Evidence;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderDataInternal;
import org.json.JSONObject;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class JavaScriptBuilderTests {
    private TestLoggerFactory loggerFactory;
    private JsonBuilder jsonBuilderElement;
    private ElementData elementDataMock;
    private FlowData flowData;
    private JavaScriptBuilderData result;
    private JavaScriptBuilderElement javaScriptBuilderElement;
    private JSONObject json;
    
    public JavaScriptBuilderTests() {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }
    
    @BeforeEach
    public void Init() {
        flowData = mock(FlowData.class);
        
        jsonBuilderElement = mock(JsonBuilder.class);
        
        List<ElementPropertyMetaData> properties = new ArrayList<>();
        
        properties.add(new ElementPropertyMetaDataDefault("property", jsonBuilderElement, "", String.class, true));
        
        doReturn(properties).when(jsonBuilderElement).getProperties();

        javaScriptBuilderElement = new JavaScriptBuilderElementBuilder(loggerFactory).build();
        
        elementDataMock = mock(ElementData.class);
        
        Map<String,Object> data = new HashMap<>();
        data.put("property", "thisIsAValue" );
        
        doReturn(data).when(elementDataMock).asKeyMap();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void Valid_Js() throws Exception {
        json = new JSONObject();
        
        JSONObject device = new JSONObject();
        device.put("ismobile", true);
        device.put("browsername", JSONObject.NULL);
        
        json.put("device", device);
        json.put("nullValueReasons", new JSONObject().put("device.browsername", "property missing"));
        
        doAnswer(new Answer<Object>() { 
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                JsonBuilderData result = mock(JsonBuilderData.class);
                when(result.getJson()).thenReturn(json.toString(2));
                return result;
            }
        }).when(flowData).get(JsonBuilderData.class);
        
        when(flowData.getAs(anyString(), any(Class.class))).thenReturn(new AspectPropertyValueDefault<>("None"));

        final Map<String, Object> evidence = new HashMap<>(); 
        evidence.put( fiftyone.pipeline.javascriptbuilder.Constants.EVIDENCE_HOST_KEY, "localhost" );
        evidence.put(fiftyone.pipeline.core.Constants.EVIDENCE_PROTOCOL, "https" );
        
        Evidence evidenceObj = mock(Evidence.class);
        when(evidenceObj.asKeyMap()).thenReturn(evidence);
        
        when(flowData.getEvidence()).thenReturn(evidenceObj);
        
        when(flowData.tryGetEvidence(anyString(), any(Class.class))).thenAnswer(
            new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    TryGetResult<Object> result = new TryGetResult<>();
                    if (evidence.containsKey((String)invocation.getArgument(0))) {
                        result.setValue(evidence.get((String)invocation.getArgument(0)));
                    }
                    return result;
                }
            }
        );
        
        when(flowData.get(anyString())).thenReturn(elementDataMock);
        
        doAnswer(new Answer<Object>() { 
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                FlowElement.DataFactory<JavaScriptBuilderData> factory =
                        invocation.getArgument(1);
                result = factory.create(flowData);
                return result;
            }
        }).when(flowData).getOrAdd(anyString(), any(FlowElement.DataFactory.class));
        
        javaScriptBuilderElement.process(flowData);
     
        assertTrue(isValidFodObject(result.getJavaScript(), "device", "ismobile", true));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void JavaScriptBuilder_NoJson() throws Exception {
        doAnswer(new Answer<Object>() { 
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                JsonBuilderData result = mock(JsonBuilderData.class);
                when(result.getJson()).thenThrow(new PipelineDataException("nope"));
                return result;
            }
        }).when(flowData).get(JsonBuilderData.class);

        when(flowData.getAs(anyString(), any(Class.class))).thenReturn(new AspectPropertyValueDefault<>("None"));
        
        final Map<String, Object> evidence = new HashMap<>(); 
        evidence.put(fiftyone.pipeline.javascriptbuilder.Constants.EVIDENCE_HOST_KEY, "localhost" );
        evidence.put(fiftyone.pipeline.core.Constants.EVIDENCE_PROTOCOL, "https" );
        
        Evidence evidenceObj = mock(Evidence.class);
        when(evidenceObj.asKeyMap()).thenReturn(evidence);
        
        when(flowData.getEvidence()).thenReturn(evidenceObj);
        
        when(flowData.tryGetEvidence(anyString(), any(Class.class))).thenAnswer(
            new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    TryGetResult<Object> result = new TryGetResult<>();
                    if (evidence.containsKey((String)invocation.getArgument(0))) {
                        result.setValue(evidence.get((String)invocation.getArgument(0)));
                    }
                    return result;
                }
            }
        );
        
        when(flowData.get(anyString())).thenReturn(elementDataMock);
        
        doAnswer(new Answer<Object>() { 
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                FlowElement.DataFactory<JavaScriptBuilderData> factory =
                        invocation.getArgument(1);
                result = factory.create(flowData);
                return result;
            }
        }).when(flowData).getOrAdd(anyString(), any(FlowElement.DataFactory.class));

        assertThrows(
            PipelineConfigurationException.class,
            () -> javaScriptBuilderElement.process(flowData));
    }

    /**
     * Verify that valid JavaScript is produced when there are delayed execution
     * properties in the payload.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void JavaScriptBuilderElement_DelayExecution() throws Exception {
        javaScriptBuilderElement =
            new JavaScriptBuilderElementBuilder(loggerFactory)
                .build();

        final JSONObject json = new JSONObject();

        JSONObject locationData = new JSONObject();
        locationData.put("postcode", (String)null);
        locationData.put("postcodenullreason",
            "Evidence for this property has not been retrieved. Ensure the 'complete' method is called, passing the name of this property in the second parameter.");
        locationData.put("postcodeevidenceproperties", new String[] { "location.javascript" });
        locationData.put("javascript", "if (navigator.geolocation) { navigator.geolocation.getCurrentPosition(function() { // 51D replace this comment with callback function. }); }");
        locationData.put("javascriptdelayexecution", true);
        json.put("location", locationData);

        final FlowData flowData = mock(FlowData.class);

        when(flowData.get(JsonBuilderData.class)).thenAnswer(
            new Answer<JsonBuilderData>() {
                @Override
                public JsonBuilderData answer(InvocationOnMock invocationOnMock) throws Throwable {
                    JsonBuilderData d = new JsonBuilderDataInternal(mock(Logger.class), flowData);
                    d.put("json", json.toString());
                    return d;
                }
            }
        );


        final Map<String, Object> evidence = new HashMap<>();
        evidence.put(fiftyone.pipeline.javascriptbuilder.Constants.EVIDENCE_HOST_KEY, "localhost" );
        evidence.put(fiftyone.pipeline.core.Constants.EVIDENCE_PROTOCOL, "https" );

        Evidence evidenceObj = mock(Evidence.class);
        when(evidenceObj.asKeyMap()).thenReturn(evidence);

        when(flowData.getEvidence()).thenReturn(evidenceObj);
        when(flowData.getAs(anyString(), any(Class.class))).thenReturn(new AspectPropertyValueDefault<>("None"));

        when(flowData.tryGetEvidence(anyString(), any(Class.class))).thenAnswer(
            new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    TryGetResult<Object> result = new TryGetResult<>();
                    if (evidence.containsKey((String)invocation.getArgument(0))) {
                        result.setValue(evidence.get((String)invocation.getArgument(0)));
                    }
                    return result;
                }
            }
        );

        //configure(flowData, json);

        final JavaScriptBuilderData[] result = {null};
        when(flowData.getOrAdd(
            any(String.class),
            any(FlowElement.DataFactory.class)))
            .thenAnswer((Answer<JavaScriptBuilderData>) invocationOnMock -> {
                @SuppressWarnings("rawtypes")
                FlowElement.DataFactory dataFactory = invocationOnMock.getArgument(1);
                result[0] = (JavaScriptBuilderData) dataFactory.create(flowData);
                return result[0];
            });
        javaScriptBuilderElement.process(flowData);

        assertTrue(
            result[0].getJavaScript().contains("getEvidencePropertiesFromObject"),
            "Expected the generated JavaScript to contain the " +
                "'getEvidencePropertiesFromObject' function but it does not.");
    }

    private boolean isValidFodObject(String javaScript, String key, String property, Object value) throws Exception
    {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        ScriptContext context = engine.getContext();
        // Evaluate the JavaScript include.
        engine.eval(javaScript, context);

        Object res = engine.eval("fod."+key+"."+property+";", context);

        return res.toString().equals(value.toString());
    }
}

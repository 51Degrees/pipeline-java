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

package fiftyone.pipeline.jsonbuilder;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.PropertyMatcher;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElement.DataFactory;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import fiftyone.pipeline.engines.testhelpers.flowelements.*;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class JsonBuilderTests {
    private TestLoggerFactory loggerFactory;
    private EmptyEngine testEngine;
    private JsonBuilderElement jsonBuilderElement;
    private ElementData elementDataMock;
    private FlowData flowData;
    private JsonBuilderData result;
    
    public JsonBuilderTests() {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }
    
    @Before
    public void Init() {
        flowData = mock(FlowData.class);
        
        testEngine = mock(EmptyEngine.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new Exception("The method '" +
                invocationOnMock.getMethod().getName() + "' should not have been called.");
            }});
            
        List<AspectPropertyMetaData> properties = new ArrayList<AspectPropertyMetaData>();
        properties.add(new AspectPropertyMetaDataDefault("property", testEngine, "", String.class, new ArrayList<String>(), true));
        properties.add(new AspectPropertyMetaDataDefault("jsproperty", testEngine, "", String.class, new ArrayList<String>(), true));
        
        doReturn(properties).when(testEngine).getProperties();
        doReturn("test").when(testEngine).getElementDataKey();
        
        jsonBuilderElement = (JsonBuilderElement)new JsonBuilderElementBuilder(loggerFactory)
            .build();

        elementDataMock = mock(ElementData.class);
        
        Map<String,Object> data = new HashMap<>();
        data.put("property", "thisIsAValue" );
        data.put("jsproperty", "var = 'some js code';");
        
        doReturn(data).when(elementDataMock).asKeyMap();
    }
    
    @Test
    public void Valid_Json() throws Exception {
        final Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put("query.session-id", "somesessionid");
        evidenceData.put("query.sequence", 1);
        
        Map<String, Object> elementData = new HashMap<>();
        elementData.put("test", elementDataMock);
                
        when(flowData.elementDataAsMap()).thenReturn(elementData);
        
        doAnswer(new Answer<Object>() { 
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                FlowElement.DataFactory<JsonBuilderData> factory =
                        invocation.getArgument(1);
                result = factory.create(flowData);
                return result;
            }
        }).when(flowData).getOrAdd(anyString(), any(DataFactory.class));
        
        when(flowData.tryGetEvidence(anyString(), any(Class.class))).thenAnswer(
            new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    TryGetResult<Object> result = new TryGetResult<>();
                    if (evidenceData.containsKey((String)invocation.getArgument(0))) {
                        result.setValue(evidenceData.get((String)invocation.getArgument(0)));
                    }
                    return result;
                }
            }
        );
        
        jsonBuilderElement.process(flowData);
        
        assertTrue(isValidJson(result.getJson()));
        
    }
    
    private boolean isValidJson(String json)
    {
        JSONObject obj = new JSONObject(json);
        return obj.getJSONObject("test").has("property") &&
                obj.getJSONObject("test").has("jsproperty");
    }
    
    @Test(expected = PipelineConfigurationException.class)
    public void JsonBuilder_NoSequenceEvidence() throws Exception {
        final Map<String, Object> evidenceData = new HashMap<>();
        
        Map<String, Object> elementData = new HashMap<>();
        elementData.put("test", elementDataMock);
                
        when(flowData.elementDataAsMap()).thenReturn(elementData);
        
        doAnswer(new Answer<Object>() { 
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                FlowElement.DataFactory<JsonBuilderData> factory =
                        invocation.getArgument(1);
                result = factory.create(flowData);
                return result;
            }
        }).when(flowData).getOrAdd(anyString(), any(DataFactory.class));
        
        when(flowData.tryGetEvidence(anyString(), any(Class.class))).thenAnswer(
            new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    TryGetResult<Object> result = new TryGetResult<>();
                    if (evidenceData.containsKey((String)invocation.getArgument(0))) {
                        result.setValue(evidenceData.get((String)invocation.getArgument(0)));
                    }
                    return result;
                }
            }
        );
        
        jsonBuilderElement.process(flowData);
    }
    
    @Test
    public void JsonBuilder_MaxIterations() throws Exception {
        for (int i = 0; true; i++)
        {
            String json = TestIteration(i);
            boolean result = TestJsonIterations(json);

            if (i >= fiftyone.pipeline.jsonbuilder.Constants.MAX_JAVASCRIPT_ITERATIONS)
            {
                assertTrue(result);
                break;
            }
            else
            {
                assertFalse(result);
            }
        }
    }
    
    private String TestIteration(int iteration) throws Exception
    {
        final Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put("query.session-id", "somesessionid");
        evidenceData.put("query.sequence", iteration);
        
        final FlowData flowData = mock(FlowData.class);

        Map<String, Object> elementData = new HashMap<>();
        elementData.put("test", elementDataMock);
                
        when(flowData.elementDataAsMap()).thenReturn(elementData);
        
        when(flowData.tryGetEvidence(anyString(), any(Class.class))).thenAnswer(
            new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    TryGetResult<Object> result = new TryGetResult<>();
                    if (evidenceData.containsKey((String)invocation.getArgument(0))) {
                        result.setValue(evidenceData.get((String)invocation.getArgument(0)));
                    }
                    return result;
                }
            }
        );
        
        Map<String, String> properties = new HashMap<>();
        properties.put("test.jsproperty", "var = 'some js code';");
        
        when(flowData.getWhere(any(PropertyMatcher.class))).thenReturn(properties);

        doAnswer(new Answer<Object>() { 
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                FlowElement.DataFactory<JsonBuilderData> factory =
                        invocation.getArgument(1);
                result = factory.create(flowData);
                return result;
            }
        }).when(flowData).getOrAdd(anyString(), any(DataFactory.class));

        jsonBuilderElement.process(flowData);

        String json = result.getJson();

        return json;
    }

    private boolean TestJsonIterations(String json)
    {
        JSONObject obj = new JSONObject(json);
        Iterator<String> results = obj.keys();

        while(results.hasNext())
        {
            String res = results.next();
            if (res.contains("javascriptProperties"))
            {
                return false;
            }
        }
        return true;
    }
}

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

package fiftyone.pipeline.web;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import fiftyone.pipeline.core.data.DataKey;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;
import fiftyone.pipeline.web.services.ClientsidePropertyServiceCore;
import fiftyone.pipeline.web.services.FlowDataProviderCore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ClientsidePropertyServiceTests {
  
  private ClientsidePropertyServiceCore service;
  private FlowDataProviderCore flowDataProvider;
  private Pipeline pipeline;
  private FlowData flowData;
  private DataKey dataKey;
  private JavaScriptBuilderElement jsElement;
  private JsonBuilderElement jsonElement;
  private JavaScriptBuilderData jsData;
  private JsonBuilderData jsonData;

  private HttpServletRequest request;
  private HttpServletResponse response;
  private StringWriter responseString;

  private static final String JS_CONTENT = "JAVASCRIPT CONTENT";
  private static final String JSON_CONTENT = "JSON CONTENT";
  private static final String JS_CONTENT_TYPE = "application/x-javascript";
  private static final String JSON_CONTENT_TYPE = "application/json";

	@Before
  public void init() throws IOException {
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    responseString = new StringWriter();
    PrintWriter responseWriter = new PrintWriter(responseString);
    when(response.getWriter()).thenReturn(responseWriter);

    jsElement = mock(JavaScriptBuilderElement.class);
    jsonElement = mock(JsonBuilderElement.class);

    flowData = mock(FlowData.class);
    dataKey = mock(DataKey.class);
    when(flowData.generateKey(any(EvidenceKeyFilter.class))).thenReturn(dataKey);
    jsData = mock(JavaScriptBuilderData.class);
    when(jsData.getJavaScript()).thenReturn(JS_CONTENT);
    when(flowData.getFromElement(jsElement)).thenReturn(jsData);
    jsonData = mock(JsonBuilderData.class);
    when(jsonData.getJson()).thenReturn(JSON_CONTENT);
    when(flowData.getFromElement(jsonElement)).thenReturn(jsonData);
    
    flowDataProvider = mock(FlowDataProviderCore.class);
    when(flowDataProvider.getFlowData(any(HttpServletRequest.class))).thenReturn(flowData);

  }

  
  /**
   * @throws IOException
   */   
  @Test
  public void JavaScript_VaryHeader_UA() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element =  Arrays.asList(
      "header.User-Agent");
    elements.add(element);

    JavaScript_VaryHeader(elements, "User-Agent");
  }

  @Test
  public void JavaScript_VaryHeader_UA_AND_SEC() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.Sec-ch-ua",
      "header.User-Agent");
    elements.add(element);

    JavaScript_VaryHeader(elements, "User-Agent,Sec-ch-ua");
  }

  @Test
  public void JavaScript_VaryHeader_PSEUDO() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.sec-ch-uasec-ch-ua-full-version");
    elements.add(element);

    JavaScript_VaryHeader(elements, "");
  }

  @Test
  public void JavaScript_VaryHeader_UA_AND_PSEUDO() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.User-Agent",
      "header.sec-ch-uasec-ch-ua-full-version");
    elements.add(element);

    JavaScript_VaryHeader(elements, "User-Agent");
  }

  @Test
  public void JavaScript_VaryHeader_2Element_UA() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.User-Agent");
    List<String> element2 = Arrays.asList(
      "header.User-Agent");
    elements.add(element);
    elements.add(element2);

    JavaScript_VaryHeader(elements, "User-Agent");
  }

  @Test
  public void JavaScript_VaryHeader_2Element_UA_AND_PSEUDO() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.User-Agent");
    List<String> element2 = Arrays.asList(
      "header.sec-ch-uasec-ch-ua-full-version");
    elements.add(element);
    elements.add(element2);

    JavaScript_VaryHeader(elements, "User-Agent");
  }

  @Test
  public void JavaScript_VaryHeader_2Element_UA_AND_SEC() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.User-Agent");
    List<String> element2 = Arrays.asList(      
      "header.Sec-ch-ua");
    elements.add(element);
    elements.add(element2);

    JavaScript_VaryHeader(elements, "User-Agent,Sec-ch-ua");
  }

  private void JavaScript_VaryHeader(
    List<List<String>> elements, 
    String expectedVary) throws IOException {

    Configure(ConfigureElements(elements));
    service.serveJavascript(request, response);

    ValidateResponse(
      JS_CONTENT,
      JS_CONTENT.length(),
      JS_CONTENT_TYPE,
      200,
      String.valueOf(dataKey.hashCode()),
      expectedVary);
  }

  private void ValidateResponse(
    String expectedContent,
    Integer contentLength, 
    String contentType,
    int expectedStatusCode,
    String expectedETag,
    String expectedVary)
  {
    if (expectedContent == null) { expectedContent = ""; }
    assertEquals(expectedContent, responseString.toString());
    
    verify(response).setStatus(eq(expectedStatusCode));

    if (contentType != null) {
      verify(response).setContentType(eq(contentType));
    }
    else {
      verify(response, times(0)).setContentType(anyString());
    }

    if (contentLength != null) {
      verify(response).setContentLength(eq(contentLength));
    }
    else {
      verify(response, times(0)).setContentLength(anyInt());
    }
    
    if (expectedStatusCode == 200) {
      verify(response).setHeader(eq("Cache-Control"), eq("private,max-age=1800"));
    }
    else{
      verify(response, times(0)).setHeader(eq("Cache-Control"), anyString());
    }

    if (expectedVary != null) {
      String[] values = expectedVary.split(",");
      for (String value : values) {
        verify(response).setHeader(eq("Vary"), argThat(new CommaCountMatcher(values.length - 1)));
        verify(response).setHeader(eq("Vary"), contains(value));     
      }
    }
    else {
      verify(response, times(0)).setHeader(eq("Vary"), anyString());
    }
    
    if (expectedETag != null) {
      verify(response).setHeader(eq("ETag"), eq(expectedETag));
    }
    else {
      verify(response, times(0)).setHeader(eq("ETag"), anyString());
    }

    verify(response).setHeader(eq("Access-Control-Allow-Origin"), eq("*"));
  }

  @SuppressWarnings("rawtypes")
  private List<FlowElement> ConfigureElements(List<List<String>> evidenceHeaders) {
    List<FlowElement> elements = new ArrayList<FlowElement>();

    for (List<String> entry : evidenceHeaders) {
      if(entry != null) {
        FlowElement element = mock(FlowElement.class);
        EvidenceKeyFilter filter = new EvidenceKeyFilterWhitelist(entry);
        when(element.getEvidenceKeyFilter()).thenReturn(filter);

        elements.add(element);
      }
    }

    return elements;
  }

  @SuppressWarnings("rawtypes")
  private void Configure(List<FlowElement> elements) {
    if(elements == null){
      elements = new ArrayList<>();
    }
    
    pipeline = mock(Pipeline.class);
    when(pipeline.getFlowElements()).thenReturn(elements);
    when(pipeline.getElement(JavaScriptBuilderElement.class)).thenReturn(jsElement);
    when(pipeline.getElement(JsonBuilderElement.class)).thenReturn(jsonElement);
    when(pipeline.getEvidenceKeyFilter())
      .thenReturn(new EvidenceKeyFilterWhitelist(new ArrayList<String>()));

    when(flowData.getPipeline()).thenReturn(pipeline);

    service = new ClientsidePropertyServiceCore.Default(
      flowDataProvider, pipeline);
  }

  public class CommaCountMatcher implements ArgumentMatcher<String> {

    private int expectedCommaCount;

    public CommaCountMatcher(int expectedCommaCount) {
      this.expectedCommaCount = expectedCommaCount;
    }

    @Override
    public boolean matches(String actualValue) {
      int count = 0;
      for (char chr : actualValue.toCharArray()) {
        if(chr == ',') count++;
      };
      return count == expectedCommaCount;
    }
}
}

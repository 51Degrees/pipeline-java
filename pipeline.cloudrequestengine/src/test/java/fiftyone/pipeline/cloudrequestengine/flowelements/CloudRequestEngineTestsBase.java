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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.engines.services.HttpClient;

public class CloudRequestEngineTestsBase {
	HttpClient httpClient;
    protected TestLoggerFactory loggerFactory;

    protected URL expectedUrl = new URL("https://cloud.51degrees.com/api/v4/resource_key.json");
    protected String jsonResponse = "{'device':{'value':'1'}}";
    protected String evidenceKeysResponse = "['query.User-Agent']";
    protected String accessiblePropertiesResponse =
            "{'Products': {'device': {'DataTier': 'tier','Properties': [{'Name': 'value','Type': 'String','Category': 'Device'}]}}}";
    protected int accessiblePropertiesResponseCode = 200;
    protected String propertiesEndPoint = "";
 
    public CloudRequestEngineTestsBase() throws MalformedURLException {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }
    
    protected void configureMockedClient() throws IOException {
        // ARRANGE
        httpClient = mock(HttpClient.class, new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new Exception("The method '" +
                        invocationOnMock.getMethod().getName() + "' should not have been called.");
            }
        });
        final HttpURLConnection connection = mock(HttpURLConnection.class, new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new Exception("The method '" +
                        invocationOnMock.getMethod().getName() + "' should not have been called.");
            }
        });
        doNothing().when(connection).setConnectTimeout(anyInt());
        doNothing().when(connection).setReadTimeout(anyInt());
        Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();
        doReturn(responseHeaders).when(connection).getHeaderFields();
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                URL url = (URL)invocationOnMock.getArgument(0);              
                doReturn(url).when(connection).getURL();                
                if (url.getPath().endsWith("properties")) {
                	propertiesEndPoint = url.getQuery();
                    doReturn(accessiblePropertiesResponseCode).when(connection).getResponseCode();
                }
                else {
                    doReturn(200).when(connection).getResponseCode();
                }
                return (Object)connection;
            }
        }).when(httpClient).connect(any(URL.class));

        Answer<Object> responseStringAnswer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                URL url = ((HttpURLConnection)invocationOnMock.getArgument(0)).getURL();
                if (url.getPath().endsWith("properties")) {
                    return accessiblePropertiesResponse;
                }
                else if (url.getPath().endsWith("evidencekeys")) {
                    return evidenceKeysResponse;
                }
                else {
                    throw new Exception("A request was made with the URL '" +
                            url + "'");
                }
            }
        };

        doAnswer(responseStringAnswer)
            .when(httpClient)
            .getResponseString(any(HttpURLConnection.class));
        doAnswer(responseStringAnswer)
            .when(httpClient)
            .getResponseString(any(HttpURLConnection.class), anyMap());

        doReturn(jsonResponse)
                .when(httpClient)
                .postData(
                        any(HttpURLConnection.class),
                        ArgumentMatchers.<String, String>anyMap(),
                        (byte[])any());
    }
}

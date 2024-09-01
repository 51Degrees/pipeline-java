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

import fiftyone.pipeline.cloudrequestengine.CloudRequestException;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class CloudRequestEngineBuilderTests extends CloudRequestEngineTestsBase {
    private final String testResourceKey = "resource_key";
	private final String testEndpoint = "https://testEndpoint/";
	private final String testEnvVarEndpoint = "https://testEnvVarEndpoint/";
	
	public CloudRequestEngineBuilderTests() throws MalformedURLException {
        super();
    }
	
    @BeforeEach
    public void setUp() throws IOException {
    	configureMockedClient();
    }
    
    @Test
    @SuppressWarnings("unused")
    public void BuildEngine_ResourceKey_NotSet() {
        assertThrows(PipelineConfigurationException.class, () -> {
            CloudRequestEngine cloudRequestsEngine =
                new CloudRequestEngineBuilder(
                    LoggerFactory.getILoggerFactory(),
                    new HttpClientDefault())
                    .build();
        });
    }

    private void setExpectedEnvVarValue(
    	CloudRequestEngineBuilder builder,
    	String expectedValue) throws Exception {
    	Field getEnvVarField = builder.getClass().getDeclaredField("getEnvVar");
    	getEnvVarField.setAccessible(true);
    	getEnvVarField.set(builder, (Function<String, String>)(name) -> {
    		return expectedValue;
    	});
    }
    
    private String getEndpointValue(
    		CloudRequestEngineBuilder builder) throws Exception {
    	Field endPointField = builder.getClass().getDeclaredField("endPoint");
    	endPointField.setAccessible(true);
    	return (String)endPointField.get(builder);
    }
 
    private String getPropertiesEndpointValue(
    		CloudRequestEngineBuilder builder) throws Exception {
    	Field propertiesEndPointField = builder.getClass().getDeclaredField("propertiesEndpoint");
    	propertiesEndPointField.setAccessible(true);
    	return (String)propertiesEndPointField.get(builder);
    }
    
    @Test
    public void CloudEndPoint_Explicit_Setting() throws Exception {
    	CloudRequestEngineBuilder builder =
    		new CloudRequestEngineBuilder(loggerFactory, httpClient);
    	setExpectedEnvVarValue(builder, testEnvVarEndpoint);
    	
    	builder.setResourceKey(testResourceKey)
    		.setEndpoint(testEndpoint)
    		.build();
    	assertEquals(testEndpoint, getEndpointValue(builder));
    	assertEquals(testEndpoint + "accessibleproperties", getPropertiesEndpointValue(builder));
    }
    
    @Test
    public void CloudEndPoint_Environment_Setting() throws Exception {
    	CloudRequestEngineBuilder builder =
    		new CloudRequestEngineBuilder(loggerFactory, httpClient);
    	setExpectedEnvVarValue(builder, testEnvVarEndpoint);
    	
    	builder.setResourceKey(testResourceKey)
    		.build();
    	assertEquals(testEnvVarEndpoint, getEndpointValue(builder));
    	assertEquals(testEnvVarEndpoint + "accessibleproperties", getPropertiesEndpointValue(builder));
    }
    
    @Test
    public void CloudEndPoint_Default_Setting() throws Exception {
    	CloudRequestEngineBuilder builder =
    		new CloudRequestEngineBuilder(loggerFactory, httpClient);
    	setExpectedEnvVarValue(builder, null);
    	
    	builder.setResourceKey(testResourceKey)
    		.build();
    	assertEquals(
    		Constants.END_POINT_DEFAULT + "/", getEndpointValue(builder));
    }
}

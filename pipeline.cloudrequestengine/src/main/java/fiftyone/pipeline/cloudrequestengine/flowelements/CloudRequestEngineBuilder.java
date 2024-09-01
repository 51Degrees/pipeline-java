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


import fiftyone.pipeline.annotations.DefaultValue;
import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.flowelements.AspectEngineBuilderBase;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.slf4j.ILoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 * Builder for the {@link CloudRequestEngine}.
 */
@ElementBuilder
public class CloudRequestEngineBuilder extends
    AspectEngineBuilderBase<CloudRequestEngineBuilder, CloudRequestEngine> {
	
    private final HttpClient httpClient;

    private String endPoint = null;
    private String dataEndpoint = null;
    private String propertiesEndpoint = null;
    private String evidenceKeysEndpoint = null;
    private String resourceKey = null;
    private String licenseKey = null;
    private String cloudRequestOrigin = null;
    private int timeoutMillis = Constants.DEFAULT_TIMEOUT_MILLIS;
    
    // Function to get environment variable value. This enable testable code.
    private Function<String, String> getEnvVar = (name) -> {
    	return System.getenv(name);
    };

    public CloudRequestEngineBuilder(ILoggerFactory loggerFactory) {
        this(loggerFactory, new HttpClientDefault());
    }

    public CloudRequestEngineBuilder(
        ILoggerFactory loggerFactory,
        HttpClient httpClient) {
        super(loggerFactory);
        this.httpClient = httpClient;
    }

    @Override
    protected CloudRequestEngine newEngine(List<String> properties) throws Exception {
        if(resourceKey == null || resourceKey.isEmpty()){
            throw new PipelineConfigurationException("A resource key is " +
                    "required to access the cloud server. Please use the " +
                    "'setResourceKey(String) method to supply your resource " +
                    "key obtained from https://configure.51degrees.com");
        }

        /*
         *  Check if endPoint has been explicitly set via setEndpoint.
         *  If not check for environment variable FOD_CLOUD_API_URL.
         *  If nothing else is set, use default value.
         */
        if (endPoint == null || endPoint.isEmpty()) {
        	String envVarEndPoint = getEnvVar.apply(
        		Constants.FOD_CLOUD_API_URL);
        	if (envVarEndPoint == null || envVarEndPoint.isEmpty()) {
        		setEndpoint(Constants.END_POINT_DEFAULT);
        	}
        	else {
        		setEndpoint(envVarEndPoint);
        	}
        }
        
        return new CloudRequestEngineDefault(
            loggerFactory.getLogger(CloudRequestEngine.class.getName()),
            new CloudRequestDataFactory(loggerFactory),
            httpClient,
            dataEndpoint,
            resourceKey,
            licenseKey,
            propertiesEndpoint,
            evidenceKeysEndpoint,
                timeoutMillis,
            cloudRequestOrigin);
    }
    
    private String getEnvironmentVariable(String name) {
    	return System.getenv(name);
    }

    public CloudRequestEngine build() throws Exception {
        return buildEngine();
    }

    /**
     * The root endpoint which the CloudRequestsEngine will query. This will set
     * the data, properties and evidence keys endpoints.
     * <p>
     * By default, "https://cloud.51degrees.com/api/v4"
     * @param uri root endpoint
     * @return this builder
     */
    @DefaultValue(Constants.END_POINT_DEFAULT)
    public CloudRequestEngineBuilder setEndpoint(String uri) {
        if (uri.endsWith("/") == false) {
            uri += '/';
        }
        endPoint = uri;
        return setDataEndpoint(uri + (resourceKey != null ? resourceKey + "." : "") + "json")
            .setPropertiesEndpoint(uri + "accessibleproperties")
            .setEvidenceKeysEndpoint(uri + "evidencekeys");

    }

    /**
     * The endpoint the CloudRequestEngine will query to get a processing result.
     * <p>
     * By default, this is the endpoint value suffixed with the resourcekey and ".json"
     * @param uri data endpoint
     * @return this builder
     */
    @DefaultValue(Constants.END_POINT_DEFAULT + "{resourcekey}.json")
    public CloudRequestEngineBuilder setDataEndpoint(String uri) {
        dataEndpoint = uri;
        return this;
    }

    /**
     * The endpoint the cloudRequestEngine will query to get the available
     * properties.
     * <p>
     * By default, this is the endpoint value suffixed with "accessibleproperties"
     * @param uri properties endpoint
     * @return this builder
     */
    @DefaultValue(Constants.END_POINT_DEFAULT + "accessibleproperties")
    public CloudRequestEngineBuilder setPropertiesEndpoint(String uri) {
        propertiesEndpoint = uri;
        return this;
    }

    /**
     * The endpoint the cloudRequestEngine will query to get the required
     * evidence keys.
     * <p>
     * By default, this is the endpoint value suffixed with "evidencekeys"
     * @param uri evidence keys endpoint
     * @return this builder
     */
    @DefaultValue(Constants.END_POINT_DEFAULT + "evidencekeys")
    public CloudRequestEngineBuilder setEvidenceKeysEndpoint(String uri) {
        evidenceKeysEndpoint = uri;
        return this;
    }

    /**
     * The resource key to query the endpoint with.
     * <p>
     * No default, a value must be supplied
     * @param resourceKey resource key
     * @return this builder
     */
    @DefaultValue("No default - a resource key must be supplied")
    public CloudRequestEngineBuilder setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
        return this;
    }

    /**
     * The license key to query the endpoint with.
     * <p>
     * There is no default
     * @param licenseKey license key
     * @return this builder
     */
    @DefaultValue("No default")
    public CloudRequestEngineBuilder setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
        return this;
    }

    /**
     * Timeout in seconds for the request to the endpoint.
     * <p>
     * Default value is 100 seconds
     * @param timeout in seconds
     * @return this builder
     */
    @DefaultValue(intValue = Constants.DEFAULT_TIMEOUT_MILLIS / 1000)
    public CloudRequestEngineBuilder setTimeOutSeconds(int timeout) {
        this.timeoutMillis = timeout * 1000;
        return this;
    }

    /**
     * The value to set for the Origin header when making requests
     * to the cloud service.
     * This is used by the cloud service to check that the request
     * is being made from a origin matching those allowed by the 
     * resource key.
     * For more detail, see the 'Request Headers' section in the 
     * <a href="https://cloud.51degrees.com/api-docs/index.html">cloud documentation</a>.
     * <p>
     * There is no default value
     * @param cloudRequestOrigin The value to use for the Origin header.
     * @return this builder
     */
    @DefaultValue("None")
    public CloudRequestEngineBuilder setCloudRequestOrigin(String cloudRequestOrigin) {
        this.cloudRequestOrigin = cloudRequestOrigin;
        return this;
    }

    private static class CloudRequestDataFactory
        implements ElementDataFactory<CloudRequestData> {

        private final ILoggerFactory loggerFactory;

        public CloudRequestDataFactory(ILoggerFactory loggerFactory) {
            this.loggerFactory = loggerFactory;
        }

        @Override
        public CloudRequestData create(
            FlowData flowData,
            FlowElement<CloudRequestData, ?> engine) {
            return new CloudRequestData(
                loggerFactory.getLogger(CloudRequestData.class.getName()),
                flowData,
                (CloudRequestEngine) engine);
        }
    }


    /**
     * Configure the size of a {@link fiftyone.caching.LruPutCache} cache to use.
     * <p>
     * Default is that there is no cache unless one is configured using this method
     * or by using {@link AspectEngineBuilderBase#setCache(CacheConfiguration)} - or
     * if it is set in a pipelineBuilder e.g.
     * {@link fiftyone.pipeline.engines.flowelements.PrePackagedPipelineBuilderBase#useResultsCache()}
     * @param size the size of the cache
     * @return this builder
     */
    @DefaultValue("No cache")
    public CloudRequestEngineBuilder setCacheSize(int size) {
        this.cacheConfig = new CacheConfiguration(size);
        return this;
    }
}

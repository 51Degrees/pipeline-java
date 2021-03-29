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

package fiftyone.pipeline.cloudrequestengine.flowelements;


import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.flowelements.AspectEngineBuilderBase;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.slf4j.ILoggerFactory;

import java.util.List;

/**
 * Builder for the {@link CloudRequestEngine}.
 */
@ElementBuilder
public class CloudRequestEngineBuilder extends
    AspectEngineBuilderBase<CloudRequestEngineBuilder, CloudRequestEngine> {

    private final HttpClient httpClient;

    private String endPoint = "https://cloud.51degrees.com/api/v4";
    private String dataEndpoint = null;
    private String propertiesEndpoint = null;
    private String evidenceKeysEndpoint = null;
    private String resourceKey = null;
    private String licenseKey = null;
    private int timeout = 100000;

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
    protected CloudRequestEngine newEngine(List<String> properties) {
        if(resourceKey == null || resourceKey.isEmpty()){
            throw new PipelineConfigurationException("A resource key is " +
                    "required to access the cloud server. Please use the " +
                    "'setResourceKey(String) method to supply your resource " +
                    "key obtained from https://configure.51degrees.com");
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
            timeout);
    }

    public CloudRequestEngine build() throws Exception {
        return buildEngine();
    }

    /**
     * The root endpoint which the CloudRequestsEngine will query. This will set
     * the data, properties and evidence keys endpoints.
     * @param uri root endpoint
     * @return this builder
     */
    public CloudRequestEngineBuilder setEndpoint(String uri) {
        if (uri.endsWith("/") == false) {
            uri += '/';
        }
        endPoint = uri;
        return setDataEndpoint(uri + (resourceKey != null ? resourceKey + "." : "") + "json")
            .setPropertiesEndpoint(uri + "accessibleproperties" + (resourceKey != null ? "?Resource=" +resourceKey : ""))
            .setEvidenceKeysEndpoint(uri + "evidencekeys");

    }

    /**
     * The endpoint the CloudRequestEngine will query to get a processing result.
     * @param uri data endpoint
     * @return this builder
     */
    public CloudRequestEngineBuilder setDataEndpoint(String uri) {
        dataEndpoint = uri;
        return this;
    }

    /**
     * The endpoint the cloudRequestEngine will query to get the available
     * properties.
     * @param uri properties endpoint
     * @return this builder
     */
    public CloudRequestEngineBuilder setPropertiesEndpoint(String uri) {
        propertiesEndpoint = uri;
        return this;
    }

    /**
     * The endpoint the cloudRequestEngine will query to get the required
     * evidence keys.
     * @param uri evidence keys endpoint
     * @return this builder
     */
    public CloudRequestEngineBuilder setEvidenceKeysEndpoint(String uri) {
        evidenceKeysEndpoint = uri;
        return this;
    }

    /**
     * The resource key to query the endpoint with.
     * @param resourceKey resource key
     * @return this builder
     */
    public CloudRequestEngineBuilder setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
        return this.setEndpoint(endPoint);
    }

    /**
     * The license key to query the endpoint with.
     * @param licenseKey license key
     * @return this builder
     */
    public CloudRequestEngineBuilder setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
        return this;
    }

    /**
     * Timeout in seconds for the request to the endpoint.
     * @param timeout in seconds
     * @return this builder
     */
    public CloudRequestEngineBuilder setTimeOutSeconds(int timeout) {
        this.timeout = timeout;
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
}

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

package fiftyone.pipeline.engines.flowelements;

import org.slf4j.ILoggerFactory;

/**
 * Base class for pipeline builders that will produce a pipeline with specific
 * flow elements.
 * @param <TBuilder> the builder type
 */
public abstract class CloudPipelineBuilderBase<
    TBuilder extends CloudPipelineBuilderBase<TBuilder>>
    extends PrePackagedPipelineBuilderBase<TBuilder> {

    protected String url = "";

    protected String dataEndpoint = "";

    protected String propertiesEndpoint = "";

    protected String evidenceKeysEndpoint = "";

    protected String resourceKey = "";

    protected String licenseKey = "";

    protected String cloudRequestOrigin = "";

    /**
     * Construct a new instance.
     * @param loggerFactory the {@link ILoggerFactory} used to create any
     *                      loggers required by instances being built by the
     *                      builder
     */
    public CloudPipelineBuilderBase(ILoggerFactory loggerFactory) {
        super(loggerFactory);
    }

    /**
     * Set the endpoint to use when calling the cloud service. This will also
     * set the data, properties, and evidence keys endpoints using this as the
     * base URL.
     * @param url endpoint URL
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setEndPoint(String url) {
        if (url.endsWith("/") == false) {
            url += '/';
        }
        this.url = url;
        return (TBuilder)this;
    }

    /**
     * Set the URL for the data endpoint to be called during processing.
     * @param url data URL
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataEndpoint(String url) {
        this.dataEndpoint = url;
        return (TBuilder)this;
    }

    /**
     * Set the URL for the properties endpoint to be called when setting up the
     * engine.
     * @param propertiesEndpoint properties URL
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setPropertiesEndpoint(String propertiesEndpoint) {
        this.propertiesEndpoint = propertiesEndpoint;
        return (TBuilder)this;
    }

    /**
     * Set the URL for the evidence keys endpoint to be called when setting up
     * the engine.
     * @param evidenceKeysEndpoint evidence keys URL
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setEvidenceKeysEndpoint(String evidenceKeysEndpoint) {
        this.evidenceKeysEndpoint = evidenceKeysEndpoint;
        return (TBuilder)this;
    }

    /**
     * Set the resource key to be used when calling the endpoints.
     * @param key resource key
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setResourceKey(String key) {
        this.resourceKey = key;
        return (TBuilder)this;
    }

    /**
     * Set the license key to be used when calling the endpoints.
     * @param key license key
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setLicenseKey(String key) {
        this.licenseKey = key;
        return (TBuilder)this;
    }

    /**
     * The value to set for the Origin header when making requests
     * to the cloud service.
     * This is used by the cloud service to check that the request
     * is being made from a origin matching those allowed by the 
     * resource key.
     * For more detail, see the 'Request Headers' section in the 
     * <a href="https://cloud.51degrees.com/api-docs/index.html">cloud documentation</a>.
     * @param cloudRequestOrigin the value to set the origin header to
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setCloudRequestOrigin(String cloudRequestOrigin) {
        this.cloudRequestOrigin = cloudRequestOrigin;
        return (TBuilder)this;
    }
}

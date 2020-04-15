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

package fiftyone.pipeline.engines.flowelements;

import org.slf4j.ILoggerFactory;

public abstract class CloudPipelineBuilderBase<TBuilder extends CloudPipelineBuilderBase<TBuilder>>
    extends PrePackagedPipelineBuilderBase<TBuilder> {

    protected String url = "";

    protected String dataEndpoint = "";

    protected String propertiesEndpoint = "";

    protected String evidenceKeysEndpoint = "";

    protected String resourceKey = "";

    protected String licenseKey = "";

    public CloudPipelineBuilderBase(ILoggerFactory loggerFactory) {
        super(loggerFactory);
    }

    public TBuilder setEndPoint(String url)
    {
        if (url.endsWith("/") == false) {
            url += '/';
        }
        this.url = url;
        return (TBuilder)this;
    }

    public TBuilder setDataEndpoint(String url) {
        this.dataEndpoint = url;
        return (TBuilder)this;
    }

    public TBuilder setPropertiesEndpoint(String propertiesEndpoint) {
        this.propertiesEndpoint = propertiesEndpoint;
        return (TBuilder)this;
    }

    public TBuilder setEvidenceKeysEndpoint(String evidenceKeysEndpoint) {
        this.evidenceKeysEndpoint = evidenceKeysEndpoint;
        return (TBuilder)this;
    }

    public TBuilder setResourceKey(String key) {
        this.resourceKey = key;
        return (TBuilder)this;
    }

    public TBuilder setLicenseKey(String key) {
        this.licenseKey = key;
        return (TBuilder)this;
    }
}

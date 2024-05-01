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

package fiftyone.pipeline.javascriptbuilder.flowelements;

import fiftyone.pipeline.annotations.DefaultValue;
import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.exceptions.*;
import fiftyone.pipeline.javascriptbuilder.Constants;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

//! [class]
/**
 * Builder for the {@link JavaScriptBuilderElement}
 */
@ElementBuilder
public class JavaScriptBuilderElementBuilder {
    public static final boolean ENABLE_COOKIES = true;
    private final ILoggerFactory loggerFactory;
    private final Logger logger;
    
    protected String host = "";
    protected String endpoint = "";
    protected String protocol = "";
    protected String contextRoot = "";
    protected String objName = "";
    private boolean enableCookies = ENABLE_COOKIES;

    /**
     * Construct a new instance.
     * @param loggerFactory the {@link ILoggerFactory} to use when creating
     *                      loggers for the element
     */
    public JavaScriptBuilderElementBuilder(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
        this.logger = loggerFactory.getLogger(
            JavaScriptBuilderElementBuilder.class.getName());
    }
    
    /**
     * Set the host that the client JavaScript should query for updates.
     * <p>
     * By default, the host from the request will be used.
     * @param host the hostname
     * @return this builder
     */
    @DefaultValue("The host from the request")
    public JavaScriptBuilderElementBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * Set the endpoint which will be queried on the host. e.g /api/v4/json
     * <p>
     * By default, this value is an empty string
     * @param endpoint the endpoint
     * @return this builder
     */
    @DefaultValue("Empty string")
    public JavaScriptBuilderElementBuilder setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }
    /**
     * Set the evidence value for the context root
     * <p>
     * Default is value from evidence "server.contextroot"
     * @param contextRoot
     * @return this builder
     */
    @DefaultValue("Value from evidence " + fiftyone.pipeline.core.Constants.EVIDENCE_WEB_CONTEXT_ROOT)
    public JavaScriptBuilderElementBuilder setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
        return this;
    }

    /**
     * The protocol that the client JavaScript will use when querying
     * for updates.
     * <p>
     * By default, the protocol from the request will be
     * used.
     * @param protocol The protocol to use (http / https)
     * @return this builder
     */
    @DefaultValue("The protocol from the request")
    public JavaScriptBuilderElementBuilder setProtocol(String protocol) {
        if (protocol.equalsIgnoreCase("http") ||
            protocol.equalsIgnoreCase("https")) {
            this.protocol = protocol;
        }
        else {
            throw new PipelineConfigurationException(
                "Invalid protocol in configuration (" + protocol +
                "), must be 'http' or https'");

        }
        return this;
    }

    /**
     * The default name of the object instantiated by the client
     * JavaScript.
     * <p>
     * Default is "fod"
     * @param objName the object name to use
     * @return this builder
     */
    @DefaultValue(Constants.DEFAULT_OBJECT_NAME)
    public JavaScriptBuilderElementBuilder setObjectName(String objName) {
        Pattern pattern = Pattern.compile("[a-zA-Z_$][0-9a-zA-Z_$]*");
        Matcher match = pattern.matcher(objName);
        if (match.matches())
        {
            this.objName = objName;
        }
        else
        {
            PipelineConfigurationException ex =
                new PipelineConfigurationException("JavaScriptBuilder" +
                " ObjectName is invalid. This must be a valid JavaScript" +
                " type identifier.");
            
            throw ex;
        }

        return this;
    }
    
    /**
     * Set whether the client JavaScript stores results of client side
     * processing in cookies. If set to false, the JavaScript will not populate
     * any cookies, and will instead use session storage.
     * <p>
     * This can also be set per request, using the "query.fod-js-enable-cookies"
     * evidence key. For more details on personal data policy, see
     * http://51degrees.com/terms/client-services-privacy-policy/
     * <p>
     * Default is true
     * @param enableCookies should enable cookies?
     * @return this builder
     */
    @DefaultValue(booleanValue = ENABLE_COOKIES)
    public JavaScriptBuilderElementBuilder setEnableCookies(
        boolean enableCookies) {
        this.enableCookies = enableCookies;
        return this;
    }

    /**
     * Build the {@link JavaScriptBuilderElement}
     * @return new {@link JavaScriptBuilderElement} instance
     */
    public JavaScriptBuilderElement build() {
        return new JavaScriptBuilderElement(
            loggerFactory.getLogger(JavaScriptBuilderElement.class.getName()),
            new ElementDataFactory<JavaScriptBuilderData>() {
                @Override
                public JavaScriptBuilderData create(
                    FlowData flowData,
                    FlowElement<JavaScriptBuilderData, ?> flowElement) {
                    return new JavaScriptBuilderDataInternal(
                        loggerFactory.getLogger(
                            JavaScriptBuilderDataInternal.class.getName()),
                        flowData);
                }
            },
            endpoint,
            objName,
            enableCookies,
            host,
            protocol,
            contextRoot);
    } 
}
//! [class]

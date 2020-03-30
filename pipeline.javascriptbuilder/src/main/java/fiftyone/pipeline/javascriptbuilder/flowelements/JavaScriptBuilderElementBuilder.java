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

package fiftyone.pipeline.javascriptbuilder.flowelements;

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
 * Builder for the @see JavaScriptBuilderElement
 */
@ElementBuilder
public class JavaScriptBuilderElementBuilder {
    private final ILoggerFactory loggerFactory;
    private final Logger logger;
    
    protected String host = "";
    protected boolean overrideHost = false;
    protected String endpoint = "";
    protected String protocol = "";
    protected boolean overrideProtocol = false;
    protected String objName = "";
    private boolean enableCookies = false;

    public JavaScriptBuilderElementBuilder(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
        this.logger = loggerFactory.getLogger(JavaScriptBuilderElementBuilder.class.getName());
    }
    
    /**
     * Set the host that the client JavaScript should query for updates.
     * @param host the hostname.
     * @return JavaScriptBuilderElementBuilder
     */
    public JavaScriptBuilderElementBuilder setHost(String host)
    {
        this.host = host;
        return this;
    }

    /**
     * Set whether host should be determined from the origin or referer header.
     * @param overrideHost Should override host?
     * @return JavaScriptBuilderElementBuilder
     */
    public JavaScriptBuilderElementBuilder setOverrideHost(boolean overrideHost)
    {
        this.overrideHost = overrideHost;
        return this;
    }

    /**
     * Set the endpoint which will be queried on the host. e.g /api/v4/json
     * @param endpoint The endpoint.
     * @return JavaScriptBuilderElementBuilder
     */
    public JavaScriptBuilderElementBuilder setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * The default protocol that the client JavaScript will use when querying
     * for updates.
     * @param protocol The protocol to use (http / https)
     * @return JavaScriptBuilderElementBuilder
     */
    public JavaScriptBuilderElementBuilder setDefaultProtocol(String protocol)
    {
        boolean empty = protocol.isEmpty();
        boolean http = protocol.equals("http");
        boolean https = protocol.equals("https");

        if ((http || https) && empty == false)
        {
            this.protocol = protocol;
        }
        else
        {
            this.protocol = Constants.DEFAULT_PROTOCOL;
            logger.warn("No/Invalid protocol in configuration," +
                " JavaScriptBuilderElement is using the default protocol: " +
                Constants.DEFAULT_PROTOCOL);
        }
        return this;
    }

    /**
     * Set whether the host should be overridden by evidence, e.g when the
     * host can be determined from the incoming request.
     * @param overrideProto Should override the protocol?
     * @return JavaScriptBuilderElementBuilder
     */
    public JavaScriptBuilderElementBuilder setOverrideDefaultProtocol(boolean overrideProto){
        overrideProtocol = overrideProto;
        return this;
    }

    /**
     * The default name of the object instantiated by the client
     * JavaScript.
     * @param objName The object name to use.
     * @return JavaScriptBuilderElementBuilder
     */
    public JavaScriptBuilderElementBuilder setObjectName(String objName)
    {
        Pattern pattern = Pattern.compile("[a-zA-Z_$][0-9a-zA-Z_$]*");
        Matcher match = pattern.matcher(objName);
        if (match.matches())
        {
            this.objName = objName;
        }
        else
        {
            PipelineConfigurationException ex = new PipelineConfigurationException("JavaScriptBuilder" +
                " ObjectName is invalid. This must be a valid JavaScript" +
                " type identifier.");

            logger.error("Value for ObjectName is invalid.", ex);
            throw ex;
        }

        return this;
    }
    
    /**
     * Set whether the client JavaScript stores results of client side
     * processing in cookies.
     * @param enableCookies Should enable cookies?
     * @return JavaScriptBuilderElementBuilder
     */
    public JavaScriptBuilderElementBuilder setEnableCookies(boolean enableCookies)
    {
        this.enableCookies = enableCookies;
        return this;
    }

    /**
     * Build the @see JavaScriptBuilderElement
     * @return JavaScriptBuilderElement
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
                        loggerFactory.getLogger(JavaScriptBuilderDataInternal.class.getName()),
                        flowData);
                }
            },
            host,
            overrideHost,
            endpoint,
            protocol,
            overrideProtocol,
            objName,
            enableCookies);
    } 
}
//! [class]

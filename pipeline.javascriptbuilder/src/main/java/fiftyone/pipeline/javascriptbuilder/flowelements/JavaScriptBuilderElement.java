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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.javascriptbuilder.Constants;
import fiftyone.pipeline.javascriptbuilder.data.*;
import fiftyone.pipeline.javascriptbuilder.templates.*;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import org.slf4j.Logger;

import java.util.*;

import static fiftyone.pipeline.core.Constants.EVIDENCE_QUERY_PREFIX;
import static fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR;
import static fiftyone.pipeline.javascriptbuilder.Constants.EVIDENCE_OBJECT_NAME;

//! [class]

/**
 * JavaScript Builder Element generates a JavaScript include to be run on the
 * client device.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/javascript-builder.md">Specification</a>
 */
public class JavaScriptBuilderElement
    extends FlowElementBase<JavaScriptBuilderData, ElementPropertyMetaData> {

    protected String host;
    protected String endpoint;
    protected String protocol;
    protected String contextRoot;
    protected final String objName;
    protected final boolean enableCookies;
    private final Mustache mustache;
    
    //! [constructor]
    /**
     * Default constructor.
     * @param logger The logger.
     * @param elementDataFactory The element data factory.
     * @param endpoint Set the endpoint which will be queried on the host.
     *                 e.g /api/v4/json
     * @param objName The default name of the object instantiated by the client
     *                JavaScript.
     * @param enableCookies Set whether the client JavaScript stored results of
     * client side processing in cookies.
     * @param host The host that the client JavaScript should query for updates.
     * If null or blank then the host from the request will be used
     * @param protocol The protocol (HTTP or HTTPS) that the client JavaScript
     *                 will use when querying for updates. If null or blank
     *                 then the protocol from the request will be used
     */
    public JavaScriptBuilderElement(
            Logger logger,
            ElementDataFactory<JavaScriptBuilderData> elementDataFactory,
            String endpoint,
            String objName,
            boolean enableCookies,
            String host,
            String protocol) {
        this(logger, elementDataFactory, endpoint, objName, enableCookies, host, protocol, null);        
    }
    //! [constructor]
    
    //! [constructor]
    /**
     * Default constructor.
     * @param logger The logger.
     * @param elementDataFactory The element data factory.
     * @param endpoint Set the endpoint which will be queried on the host.
     *                 e.g /api/v4/json
     * @param objName The default name of the object instantiated by the client
     *                JavaScript.
     * @param enableCookies Set whether the client JavaScript stored results of
     * client side processing in cookies.
     * @param host The host that the client JavaScript should query for updates.
     * If null or blank then the host from the request will be used
     * @param protocol The protocol (HTTP or HTTPS) that the client JavaScript
     *                 will use when querying for updates. If null or blank
     *                 then the protocol from the request will be used
     * @param contextRoot The &lt;context-root&gt; setting from the web.xml.
     *                 This is needed when creating the callback URL.
     */
    public JavaScriptBuilderElement(
            Logger logger,
            ElementDataFactory<JavaScriptBuilderData> elementDataFactory,
            String endpoint,
            String objName,
            boolean enableCookies,
            String host,
            String protocol,
            String contextRoot) {
        super(logger, elementDataFactory);
        
        MustacheFactory mf = new DefaultMustacheFactory();
        InputStream in = getClass().getResourceAsStream(Constants.TEMPLATE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        mustache = mf.compile(reader, "template");
        
        this.host = host;
        this.endpoint = endpoint;
        this.protocol = protocol;
        this.objName = objName.isEmpty() ? Constants.DEFAULT_OBJECT_NAME : objName;
        this.enableCookies = enableCookies;
        this.contextRoot = contextRoot;
    }
    //! [constructor]

    @Override
    protected void processInternal(FlowData data) throws Exception {
        String reqHost = this.host;
        String reqProtocol = this.protocol;
        boolean supportsPromises;

        // Try and get the request host name so it can be used to request
        // the Json refresh in the JavaScript code.
        if (reqHost == null || reqHost.isEmpty()) {
            TryGetResult<String> hostEvidence = data.tryGetEvidence(
                Constants.EVIDENCE_HOST_KEY,
                String.class);
            if (hostEvidence.hasValue()) {
                reqHost = hostEvidence.getValue();
            }
        }
        
        // Try and get the web server context root evidence so it can be 
        // used to construct the correct path for the Json refresh.
        if(this.contextRoot == null || this.contextRoot.isEmpty()) {
            TryGetResult<String> contextRoot = data.tryGetEvidence(
                fiftyone.pipeline.core.Constants.EVIDENCE_WEB_CONTEXT_ROOT,
                String.class);
            if(contextRoot.hasValue()) {
                this.contextRoot = contextRoot.getValue();
            }
        }

        // Try and get the request protocol so it can be used to request
        // the JSON refresh in the JavaScript code.
        if (reqProtocol == null || reqProtocol.isEmpty()) {
            TryGetResult<String> protocolEvidence = data.tryGetEvidence(
                fiftyone.pipeline.core.Constants.EVIDENCE_PROTOCOL,
                String.class);
            if (protocolEvidence.hasValue()) {
                reqProtocol = protocolEvidence.getValue();
            }
        }

        // Couldn't get protocol from anywhere
        if (reqProtocol == null || reqProtocol.isEmpty()) {
            reqProtocol = Constants.DEFAULT_PROTOCOL;
        }

        // If device detection is enabled then try and get whether the
        // requesting browser supports promises. If not then default to false.
        try {
            AspectPropertyValue<?> supportsPromisesValue =
                data.getAs("Promise", AspectPropertyValue.class);
            supportsPromises = supportsPromisesValue.hasValue() &&
                supportsPromisesValue.getValue() == "Full";
        }
        catch (PipelineDataException e) {
            supportsPromises = false;
        }

        // Get the JSON include to embed into the JavaScript include.
        String jsonObject;
        
        try {
            jsonObject = data.get(JsonBuilderData.class).getJson();
        } catch (PipelineDataException e){
            throw new PipelineConfigurationException("Json data is missing,"
                    + " make sure there is a JsonBuilder element before this"
                    + " JavaScriptBuilderElement in the pipeline", e);
        }
        // Generate any required parameters for the JSON request.
        List<String> parameters = new ArrayList<>();

        Map<String, Object> queryEvidence = data
            .getEvidence()
            .asKeyMap();
        
        for(Map.Entry<String, Object> entry : queryEvidence.entrySet()){
            if(entry.getKey().startsWith(EVIDENCE_QUERY_PREFIX)){
                parameters.add(entry.getKey().substring(
                    entry.getKey().indexOf(EVIDENCE_SEPERATOR) + 1) +
                    "=" + entry.getValue());
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int s = 0; s < parameters.size(); s++)
        {
            sb.append(URLEncoder.encode(parameters.get(s), "UTF-8"));
            if(s < parameters.size() - 1)
                sb.append("&");
        }
        
        String queryParams = sb.toString();
        
        String url = null;
        if(reqProtocol != null && reqProtocol.isEmpty() == false &&
            reqHost != null && reqHost.isEmpty() == false &&
            endpoint != null && endpoint.isEmpty() == false) {
            boolean contextRootPopulated = contextRoot != null && 
                contextRoot.isEmpty() == false && contextRoot != "/";

            // Make sure that each part of the URL except host starts with a '/'
            // and each part except endpoint does NOT end with one. 
            if (endpoint.charAt(0) != '/') {
                endpoint = "/" + endpoint;
            }
            if (contextRootPopulated && contextRoot.charAt(0) != '/') {
                contextRoot = "/" + contextRoot;
            }
            if (reqHost.charAt(reqHost.length() - 1) == '/') {
                reqHost = reqHost.substring(0, reqHost.length() - 1);
            }
            if (contextRootPopulated && contextRoot.charAt(contextRoot.length() - 1) == '/') {
                contextRoot = contextRoot.substring(0, contextRoot.length() - 1);
            }

            url = reqProtocol + "://" + reqHost + 
                (contextRootPopulated ? contextRoot : "") + 
                endpoint +
                (queryParams.isEmpty() ? "" : "?" + queryParams);
        }

        // With the gathered resources, build a new JavaScriptResource.
        buildJavaScript(data, jsonObject, supportsPromises, url);
    }

    @Override
    public String getElementDataKey() {
        return "javascript-builder";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return new EvidenceKeyFilterWhitelist(Arrays.asList(
                Constants.EVIDENCE_HOST_KEY,
                fiftyone.pipeline.core.Constants.EVIDENCE_PROTOCOL,
                EVIDENCE_OBJECT_NAME,
                fiftyone.pipeline.core.Constants.EVIDENCE_WEB_CONTEXT_ROOT),
            String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return Collections.singletonList(
            (ElementPropertyMetaData) new ElementPropertyMetaDataDefault(
                "javascript",
                this,
                "javascript",
                String.class,
                true));
    }

    @Override
    protected void managedResourcesCleanup() {
        // Nothing to clean up here.
    }

    @Override
    protected void unmanagedResourcesCleanup() {
        // Nothing to clean up here.
    }

    private void buildJavaScript(
        FlowData data,
        String jsonObject,
        boolean supportsPromises,
        String url) {
        JavaScriptBuilderDataInternal elementData =
            (JavaScriptBuilderDataInternal)data.getOrAdd(
                getElementDataKey(),
                getDataFactory());

        String objectName;
        // Try and get the requested object name from evidence.
        TryGetResult<String> res = data.tryGetEvidence(
            EVIDENCE_OBJECT_NAME,
            String.class );
        if (res.hasValue() == false ||
            res.getValue().isEmpty()) {
            objectName = objName;
        } else {
            objectName = res.getValue();
        }

        boolean updateEnabled = url != null && url.isEmpty() == false;

        // This check won't be 100% fool-proof but it only needs to be
        // reasonably accurate and not take too long.
        boolean hasDelayedProperties = jsonObject != null &&
            jsonObject.contains("delayexecution");

        JavaScriptResource javaScriptObj = new JavaScriptResource(
            objectName,
            jsonObject,
            supportsPromises,
            url,
            enableCookies,    
            updateEnabled,
            hasDelayedProperties);
      
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, javaScriptObj.asMap());

        String content = stringWriter.toString();

        elementData.setJavaScript(content);
    }
}
//! [class]

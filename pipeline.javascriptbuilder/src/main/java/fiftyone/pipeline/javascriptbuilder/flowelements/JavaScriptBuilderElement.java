/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.javascriptbuilder.Constants;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import fiftyone.pipeline.javascriptbuilder.templates.JavaScriptResource;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Pattern;

import static fiftyone.pipeline.core.Constants.*;
import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.EVIDENCE_SESSIONID;
import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.EVIDENCE_SEQUENCE;
import static fiftyone.pipeline.javascriptbuilder.Constants.EVIDENCE_ENABLE_COOKIES;
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

    /**
     * The object name is written into the script as the name of a global
     * variable, as a session storage key and inside string literals, with no
     * escaping of JavaScript. A name that is not a plain JavaScript identifier
     * would therefore break the script or change what it does, so only names
     * matching this pattern are used.
     */
    private static final Pattern OBJECT_NAME_PATTERN =
        Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    /**
     * Words the pattern accepts that cannot be the name of the object. These
     * are the reserved words of the language, including those reserved only
     * in strict mode, plus the three global values a top level var cannot
     * replace, where the object would silently never be created. The last
     * entry is the constructor the script itself defines and calls to create
     * the object, so that name would clash with it.
     */
    private static final Set<String> RESERVED_OBJECT_NAMES =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "await", "break", "case", "catch", "class", "const", "continue",
            "debugger", "default", "delete", "do", "else", "enum", "export",
            "extends", "false", "finally", "for", "function", "if",
            "implements", "import", "in", "instanceof", "interface", "let",
            "new", "null", "package", "private", "protected", "public",
            "return", "static", "super", "switch", "this", "throw", "true",
            "try", "typeof", "var", "void", "while", "with", "yield",
            "Infinity", "NaN", "undefined",
            "fiftyoneDegreesManager")));

    /**
     * The message of the exception thrown when a configured object name is
     * not valid.
     */
    public static final String INVALID_OBJECT_NAME_MESSAGE =
        "JavaScriptBuilder ObjectName is invalid. This must be a valid " +
        "JavaScript identifier that is not a reserved word.";

    /**
     * Whether a name can be used as the name of the object instantiated by
     * the client JavaScript.
     * @param name the requested name
     * @return true if the name is a valid JavaScript identifier and not a
     * reserved word
     */
    public static boolean isValidObjectName(String name) {
        return name != null &&
            OBJECT_NAME_PATTERN.matcher(name).matches() &&
            RESERVED_OBJECT_NAMES.contains(name) == false;
    }

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
     * @throws PipelineConfigurationException if objName is not null and is
     * not a valid JavaScript identifier, see {@link #isValidObjectName}.
     * A null name means the default name is used, as no name was
     * configured, and an empty name is refused
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
        if (objName == null) {
            this.objName = Constants.DEFAULT_OBJECT_NAME;
        } else if (isValidObjectName(objName)) {
            this.objName = objName;
        } else {
            throw new PipelineConfigurationException(
                INVALID_OBJECT_NAME_MESSAGE);
        }
        this.enableCookies = enableCookies;
        this.contextRoot = contextRoot;
    }
    //! [constructor]

    @Override
    protected void processInternal(FlowData data) throws Exception {
        String reqHost = getHost(data);
        String reqProtocol = getProtocol(data);
        boolean supportsPromises = getSupportsPromises(data);

        // Try and get the web server context root evidence so it can be 
        // used to construct the correct path for the Json refresh.
        if (contextRoot == null || contextRoot.isEmpty()) {
            contextRoot = getContextRoot(data);
        }

        // Get the JSON include to embed into the JavaScript include.
        String jsonObject = getJsonObject(data);
        // Generate any required parameters for the JSON request.
        // The parameters reach the script only through the serialized
        // parameters object, so the URL carries no query string. The .NET
        // builder renders the URL the same way.
        Map<String, String> parameters = getParameters(data);

        String url = getUrl(reqProtocol, reqHost);

        String sessionId = getSessionId(data);
        int sequence = getSequence(data);
        String serializedParameters = serializeParameters(parameters);

        // With the gathered resources, build a new JavaScriptResource.
        buildJavaScript(data, jsonObject, supportsPromises, url, serializedParameters, sessionId, sequence);
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
                EVIDENCE_ENABLE_COOKIES,
                EVIDENCE_WEB_CONTEXT_ROOT),
                String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return Collections.singletonList(
                new ElementPropertyMetaDataDefault(
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

    private String getHost(FlowData data) {
        String reqHost = this.host;

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

        return reqHost;
    }

    private String getProtocol(FlowData data) {
        String reqProtocol = this.protocol;

        // Try and get the request protocol so it can be used to request
        // the JSON refresh in the JavaScript code.
        if (reqProtocol == null || reqProtocol.isEmpty()) {
            TryGetResult<String> protocolEvidence = data.tryGetEvidence(
                    fiftyone.pipeline.core.Constants.EVIDENCE_PROTOCOL,
                    String.class);
            if (protocolEvidence.hasValue()) {
                reqProtocol = protocolEvidence.getValue();
            } else {
                // Couldn't get protocol from anywhere
                reqProtocol = Constants.DEFAULT_PROTOCOL;
            }
        }

        return reqProtocol;
    }

    private boolean getSupportsPromises(FlowData data) {
        boolean supportsPromises;

        // If device detection is enabled then try and get whether the
        // requesting browser supports promises. If not then default to false.
        try {
            AspectPropertyValue<?> supportsPromisesValue =
                    data.getAs("Promise", AspectPropertyValue.class);
            supportsPromises = supportsPromisesValue.hasValue() &&
                    supportsPromisesValue.getValue() == "Full";
        } catch (PipelineDataException | PropertyMissingException e) {
            supportsPromises = false;
        }

        return supportsPromises;
    }

    private String getContextRoot(FlowData data) {
        String root = null;

        // Try and get the web server context root evidence so it can be
        // used to construct the correct path for the Json refresh.
        TryGetResult<String> contextRoot = data.tryGetEvidence(
                EVIDENCE_WEB_CONTEXT_ROOT,
                String.class);
        if (contextRoot.hasValue()) {
            root = contextRoot.getValue();
        }
        return root;
    }

    private String getJsonObject(FlowData data) {
        String jsonObject;

        try {
            jsonObject = data.get(JsonBuilderData.class).getJson();
        } catch (PipelineDataException e){
            throw new PipelineConfigurationException("Json data is missing,"
                    + " make sure there is a JsonBuilder element before this"
                    + " JavaScriptBuilderElement in the pipeline", e);
        }

        return jsonObject;
    }

    /**
     * Evidence the rendered script is not configured with. The script appends
     * both to its own request itself, so naming them here as well would send
     * each twice and, worse, put the session id into the record the script
     * keeps of a request's inputs. That record decides whether a later page
     * view in the same tab can be served from the cached response, and a
     * session id is different on every page view, so a record holding one
     * could never match. The .NET builder excludes the same two and is the
     * reference for this behaviour.
     */
    private static final Set<String> EXCLUDED_PARAMETERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    EVIDENCE_SESSIONID,
                    EVIDENCE_SEQUENCE)));

    private Map<String, String> getParameters(FlowData data) throws UnsupportedEncodingException {
        HashMap<String, String> parameters = new HashMap<>();

        Map<String, Object> queryEvidence = data
                .getEvidence()
                .asKeyMap();

        for(Map.Entry<String, Object> entry : queryEvidence.entrySet()){
            if(entry.getKey().startsWith(EVIDENCE_QUERY_PREFIX) &&
                    EXCLUDED_PARAMETERS.contains(entry.getKey()) == false){
                String key = entry.getKey().substring(entry.getKey().indexOf(EVIDENCE_SEPERATOR) + 1);
                key = URLEncoder.encode(key, "UTF-8");
                String value = URLEncoder.encode(entry.getValue().toString(), "UTF-8");
                parameters.put(key, value);
            }
        }

        return parameters;
    }

    private String serializeParameters(Map<String, String> parameters) {
        JSONObject jsonObject = new JSONObject(parameters);
        return jsonObject.toString(0);
    }

    private String getUrl(String protocol, String host) {
        String url = null;
        if (protocol != null && !protocol.isEmpty() &&
                host != null && !host.isEmpty() &&
                endpoint != null && !endpoint.isEmpty()) {
            boolean contextRootPopulated = contextRoot != null &&
                    !contextRoot.isEmpty() && !contextRoot.equals("/");

            // Make sure that each part of the URL except host starts with a '/'
            // and each part except endpoint does NOT end with one.
            if (endpoint.charAt(0) != '/') {
                endpoint = "/" + endpoint;
            }
            if (contextRootPopulated && contextRoot.charAt(0) != '/') {
                contextRoot = "/" + contextRoot;
            }
            if (host.charAt(host.length() - 1) == '/') {
                host = host.substring(0, host.length() - 1);
            }
            if (contextRootPopulated && contextRoot.charAt(contextRoot.length() - 1) == '/') {
                contextRoot = contextRoot.substring(0, contextRoot.length() - 1);
            }

            url = protocol + "://" + host +
                    (contextRootPopulated ? contextRoot : "") +
                    endpoint;
        }

        return url;
    }

    /**
     * The session id is written into the script inside double quotes with no
     * escaping, so only a value of 1 to 64 ASCII letters, digits and hyphens
     * is written. Any other value could break the script or change what it
     * does, and is written as an empty string instead. The id the Sequence Element
     * creates always matches.
     */
    private static final Pattern SESSION_ID_PATTERN =
        Pattern.compile("[A-Za-z0-9-]{1,64}");

    /**
     * A sequence given as text is only read when it is made of digits, and
     * one of more than ten digits cannot be a 32 bit integer.
     */
    private static final Pattern SEQUENCE_PATTERN =
        Pattern.compile("[0-9]{1,10}");

    /**
     * Gets the session id to write into the script. This is the session id
     * evidence, set by the Sequence Element or by the request, when it
     * matches SESSION_ID_PATTERN, and an empty string otherwise.
     * @param data the flow data
     * @return the session id, or an empty string
     */
    private String getSessionId(FlowData data) {
        TryGetResult<Object> trySessionId =
            data.tryGetEvidence(EVIDENCE_SESSIONID, Object.class);
        if (trySessionId.hasValue() &&
            trySessionId.getValue() instanceof String) {
            String sessionId = (String) trySessionId.getValue();
            if (SESSION_ID_PATTERN.matcher(sessionId).matches()) {
                return sessionId;
            }
        }
        return "";
    }

    /**
     * Gets the sequence to write into the script. This is the sequence
     * evidence, as a number or as text, when it is a positive 32 bit
     * integer, and 1 otherwise, including when there is no Sequence Element.
     * The value is written into the script as code, so text that is not a
     * number could break the script, and a number below 1 is not a sequence
     * the Sequence Element would ever give.
     * @param data the flow data
     * @return the sequence, which is always at least 1
     */
    private int getSequence(FlowData data) {
        TryGetResult<Object> trySequence =
            data.tryGetEvidence(EVIDENCE_SEQUENCE, Object.class);
        long sequence = 0;
        if (trySequence.hasValue()) {
            Object value = trySequence.getValue();
            if (value instanceof Integer) {
                sequence = (Integer) value;
            } else if (value instanceof String &&
                SEQUENCE_PATTERN.matcher((String) value).matches()) {
                sequence = Long.parseLong((String) value);
            }
        }
        return sequence >= 1 && sequence <= Integer.MAX_VALUE
            ? (int) sequence
            : 1;
    }

    private void buildJavaScript(
        FlowData data,
        String jsonObject,
        boolean supportsPromises,
        String url,
        String parameters,
        String sessionId,
        int sequence) {
        JavaScriptBuilderDataInternal elementData =
            (JavaScriptBuilderDataInternal)data.getOrAdd(
                getElementDataKey(),
                getDataFactory());

        String objectName = objName;
        // Try and get the requested object name from evidence. A name that
        // is not a valid identifier is ignored and the configured name is
        // used.
        TryGetResult<String> res = data.tryGetEvidence(
            EVIDENCE_OBJECT_NAME,
            String.class );
        if (res.hasValue()) {
            if (isValidObjectName(res.getValue())) {
                objectName = res.getValue();
            } else {
                logger.warn("The requested JavaScript object name is not a " +
                    "valid JavaScript identifier, so the configured name '" +
                    objName + "' was used instead.");
            }
        }

        boolean cookies;
        // Try and get the requested enable cookies from evidence.
        TryGetResult<String> cookieVal = data.tryGetEvidence(
            EVIDENCE_ENABLE_COOKIES,
            String.class);
        if (cookieVal.hasValue() == false ||
            cookieVal.getValue().isEmpty()) {
            cookies = enableCookies;
        } else {
            cookies = Boolean.parseBoolean(cookieVal.getValue());
        }

        boolean updateEnabled = url != null && url.isEmpty() == false;

        // This check won't be 100% fool-proof but it only needs to be
        // reasonably accurate and not take too long.
        boolean hasDelayedProperties = jsonObject != null &&
            jsonObject.contains("delayexecution");

        JavaScriptResource javaScriptObj = new JavaScriptResource(
            objectName,
            jsonObject,
            sessionId,
            sequence,
            supportsPromises,
            url,
            parameters,
            cookies,
            updateEnabled,
            hasDelayedProperties);
      
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, javaScriptObj.asMap());

        String content = stringWriter.toString();

        elementData.setJavaScript(content);
    }
}
//! [class]

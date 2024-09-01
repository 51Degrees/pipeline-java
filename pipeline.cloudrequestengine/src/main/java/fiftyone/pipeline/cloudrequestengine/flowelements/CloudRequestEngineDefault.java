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
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.Constants;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngineBase;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.exceptions.AggregateException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fiftyone.pipeline.cloudrequestengine.Constants.Messages.*;
import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Engine that makes requests to the 51Degrees cloud service.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/cloud-request-engine.md">Specification</a>
 */
public class CloudRequestEngineDefault
    extends AspectEngineBase<CloudRequestData, AspectPropertyMetaData>
    implements CloudRequestEngine {
    private HttpClient httpClient;

    private String endPoint;
    private String resourceKey;
    private String licenseKey;
    private String propertiesEndpoint;
    private String evidenceKeysEndpoint;
    private String cloudRequestOrigin;
    private Integer timeoutMillis;

    private List<AspectPropertyMetaData> propertyMetaData;
    private Map<String, AccessiblePropertyMetaData.ProductMetaData> publicProperties;

    private EvidenceKeyFilter evidenceKeyFilter;

    public CloudRequestEngineDefault(
        Logger logger,
        ElementDataFactory<CloudRequestData> aspectDataFactory,
        HttpClient httpClient,
        String endPoint,
        String resourceKey,
        String propertiesEndpoint,
        String evidenceKeysEndpoint,
        int timeoutMillis) throws Exception {
        this(
            logger,
            aspectDataFactory,
            httpClient,
            endPoint,
            resourceKey,
            null,
            propertiesEndpoint,
            evidenceKeysEndpoint,
            timeoutMillis,
            null);
    }
    public CloudRequestEngineDefault(
        Logger logger,
        ElementDataFactory<CloudRequestData> aspectDataFactory,
        HttpClient httpClient,
        String endPoint,
        String resourceKey,
        String propertiesEndpoint,
        String evidenceKeysEndpoint,
        int timeoutMillis,
        String cloudRequestOrigin) throws Exception {
        this(
            logger,
            aspectDataFactory,
            httpClient,
            endPoint,
            resourceKey,
            null,
            propertiesEndpoint,
            evidenceKeysEndpoint,
            timeoutMillis,
            cloudRequestOrigin);
    }
    public CloudRequestEngineDefault(
        Logger logger,
        ElementDataFactory<CloudRequestData> aspectDataFactory,
        HttpClient httpClient,
        String endPoint,
        String resourceKey,
        String licenseKey,
        String propertiesEndpoint,
        String evidenceKeysEndpoint,
        int timeoutMillis,
        String cloudRequestOrigin) throws Exception {
        super(logger, aspectDataFactory);
        try
        {
            this.endPoint = endPoint;
            this.resourceKey = resourceKey;
            this.licenseKey = licenseKey;
            this.propertiesEndpoint = propertiesEndpoint;
            this.evidenceKeysEndpoint = evidenceKeysEndpoint;
            this.httpClient = httpClient;
            this.cloudRequestOrigin = cloudRequestOrigin;

            if (timeoutMillis > 0) {
                this.timeoutMillis = timeoutMillis;
            }
            else {
                this.timeoutMillis = null;
            }

            getCloudProperties();

            getCloudEvidenceKeys();

            propertyMetaData = new ArrayList<>();
            propertyMetaData.add(new AspectPropertyMetaDataDefault(
                "json-response",
                this,
                "",
                String.class,
                new ArrayList<String>(),
                true));
            propertyMetaData.add(new AspectPropertyMetaDataDefault(
                    "process-started",
                    this,
                    "",
                    Boolean.class,
                    new ArrayList<String>(),
                    true));
        }
        catch (Exception ex) {
            logger.error("Error creating " + this.getClass().getName(), ex);
            throw ex;
        }
    }

    @Override
    public List<AspectPropertyMetaData> getProperties() {
        return propertyMetaData;
    }

    @Override
    public String getDataSourceTier() {
        return "cloud";
    }

    @Override
    public String getElementDataKey() {
        return "cloud-response";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceKeyFilter;
    }

    @Override
    public Map<String, AccessiblePropertyMetaData.ProductMetaData>
    getPublicProperties() {
        return publicProperties;
    }

    @Override
    protected void processEngine(FlowData data, CloudRequestData aspectData) throws IOException {
        byte[] content = getContent(data);
        HttpURLConnection connection = httpClient.connect(new URL(endPoint.trim()));
        if (timeoutMillis != null) {
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
        }
        ((CloudRequestDataInternal)aspectData).setProcessStarted(true);
        
        Map<String, String> headers = new HashMap<>();
        setCommonHeaders(headers);
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Content-Length", Integer.toString(content.length));

        String response = httpClient.postData(connection, headers, content);

        ((CloudRequestDataInternal)aspectData).setJsonResponse(response);

        validateResponse(response, connection);
    }

    /**
     * Generate the Content to send in the POST request. The evidence keys
     * e.g. 'query.' and 'header.' have an order of precedence. These are
     * added to the evidence in reverse order, if there is conflict then
     * the queryData value is overwritten.
     * 'query.' evidence should take precedence over all other evidence.
     * If there are evidence keys other than 'query.' that conflict then
     * this is unexpected so a warning will be logged.
     * @param data the FlowData
     * @return form content for a POST request
     * @throws UnsupportedEncodingException
     */
    private byte[] getContent(FlowData data) throws UnsupportedEncodingException {

        Map<String, Object> formData = getFormData(data);
        List<String> formItems = new ArrayList<>();

        formItems.add("resource=" + resourceKey);
        if(licenseKey != null && licenseKey.isEmpty() == false){
            formItems.add("license=" + licenseKey);
        }

        List<String> formKeys = Arrays.asList(formData.keySet().toArray(new String[0]));
        Collections.sort(formKeys, Collections.reverseOrder());
        for (String key : formKeys) {
            formItems.add(key + "=" + URLEncoder.encode(formData.get(key).toString(), "UTF-8"));

        }

        String string = stringJoin(formItems, "&");

        return string.getBytes(StandardCharsets.UTF_8);
    }

    Map<String, Object> getFormData(FlowData flowData) {
        Map<String, Object> evidence = flowData.getEvidence().asKeyMap();
        Map<String, Object> formData = new HashMap<>();

        // Add evidence in reverse alphabetical order, excluding special keys.
        addFormData(formData, evidence, getSelectedEvidence(evidence, "other"));
        // Add cookie evidence.
        addFormData(formData, evidence, getSelectedEvidence(evidence, "cookie"));
        // Add header evidence.
        addFormData(formData, evidence, getSelectedEvidence(evidence, "header"));
        // Add query evidence.
        addFormData(formData, evidence, getSelectedEvidence(evidence, "query"));

        return formData;
    }

    /**
     * Add form data to the evidence.
     * @param formData the destination map to add the data to
     * @param allEvidence all evidence in the FlowData. This is used to
     *                    report which evidence keys are conflicting
     * @param evidence evidence to add to the form data
     */
    private void addFormData(
        Map<String, Object> formData,
        Map<String, Object> allEvidence,
        Map<String, Object> evidence) {

        List<String> evidenceKeys = Arrays.asList(evidence.keySet().toArray(new String[0]));
        Collections.sort(evidenceKeys, Collections.reverseOrder());

        for (String evidenceKey : evidenceKeys) {
            // Get the key parts
            String[] evidenceKeyParts = evidenceKey.split(Pattern.quote(Constants.EVIDENCE_SEPERATOR));
            String prefix = evidenceKeyParts[0];
            String suffix = evidenceKeyParts[1];

            // Check and add the evidence to the query parameters.
            if (formData.containsKey(suffix) == false) {
                formData.put(suffix, evidence.get(evidenceKey));
            }
            else {
                // If the queryParameter exists already.
                // Get the conflicting pieces of evidence and then log a
                // warning, if the evidence prefix is not query. Otherwise a
                // warning is not needed as query evidence is expected
                // to overwrite any existing evidence with the same suffix.
                if (prefix.equals("query") == false) {
                    Map<String, Object> conflicts = new HashMap<>();
                    for (String key : allEvidence.keySet()) {
                        if (key.equals(evidenceKey) == false && key.contains(suffix)) {
                            conflicts.put(key, allEvidence.get(key));
                        }
                    }

                    StringBuilder conflictStr = new StringBuilder();
                    for (Map.Entry<String, Object> conflict : conflicts.entrySet()) {
                        if (conflictStr.length() > 0) {
                            conflictStr.append(", ");
                        }
                        conflictStr.append(String.format("%s:%s", conflict.getKey(), conflict.getValue()));
                    }

                    String warningMessage = String.format(
                            fiftyone.pipeline.cloudrequestengine.Constants.Messages.EvidenceConflict,
                            evidenceKey,
                            evidence.get(evidenceKey),
                            conflictStr.toString());
                    logger.warn(warningMessage);
                }
                // Overwrite the existing queryParameter value.
                formData.put(suffix, evidence.get(evidenceKey));
            }
        }
    }

    /**
     * Get evidence with specified prefix.
     * @param evidence all evidence in the FlowData
     * @param type required evidence key prefix
     * @return evidence with the required key prefix
     */
    Map<String, Object> getSelectedEvidence(Map<String, Object> evidence, String type) {
        Map<String, Object> selectedEvidence = new HashMap<>();

        if (type.equals("other")) {
            for (Map.Entry<String, Object> entry : evidence.entrySet()) {
                if (hasKeyPrefix(entry.getKey(), "query") == false &&
                    hasKeyPrefix(entry.getKey(), "header") == false &&
                    hasKeyPrefix(entry.getKey(), "cookie") == false ) {
                    selectedEvidence.put(entry.getKey(), entry.getValue());
                }
            }
        }
        else {
            for (Map.Entry<String, Object> entry : evidence.entrySet()) {
                if (hasKeyPrefix(entry.getKey(), type)) {
                    selectedEvidence.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return selectedEvidence;
    }

    /**
     * Check that the key of a KeyValuePair has the given prefix.
     * @param itemKey key to check
     * @param prefix the prefix to check for
     * @return true if the key has the prefix
     */
    private boolean hasKeyPrefix(String itemKey, String prefix) {
        return itemKey.startsWith(prefix + ".");
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }

    private void setCommonHeaders(Map<String, String> headers) {        
        if(cloudRequestOrigin != null &&
            cloudRequestOrigin.length() > 0) {
            headers.put(fiftyone.pipeline.cloudrequestengine.Constants.OriginHeaderName, cloudRequestOrigin);
        }
    }

    private void getCloudProperties() throws CloudRequestException, AggregateException, IOException {
        String jsonResult;

        Map<String, String> headers = new HashMap<>();
        setCommonHeaders(headers);
        
        HttpURLConnection connection = httpClient.connect(new URL(propertiesEndpoint.trim() + (resourceKey != null ? "?Resource=" + resourceKey : "")));
        jsonResult = httpClient.getResponseString(connection, headers);
        validateResponse(jsonResult, connection);

        JSONObject jsonObj = null;
        if (jsonResult.isEmpty() == false) {
            jsonObj = new JSONObject(jsonResult);
        }

        if (jsonObj != null) {
            AccessiblePropertyMetaData.LicencedProducts accessiblePropertyData =
                new AccessiblePropertyMetaData.LicencedProducts(jsonObj.getJSONObject("Products"));

            publicProperties = accessiblePropertyData.products;
        }
        else {
            throw new RuntimeException("Failed to retrieve available properties " +
                "from cloud service at " + propertiesEndpoint + ".");
        }
    }

    private void getCloudEvidenceKeys() throws CloudRequestException, AggregateException, IOException {
        String jsonResult;

        Map<String, String> headers = new HashMap<>();
        setCommonHeaders(headers);

        HttpURLConnection connection = httpClient.connect(new URL(evidenceKeysEndpoint.trim()));
        jsonResult = httpClient.getResponseString(connection, headers);
        validateResponse(jsonResult, connection, false);

        if (jsonResult != null && jsonResult.isEmpty() == false) {
            JSONArray jsonArray = new JSONArray(jsonResult);
            List<String> keys = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                keys.add(jsonArray.get(i).toString());
            }
            evidenceKeyFilter = new EvidenceKeyFilterWhitelist(keys,
                String.CASE_INSENSITIVE_ORDER);
        }
    }

    /**
     * Validate the JSON response from the cloud service.
     * @param jsonResult the JSON content that is returned from the cloud
     * @param connection a HttpURLConnection
     */
    private void validateResponse(String jsonResult, HttpURLConnection connection)
        throws IOException, CloudRequestException, AggregateException {
        validateResponse(jsonResult, connection, true);
    }
    
    /**
     * Validate the JSON response from the cloud service.
     * @param jsonResult the JSON content that is returned from the cloud
     * @param connection connection used when making the request
     * @param checkForErrorMessages Set to false if the response will 
     * never contain error message text.
     */
    private void validateResponse(String jsonResult, 
        HttpURLConnection connection,
        boolean checkForErrorMessages) 
        throws IOException, CloudRequestException, AggregateException {

        int code = connection.getResponseCode();
        boolean hasData = jsonResult != null && jsonResult.isEmpty() == false;
        List<String> messages = new ArrayList<>();

        if (hasData && checkForErrorMessages) {
            JSONObject jObj = new JSONObject(jsonResult);
            boolean hasErrors = jObj.keySet().contains("errors");
            hasData = hasErrors ?
                jObj.keySet().size() > 1 :
                jObj.keySet().size() > 0;

            if (hasErrors) {
                JSONArray errors = jObj.getJSONArray("errors");
                messages.addAll(
                    errors.toList()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.toList()));
            }
        }

        // If there were no errors but there was also no other data
        // in the response then add an explanation to the list of
        // messages.
        if (messages.size() == 0 && hasData == false) {
            String message = String.format(
                MessageNoDataInResponse,
                this.endPoint);
            messages.add(message);
        }
        // If there is no detailed error message, but we got a
        // non-success status code, then add a message to the list
        else if (messages.size() == 0 && code != 200)
        {
            String message = String.format(
                MessageErrorCodeReturned,
                this.endPoint,
                code,
                jsonResult);
            messages.add(message);
        }

        Map<String, List<String>> headers = null;
        if (messages.size() > 0){
            headers = connection.getHeaderFields();
        }
        final Map<String, List<String>> finalHeaders = headers;

        // If there are any errors returned from the cloud service
        // then throw an exception
        if (messages.size() > 1) {
            throw new AggregateException(
                ExceptionCloudErrorsMultiple,
                messages.stream()
                    .map(m -> new CloudRequestException(m, code, finalHeaders))
                    .collect(Collectors.toList()));
        }
        else if (messages.size() == 1) {
            String message = String.format(
                ExceptionCloudError,
                messages.get(0));
            throw new CloudRequestException(message, code, finalHeaders);
        }
    }
}

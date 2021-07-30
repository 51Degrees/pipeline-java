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

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.Constants;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fiftyone.pipeline.cloudrequestengine.Constants.Messages.*;
import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Engine that makes requests to the 51Degrees cloud service based on the
 * details passed at creation and the evidence in the FlowData instance. The
 * unprocessed JSON response is stored in the FlowData for other engines to make
 * use of.
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
        int timeoutMillis) {
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
        String cloudRequestOrigin) {
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
        String cloudRequestOrigin) {
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

        Map<String, String> headers = new HashMap<>();
        setCommonHeaders(headers);
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("Content-Length", Integer.toString(content.length));

        String response = httpClient.postData(connection, headers, content);

        ((CloudRequestDataInternal)aspectData).setJsonResponse(response);

        validateResponse(response, connection.getResponseCode());
    }

    private byte[] getContent(FlowData data) throws UnsupportedEncodingException {
        Map<String, Object> evidence = data.getEvidence().asKeyMap();
        List<String> formData = new ArrayList<>();

        formData.add("resource=" + resourceKey);

        if(licenseKey != null && licenseKey.isEmpty() == false){
            formData.add("license=" + licenseKey);
        }

        for (Map.Entry<String, Object> item : evidence.entrySet()) {
            String[] key = item.getKey().split(Pattern.quote(Constants.EVIDENCE_SEPERATOR));
            formData.add(key[key.length - 1] + "=" + URLEncoder.encode(item.getValue().toString(), "UTF-8"));
        }

        String string = stringJoin(formData, "&");

        return string.getBytes(StandardCharsets.UTF_8);
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

    private void getCloudProperties() {
        int response;
        String jsonResult;

        Map<String, String> headers = new HashMap<>();
        setCommonHeaders(headers);

        try {
            HttpURLConnection connection = httpClient.connect(new URL(propertiesEndpoint.trim()));
            jsonResult = httpClient.getResponseString(connection, headers);
            response = connection.getResponseCode();
        }
        catch (Exception ex) {
            throw new RuntimeException("Failed to retrieve available properties " +
                "from cloud service at " + propertiesEndpoint + ".", ex);
        }
        
        if (response >= 400)
        {
            RuntimeException exception = new RuntimeException();
            if (jsonResult.isEmpty() == false)
            {
                JSONObject jsonObj = new JSONObject(jsonResult);
                for (Object o : jsonObj.getJSONArray("errors"))
                {
                    if(o instanceof String){
                        exception.addSuppressed(new Exception(o.toString()));
                    }
                }
            }
            throw exception;
        }


        if (jsonResult != null && jsonResult.isEmpty() == false) {
            JSONObject jsonObj = new JSONObject(jsonResult);
            AccessiblePropertyMetaData.LicencedProducts accessiblePropertyData =
                new AccessiblePropertyMetaData.LicencedProducts(jsonObj.getJSONObject("Products"));

            publicProperties = accessiblePropertyData.products;
        }
        else {
            throw new RuntimeException("Failed to retrieve available properties " +
                "from cloud service at " + propertiesEndpoint + ".");
        }
    }

    private void getCloudEvidenceKeys() {
        String jsonResult;

        Map<String, String> headers = new HashMap<>();
        setCommonHeaders(headers);

        try {
            HttpURLConnection connection = httpClient.connect(new URL(evidenceKeysEndpoint.trim()));
            jsonResult = httpClient.getResponseString(connection, headers);
        }
        catch (Exception ex) {
            throw new RuntimeException("Failed to retrieve evidence keys " +
                "from the cloud service at " + evidenceKeysEndpoint + ".", ex);
        }

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
     * @param code HTTP response code
     * service
     */
    private void validateResponse(String jsonResult, int code) {
        boolean hasData = jsonResult != null && jsonResult.isEmpty() == false;
        List<String> messages = new ArrayList<>();

        if (hasData) {
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

        // If there are any errors returned from the cloud service
        // then throw an exception
        if (messages.size() > 1) {
            throw new AggregateException(
                ExceptionCloudErrorsMultiple,
                messages.stream()
                    .map(m -> new PipelineDataException(m))
                    .collect(Collectors.toList()));
        }
        else if (messages.size() == 1) {
            String message = String.format(
                ExceptionCloudError,
                messages.get(0));
            throw new PipelineDataException(message);
        }

        // If there were no errors returned but the response code was non
        // success then throw an exception.
        if (messages.size() == 0 && code != 200) {

            throw new PipelineDataException("Received response code " +
                code +
                ". Content was: " +
                jsonResult);
        }
    }
}

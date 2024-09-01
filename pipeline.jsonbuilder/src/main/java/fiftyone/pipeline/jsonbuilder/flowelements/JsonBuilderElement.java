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

package fiftyone.pipeline.jsonbuilder.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.data.PropertyMatcher;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.NoValueException;
import fiftyone.pipeline.engines.fiftyone.flowelements.SetHeadersElement;
import fiftyone.pipeline.jsonbuilder.Constants;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;
import org.slf4j.Logger;

import static fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR;

//! [class]
//! [constructor]
/**
 * The JsonBuilderElement takes accessible properties and adds the property
 * key:values to the Json object.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/json-builder.md">Specification</a>
 */
public class JsonBuilderElement
    extends FlowElementBase<JsonBuilderData, ElementPropertyMetaData>
    implements JsonBuilder {
    private static final String JAVASCRIPT_PROPERTIES_NAME = "javascriptProperties";
    private final EvidenceKeyFilter evidenceKeyFilter;
    private final List<ElementPropertyMetaData> properties;
    private final List<String> blacklist;
    private final Set<String> elementBlacklist;

    /**
     * Default constructor.
     * @param logger The logger.
     * @param elementDataFactory The element data factory.
     */
    public JsonBuilderElement(
            Logger logger,
            ElementDataFactory<JsonBuilderData> elementDataFactory) {
        super(logger, elementDataFactory);
        // Set the evidence key filter for the flow data to use.
        List<String> whiteList = new ArrayList<>();
        evidenceKeyFilter = new EvidenceKeyFilterWhitelist(
            whiteList,
            String.CASE_INSENSITIVE_ORDER);

        properties = Collections.singletonList(
            (ElementPropertyMetaData)new ElementPropertyMetaDataDefault(
                "json",
                this,
                "",
                String.class,
                true));

        // Blacklist of properties which should not be added to the Json.
        blacklist = Arrays.asList("products", "properties");
        // Blacklist of the element data keys of elements that should
        // not be added to the Json.
        elementBlacklist = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        elementBlacklist.add("cloud-response");
        elementBlacklist.add("json-builder");
        elementBlacklist.add(SetHeadersElement.SET_HEADER_ELEMENT_DATAKEY);
    }
//! [constructor]

    /**
     * Contains configuration information relating to a particular
     * pipeline.
     * In most cases, a single instance of this element will only
     * be added to one pipeline at a time but it does support being
     * added to multiple pipelines.
     * simultaneously.
     */
    protected class PipelineConfig {
        /**
         * A collection of the complete string names of any properties
         * with the 'delay execution' flag set to true.
         * Note that 'complete name' means that the name will include
         * the element data key and any other parts of the segmented
         * name.
         * For example, `device.ismobile`
         */
        public HashSet<String> delayedExecutionProperties = new HashSet<>();

        /**
         * A collection containing the details of relevant evidence
         * properties.
         * The key is the complete property name.
         * Note that 'complete name' means that the name will include
         * the element data key and any other parts of the segmented
         * name.
         * For example, `device.ismobile`
         * The value is a list of the JavaScript properties that,
         * when executed, will provide values that can help determine
         * the value of the key property.
         */
        public Map<String, List<String>> delayedEvidenceProperties = new HashMap<>();
    }


    private ConcurrentHashMap<Pipeline, PipelineConfig> pipelineConfigs
        = new ConcurrentHashMap<>();

    @Override
    protected void processInternal(FlowData data) throws Exception {
        PipelineConfig config;
        if (pipelineConfigs.containsKey(data.getPipeline())) {
            config = pipelineConfigs.get(data.getPipeline());
        }
        else {
            config = populateMetaDataCollections(data.getPipeline());
            pipelineConfigs.putIfAbsent(data.getPipeline(), config);
            config = pipelineConfigs.get(data.getPipeline());
        }

        JsonBuilderDataInternal elementData =
            (JsonBuilderDataInternal)data.getOrAdd(
                getElementDataKey(),
                getDataFactory());
        
        String jsonString = buildJson(data, config);
        
        elementData.setJson(jsonString);
    }

    @Override
    public String getElementDataKey() {
        return "json-builder";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceKeyFilter;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return properties;
    }

    @Override
    protected void managedResourcesCleanup() {
        // Nothing to clean up here.
    }

    @Override
    protected void unmanagedResourcesCleanup() {
        // Nothing to clean up here.
    }

    /**
     * Create and populate a JSON string from the specified data.
     * @param data to convert to JSON
     * @param config the configuration to use
     * @return a string containing the data in JSON format
     * @throws Exception
     */
    private String buildJson(FlowData data, PipelineConfig config) throws Exception {
        
        Integer sequenceNumber;
        try {
          sequenceNumber = getSequenceNumber(data);
        } catch (Exception e) {
            throw new PipelineConfigurationException("Make sure there is a "
                    + "SequenceElement placed before this JsonBuilderElement "
                    + "in the pipeline", e);
        }
        
        Map<String, Object> allProperties = getAllProperties(data, config);
        
        // Only populate the javascript properties if the sequence 
        // has not reached max iterations.
        if (sequenceNumber < Constants.MAX_JAVASCRIPT_ITERATIONS) {
            addJavaScriptProperties(data, allProperties);
        }        
        addErrors(data, allProperties);
        
        return buildJson(allProperties);
    }

    private int getSequenceNumber(FlowData data) throws Exception {
        TryGetResult<Integer> sequence = data.tryGetEvidence(
            "query.sequence",
            Integer.class);
        if(sequence.hasValue() == false) {
            throw new Exception("Sequence number not present in evidence. " +
                "this is mandatory.");
        }
        return sequence.getValue();
    }
    
    private Map<String, Object> getAllProperties(
        FlowData data,
        PipelineConfig config) throws NoValueException {
        if (data == null) throw new IllegalArgumentException("data");
        if (config == null) throw new IllegalArgumentException("config");

        Map<String, Object> allProperties = new HashMap<>();

        for (Map.Entry<String, Object> element : data.elementDataAsMap().entrySet()) {
            if (elementBlacklist.contains(element.getKey()) == false &&
                allProperties.containsKey(element.getKey().toLowerCase()) == false) {
                Map<String, Object> elementProperties = getValues(
                    element.getKey().toLowerCase(),
                    ((ElementData)element.getValue()).asKeyMap(),
                    config);
                allProperties.put(element.getKey().toLowerCase(), elementProperties);
            }
        }

        return allProperties;
    }

    /**
     * Get the names and values for all the JSON properties required
     * to represent the given source data.
     * The method adds meta-properties as required such as
     * *nullreason, *delayexecution, etc.
     * @param dataPath the . separated name of the container that the supplied
     *                 data will be added to. For example, 'location' or
     *                 'devices.profiles'
     * @param sourceData the source data to use when populating the result
     * @param config the configuration to use
     * @return a new dictionary with string keys and object values
     */
    private Map<String, Object> getValues(
        String dataPath,
        Map<String, Object> sourceData,
        PipelineConfig config) throws NoValueException {
        if (dataPath == null) {
            throw new IllegalArgumentException("dataPath");
        }
        if (sourceData == null) {
            throw new IllegalArgumentException("sourceData");
        }
        if (config == null) {
            throw new IllegalArgumentException("config");
        }

        dataPath = dataPath.toLowerCase();
        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, Object> value : sourceData.entrySet()) {
            Object propertyValue = null;

            if (value.getValue() instanceof AspectPropertyValue){
                AspectPropertyValue<?> aspectProperty =
                    (AspectPropertyValue<?>)value.getValue();
                if (aspectProperty.hasValue()) {
                    propertyValue = aspectProperty.getValue();
                }
                else {
                    values.put(value.getKey().toLowerCase(), null);
                    values.put(value.getKey().toLowerCase() + "nullreason",
                        aspectProperty.getNoValueMessage());
                }
            }
            else {
                propertyValue = value.getValue();
            }

            String completeName = dataPath +
                EVIDENCE_SEPERATOR +
                value.getKey().toLowerCase();

            if (propertyValue != null) {
                // If the value is a list of complex types then
                // recursively call this method for each instance
                // in the list.
                if (propertyValue instanceof List &&
                    ((List<?>) propertyValue).size() > 0 &&
                    ElementData.class.isAssignableFrom(((List<?>) propertyValue).get(0).getClass())) {
                    @SuppressWarnings("unchecked")
                    List<Object> elementDatas = (List<Object>) propertyValue;
                    List<Object> results = new ArrayList<>();
                    for (Object elementData : elementDatas) {
                        results.add(getValues(
                            dataPath + "." + value.getKey().toLowerCase(),
                            ((ElementData) elementData).asKeyMap(),
                            config));
                    }
                    propertyValue = results;
                }

                // Add this value to the output
                values.put(value.getKey().toLowerCase(), propertyValue);

                // Add 'delayexecution' flag if needed.
                if (config.delayedExecutionProperties.contains(completeName)) {
                    values.put(value.getKey().toLowerCase() + "delayexecution", true);
                }
            }
            // Add evidence properties list if needed.
            // (i.e. if the evidence property has delay execution = true)
            if (config.delayedEvidenceProperties.containsKey(completeName)) {
                List<String> evidenceProperties = config.delayedEvidenceProperties
                    .get(completeName);
                values.put(value.getKey().toLowerCase() + "evidenceproperties", evidenceProperties);
            }
        }
        return values;
    }

    private void addJavaScriptProperties(
        FlowData data,
        Map<String, Object> allProperties) {
        List<String> javascriptProperties =
            getJavaScriptProperties(data, allProperties);
        if (javascriptProperties.size() > 0) {
            allProperties.put("javascriptProperties", javascriptProperties);
        }
    }

    private List<String> getJavaScriptProperties(
        FlowData data,
        Map<String, Object> allProperties) {
        // Create a list of the available properties in the form of 
        // "elementdatakey.property" from a 
        // Dictionary<string, Dictionary<string, object>> of properties
        // structured as <element<prefix,prop>>  
        List<String> props = new ArrayList<>();

        for (Map.Entry<String, Object> element : allProperties.entrySet()) {
            Object entryObject = element.getValue();
            if (entryObject instanceof Map) {
                Map<?, ?> entry = (Map<?, ?>)entryObject;
                for (Object propertyObject : entry.entrySet()) {
                    Map.Entry<?,?> property = (Entry<?, ?>)propertyObject;
                    props.add(
                        element.getKey().toLowerCase() + EVIDENCE_SEPERATOR + property.getKey().toString().toLowerCase());
                }
            }
        }
        return getJavaScriptProperties(data, props);
    }

    private boolean containsIgnoreCase(List<String> list, String key) {
        for (String item : list) {
            if (item.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }
    
    private List<String> getJavaScriptProperties(
        FlowData data,
        List<String> props) {
        // Get a list of all the JavaScript properties which are available.
        Map<String, String> javascriptPropertiesMap =
            data.getWhere(new JsPropertyMatcher());
        
        // Copy the keys to an array, otherwise we are removing from the same
        // Map we are iterating over, which causes a concurrent modification
        // exception.
        String[] keys = new String[0];
        keys = javascriptPropertiesMap.keySet().toArray(keys);
        for(String key : keys) {
            if(containsIgnoreCase(props, key) == false) {
                javascriptPropertiesMap.remove(key.toLowerCase());
            }
        }

        List<String> javascriptPropertyNames = new ArrayList<>();
        for (String name : javascriptPropertiesMap.keySet()) {
            javascriptPropertyNames.add(name.toLowerCase());
        }
        return javascriptPropertyNames;
    }

    private void addErrors(FlowData data, Map<String, Object> allProperties) {
        // If there are any errors then add them to the Json.
        if (data.getErrors() != null && data.getErrors().size() > 0) {
            Map<String, List<String>> errors = new HashMap<>();
            for (FlowError error : data.getErrors())
            {
                if (errors.containsKey(error.getFlowElement().getElementDataKey())) {
                    errors.get(error.getFlowElement().getElementDataKey()).add(error.getThrowable().getMessage());
                } else {
                    errors.put(error.getFlowElement().getElementDataKey(),
                        Collections.singletonList(
                            error.getThrowable().getMessage()));
                }
            }
            allProperties.put("errors", errors);
        }
    }

    static String resolveName(String name) {
        return name == JAVASCRIPT_PROPERTIES_NAME ? name : name.toLowerCase();
    }

    private String buildJson(Map<String, Object> allProperties) {
        JSONObject json = new JSONObject();

        for (Map.Entry<String, Object> entry : allProperties.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> map = new HashMap<>();
                @SuppressWarnings("unchecked")
                Map<String, Object> properties = (Map<String, Object>)entry.getValue();

                for (Map.Entry<String, Object> ent : properties.entrySet()) {
                    Object value = ent.getValue();
                    
                    if(value instanceof JavaScript)
                        value = value.toString();

                    map.put(resolveName(ent.getKey()), value);
                }
                json.put(resolveName(entry.getKey()), map);
            } else {

                json.put(resolveName(entry.getKey()), entry.getValue());
            }
        }
        return json.toString(2);
    }


    /**
     * Executed on first request in order to build some collections
     * from the meta-data exposed by the Pipeline.
     */
    private PipelineConfig populateMetaDataCollections(Pipeline pipeline) {
        PipelineConfig config = new PipelineConfig();

        // Populate the collection that contains a list of the
        // properties with 'delay execution' = true.
        for (Map.Entry<String, Map<String, ElementPropertyMetaData>> element :
            pipeline.getElementAvailableProperties().entrySet()) {

            for (String propertyName : getDelayedPropertyNames(
                element.getKey().toLowerCase(),
                element.getValue().values())) {
                config.delayedExecutionProperties.add(propertyName);
            }
        }

        // Now use that information to populate a list of the
        // evidence property links that we need.
        // This means only those where the evidence property has
        // the delayed execution flag set.
        for (Map.Entry<String, Map<String, ElementPropertyMetaData>> element :
            pipeline.getElementAvailableProperties().entrySet()) {
            for (Map.Entry<String, List<String>> property : getEvidencePropertyNames(
                config.delayedExecutionProperties,
                element.getKey().toLowerCase(),
                element.getKey().toLowerCase(),
                element.getValue().values()).entrySet()) {
                config.delayedEvidenceProperties.put(
                    property.getKey(),
                    property.getValue());
            }
        }

        return config;
    }

    /**
     * Get the complete names of any properties that have the
     * delay execution flag set.
     */
    private List<String> getDelayedPropertyNames (
        String dataPath,
        Collection<ElementPropertyMetaData> properties) {
        List<String> result = new ArrayList<>();
        // Return the names of any delayed execution properties.
        for (ElementPropertyMetaData property : properties) {
            if (property.getDelayExecution() == true &&
                property.getType().equals(JavaScript.class)) {
                result.add(dataPath +
                    EVIDENCE_SEPERATOR +
                    property.getName().toLowerCase());
            }

            // Call recursively for any properties that have sub-properties.
            if (property.getItemProperties() != null &&
                property.getItemProperties().size() > 0) {
                for (String propertyName : getDelayedPropertyNames(dataPath +
                    EVIDENCE_SEPERATOR +
                    property.getName(),
                    property.getItemProperties())) {
                    result.add(propertyName);
                }
            }
        }
        return result;
    }

    private Map<String, List<String>> getEvidencePropertyNames(
        HashSet<String> delayedExecutionProperties,
        String elementDataKey,
        String propertyDataPath,
        Collection<ElementPropertyMetaData> properties) {
        Map<String, List<String>> result = new HashMap<>();
        for (ElementPropertyMetaData property : properties) {
            // Build a list of any evidence properties for this property
            // where the evidence property has the delayed execution
            // flag set.
            List<String> evidenceProperties = new ArrayList<>();
            if (property.getEvidenceProperties() != null) {
                for (String evidenceProperty : property.getEvidenceProperties()) {
                    String evidenceName = elementDataKey +
                        EVIDENCE_SEPERATOR +
                        evidenceProperty.toLowerCase();
                    if (delayedExecutionProperties.contains(evidenceName)) {
                        evidenceProperties.add(evidenceName);
                    }
                }
            }
            // Only return an entry for this property if it has one or
            // more evidence properties.
            if (evidenceProperties.size() > 0) {
                result.put(propertyDataPath + EVIDENCE_SEPERATOR +
                    property.getName().toLowerCase(),
                    evidenceProperties);
            }

            // Call recursively for any properties that have sub-properties.
            if (property.getItemProperties() != null &&
                property.getItemProperties().size() > 0) {
                result.putAll(getEvidencePropertyNames(
                    delayedExecutionProperties,
                    elementDataKey,
                    propertyDataPath + EVIDENCE_SEPERATOR +
                        property.getName(),
                    property.getItemProperties()));
                }
            }
            return result;
        }
    }

class JsPropertyMatcher implements PropertyMatcher {

    @Override
    public boolean isMatch(ElementPropertyMetaData property) {
        return property.getType() == JavaScript.class;
    }

}
//! [class]

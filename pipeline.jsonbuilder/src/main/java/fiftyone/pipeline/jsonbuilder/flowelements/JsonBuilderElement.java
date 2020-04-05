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
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.NoValueException;
import fiftyone.pipeline.jsonbuilder.Constants;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;

//! [class]
//! [constructor]
public class JsonBuilderElement extends FlowElementBase<JsonBuilderData, ElementPropertyMetaData> implements JsonBuilder {

    /**
     * Default constructor.
     * @param logger The logger.
     * @param elementDataFactory The element data factory.
     */
    public JsonBuilderElement(
            Logger logger,
            ElementDataFactory<JsonBuilderData> elementDataFactory) {
        super(logger, elementDataFactory);
    }
//! [constructor]
    @Override
    protected void processInternal(FlowData data) throws Exception {
        JsonBuilderDataInternal elementData = (JsonBuilderDataInternal)data.getOrAdd(getElementDataKey(), getDataFactory());
        
        String jsonString = BuildJson(data);
        
        elementData.setJson(jsonString);
    }

    @Override
    public String getElementDataKey() {
        return "json-builder";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return new EvidenceKeyFilterWhitelist(new ArrayList<String>(){},
            String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return Arrays.asList(
            (ElementPropertyMetaData)new ElementPropertyMetaDataDefault(
                "json",
                this,
                "json",
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

    private String BuildJson(FlowData data) throws Exception {
        
        Integer sequenceNumber;
        try{
          sequenceNumber = GetSequenceNumber(data);
        } catch (Exception e){
            throw new PipelineConfigurationException("Make sure there is a "
                    + "SequenceElement placed before this JsonBuilderElement "
                    + "in the pipeline", e);
        }
        
        Map<String, Object> allProperties = GetAllProperties(data);
        
        // Only populate the javascript properties if the sequence 
        // has not reached max iterations.
        if (sequenceNumber < Constants.MAX_JAVASCRIPT_ITERATIONS)
        {
            AddJavaScriptProperties(data, allProperties);
        }
        
        AddErrors(data, allProperties);
        
        return BuildJson(allProperties);
    }
    
    private int GetSequenceNumber(FlowData data) throws Exception {
        TryGetResult<Integer> sequence = data.tryGetEvidence("query.sequence", Integer.class);
        if(sequence.hasValue() == false)
        {
            throw new Exception("Sequence number not present in evidence. " +
                "this is mandatory.");
        }
        return sequence.getValue();
    }
    
    private Map<String, Object> GetAllProperties(FlowData data) throws NoValueException {
        Map<String, Object> allProperties = new HashMap<>();

        for (Map.Entry<String, Object> element : data.elementDataAsMap().entrySet())
        {
            if (allProperties.containsKey(element.getKey()) == false)
            {
                Map<String, Object> elementProperties = new HashMap<>();
                ElementData datum = (ElementData)(element.getValue());
                for (Map.Entry<String, Object> elementProperty : datum.asKeyMap().entrySet())
                {
                    Object value = elementProperty.getValue();
                    Object nullReason = "Unknown";

                    if(elementProperty.getValue() instanceof AspectPropertyValue)
                    {
                        AspectPropertyValue apv = (AspectPropertyValue)elementProperty.getValue();
                        if(apv.hasValue())
                        {
                            value = apv.getValue();
                        }
                        else 
                        {
                            value = null;
                            nullReason = apv.getNoValueMessage();
                        }
                    }
                                        
                    elementProperties.put(elementProperty.getKey(), value);
                    if(value == null) 
                    {
                        elementProperties.put(elementProperty.getKey() + "nullreason", nullReason);
                    }
                }
                allProperties.put(element.getKey(), elementProperties);
            }
        }

        return allProperties;
    }

    private void AddJavaScriptProperties(FlowData data, Map<String, Object> allProperties) {
        List<String> javascriptProperties = GetJavaScriptProperties(data, allProperties);
        if (javascriptProperties != null &&
            javascriptProperties.size() > 0)
        {
            allProperties.put("javascriptProperties", javascriptProperties);
        }
    }

    private List<String> GetJavaScriptProperties(FlowData data, Map<String, Object> allProperties) {
        // Create a list of the available properties in the form of 
        // "elementdatakey.property" from a 
        // Dictionary<string, Dictionary<string, object>> of properties
        // structured as <element<prefix,prop>>  
        List<String> props = new ArrayList<>();

        for(Map.Entry<String, Object> element : allProperties.entrySet())
            for (Map.Entry<String, Object> property : ((Map<String, Object>)element.getValue()).entrySet())
                props.add(element.getKey() + fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR + property.getKey());

        return GetJavaScriptProperties(data, props);
    }

    private List<String> GetJavaScriptProperties(FlowData data, List<String> props) {
        // Get a list of all the JavaScript properties which are available.
        Map<String, String> javascriptPropertiesMap =
            data.getWhere(new JsPropertyMatcher());
                
        for(Map.Entry<String, String> entry : javascriptPropertiesMap.entrySet()){
            if(props.contains(entry.getKey()) == false)
            {
                javascriptPropertiesMap.remove(entry.getKey());
            }
        }

        return new ArrayList<>(javascriptPropertiesMap.keySet());
    }

    private void AddErrors(FlowData data, Map<String, Object> allProperties) {
        // If there are any errors then add them to the Json.
        if (data.getErrors() != null && data.getErrors().size() > 0) {
            Map<String, List<String>> errors = new HashMap<>();
            for (FlowError error : data.getErrors())
            {
                if (errors.containsKey(error.getFlowElement().getElementDataKey())) {
                    errors.get(error.getFlowElement().getElementDataKey()).add(error.getThrowable().getMessage());
                } else {
                    errors.put(error.getFlowElement().getElementDataKey(),
                            Arrays.asList(error.getThrowable().getMessage()));
                }
            }
            allProperties.put("errors", errors);
        }
    }

    private String BuildJson(Map<String, Object> allProperties) {
        JSONObject json = new JSONObject();
        
        for(Map.Entry<String, Object> entry : allProperties.entrySet()){
            if(entry.getValue() instanceof Map){
                Map<String, Object> map = new HashMap<>();
                Map<String, Object> properties = (Map)entry.getValue();
                
                for(Map.Entry<String, Object> ent : properties.entrySet())
                {
                    Object value = ent.getValue();
                    
                    if(value instanceof JavaScript)
                        value = ((JavaScript)value).toString();
                    
                    map.put(ent.getKey(), value);
                }
                json.put(entry.getKey(), map);
            } else {
            
                json.put(entry.getKey(), entry.getValue());
            }
        }
        return json.toString(2);
    }
}

class JsPropertyMatcher implements PropertyMatcher{

    @Override
    public boolean isMatch(ElementPropertyMetaData property) {
        boolean match = property.getType() == JavaScript.class;
        return match;
    }

}
//! [class]

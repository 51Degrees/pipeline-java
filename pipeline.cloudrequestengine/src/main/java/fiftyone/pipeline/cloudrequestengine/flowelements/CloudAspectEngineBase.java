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

import static fiftyone.pipeline.cloudrequestengine.Constants.Messages.ExceptionFailedToLoadProperties;
import static fiftyone.pipeline.cloudrequestengine.Constants.Messages.ProcessCloudEngineNotImplemented;

import fiftyone.pipeline.cloudrequestengine.NotImplementedException;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.engines.data.*;
import fiftyone.pipeline.engines.flowelements.AspectEngineBase;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngine;
import fiftyone.pipeline.util.Types;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for 51Degrees Cloud Aspect Engines
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/cloud-aspect-engine.md">Specification</a>
 */
public abstract class CloudAspectEngineBase<TData extends AspectData>
    extends AspectEngineBase<TData, AspectPropertyMetaData>
    implements CloudAspectEngine<TData, AspectPropertyMetaData>
{
    /**
     * Internal class that is used to retrieve the CloudRequestEngine
     * that will be making requests of behalf of this engine. 
     */
    protected class RequestEngineAccessor {
        private final List<Pipeline> pipelines;
        private volatile CloudRequestEngine cloudRequestEngine;
        private FlowElement<?, ?> currentElement;

        public RequestEngineAccessor(List<Pipeline> pipelines, FlowElement<?, ?> currentElement) {
            this.pipelines = pipelines;
            this.currentElement = currentElement;
        }

        /**
         * Get the CloudRequestEngine that will be making requests on
         * behalf of this engine.
         * @return the CloudRequestEngine
         * @throws PipelineConfigurationException Thrown if the 
         * CloudRequestEngine could not be determined for some reason.
         */
        public CloudRequestEngine getInstance() throws PipelineConfigurationException {
            if(cloudRequestEngine == null) {
                synchronized(this) {
                    if(cloudRequestEngine == null){
                        if(pipelines.size() > 1) {
                            throw new PipelineConfigurationException("'" + currentElement.getClass().getName() +
                                    "' does not support being added to multiple pipelines");
                        } else if (pipelines.size() == 0) {
                            throw new PipelineConfigurationException("'" + currentElement.getClass().getName() +
                                    "' has not yet been added to a Pipeline.");
                        }

                        cloudRequestEngine = pipelines.get(0).getElement(CloudRequestEngine.class);

                        if (cloudRequestEngine == null) {
                            throw new PipelineConfigurationException("'" + currentElement.getClass().getName() +
                                    "' requires a 'CloudRequestEngine' before it in the Pipeline." +
                                    "This engine will be unable to produce results until this" +
                                    "is corrected.");
                        }
                    }
                }
            }
            return cloudRequestEngine;
        }
    }

    private volatile List<AspectPropertyMetaData> aspectProperties;
    private String dataSourceTier;
    private RequestEngineAccessor requestEngine;

    /**
     * Construct a new instance of the {@link CloudAspectEngineBase}.
     * @param logger logger instance to use for logging
     * @param aspectDataFactory the factory to use when creating a TData
     *                          instance
     */
    public CloudAspectEngineBase(
        Logger logger,
        ElementDataFactory<TData> aspectDataFactory) {
        super(logger, aspectDataFactory);
        this.setRequestEngine(new RequestEngineAccessor(this.getPipelines(), this));
    }

    @Override
    public String getDataSourceTier() {
        return dataSourceTier;
    }
    /**
     * Used to access the CloudRequestEngine that will be making HTTP
     * requests on behalf of this engine. 
     * @return A RequestEngineAccessor.
     */
    protected RequestEngineAccessor getRequestEngine() {
        return requestEngine;
    }

    protected void setRequestEngine(RequestEngineAccessor requestEngine) {
        this.requestEngine = requestEngine;
    }

    @Override
    public List<AspectPropertyMetaData> getProperties() {
        List<AspectPropertyMetaData> localRef = aspectProperties;
        if(localRef == null) {
            synchronized (this) {
                localRef = aspectProperties;
                if (localRef == null) {
                    if(loadAspectProperties() == false) {
                        throw new RuntimeException(
                            String.format(
                                ExceptionFailedToLoadProperties,
                                this.getElementDataKey(),
                                this.getElementDataKey())
                        );
                    }
                }
            }
        }
        return aspectProperties;
    }

    @Override
    public TypedKey<TData> getTypedDataKey() {
        if (typedKey == null) {
            typedKey = new TypedKeyDefault<>(
                getElementDataKey(),
                Types.findSubClassParameterType(this, CloudAspectEngineBase.class, 0));
        }
        return typedKey;
    }

    /**
     * Get property meta data from the CloudRequestEngine
     * for properties relating to this engine instance.
     * This method will populate the aspectProperties field.
     * 
     * There will be one CloudRequestEngine in a
     * Pipeline that makes the actual web requests to the cloud service.
     * One or more cloud aspect engines will take the response from these
     * cloud requests and convert them into strongly typed objects.
     * Given this model, the cloud aspect engines have no knowledge
     * of which properties the CloudRequestEngine can
     * return.
     * This method enables the cloud aspect engine to extract the 
     * properties relevant to them from the meta-data for all properties 
     * that the CloudRequestEngine exposes.
     * @return true if the aspectProperties has been successfully populated
     * with the relevant property meta-data. False if something has gone wrong
     */
    private boolean loadAspectProperties() {
        CloudRequestEngine requestEngine = getRequestEngine().getInstance();
        Map<String, AccessiblePropertyMetaData.ProductMetaData> map = 
            requestEngine.getPublicProperties();

        if(map != null &&
            map.size() > 0 &&
            map.containsKey(this.getElementDataKey())) {
                List<AspectPropertyMetaData> properties = new ArrayList<>();
                dataSourceTier = map.get(this.getElementDataKey()).dataTier;

                for (AccessiblePropertyMetaData.PropertyMetaData item :
                    map.get(this.getElementDataKey()).properties) {
                    AspectPropertyMetaData property = new AspectPropertyMetaDataDefault(
                        item.name,
                        this,
                        item.category,
                        item.getPropertyType(),
                        new ArrayList<String>(),
                        true,
                        loadElementProperties(item.itemProperties),
                        item.delayExecution != null ? item.delayExecution : false,
                        item.evidenceProperties != null ? item.evidenceProperties : new ArrayList<String>());
                    properties.add(property);
                }
                aspectProperties = properties;
                return true;
        } else {
            logger.error("Aspect properties could not be loaded for " + 
                this.getClass().getName(), this);
            return false;
        }
    }

    private List<ElementPropertyMetaData> loadElementProperties(
        List<AccessiblePropertyMetaData.PropertyMetaData> itemProperties) {
        List<ElementPropertyMetaData> result = null;
        if (itemProperties != null) {
            result = new ArrayList<>();
            for (AccessiblePropertyMetaData.PropertyMetaData item : itemProperties) {
                result.add(new ElementPropertyMetaDataDefault(
                    item.name,
                    this,
                    item.category,
                    item.getPropertyType(),
                    true,
                    loadElementProperties(item.itemProperties)));
            }
        }
        return result;
    }

    private Map<String, ElementPropertyMetaData> buildMetaDataMap(
        List<ElementPropertyMetaData> properties) {
        Map<String, ElementPropertyMetaData> result = new HashMap<>();
        for (ElementPropertyMetaData property : properties) {
            result.put(property.getName().toLowerCase(), property);
        }
        return result;
    }

    /**
     * Use the supplied cloud data to create a map of {@link AspectPropertyValue}
     * instances.
     * A new instance of {@link AspectPropertyValue} will be created for each
     * value and the value from the cloud data assigned to it.
     * If the value is null, then the code will look for a property in the cloud
     * data with the same name suffixed with 'nullreason'. If it exists, then
     * its value will be used to set the no value message in the new
     * {@link AspectPropertyValue}.
     * @param cloudData the cloud data to be processed. Keys are flat property
     *                  names (i.e. no '.' separators)
     * @param propertyMetaData the meta data for the properties in the data.
     *                         This will usually be the list from
     *                         {@link #getProperties()} but will be different if
     *                         dealing with sub-properties
     * @return a map containing the original values converted to
     * {@link AspectPropertyValue} instances. Any entries in the source map
     * where the key ends with 'nullreason' will not appear in the output
     */
    protected Map<String, Object> createAPVMap(
        Map<String, Object> cloudData,
        List<ElementPropertyMetaData> propertyMetaData) {
        // Convert the meta-data to a map for faster access.
        Map<String, ElementPropertyMetaData> metaDataMap =
            buildMetaDataMap(propertyMetaData);

        Map<String, Object> result = new HashMap<>();
        // Iterate through all entries in the source data where the
        // key is not suffixed with 'nullreason'.
        for (Map.Entry<String, Object> property : cloudData.entrySet()) {
            if (property.getKey().endsWith("nullreason") == false) {
                Object outputValue = property.getValue();
                if (metaDataMap.containsKey(property.getKey().toLowerCase())) {
                    ElementPropertyMetaData metaData =
                        metaDataMap.get(property.getKey().toLowerCase());

                    // If this property has a type of AspectPropertyValue
                    // then create a new instance and populate it.
                    AspectPropertyValue<Object> apv = new AspectPropertyValueDefault<>();
                    if (property.getValue() != null) {
                        Object newValue = property.getValue();
                        if (metaData.getType().equals(JavaScript.class)) {
                            newValue = new JavaScript(newValue.toString());
                        }
                        //noinspection unchecked
                        apv.setValue(newValue);
                    }
                    else {
                        // Value is null so check if we have a
                        // corresponding reason.
                        if (cloudData.containsKey(
                            property.getKey() + "nullreason")) {
                            apv.setNoValueMessage(cloudData.get(
                                property.getKey() + "nullreason").toString());
                        }
                        else {
                            apv.setNoValueMessage("Unknown");
                        }
                    }
                    outputValue = apv;
                }
                else {
                    logger.warn("No meta-data entry for property '" +
                        property.getKey() + "' in '" +
                        getClass().getSimpleName() + "'");
                }
                result.put(property.getKey(), outputValue);
            }
        }
        return result;
    }

    /**
     * Retrieve the raw JSON response from the
     * {@link CloudRequestEngine} in this pipeline, extract
     * the data for this specific engine and populate the TData instance
     * accordingly.
     * @param data to get the raw JSON data from.
     * @param aspectData instance to populate with values.
     */
	protected void processEngine(FlowData data, TData aspectData) {

        CloudRequestData requestData;
        
        // Get requestData from CloudRequestEngine. If requestData does not
        // exist in the element data TypedKeyMap then the CloudRequestEngine either 
        // does not exist in the Pipeline or is not run before this engine.        
        try {
            requestData = data.getFromElement(
                    getRequestEngine().getInstance());        	
        } 
        catch(Exception ex) {
            throw new PipelineConfigurationException(
                    "The " + this.getClass().getSimpleName() + " requires a 'CloudRequestEngine'"  +
                    "before it in the Pipeline. This engine will be unable " +
                    "to produce results until this is corrected", ex);
        }

        // Check the requestData ProcessStarted flag which informs whether
        // the cloud request engine process method was called.
        if (requestData.getProcessStarted() == false)
        {
            throw new PipelineConfigurationException(
                "The " + this.getClass().getSimpleName() + " requires a 'CloudRequestEngine' " +
                "before it in the Pipeline. This engine will be unable " +
                "to produce results until this is corrected.");
        }
        
        String json = requestData == null ? null : requestData.getJsonResponse();
        
        // If the JSON is empty or null then do not Process the CloudAspectEngine.
        // Empty or null JSON indicates that an error has occurred in the 
        // CloudRequestEngine. The error will have been reported by the 
        // CloudRequestEngine so just log a warning that this 
        // CloudAspectEngine did not process.
        if (json != null && !json.isEmpty()) {
        	 processCloudEngine(data, aspectData, json);
        } else {
            logger.warn("The  " + this.getClass().getSimpleName() + "  did not process " +
                    "as the JSON response from the CloudRequestEngine was null " +
                    "or empty. Please refer to errors generated by the " +
                    "CloudRequestEngine in the logs as this indicates an error " +
                    "occurred there.");
       }
	}
	
    /**
     * A virtual method to be implemented by the derived class which
     * uses the JsonResponse from the CloudRequestEngine to populate the TData
     * instance accordingly.
     * @param data to get the raw JSON data from.
     * @param aspectData instance to populate with values.
     * @param json The JSON response from the {@link CloudRequestEngine}
     */
	protected void processCloudEngine(FlowData data, TData aspectData, String json) {
		throw new NotImplementedException(ProcessCloudEngineNotImplemented);
		
	}	
}

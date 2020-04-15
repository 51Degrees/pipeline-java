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

import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngineBase;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngine;
import fiftyone.pipeline.util.Types;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base class for 51Degrees cloud aspect engines.
 * Contains functionality for getting property meta-data and exposing
 * that information to callers.
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
        private List<Pipeline> pipelines;
        private volatile CloudRequestEngine cloudRequestEngine;

        public RequestEngineAccessor(List<Pipeline> pipelines) {
            this.pipelines = pipelines;
        }

        /**
         * Get the CloudRequestEngine that will be making requests on
         * behalf of this engine.
         * @return the CloudRequestEngine
         * @throws PipelineConfigurationException Thrown if the 
         * CloudRequestEngine could not be determined for some reason.
         */
        public CloudRequestEngine getInstance() throws PipelineConfigurationException {
            CloudRequestEngine localRef = cloudRequestEngine;
            if(localRef == null) {
                synchronized(this) {
                    localRef = cloudRequestEngine;
                    if(localRef == null){
                        if(pipelines.size() > 1) {
                            throw new PipelineConfigurationException("'" + this.getClass().getName() +
                                    "' does not support being added to multiple pipelines");
                        } else if (pipelines.size() == 0) {
                            throw new PipelineConfigurationException("'" + this.getClass().getName() +
                                    "' has not yet been added to a Pipeline.");
                        }

                        cloudRequestEngine = localRef = pipelines.get(0).getElement(CloudRequestEngine.class);

                        if(cloudRequestEngine == null){
                            throw new PipelineConfigurationException("'" + this.getClass().getName() +
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

    public String getDataSourceTier() {
        return dataSourceTier;
    }

    private RequestEngineAccessor requestEngine;
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

    /**
     * Get property meta-data for properties populated by this engine.
     */
    public List<AspectPropertyMetaData> getProperties() {
        List<AspectPropertyMetaData> localRef = aspectProperties;
        if(localRef == null) {
            synchronized (this) {
                localRef = aspectProperties;
                if (localRef == null) {
                    if(loadAspectProperties() == false) {
                        aspectProperties = null;
                    }
                }
            }
        }
        return aspectProperties;
    }

    public TypedKey<TData> getTypedDataKey() {
        if (typedKey == null) {
            typedKey = new TypedKeyDefault<>(getElementDataKey(), Types.findSubClassParameterType(this, CloudAspectEngineBase.class, 0));
        }
        return typedKey;
    }
    
    public CloudAspectEngineBase(Logger logger, ElementDataFactory<TData> aspectDataFactory) {
        super(logger, aspectDataFactory);
        this.setRequestEngine(new RequestEngineAccessor(this.getPipelines()));
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
     * @return True if the aspectProperties has been successfully populated
     * with the relevant property meta-data.
     * False if something has gone wrong.
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
                        loadElementProperties(item.itemProperties));
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
}

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineDataException;

/**
 * Internal interface for a pipeline.
 * Allows {@link FlowData} to call the pipeline's process method.
 */
public interface PipelineInternal extends Pipeline {

    /**
     * Process the given {@link FlowData} using the {@link FlowElement}s in the
     * pipeline.
     * @param data the {@link FlowData} that contains the evidence and will
     *             allow the user to access the results
     */
    void process(FlowData data);

    /**
     * Get the meta data for the specified property name.
     * If there are no properties with that name or multiple
     * properties on different elements then an exception will
     * be thrown.
     * @param propertyName the property name to find the meta data for
     * @return the meta data associated with the specified property name
     * @throws PipelineDataException if the property name is associated with
     * zero or multiple elements.
     */
    ElementPropertyMetaData getMetaDataForProperty(String propertyName) throws PipelineDataException;
}
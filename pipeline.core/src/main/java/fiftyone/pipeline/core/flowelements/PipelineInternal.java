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

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineDataException;

/**
 * Internal interface for a pipeline.
 * Allows {@link FlowData} to call the pipeline's process method.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#pipeline">Specification</a>
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
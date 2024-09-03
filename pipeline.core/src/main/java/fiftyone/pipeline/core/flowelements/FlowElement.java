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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.typed.TypedKey;

import java.util.List;

/**
 * A component that processes a {@link FlowData} (by examining its evidence, or
 * by examining data processed by other FlowElements). It MAY contribute to the
 * {@link FlowData} by adding its own data.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#flow-element">Specification</a>
 * 
 * @param <TData> the type of element data that the flow element will write to
 * @param <TProperty> the type of meta data that the flow element will supply
 *                    about the properties it populates
 */
public interface FlowElement<
    TData extends ElementData,
    TProperty extends ElementPropertyMetaData>
    extends AutoCloseable {
    /**
     * Carry out whatever operations this element is designed to do using the
     * {@link FlowData} passed.
     * @param data the {@link FlowData} instance that provides input evidence
     *             and carries the output data to the user.
     * @throws Exception if there was an error while processing. Note that the
     * internal errors will only result in an exception if the {@link Pipeline}
     * has been configured not to suppress exceptions using the
     * {@link PipelineBuilder#setSuppressProcessException(boolean)} option
     */
    void process(FlowData data) throws Exception;

    /**
     * Called when this element is added to a pipeline.
     * @param pipeline the pipeline that the element has been added to
     */
    void addPipeline(Pipeline pipeline);

    /**
     * Get a filter that will only include the evidence keys that this element
     * can make use of.
     * @return this element's {@link EvidenceKeyFilter}
     */
    EvidenceKeyFilter getEvidenceKeyFilter();

    /**
     * Get the string name of the key used to access the data populated by this
     * element in the {@link FlowData}.
     * @return this element's data key string
     */
    String getElementDataKey();

    /**
     * Get the typed data key used for retrieving strongly typed element data.
     * @return this element's typed data key
     */
    TypedKey<TData> getTypedDataKey();

    /**
     * Get details of the properties that this element can populate.
     * @return this element's properties as a list
     */
    List<TProperty> getProperties();

    /**
     * Get a property from the properties that this element can populate using
     * its name.
     * @param name the name of the property
     * @return the property or null if it is not found
     */
    TProperty getProperty(String name);

    /**
     * if true, requires that the Pipeline guards against concurrent access to
     * {@link FlowData} structures
     * @return true if the element starts multiple threads. False otherwise
     */
    boolean isConcurrent();

    /**
     * Indicates whether the element has been closed using the {@link #close()}
     * method, either explicitly or as a result of a 'try with resource'.
     * @return true if the element has been closed
     */
    boolean isClosed();

    /**
     * Get the factory used to create the element data instances that are
     * populated by this flow element.
     * @return the factory for TData
     */
    DataFactory<TData> getDataFactory();

    /**
     * Data factory interface which needs an implementation specific to a
     * {@link FlowElement} implementation in order to construct element data
     * when calling the {@link FlowData#getOrAdd(String, FlowElement.DataFactory)}
     * method.
     * @param <T> the type of data built by the factory
     */
    interface DataFactory<T extends ElementData> {
        /**
         * Create a new instance of class T.
         * @param flowData the {@link FlowData} to link the new instance to
         * @return a new data instance
         */
        T create(FlowData flowData);
    }
}

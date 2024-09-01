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

package fiftyone.pipeline.core.data;

import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A closeable structure to contain the inputs and outputs of {@link Pipeline}
 * processing.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#flow-data">Specification</a>
 */
public interface FlowData extends AutoCloseable {

    /**
     * Process this FlowData via the owning Pipeline. Once this method returns
     * the FlowData will not be changed further and cannot be processed again.
     */
    void process();

    /**
     * If this flag is set to true, future FlowElements will not process this
     * FlowData.
     * @return should stop processing
     */
    boolean isStopped();

    /**
     * Set the stop flag.
     * @see #isStopped
     */
    void stop();

    /**
     * Get the {@link Pipeline} which created this FlowData instance.
     * @return creating {@link Pipeline}
     */
    Pipeline getPipeline();

    /**
     * Obtain the collection of errors which occurred so far. This cannot be
     * added to externally.
     * @return a collection of FlowErrors
     */
    Collection<FlowError> getErrors();

    /**
     * Add an error which has occurred during processing.
     * @param error which was thrown
     */
    void addError(FlowError error);

    /**
     * Add an error which has occurred during processing.
     * @param e error which was thrown
     * @param element the {@link FlowElement} which threw the error
     */
    @SuppressWarnings("rawtypes")
    void addError(Throwable e, FlowElement element);

    /**
     * Get an {@link Evidence} collection. Evidence should only be added via the
     * {@link #addEvidence(Map)}  and {@link #addEvidence(String, Object)}
     * methods.
     * @return evidence collection
     */
    Evidence getEvidence();

    /**
     * Try to get the data value from evidence.
     * @param key evidence key to get the value of
     * @param type the type of value to get
     * @param <T> the type of value to get
     * @return a 'true' {@link TryGetResult} if a value for a given key is found
     * and a 'false' {@link TryGetResult} if the key is not found or if the
     * method cannot cast the value to the requested type.
     */
    <T> TryGetResult<T> tryGetEvidence(String key, Class<T> type);

    /**
     * Get a list of keys for elements of the Pipeline that have created data.
     * (so far).
     * @return keys added to data
     */
    List<String> getDataKeys();

    /**
     * Add a single item of evidence.
     * @param key name identifying piece of evidence
     * @param value the piece of evidence
     * @return this {@link FlowData} instance
     */
    FlowData addEvidence(String key, Object value);

    /**
     * Add a map of evidence items.
     * @param values to add
     * @return this {@link FlowData} instance
     */
    FlowData addEvidence(Map<String, ?> values);

    /**
     * Get the {@link ElementData} for the requested aspect.
     * @param key the aspect to get
     * @param <T> type type of the aspect
     * @return aspect's {@link ElementData} or null if not present
     */
    <T extends ElementData> T get(TypedKey<T> key);

    /**
     * Get the {@link ElementData} for the requested data key.
     * @param key to the aspect to get
     * @return aspect's {@link ElementData} or null if not present
     */
    ElementData get(String key);

    /**
     * Get the {@link ElementData} for the requested {@link ElementData}
     * implementation. If multiple instances of the type exist then an exception
     * is thrown.
     * @param type the type of {@link ElementData} to return
     * @param <T> the type of {@link ElementData} to return
     * @return aspect's {@link ElementData} or null if the type is not present
     */
    <T extends ElementData> T get(Class<T> type);

    /**
     * Get the ElementData for the requested {@link FlowElement}. This method
     * uses the data type returned by the Element, and the key which belongs
     * to the Element.
     *
     * @param element {@link FlowElement} to get the data for
     * @param <T>     {@link ElementData} stored by the element
     * @return element data stored by the element
     */
    <T extends ElementData> T getFromElement(FlowElement<T, ?> element);

    /**
     * Get the specified property as the specified type.
     * @param <T> the type to return the property value as
     * @param key the name of the property to get
     * @param type the type to return the property value as
     * @return the property value
     */
    <T> T getAs(String key, Class<T> type);

    /**
     * Check if the flow data contains an item with the specified key name and
     * type. If it does exist, retrieve it.
     * @param key the key to check for
     * @param <T> the type of value to return
     * @return a 'true' {@link TryGetResult} if an entry matching the key
     * exists, a 'false' {@link TryGetResult} otherwise
     */
    <T extends ElementData> TryGetResult<T> tryGetValue(TypedKey<T> key);

    /**
     * Get the specified property as a string.
     * @param propertyName the name of the property to get
     * @return the property value
     */
    String getAsString(String propertyName);

    /**
     * Get the specified property as a boolean.
     * @param propertyName the name of the property to get
     * @return the property value
     */
    Boolean getAsBool(String propertyName);

    /**
     * Get the specified property as an integer.
     * @param propertyName the name of the property to get
     * @return the property value
     */
    Integer getAsInt(String propertyName);

    /**
     * Get the specified property as a long.
     * @param propertyName the name of the property to get
     * @return the property value
     */
    Long getAsLong(String propertyName);

    /**
     * Get the specified property as a float.
     * @param propertyName the name of the property to get
     * @return the property value
     */
    Float getAsFloat(String propertyName);

    /**
     * Get the specified property as a double.
     * @param propertyName the name of the property to get
     * @return the property value
     */
    Double getAsDouble(String propertyName);

    /**
     * Get or add the specified element data to the internal map.
     * @param <T> the type of the data being stored
     * @param elementDataKey the name of the element to store the data under
     * @param dataFactory the {@link fiftyone.pipeline.core.flowelements.FlowElement.DataFactory}
     *                    to use to create a new data to store if one does not
     *                    already exist
     * @return existing data matching the key, or newly added data
     */
    <T extends ElementData> T getOrAdd(
        String elementDataKey,
        FlowElement.DataFactory<T> dataFactory);

    /**
     * Get or add the specified element data to the internal map.
     * @param <T> the type of the data being stored
     * @param key the typed key of the element to store the data under
     * @param dataFactory the {@link fiftyone.pipeline.core.flowelements.FlowElement.DataFactory}
     *                    to use to create a new data to store if one does not
     *                    already exist
     * @return existing data matching the key, or newly added data
     */
    <T extends ElementData> T getOrAdd(
        TypedKey<T> key,
        FlowElement.DataFactory<T> dataFactory);

    /**
     * Get the {@link ElementData} for this instance as a {@link Map}.
     * @return a {@link Map} containing the {@link ElementData}
     */
    Map<String, Object> elementDataAsMap();

    /**
     * Get the {@link ElementData} for this instance as an {@link Iterable}.
     * @return an {@link Iterable} containing the {@link ElementData}
     */
    Iterable<ElementData> elementDataAsIterable();

    /**
     * Get all element data values that match the specified predicate.
     * @param matcher if a property passed to this matcher returns true then it
     *                will be included in the results
     * @return all the element data values that match the matcher
     */
    Map<String, String> getWhere(PropertyMatcher matcher);

    /**
     * Generate a {@link DataKey} that contains the evidence data from this
     * instance that matches the specified filter.
     * @param filter an {@link EvidenceKeyFilter} instance that defines the
     *              values to include in the generated key
     * @return a new {@link DataKey} instance
     */
    DataKey generateKey(EvidenceKeyFilter filter);

    /**
     * Get a filter that will only include the evidence keys that can be used by
     * the elements within the pipeline that created this flow element.
     * @return {@link EvidenceKeyFilter} for this instance
     */
    EvidenceKeyFilter getEvidenceKeyFilter();
}

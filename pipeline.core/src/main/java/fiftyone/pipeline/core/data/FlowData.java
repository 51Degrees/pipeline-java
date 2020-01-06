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

package fiftyone.pipeline.core.data;

import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A closeable structure to contain the inputs and outputs of Pipeline
 * processing.
 * <p>
 * <p>The Pipeline that creates FlowData must not be disposed of until the
 * FlowData instance is finished with. Calling a FlowData method which then
 * calls its creator will throw a runtime error.
 */
public interface FlowData {

    /**
     * Process this FlowData via the owning Pipeline. Once this method returns
     * the FlowData will not be changed further and cannot be processed again.
     */
    void process();

    /**
     * If this flag is set to true, future FlowElements will not process this
     * FlowData.
     *
     * @return should stop processing
     */
    boolean isStopped();

    /**
     * Set the stop flag.
     *
     * @see #isStopped
     */
    void stop();

    Pipeline getPipeline();

    /**
     * Obtain the collection of errors which occurred so far. This cannot be
     * added to externally.
     *
     * @return a collection of FlowErrors
     */
    Collection<FlowError> getErrors();

    /**
     * Add an error which has occurred during processing.
     *
     * @param error which was thrown
     */
    void addError(FlowError error);

    void addError(Throwable e, FlowElement element);

    /**
     * Get an immutable {@link Evidence} collection. Evidence can only be added
     * via the {@link #addEvidence(Map)}  and {@link #addEvidence(String, Object)}
     * methods.
     *
     * @return evidence collection
     */
    Evidence getEvidence();

    <T> TryGetResult<T> tryGetEvidence(String key, Class<T> type);

    /**
     * Get a list of keys for elements of the Pipeline that have created data.
     * (so far).
     *
     * @return keys added to data
     */
    List<String> getDataKeys();

    /**
     * Add a single item of evidence.
     *
     * @param key   name identifying piece of evidence
     * @param value the piece of evidence
     * @return this {@link FlowData} instance
     */
    FlowData addEvidence(String key, Object value);

    /**
     * Add a map of evidence items.
     *
     * @param values to add
     * @return this {@link FlowData} instance
     */
    FlowData addEvidence(Map<String, ?> values);

    /**
     * Get the ElementData for the requested aspect.
     *
     * @param key the aspect to get
     * @param <T> type type of the aspect
     * @return aspect's {@link ElementData} or null if not present
     */
    <T extends ElementData> T get(TypedKey<T> key);

    ElementData get(String key);

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

    <T extends Object> T getAs(String key, Class<T> type);

    <T extends ElementData> TryGetResult<T> tryGetValue(TypedKey<T> key);

    String getAsString(String propertyName);

    Boolean getAsBool(String propertyName);

    Integer getAsInt(String propertyName);

    Long getAsLong(String propertyName);

    Float getAsFloat(String propertyName);

    Double getAsDouble(String propertyName);

    <T extends ElementData> T getOrAdd(
        String elementDataKey,
        FlowElement.DataFactory<T> dataFactory);

    <T extends ElementData> T getOrAdd(
        TypedKey<T> key,
        FlowElement.DataFactory<T> dataFactory);

    Map<String, Object> elementDataAsMap();

    Iterable<ElementData> elementDataAsIterable();

    Map<String, String> getWhere(PropertyMatcher matcher);

    DataKey generateKey(EvidenceKeyFilter filter);

    EvidenceKeyFilter getEvidenceKeyFilter();
}

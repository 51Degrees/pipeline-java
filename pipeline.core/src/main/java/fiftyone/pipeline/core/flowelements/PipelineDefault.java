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

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterAggregator;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.FlowDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.exceptions.AggregateException;
import org.slf4j.Logger;

import java.util.*;

class PipelineDefault implements PipelineInternal {

    private final Logger logger;

    private final FlowDataFactory flowDataFactory;

    private final boolean autoDisposeElements;

    private final boolean suppressProcessExceptions;

    private final Map<String, Map<String, ElementPropertyMetaData>>
        elementAvailableProperties;

    private volatile Object lock = new Object();

    private volatile boolean isClosed = false;

    private boolean isConcurrent;

    private List<FlowElement> flowElements;

    private EvidenceKeyFilter evidenceKeyFilter = null;

    private Map<Class, List<FlowElement>> elementsByType = new HashMap<>();

    private Map<String, ElementPropertyMetaData> metaDataByPropertyName =
        new HashMap<>();

    PipelineDefault(
        Logger logger,
        List<FlowElement> flowElements,
        FlowDataFactory flowDataFactory,
        boolean autoDisposeElements,
        boolean suppressProcessExceptions) {
        this.logger = logger;
        this.flowDataFactory = flowDataFactory;
        this.flowElements = flowElements;
        this.autoDisposeElements = autoDisposeElements;
        this.suppressProcessExceptions = suppressProcessExceptions;

        addElementsByType(flowElements);

        isConcurrent = false;
        for (FlowElement element : this.flowElements) {
            isConcurrent = isConcurrent || element.isConcurrent();
        }
        this.elementAvailableProperties =
            getElementAvailableProperties(flowElements);

        logger.info("Pipeline '" + hashCode() + "' created.");

    }

    @Override
    public FlowData createFlowData() throws RuntimeException {
        if (isClosed) {
            throw new RuntimeException("Pipeline is closed.");
        }
        return flowDataFactory.create(this);
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        if (evidenceKeyFilter == null) {
            synchronized (lock) {
                if (evidenceKeyFilter == null) {
                    EvidenceKeyFilterAggregator filter =
                        new EvidenceKeyFilterAggregator();
                    for (FlowElement element : flowElements) {
                        filter.addFilter(element.getEvidenceKeyFilter());
                    }
                    evidenceKeyFilter = filter;
                }
            }
        }
        return evidenceKeyFilter;
    }

    @Override
    public boolean isConcurrent() {
        return isConcurrent;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    private Map<String, Map<String, ElementPropertyMetaData>>
    getElementAvailableProperties(List<FlowElement> elements) {
        Map<String, Map<String, ElementPropertyMetaData>> map =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        addAvailableProperties(elements, map);
        Map<String, Map<String, ElementPropertyMetaData>> mapOfReadonly =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, Map<String, ElementPropertyMetaData>> entry : map.entrySet()) {
            mapOfReadonly.put(entry.getKey(),
                Collections.unmodifiableMap(entry.getValue()));
        }
        return Collections.unmodifiableMap(mapOfReadonly);
    }

    private void addAvailableProperties(
        List<FlowElement> elements,
        Map<String, Map<String, ElementPropertyMetaData>> map) {
        for (FlowElement element : elements) {
            if (element instanceof ParallelElements) {
                addAvailableProperties(((ParallelElements) element).getFlowElements(), map);
            } else {
                if (map.containsKey(element.getElementDataKey()) == false) {
                    map.put(element.getElementDataKey(),
                        new TreeMap<String, ElementPropertyMetaData>(
                            String.CASE_INSENSITIVE_ORDER));
                }
                Map<String, ElementPropertyMetaData> availableElementProperties =
                    map.get(element.getElementDataKey());
                for (ElementPropertyMetaData property : (List<ElementPropertyMetaData>) element.getProperties()) {
                    if (property.isAvailable() &&
                        availableElementProperties.containsKey(property.getName()) == false) {
                        availableElementProperties.put(
                            property.getName(),
                            property);
                    }
                }
            }
        }
    }

    private void addElementsByType(List<FlowElement> elements) {
        for (FlowElement element : elements) {
            element.addPipeline(this);
            Class type = element.getClass();
            // If the element is a ParallelElements then add it's child elements.
            if (element instanceof ParallelElements) {
                addElementsByType(((ParallelElements) element).getFlowElements());
            }
            // Otherwise, just add the element directly.
            else {
                List<FlowElement> typeElements;
                if (elementsByType.containsKey(type) == false) {
                    elementsByType.put(type, new ArrayList<FlowElement>());
                }
                elementsByType.get(type).add(element);
            }
        }

    }

    private <T extends FlowElement> boolean anyAssignableFrom(Map<Class, List<FlowElement>> elements, Class<T> type) {
        for (Class element : elements.keySet()) {
            if (type.isAssignableFrom(element)) {
                return true;
            }
        }
        return false;
    }

    private <T extends FlowElement> List<List<FlowElement>> getElementsWhereAssignableFrom(
        Map<Class, List<FlowElement>> elements,
        Class<T> type) {
        List<List<FlowElement>> result = new ArrayList<>();
        for (Map.Entry<Class, List<FlowElement>> element : elements.entrySet()) {
            if (type.isAssignableFrom(element.getKey())) {
                result.add(element.getValue());
            }
        }
        return result;
    }

    public <T extends FlowElement> T getElement(Class<T> type) {
        T result = null;
        if (elementsByType.containsKey(type)) {
            try {
                result = type.cast(elementsByType.get(type).get(0));
            } catch (ClassCastException e) {
                result = null;
            }
        }
        else if (anyAssignableFrom(elementsByType, type)) {
            List<List<FlowElement>> matches = getElementsWhereAssignableFrom(elementsByType, type);
            if (matches.size() == 1 &&
                matches.get(0).size() == 1) {
                result = (T)matches.get(0).get(0);
            }
        }

        return result;
    }

    @Override
    public List<FlowElement> getFlowElements() {
        return Collections.unmodifiableList(flowElements);
    }

    @Override
    public Map<String, Map<String, ElementPropertyMetaData>> getElementAvailableProperties() {
        return Collections.unmodifiableMap(elementAvailableProperties);
    }

    public void close(boolean closing) throws Exception {
        if (isClosed == false) {
            synchronized (lock) {
                if (isClosed == false) {
                    isClosed = true;
                    if (closing && autoDisposeElements) {
                        List<Exception> exceptions = new ArrayList<>();
                        for (FlowElement element : flowElements) {
                            try {
                                element.close();
                            } catch (Exception e) {
                                exceptions.add(e);
                            }
                        }
                        if (exceptions.size() > 0) {
                            throw new Exception(
                                "One or more exceptions occurred while closing " +
                                    "the pipeline",
                                exceptions.get(0));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        logger.info("Pipeline '" + hashCode() + "' closed.");
        close(true);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            logger.warn(
                "Pipeline '" + hashCode() + "' finalised. It is recommended " +
                    "that instance lifetimes are managed explicitly with a 'try' " +
                    "block or calling the close method as part of a 'finally' block.");
            // Do not change this code. Put cleanup code in close(boolean disposing) above.
            close(false);
        } finally {
            super.finalize();
        }
    }

    @Override
    public void process(FlowData data) {
        logger.debug("Pipeline '" + hashCode() + "' started processing.");

        if (isClosed) {

            throw new RuntimeException("Pipeline is closed");
        }
        for (FlowElement element : flowElements) {
            try {
                element.process(data);
                if (data.isStopped()) break;
            } catch (Exception ex) {
                // If an error occurs then store it in the
                // FlowData object.
                data.addError(ex, element);
            }
        }
        // If any errors have occurred and exceptions are not
        // suppressed, then throw an aggregate exception.
        if (data.getErrors() != null &&
            data.getErrors().size() > 0 &&
            suppressProcessExceptions == false) {
            throw new AggregateException(
                "Exception(s) occurred processing evidence.",
                data.getErrors());
        }

        logger.debug("Pipeline '" + hashCode() + "' finished processing.");
    }

    @Override
    public ElementPropertyMetaData getMetaDataForProperty(String propertyName) throws PipelineDataException {
        ElementPropertyMetaData result = null;

        if (metaDataByPropertyName.containsKey(propertyName)) {
            result = metaDataByPropertyName.get(propertyName);
        }
        else {
            List<ElementPropertyMetaData> properties = new ArrayList<>();
            for (FlowElement element : flowElements) {
                for (Object property : element.getProperties()) {
                    if (((ElementPropertyMetaData) property).getName().equalsIgnoreCase(propertyName)) {
                        properties.add((ElementPropertyMetaData) property);
                    }
                }
            }
            if (properties.size() > 1) {
                String message = "Multiple matches for property '" +
                    propertyName + "'. Flow elements that populate this " +
                    "property are: '" +
                    Arrays.toString(properties.toArray()) + "'";
                logger.error(message);
                throw new PipelineDataException(message);
            }
            if (properties.size() == 0) {
                String message = "Could not find property '" + propertyName +
                    "'.";
                logger.error(message);
                throw new PipelineDataException(message);
            }
            result = properties.get(0);
            metaDataByPropertyName.put(propertyName, result);
        }

        return result;
    }
}

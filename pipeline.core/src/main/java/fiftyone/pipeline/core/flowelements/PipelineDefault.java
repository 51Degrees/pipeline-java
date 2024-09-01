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

import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.factories.FlowDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.core.services.PipelineService;
import fiftyone.pipeline.exceptions.AggregateException;
import org.slf4j.Logger;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * A pipeline is used to create {@link FlowData} instances which then
 * use that pipeline when their {@link FlowData#process()} method
 * is called.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#pipeline">Specification</a>
 */
@SuppressWarnings("rawtypes")
class PipelineDefault implements PipelineInternal {

    /**
     * Used for logging.
     */
    private final Logger logger;

    /**
     * Factory used to create {@link FlowData} instances.
     */
    private final FlowDataFactory flowDataFactory;

    /**
     * Control flag that indicates if the Pipeline will automatically call
     * {@link #close()} on child elements when it is closed or not.
     */
    private final boolean autoDisposeElements;

    /**
     * Control flag that indicates if the Pipeline will throw an
     * {@link AggregateException} during processing or suppress it and ignore
     * the exceptions added to {@link FlowData#getErrors()}.
     */
    private final boolean suppressProcessExceptions;

    /**
     * Map of {@link ElementPropertyMetaData}s keyed first on the data key of
     * the element which they belong to, then by the property name.
     */
    private final Map<String, Map<String, ElementPropertyMetaData>>
        elementAvailableProperties;

    /**
     * Lock used for synchronised blocks.
     */
    private final Object lock = new Object();

    /**
     * True if the instance has been closed. This is used as part of the
     * closable pattern.
     */
    private volatile boolean isClosed = false;

    /**
     * True if multiple {@link FlowElement} instances will run concurrently
     * within this pipeline. False otherwise.
     */
    private boolean isConcurrent;

    /**
     * The {@link FlowElement}s that make up this Pipeline.
     */
    private final List<FlowElement> flowElements;
    
    /**
     * The services that are used by this Pipeline.
     */
    private final List<PipelineService> services;

    /**
     * A filter that will only include the evidence keys that can be used by at
     * least one {@link FlowElement} within this pipeline.
     * (Will only be populated after the {@link #getEvidenceKeyFilter()} method
     * is called.)
     */
    private EvidenceKeyFilter evidenceKeyFilter = null;

    /**
     * The pipeline maintains a map of the elements it contains indexed by type.
     * This is used by the {@link #getElement(Class)} method.
     */
    private final Map<Class, List<FlowElement>> elementsByType = new HashMap<>();

    /**
     * The pipeline maintains a map of property meta data indexed by property
     * name. This is used by the {@link #getMetaDataForProperty(String)} method.
     */
    private final ConcurrentMap<String, ElementPropertyMetaData> metaDataByPropertyName =
        new ConcurrentHashMap<>();

    /**
     * Construct a new instance.
     * @param logger used for logging
     * @param flowElements the {@link FlowElement} instances that make up this
     *                     pipeline
     * @param flowDataFactory factory used to create new {@link FlowData}
     *                        instances
     * @param autoDisposeElements if true then pipeline will call
     *                            {@link FlowElement#close()} on its child
     *                            elements when it is closed
     * @param suppressProcessExceptions if true then pipeline will suppress
     *                                  exceptions added to
     *                                  {@link FlowData#getErrors()}
     */
    PipelineDefault(
        Logger logger,
        List<FlowElement> flowElements,
        FlowDataFactory flowDataFactory,
        boolean autoDisposeElements,
        boolean suppressProcessExceptions) {
        this.logger = logger;
        this.flowDataFactory = flowDataFactory;
        this.flowElements = flowElements;
        this.services = new ArrayList<>();
        this.autoDisposeElements = autoDisposeElements;
        this.suppressProcessExceptions = suppressProcessExceptions;

        addElementsByType(flowElements);

        isConcurrent = false;
        for (FlowElement element : this.flowElements) {
            isConcurrent = isConcurrent || element.isConcurrent();
        }
        this.elementAvailableProperties =
            getElementAvailableProperties(flowElements);

        logger.debug("Pipeline '" + hashCode() + "' created.");

    }
    
    @Override
    public void addService(PipelineService service) {
    	this.services.add(service);
    }
    
    @Override
    public boolean addServices(Collection<PipelineService> services) {
    	return this.services.addAll(services);
    }
    
    @Override
	public List<PipelineService> getServices() {
		return Collections.unmodifiableList(services);
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

    /**
     * Construct the map of available properties for the elements in the
     * pipeline.
     * @param elements the elements to get the available properties from
     * @return map of available properties for elements
     */
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

    /**
     * Add all the properties which are marked as available to the map, keyed on
     * the element's data key then the property name.
     * @param elements list of elements to get the available properties for
     * @param map to add the properties to
     */
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
                for (Object propertyObject : element.getProperties()) {
                    ElementPropertyMetaData property =
                        (ElementPropertyMetaData)propertyObject;
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

    /**
     * Add the specified flow elements to the {@link #elementsByType} map, which
     * contains a list of all the elements in the pipeline indexed by type.
     * @param elements the {@link FlowElement}s to add
     */
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
                if (elementsByType.containsKey(type) == false) {
                    elementsByType.put(type, new ArrayList<FlowElement>());
                }
                elementsByType.get(type).add(element);
            }
        }

    }

    /**
     * Return true if any of the {@link FlowElement}s provided, can be assigned
     * to the specified class.
     * @param elements element to check
     * @param type the class to compare the elements to
     * @param <T> the type of the class
     * @return true if any elements are the requested type
     */
    private <T extends FlowElement> boolean anyAssignableFrom(
        Map<Class, List<FlowElement>> elements,
        Class<T> type) {
        for (Class<?> element : elements.keySet()) {
            if (type.isAssignableFrom(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a list of all {@link FlowElement}s which can be assigned the
     * requested type.
     * @param elements map of types to elements
     * @param type the type of elements to get
     * @param <T> the type of elements to get
     * @return list of elements
     */
    private <T extends FlowElement> List<List<FlowElement>>
    getElementsWhereAssignableFrom(
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

    @Override
    @SuppressWarnings("unchecked")
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
            List<List<FlowElement>> matches = getElementsWhereAssignableFrom(
                elementsByType,
                type);
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
    public Map<String, Map<String, ElementPropertyMetaData>>
    getElementAvailableProperties() {
        return Collections.unmodifiableMap(elementAvailableProperties);
    }

    public void close(boolean closing) throws Exception {
        if (isClosed == false) {
            synchronized (lock) {
                if (isClosed == false) {
                    isClosed = true;
                    if (closing && autoDisposeElements) {
                    	// Dispose all elements
                        List<Exception> exceptions = new ArrayList<>();
                        for (FlowElement element : flowElements) {
                            try {
                                element.close();
                            } catch (Exception e) {
                                exceptions.add(e);
                            }
                        }
                        
                        // Dispose the services after all elements have been
                        // disposed.
                        for (PipelineService service : services) {
                        	if (service instanceof Closeable) {
                        		((Closeable)service).close();
                        	}
                        	else if (service instanceof AutoCloseable){
                        		try {
                        			((AutoCloseable)service).close();
                        		}
                        		catch (Exception e) {
                        			exceptions.add(e);
                        		}
                        	}
                        }
                        
                        // Throw an exception if an exception occurred
                        if (exceptions.size() > 0) {
                            throw new Exception(
                                "One or more exceptions occurred while closing the pipeline",
                                exceptions.get(0));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        logger.debug("Pipeline '" + hashCode() + "' closed.");
        close(true);
    }

/*   @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
        if (isClosed) {
            logger.info("Pipeline '{}' finalizing, but already closed", hashCode());
            super.finalize();
            return;
        }
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
*/
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
                data.getErrors()
                .stream()
                .map(FlowError::getThrowable)
                .collect(Collectors.toList()));
        }

        logger.debug("Pipeline '" + hashCode() + "' finished processing.");
    }

    @Override
    public ElementPropertyMetaData getMetaDataForProperty(String propertyName)
        throws PipelineDataException {
        ElementPropertyMetaData result;

        if (metaDataByPropertyName.containsKey(propertyName)) {
            result = metaDataByPropertyName.get(propertyName);
        }
        else {
            List<ElementPropertyMetaData> properties = new ArrayList<>();
            for (FlowElement element : flowElements) {
                for (Object property : element.getProperties()) {
                    if (((ElementPropertyMetaData) property).getName()
                        .equalsIgnoreCase(propertyName)) {
                        properties.add((ElementPropertyMetaData) property);
                    }
                }
            }
            if (properties.size() > 1) {
                String message = "Multiple matches for property '" +
                    propertyName + "'. Flow elements that populate this " +
                    "property are: '" +
                    Arrays.toString(properties.toArray()) + "'";
                logger.debug(message);
                throw new PipelineDataException(message);
            }
            if (properties.size() == 0) {
                String message = "Could not find property '" + propertyName +
                    "'.";
                logger.debug(message);
                throw new PipelineDataException(message);
            }
            result = properties.get(0);
            metaDataByPropertyName.putIfAbsent(propertyName, result);
        }

        return result;
    }
}

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
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.util.Types;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides a base class from which authors may create new FlowElements. The
 * {@link #process(FlowData)} method deals with {@link FlowData#isStopped()} and
 * any errors that may be thrown, and calls the abstract
 * {@link #processInternal(FlowData)} method to do the actual processing.
 * <p>
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#flow-element">Specification</a>
 *
 * @param <TData>     the type of element data that the flow element will write to
 *                    {@link FlowData}
 * @param <TProperty> the type of meta data that the flow element will supply
 *                    about the properties it populates.
 */
public abstract class FlowElementBase<
        TData extends ElementData,
        TProperty extends ElementPropertyMetaData>
        implements FlowElement<TData, TProperty> {

    protected final Logger logger;
    protected TypedKey<TData> typedKey = null;
    private boolean closed = false;
    private final List<Pipeline> pipelines = new ArrayList<>();
    private final DataFactory<TData> dataFactory;

    /**
     * Construct a new instance of the {@link FlowElement}.
     *
     * @param logger             logger instance to use for logging
     * @param elementDataFactory the factory to use when creating a
     *                           {@link ElementDataBase} instance
     */
    public FlowElementBase(
            Logger logger,
            ElementDataFactory<TData> elementDataFactory) {
        this.logger = logger;
        logger.debug("FlowElement '" + getClass().getSimpleName() + "'-'" +
                hashCode() + "' created.");
        this.dataFactory = new DataFactoryInternal<>(elementDataFactory, this);
    }

    /**
     * Abstract method to be overridden by a FlowElement author. This is the
     * where the main processing happens, and is called by the
     * {@link #process(FlowData)} method of the base class.
     *
     * @param data containing evidence to process
     * @throws Exception to be caught by {@link #process(FlowData)}
     */
    protected abstract void processInternal(FlowData data) throws Exception;

    @Override
    public void addPipeline(Pipeline pipeline) {
        pipelines.add(pipeline);
    }

    /**
     * Get a unmodifiable list of the {@link Pipeline}s that this element has
     * been added to.
     *
     * @return list of {@link Pipeline}s
     */
    public List<Pipeline> getPipelines() {
        return Collections.unmodifiableList(pipelines);
    }

    @Override
    public abstract String getElementDataKey();

    @Override
    public abstract EvidenceKeyFilter getEvidenceKeyFilter();

    @Override
    public abstract List<TProperty> getProperties();

    @Override
    public TProperty getProperty(String name) {
        for (TProperty property : getProperties()) {
            if (property.getName().equalsIgnoreCase(name)) {
                return property;
            }
        }
        return null;
    }

    @Override
    public TypedKey<TData> getTypedDataKey() {
        if (typedKey == null) {
            typedKey = new TypedKeyDefault<>(
                    getElementDataKey(),
                    Types.findSubClassParameterType(
                            this,
                            FlowElementBase.class,
                            0));
        }
        return typedKey;
    }

    @Override
    public void process(FlowData data) throws Exception {
        long startTime = 0;
        if (data.isStopped() == false) {
            logger.debug("FlowElement '{}-{}' started processing.", getClass().getSimpleName(), hashCode());
            startTime = System.currentTimeMillis();
        }
        processInternal(data);

        logger.debug("FlowElement '{}}-{}' finished processing. Elapsed time: {}ms",
                getClass().getSimpleName(), hashCode(), (System.currentTimeMillis() - startTime));

    }


    @Override
    public DataFactory<TData> getDataFactory() {
        if (dataFactory == null) {
            logger.error("Need to specify an elementDataFactory " +
                    "in constructor for '" + getClass().getSimpleName() + "'.");
        }
        return dataFactory;
    }

    @Override
    public boolean isConcurrent() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * Cleanup any managed resources that the element is using
     */
    protected abstract void managedResourcesCleanup();

    /**
     * Cleanup any unmanaged resources that the element is using
     */
    protected abstract void unmanagedResourcesCleanup();

    protected void close(boolean closing) {
        if (closed) {
            logger.debug("FlowElement '{}'-'{}' already closed.", getClass().getSimpleName(), hashCode());
        }
        if (closing) {
            managedResourcesCleanup();
        }
        unmanagedResourcesCleanup();

        closed = true;
        logger.debug("FlowElement '{}-{}' closed.", getClass().getSimpleName(), hashCode());

    }

/*
    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
        if (closed) {
            logger.info("Finalizing '{}'-'{}' but already closed", getClass().getSimpleName(), hashCode());
            super.finalize();
            return;
        }
        logger.warn("FlowElement '" + getClass().getSimpleName() + "'-'" +
            hashCode() + "' finalised. It is recommended that instance " +
            "lifetimes are managed explicitly with a 'try' block or calling " +
            "the close method as part of a 'finally' block.");
        // Do not change this code. Put cleanup code in close(boolean closing) above.
        close(false);

        super.finalize();
    }
*/

    @Override
    public void close() throws Exception {
        close(true);
    }

/**
 * Default implementation of the {@link FlowElement.DataFactory} interface. Uses the
 * {@link ElementDataFactory} the element was constructed with.
 *
 * @param <T> the type of {@link ElementData}
 */
protected static class DataFactoryInternal<T extends ElementData>
        implements DataFactory<T> {
    private final ElementDataFactory<T> elementDataFactory;
    private final FlowElement<T, ?> element;

    /**
     * Construct a new instance for the calling element.
     *
     * @param elementDataFactory factory used to create {@link ElementData}
     * @param element            the element to create the data for
     */
    DataFactoryInternal(
            ElementDataFactory<T> elementDataFactory,
            FlowElement<T, ?> element) {
        this.elementDataFactory = elementDataFactory;
        this.element = element;
    }

    @Override
    public T create(FlowData flowData) {
        return elementDataFactory.create(flowData, element);
    }
}

/**
 * {@link FlowElement.DataFactory} implementation which should be used temporarily with
 * a single {@link ElementData} instance. An instance's
 * {@link #create(FlowData)} method will always return the element data it
 * was constructed with.
 * <p>
 * This is used within the caching mechanism where the data is already known.
 *
 * @param <T> the type of (@link ElementData}
 */
protected static class DataFactorySimple<T extends ElementData> implements DataFactory<T> {
    private final T value;

    /**
     * Create a new instance which returns the data provided.
     *
     * @param value the {@link ElementData} which should be returned by the
     *              {@link #create(FlowData)} method
     */
    public DataFactorySimple(T value) {
        this.value = value;
    }

    @Override
    public T create(FlowData flowData) {
        return value;
    }
}
}

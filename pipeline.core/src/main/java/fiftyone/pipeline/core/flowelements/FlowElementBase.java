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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
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
 * {@link #process} method deals with {@link FlowData#isStopped} and any errors
 * that may be thrown, and calls the abstract {@link #processInternal} method
 * to do the actual processing.
 * <p>
 * <p>This class is primarily of use to creators of FlowElement subclasses, end users of {@link Pipeline}s are
 * unlikely to use this class in its raw form.
 *
 * @param <TData>
 */
public abstract class FlowElementBase<
    TData extends ElementData,
    TProperty extends ElementPropertyMetaData>
    implements FlowElement<TData, TProperty> {

    protected final Logger logger;
    protected TypedKey<TData> typedKey = null;
    private boolean closed = false;
    private List<Pipeline> pipelines = new ArrayList<>();
    private DataFactory<TData> dataFactory;

    public FlowElementBase(Logger logger, ElementDataFactory<TData> elementDataFactory) {
        this.logger = logger;
        logger.info("FlowElement '" + getClass().getSimpleName() + "'-'" +
            hashCode() + "' created.");
        this.dataFactory = new DataFactoryInternal<>(elementDataFactory, this);
    }

    /**
     * Abstract method to be overridden by a FlowElement author. This is the
     * where the main processing happens, and is called by the {@link #process}
     * method of the base class.
     *
     * @param data containing evidence to process
     * @throws Exception to be caught by {@link #process}
     */
    protected abstract void processInternal(FlowData data) throws Exception;

    @Override
    public void addPipeline(Pipeline pipeline) {
        pipelines.add(pipeline);
    }

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
            typedKey = new TypedKeyDefault<>(getElementDataKey(), Types.findSubClassParameterType(this, FlowElementBase.class, 0));
        }
        return typedKey;
    }

    @Override
    public void process(FlowData data) throws Exception {
        long startTime = 0;
        if (data.isStopped() == false) {
            boolean log = logger.isDebugEnabled();
            if (log) {
                logger.debug("FlowElement '" + getClass().getSimpleName() + "-" +
                    hashCode() + "' started processing.");
                startTime = System.currentTimeMillis();
            }
            processInternal(data);
            if (log) {
                logger.debug("FlowElement '" + getClass().getSimpleName() + "-" +
                    hashCode() + "' finished processing. Elapsed time: " +
                    (System.currentTimeMillis() - startTime) + "ms");
            }
        }
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
        if (closed == false) {
            if (closing) {
                managedResourcesCleanup();
            }
            unmanagedResourcesCleanup();

            closed = true;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        logger.warn("FlowElement '" + getClass().getSimpleName() + "'-'" +
            hashCode() + "' finalised. It is recommended that instance " +
            "lifetimes are managed explicitly with a 'try' block or calling " +
            "the close method as part of a 'finally' block.");
        // Do not change this code. Put cleanup code in close(boolean closing) above.
        close(false);

        super.finalize();
    }

    @Override
    public void close() throws Exception {
        logger.info("FlowElement '" + getClass().getSimpleName() + "'-'" +
            hashCode() + "' disposed.");
        close(true);
    }

    protected static class DataFactoryInternal<T extends ElementData> implements DataFactory<T> {
        private ElementDataFactory<T> elementDataFactory;
        private FlowElement<T, ?> element;

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

    protected static class DataFactorySimple<T extends ElementData> implements DataFactory<T> {
        private final T value;

        public DataFactorySimple(T value) {
            this.value = value;
        }

        @Override
        public T create(FlowData flowData) {
            return value;
        }
    }
}

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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.FlowDataFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract base class for all Pipeline builders. The default implementation is
 * {@link PipelineBuilder}.
 * @param <T> the builder type
 */

@SuppressWarnings("rawtypes")
public abstract class PipelineBuilderBase<T extends PipelineBuilderBase> {

    /**
     * The {@link ILoggerFactory} used to create any loggers required by
     * instances being built by the builder.
     */
    protected final ILoggerFactory loggerFactory;

    /**
     * Logger created from {@link #loggerFactory} to allow logging in this
     * builder.
     */
    protected final Logger logger;

    /**
     * List of flow elements to add to the Pipeline.
     */
    protected final List<FlowElement> flowElements = new ArrayList<>();

    protected boolean autoCloseElements = false;

    protected boolean suppressProcessExceptions = false;

    /**
     * Construct a new instance using the default {@link ILoggerFactory}
     * implementation returned by the {@link LoggerFactory#getILoggerFactory()}
     * method.
     */
    public PipelineBuilderBase() {
        this(LoggerFactory.getILoggerFactory());
    }

    /**
     * Construct a new instance.
     * @param loggerFactory the {@link ILoggerFactory} used to create any
     *                      loggers required by instances being built by the
     *                      builder
     */
    public PipelineBuilderBase(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
        this.logger = loggerFactory.getLogger(getClass().getName());
    }

    protected List<FlowElement> getFlowElements() {
        return flowElements;
    }

    /**
     * Build the {@link Pipeline} using the {@link FlowElement}s added.
     * @return new {@link Pipeline} instance
     * @throws Exception if a pipeline could not be constructed
     */
    public Pipeline build() throws Exception {
        onPreBuild();
        return new PipelineDefault(
            loggerFactory.getLogger(Pipeline.class.getName()),
            flowElements,
            getFlowDataFactory(),
            autoCloseElements,
            suppressProcessExceptions);
    }

    /**
     * Add a {@link FlowElement} to the {@link Pipeline} to be run in series.
     * @param element to add
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public T addFlowElement(FlowElement element) {
        if (element.isClosed()) {
            throw new IllegalStateException("The element '" +
                element.getClass().getSimpleName() + "' was closed.");
        }
        flowElements.add(element);
        return (T) this;
    }

    /**
     * Add an array of {@link FlowElement}s to the {@link Pipeline} to be run in
     * parallel. Must contain more than 1 element.
     * @param elements to add
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public T addFlowElementsParallel(FlowElement[] elements) {
        for (FlowElement element : elements) {
            if (element.isClosed()) {
                throw new IllegalStateException("The element '" +
                    element.getClass().getSimpleName() + "' was closed.");
            }
        }
        ParallelElements parallelElements = new ParallelElements(
            loggerFactory.getLogger(ParallelElements.class.getName()),
            Arrays.asList(elements));
        flowElements.add(parallelElements);
        return (T) this;
    }

    /**
     * Configure the {@link Pipeline} to either call dispose on it's child
     * {@link FlowElement}s when it is disposed or not.
     * @param autoClose true if the Pipeline should call dispose on it's child
     *                  elements when it is disposed
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public T setAutoCloseElements(boolean autoClose) {
        this.autoCloseElements = autoClose;
        return (T) this;
    }

    /**
     * Configure the Pipeline to either suppress exceptions added to
     * {@link FlowData#getErrors()} during processing or to throw them as an
     * aggregate exception once processing is complete.
     * .
     * @param suppress if true then Pipeline will suppress exceptions added to
     *                 {@link FlowData#getErrors()}
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public T setSuppressProcessException(boolean suppress) {
        this.suppressProcessExceptions = suppress;
        return (T) this;
    }

    /**
     * Called just before a pipeline is built.
     */
    protected void onPreBuild() {

    }

    /**
     * Get the factory  that will be used by the created {@link Pipeline} to
     * create new {@link FlowData} instances.
     * @return {@link FlowDataFactory}
     */
    FlowDataFactory getFlowDataFactory() {
        return new FlowDataFactoryDefault(loggerFactory);
    }
}

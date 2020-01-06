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

import fiftyone.pipeline.core.data.factories.FlowDataFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class PipelineBuilderBase<T extends PipelineBuilderBase> {

    protected final ILoggerFactory loggerFactory;

    protected final Logger logger;

    protected List<FlowElement> flowElements = new ArrayList<>();

    protected boolean autoCloseElements = false;

    protected boolean suppressProcessExceptions = false;

    public PipelineBuilderBase() {
        this(LoggerFactory.getILoggerFactory());
    }

    public PipelineBuilderBase(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
        this.logger = loggerFactory.getLogger(getClass().getName());
    }

    protected List<FlowElement> getFlowElements() {
        return flowElements;
    }

    /**
     * Build the {@link Pipeline} using the {@link FlowElement}s added.
     *
     * @return new {@link Pipeline} instance
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
     *
     * @param element to add
     * @return this builder
     */
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
     * parallel. Must conatin more than 1 element,
     *
     * @param elements to add
     * @return this builder
     */
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

    public T setAutoCloseElements(boolean autoClose) {
        this.autoCloseElements = autoClose;
        return (T) this;
    }

    public T setSuppressProcessException(boolean suppress) {
        this.suppressProcessExceptions = suppress;
        return (T) this;
    }

    protected void onPreBuild() {

    }

    FlowDataFactory getFlowDataFactory() {
        return new FlowDataFactoryDefault(loggerFactory);
    }
}

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
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * ParallelElements executes it's child {@link FlowElement} objects in parallel.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/advanced-features/parallel-processing.md">Specification</a>
 */
@SuppressWarnings("rawtypes")
class ParallelElements
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    private final ExecutorService threadPool;
    private List<FlowElement> flowElements;

    private EvidenceKeyFilterAggregator evidenceKeyFilter;

    /**
     * Constructor
     * @param logger used for logging
     * @param flowElements the list of {@link FlowElement} instances to execute
     *                    when Process is called
     */
    ParallelElements(Logger logger, List<FlowElement> flowElements) {
        super(logger, null);
        threadPool = Executors.newCachedThreadPool();
        this.flowElements = flowElements;
    }

    /**
     * Get an unmodifiable list of the child {@link FlowElement} instances.
     * @return child {@link FlowElement}s
     */
    List<FlowElement> getFlowElements() {
        return Collections.unmodifiableList(flowElements);
    }

    @Override
    protected void processInternal(FlowData flowData) throws Exception {
        List<FlowElementCallable> parallelCallers = new ArrayList<>(flowElements.size());
        for (FlowElement element : flowElements) {
            parallelCallers.add(new FlowElementCallable(element, flowData));
        }
        try {
            List<Future<FlowError>> results = threadPool.invokeAll(parallelCallers);
            // all threads are now done
            for (Future<FlowError> future : results) {
                FlowError error;
                if ((error = future.get()) != null) {
                    flowData.addError(error);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            flowData.addError(e, this);
        }
    }

    @Override
    public String getElementDataKey() {
        // This is because ParallelElements instances cannot translate
        // FlowData to a single ElementData (because they contain
        // multiple elements and each element could have it's own data)
        throw new UnsupportedOperationException();
    }

    /**
     * Get a filter that will only include the evidence keys that can be used by
     * at least one {@link FlowElement} within this pipeline.
     * @return evidence key filter
     */
    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        if (evidenceKeyFilter == null) {
            evidenceKeyFilter = new EvidenceKeyFilterAggregator();
            for (FlowElement element : flowElements) {
                evidenceKeyFilter.addFilter(element.getEvidenceKeyFilter());
            }
        }
        return evidenceKeyFilter;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConcurrent() {
        return true;
    }

    @Override
    protected void managedResourcesCleanup() {
        for (FlowElement element : flowElements) {
            try {
                element.close();
            } catch (Exception e) {
                // do nothing, we still want to close the others.
            }
        }
        threadPool.shutdown();
        flowElements = null;

    }

    @Override
    protected void unmanagedResourcesCleanup() {

    }

    /**
     * Callable class to wrap a {@link FlowElement} in when running in parallel.
     */
    private static class FlowElementCallable implements Callable<FlowError> {

        private final FlowData _flowData;
        private final FlowElement _element;

        private FlowElementCallable(FlowElement element, FlowData flowData) {
            _element = element;
            _flowData = flowData;
        }

        /**
         * Call the {@link FlowElement#process} method and return, catching any
         * errors and returning a FlowError on failure.
         * @return null on success, FlowError on failure
         */
        @Override
        public FlowError call() {
            try {
                _element.process(_flowData);
            } catch (Throwable e) {
                return new FlowError.Default(e, _element);
            }
            return null;
        }
    }
}
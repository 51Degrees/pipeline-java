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

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.core.services.PipelineService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Helper class that allows an IPipeline instance to be used in the creation of
 * a {@link TestFlowData} instance.
 */
public class TestPipeline {
    private class PipelineAdapter implements PipelineInternal
    {
        private Pipeline pipeline;

        public PipelineAdapter(Pipeline pipeline) {
            this.pipeline = pipeline;
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return pipeline.getEvidenceKeyFilter();
        }

        @Override
        public boolean isConcurrent() {
            return pipeline.isConcurrent();
        }

        @Override
        public boolean isClosed() {
            return pipeline.isClosed();
        }
        
        @Override
        public void addService(PipelineService service) {
        	 pipeline.addService(service);
        }
        
        @Override
        public boolean addServices(Collection<PipelineService> services) {
        	return pipeline.addServices(services);
        }
        
        @Override
		public List<PipelineService> getServices() {
        	return pipeline.getServices();
		}

        @Override
        @SuppressWarnings("rawtypes")
        public List<FlowElement> getFlowElements() {
            return pipeline.getFlowElements();
        }

        @Override
        public Map<String, Map<String, ElementPropertyMetaData>> getElementAvailableProperties() {
            return pipeline.getElementAvailableProperties();
        }

        @Override
        public FlowData createFlowData() {
            return pipeline.createFlowData();
        }

        @Override
        public void close() throws Exception {
            pipeline.close();
        }

        @Override
        public ElementPropertyMetaData getMetaDataForProperty(String propertyName) throws PipelineDataException {
            if (pipeline instanceof PipelineInternal) {
                return ((PipelineInternal) pipeline).getMetaDataForProperty(propertyName);
            }
            else {
                throw new RuntimeException("Unable to get " +
                    "meta data for property using the TestPipeline class. " +
                    "Either create a real pipeline instance or avoid " +
                    "calling GetMetaDataForProperty().");
            }
        }

        @Override
        public void process(FlowData data) {
            if (pipeline instanceof PipelineInternal) {
                ((PipelineInternal) pipeline).process(data);
            }
            else {
                throw new RuntimeException("Unable to process data " +
                    "using the TestPipeline class. Either create a real " +
                    "pipeline instance or avoid calling Process().");
            }
        }

        @Override
        @SuppressWarnings("rawtypes")
        public <T extends FlowElement> T getElement(Class<T> type) {
            return pipeline.getElement(type);
        }
    }

    public TestPipeline(Pipeline pipeline) {
        internalPipeline = new PipelineAdapter(pipeline);
    }

    public PipelineInternal internalPipeline;
}

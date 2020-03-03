package fiftyone.pipeline.core.services;

import fiftyone.pipeline.core.flowelements.PipelineBuilder;

/**
 * Service interface used by {@link PipelineBuilder} to hand out services to
 * elements which required them. This is extended by any service which wants
 * to have the same instance used by all elements in a pipeline which require
 * it. All required services should be added to the pipeline builder, and
 * included as a constructor parameter to any element builders which required
 * it to build their element.
 *
 * For example, the DataUpdateService can be added to the pipeline builder.
 * Then, when building from configuration, the builder will automatically hand
 * this the update service out to any on-premise elements which require it.
 */
public interface PipelineService {
}

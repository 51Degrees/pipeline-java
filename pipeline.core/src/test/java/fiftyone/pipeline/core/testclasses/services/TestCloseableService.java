package fiftyone.pipeline.core.testclasses.services;

import java.io.Closeable;

import fiftyone.pipeline.core.services.PipelineService;

public interface TestCloseableService extends PipelineService, Closeable {

}

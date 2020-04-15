package fiftyone.pipeline.core.testclasses.flowelements;

import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.core.testclasses.services.TestService;
import org.slf4j.ILoggerFactory;

@ElementBuilder
public class ElementRequiringServiceBuilder {
    private final ILoggerFactory loggerFactory;
    private final TestService service;

    public ElementRequiringServiceBuilder(ILoggerFactory loggerFactory, TestService service) {
        this.loggerFactory = loggerFactory;
        this.service = service;
    }

    public ElementRequiringService build() {
        return new ElementRequiringService(
            loggerFactory.getLogger(ElementRequiringService.class.getSimpleName()),
            service);
    }
}

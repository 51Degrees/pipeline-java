package fiftyone.pipeline.core.testclasses.flowelements;

import fiftyone.pipeline.core.testclasses.services.TestService;
import org.slf4j.Logger;

public class ElementRequiringService extends MultiplyByFiveElement {

    private final TestService service;

    public ElementRequiringService(Logger logger, TestService service) {
        super(logger);
        this.service = service;
    }

    public TestService getService() {
        return service;
    }
}

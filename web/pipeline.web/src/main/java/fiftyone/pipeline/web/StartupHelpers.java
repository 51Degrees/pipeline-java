package fiftyone.pipeline.web;

import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;

public class StartupHelpers {

    private static int getElementIndex(
        PipelineOptions options,
        String name) {
        int index = 0;
        for (ElementOptions element : options.elements) {
            if (element.builderName.toLowerCase().contains(name.toLowerCase())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static Pipeline buildFromConfiguration(
        PipelineBuilder builder,
        PipelineOptions options,
        boolean clientSideEvidenceEnabled) throws Exception {

        if (options == null ||
            options.elements == null ||
            options.elements.size() == 0) {
            throw new PipelineConfigurationException(
                    "Could not find pipeline configuration information");
        }

        // Add the sequence element.
        if (getElementIndex(
            options,
            SequenceElement.class.getSimpleName()) == -1) {
            // The sequence element is not included so add it.
            ElementOptions sequenceElement = new ElementOptions();
            sequenceElement.builderName = SequenceElement.class.getSimpleName();
            options.elements.add(0, sequenceElement);
        }

        // TODO - client size option should ideally be added to the same configuration.
        if (clientSideEvidenceEnabled) {
            int jsIndex = getElementIndex(
                options,
                JavaScriptBuilderElement.class.getSimpleName());

            // Client-side evidence is enabled so make sure the
            // JsonBuilderElement and JavaScriptBuilderElement has been
            // included.
            if (getElementIndex(
                options,
                JsonBuilderElement.class.getSimpleName()) == -1) {
                // The json builder element is not included so add it.
                ElementOptions jsonBuilderElement = new ElementOptions();
                jsonBuilderElement.builderName = JsonBuilderElement.class.getSimpleName();
                if (jsIndex >= 0) {
                    // There is already a javascript builder element
                    // so insert the json builder before it.
                    options.elements.add(jsIndex, jsonBuilderElement);
                }
                else {
                    options.elements.add(jsonBuilderElement);
                }
            }

            if (jsIndex == -1) {
                // The json builder element is not included so add it.
                ElementOptions javaScriptBuilderElement = new ElementOptions();
                javaScriptBuilderElement.builderName = JavaScriptBuilderElement.class.getSimpleName();
                options.pipelineBuilderParameters.put("EnableCookies", true);
                options.elements.add(javaScriptBuilderElement);
            }
        }

        return builder.buildFromConfiguration(options);
    }
}

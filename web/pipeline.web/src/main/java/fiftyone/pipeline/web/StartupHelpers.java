package fiftyone.pipeline.web;

import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;

/**
 * Static methods used on server startup to configure and build the Pipeline and
 * services needed.
 */
public class StartupHelpers {

    /**
     * Get the index in the {@link PipelineOptions#elements} of the element with
     * the name required.
     * @param options the options to search
     * @param name the name to search for
     * @return the zero based index or -1 if not found
     */
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

    /**
     * Configure the extra web elements required and build the Pipeline using
     * the {@link PipelineBuilder#buildFromConfiguration(PipelineOptions)}
     * method.
     * @param builder to build the Pipeline
     * @param options to build the Pipeline with
     * @param clientSideEvidenceEnabled true if client-side evidence is enabled
     *                                  in the configuration. This will add JSON
     *                                  and JavaScript elements to the Pipeline
     * @return new {@link Pipeline} instance
     * @throws Exception if there was an error building the Pipeline
     */
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
                    jsIndex++;
                }
                else {
                    options.elements.add(jsonBuilderElement);
                }
            }

            if (jsIndex == -1) {
                // The json builder element is not included so add it.
                ElementOptions javaScriptBuilderElement = new ElementOptions();
                javaScriptBuilderElement.builderName = JavaScriptBuilderElement.class.getSimpleName();
                options.pipelineBuilderParameters.put("EndPoint", "/51dpipeline/json");
                options.elements.add(javaScriptBuilderElement);
            }
            else {
                // There is already a JavaScript builder config so check if
                // the endpoint is specified. If not, add it.
                if (options.elements.get(jsIndex).buildParameters.containsKey("EndPoint") == false) {
                    options.elements.get(jsIndex).buildParameters.put("EndPont", "/51dpipeline/json");
                }
            }

        }

        return builder.buildFromConfiguration(options);
    }
}

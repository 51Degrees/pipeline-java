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

package pipeline.developerexamples.usagesharing;

import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

/**
 * @example usagesharing/Main.java
 *
 * @include{doc} example-usage-sharing-intro.txt
 * 
 * Usage sharing is enabled by default if using a pipeline builder 
 * that is derived from FiftyOnePipelineBuilder.
 * For instance, the DeviceDetectionPipelineBuilder.
 * In this example, we show how to add a ShareUsageElement to a 
 * Pipeline using configuration.
 * 
 * As with all ElementBuilders, this can also be handled in code, 
 * using the ShareUsageBuilder. The commented section in the example 
 * demonstrates this.
 * 
 * The 51d.xml file contains all the configuration options.
 * These are all optional, so each can be omitted if the default 
 * for that option is sufficient:
 * 
 * @include pipeline.developer-examples.usage-sharing/src/main/resources/51d.xml
 * 
 * For details of what each setting does, see the reference documentation 
 * for the [share usage builder](http://51degrees.com/pipeline-java/4.3/classfiftyone_1_1pipeline_1_1engines_1_1fiftyone_1_1flowelements_1_1_share_usage_builder_base.html) 
 * 
 * This example is available in full on [GitHub](https://github.com/51Degrees/pipeline-java/blob/master/pipeline.developer-examples/pipeline.developer-examples.usage-sharing/src/main/java/pipeline/developerexamples/usagesharing/Main.java).
 * 
 * Expected output:
 * ```
 * Constructing pipeline from configuration file.
 * 
 * Pipeline created with share usage element. Evidence processed 
 * with this pipeline will now be shared with 51Degrees using the 
 * specified configuration.
 * ```
 */

public class Main {
    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();
        
    // The HTTP service that will be used to send usage
    // data back to 51Degrees.
    private static final HttpClient http = new HttpClientDefault();

    public static class Example {
        public void run() throws Exception {
            System.out.println("Constructing pipeline from configuration file.");
            System.out.println();

            // Create the configuration object from an XML file
            PipelineOptions options = PipelineOptionsFactory.getOptionsFromFile(
                    new File(Objects.requireNonNull(getClass().getClassLoader().
                            getResource("51d.xml")).getFile()));

            // Build a new Pipeline from the configuration.
            Pipeline pipeline = new PipelineBuilder()
                .addService(http)
                .buildFromConfiguration(options);
                
            // Alternatively, the commented code below shows how to
            // configure the ShareUsageElement in code, rather than
            // using a configuration file.
            //ShareUsageElement usageElement = new ShareUsageBuilder(loggerFactory, http)
            //    .setSharePercentage(0.01)
            //    .setMinimumEntriesPerMessage(2500)
            //    .build();
            //Pipeline pipeline = new PipelineBuilder()
            //    .addFlowElement(usageElement)
            //    .build();

            System.out.println(
                "Pipeline created with share usage element. Evidence processed " + 
                "with this pipeline will now be periodically shared with " +
                "51Degrees using the specified configuration.");
        }
    }

    public static void main(String[] args) throws Exception {
        new Example().run();
    }
}

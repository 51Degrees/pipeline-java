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

package pipeline.developerexamples.flowelement;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import pipeline.developerexamples.flowelement.flowelements.SimpleFlowElement;
import pipeline.developerexamples.flowelement.flowelements.SimpleFlowElementBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Main {
    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    public static class Example {
        public void run() throws Exception {

//! [usage]
            SimpleFlowElement starSignElement =
                new SimpleFlowElementBuilder(loggerFactory)
                    .build();

            Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(starSignElement)
                .build();
            Calendar dob = Calendar.getInstance();
            dob.set(1992, Calendar.DECEMBER, 18);

            try (FlowData flowData = pipeline.createFlowData()) {
	            flowData
	                .addEvidence("date-of-birth", dob.getTime())
	                .process();
	
	            System.out.println("With a date of birth of " +
	                new SimpleDateFormat("yyyy/MM/dd").format(dob.getTime()) +
	                ", your star sign is " +
	                flowData.getFromElement(starSignElement).getStarSign() + 
	                ".");
            }
//! [usage]
        }
    }

    public static void main(String[] args) throws Exception {
        new Example().run();
    }
}

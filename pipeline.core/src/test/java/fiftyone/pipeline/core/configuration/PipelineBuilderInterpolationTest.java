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

package fiftyone.pipeline.core.configuration;

import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.core.testclasses.flowelements.ListSplitterElement;
import fiftyone.pipeline.util.FileFinder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PipelineBuilderInterpolationTest {

    PipelineOptions options;

    @Test
    public void interpolationTest() throws Exception {
        options =  PipelineOptionsFactory.getOptionsFromFile(FileFinder.getFilePath(
                "options-test2.xml"));

        assertTrue(options.find("ListSplitterElement", "Delimiter")
                .contains("${TestDelimiter:-,}"));
        // default values
        System.clearProperty("TestMaxLength");
        System.clearProperty("TestDelimiter");
        try (Pipeline pipeline = new PipelineBuilder().buildFromConfiguration(options)) {
            ListSplitterElement lse = pipeline.getElement(ListSplitterElement.class);
            assertEquals(80,lse.getMaxLength());
            assertEquals(",", lse.getDelimiter());
        }
        // set values from properties
        System.setProperty("TestDelimiter", "::::");
        System.setProperty("TestMaxLength", "120");
        try (Pipeline pipeline = new PipelineBuilder().buildFromConfiguration(options)) {
            ListSplitterElement lse = pipeline.getElement(ListSplitterElement.class);
            assertEquals(120,lse.getMaxLength());
            assertEquals("::::", lse.getDelimiter());
        }
    }
}

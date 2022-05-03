package fiftyone.pipeline.core.configuration;

import fiftyone.pipeline.core.flowelements.BuilderClassPathTestRunner;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.core.testclasses.flowelements.ListSplitterElement;
import fiftyone.pipeline.util.FileFinder;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(BuilderClassPathTestRunner.class)
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

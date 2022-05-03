package fiftyone.pipeline.core.configuration;

import org.junit.Assert;
import org.junit.Test;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OptionsTest {
    
    @Test
    public void findTest() throws Exception {
        PipelineOptions options = PipelineOptionsFactory.getOptionsFromFile(
                getFilePath("options-test.xml"));
        assertEquals("!!YOUR_RESOURCE_KEY!!", options.find("CloudRequestEngine", "ResourceKey"));
    }

    @Test
    public void replaceTest() throws Exception {
        PipelineOptions options = PipelineOptionsFactory.getOptionsFromFile(
                getFilePath("options-test.xml"));
        Assert.assertTrue( options.replace("CloudRequestEngine", "ResourceKey", "something"));
        assertEquals("something", options.find("CloudRequestEngine", "ResourceKey"));
    }
    @Test
    public void findNotTest() throws Exception {
        PipelineOptions options = PipelineOptionsFactory.getOptionsFromFile(
                getFilePath("options-test.xml"));
        assertNull(options.find("CloudRequestEngine", "pink"));
        assertNull(options.find("yellow", "pink"));
    }

    @Test
    public void replaceNotTest() throws Exception {
        PipelineOptions options = PipelineOptionsFactory.getOptionsFromFile(
                getFilePath("options-test.xml"));
        Assert.assertFalse( options.replace("black", "blue", "something"));
        assertEquals("!!YOUR_RESOURCE_KEY!!", options.find("CloudRequestEngine", "ResourceKey"));
    }

    @Test
    public void findAndSubstituteTest() throws Exception {
        PipelineOptions options = PipelineOptionsFactory.getOptionsFromFile(
                getFilePath("options-test2.xml"));
        System.clearProperty("TestMaxLength");
        assertEquals("${TestMaxLength:-80}", options.find("ListSplitterElement", "MaxLength"));
        assertEquals("80", options.findAndSubstitute("ListSplitterElement", "MaxLength"));
        System.setProperty("TestMaxLength", "120");
        assertEquals("120", options.findAndSubstitute("ListSplitterElement", "MaxLength"));
    }

}

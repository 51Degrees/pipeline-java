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

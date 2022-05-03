package fiftyone.pipeline.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fiftyone.pipeline.util.FileFinder.*;

public class FileFinderTest {
    Logger logger = LoggerFactory.getLogger(FileFinder.class);
    static final String TEST_FILE = "filefinder-test.xml";

    /**
     * non existent
     **/
    @Test(expected = Exception.class)
    public void testGetFilePathNonExist() {
        getFilePath("xyzw");
    }

    /**
     * exists
     **/
    @Test()
    public void testGetFilePathExist() {
        File f = getFilePath(TEST_FILE);
        logger.info(f.getAbsolutePath());
        Assert.assertTrue(TEST_FILE.endsWith(f.getName()));
    }

}
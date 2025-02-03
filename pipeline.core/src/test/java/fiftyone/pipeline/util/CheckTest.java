/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2025 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.util;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class CheckTest {

    @Test
    public void notNullOrBlank() {
        assertFalse(Check.notNullOrBlank(null));
        assertFalse(Check.notNullOrBlank(""));
        assertFalse(Check.notNullOrBlank("  "));
        assertTrue(Check.notNullOrBlank("hello"));
    }

    @Test
    public void isNullOrBlank() {
        assertTrue(Check.isNullOrBlank(null));
        assertTrue(Check.isNullOrBlank(""));
        assertTrue(Check.isNullOrBlank("  "));
        assertFalse(Check.isNullOrBlank("hello"));
    }

    @Test
    public void fileExists() throws IOException {
        assertFalse(Check.fileExists("mumbo-jumbo"));
        Path p = Files.createTempFile("test",".check");
        assertTrue(Check.fileExists(p.toAbsolutePath().toString()));
        assertTrue(p.toFile().delete());
    }
}
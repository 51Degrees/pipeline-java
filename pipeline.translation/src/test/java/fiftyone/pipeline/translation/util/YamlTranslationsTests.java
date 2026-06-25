/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.translation.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YamlTranslationsTests {

    @Test
    public void parsesFlatMapPreservingOrder() {
        String yaml = "AD: Andorra\nAE: United Arab Emirates\nAF: Afghanistan\n";
        Map<String, String> result = YamlTranslations.parse(yaml);
        assertEquals(Arrays.asList("AD", "AE", "AF"),
            new ArrayList<>(result.keySet()));
        assertEquals("United Arab Emirates", result.get("AE"));
    }

    @Test
    public void skipsBlankAndCommentLines() {
        String yaml = "# comment\n\nAD: Andorra\n\n# another\nAE: Emirates\n";
        Map<String, String> result = YamlTranslations.parse(yaml);
        assertEquals(2, result.size());
        assertEquals("Andorra", result.get("AD"));
    }

    @Test
    public void preservesUnicodeValues() {
        String yaml = "Algeria: Algérie\nUkraine: Україна\n";
        Map<String, String> result = YamlTranslations.parse(yaml);
        assertEquals("Algérie", result.get("Algeria"));
        assertEquals("Україна",
            result.get("Ukraine"));
    }

    @Test
    public void splitsOnFirstColonOnly() {
        // Keys with no colon; values never contain a colon in the data, but
        // ensure only the first separator is used.
        String yaml = "Name: Value: With Colon\n";
        Map<String, String> result = YamlTranslations.parse(yaml);
        assertEquals("Value: With Colon", result.get("Name"));
    }

    @Test
    public void toleratesCarriageReturns() {
        String yaml = "AD: Andorra\r\nAE: Emirates\r\n";
        Map<String, String> result = YamlTranslations.parse(yaml);
        assertEquals("Andorra", result.get("AD"));
        assertEquals("Emirates", result.get("AE"));
    }
}

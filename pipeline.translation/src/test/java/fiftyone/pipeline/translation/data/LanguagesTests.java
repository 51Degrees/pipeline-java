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

package fiftyone.pipeline.translation.data;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LanguagesTests {

    private static final List<String> AVAILABLE =
        Arrays.asList("de_DE", "es_ES", "fr_FR", "it_IT");

    @Test
    public void parseOrdersByQualityDescending() {
        List<String> parsed =
            Languages.parseAcceptLanguage("es,de-DE;q=0.8,en;q=0.5");
        assertEquals(Arrays.asList("es", "de_DE", "en"), parsed);
    }

    @Test
    public void parseNormalisesDashToUnderscore() {
        List<String> parsed = Languages.parseAcceptLanguage("fr-FR");
        assertEquals(Arrays.asList("fr_FR"), parsed);
    }

    @Test
    public void resolvesExactMatch() {
        assertEquals("fr_FR",
            Languages.resolveLocale("fr_FR", AVAILABLE, "en"));
    }

    @Test
    public void resolvesTwoLetterFallback() {
        assertEquals("fr_FR",
            Languages.resolveLocale("fr", AVAILABLE, "en"));
    }

    @Test
    public void dashResolvesSameAsUnderscore() {
        assertEquals("fr_FR",
            Languages.resolveLocale("fr-FR", AVAILABLE, "en"));
    }

    @Test
    public void englishShortCircuitsToNoMatch() {
        assertNull(Languages.resolveLocale(
            "en-GB,fr;q=0.5", AVAILABLE, "en"));
    }

    @Test
    public void englishPreferredOverLowerPriorityLanguage() {
        assertNull(Languages.resolveLocale(
            "en-US,en;q=0.9,de-DE;q=0.5", AVAILABLE, "en"));
    }

    @Test
    public void higherPriorityLanguageMatchedFirst() {
        assertEquals("es_ES", Languages.resolveLocale(
            "es,de-DE;q=0.8,fr;q=0.5", AVAILABLE, "en"));
    }

    @Test
    public void matchReturnsTranslatorAndLocale() {
        Languages languages = new Languages();
        languages.addLanguage("fr_FR",
            new Translator(MissingTranslationBehavior.ORIGINAL));
        Languages.Match match = languages.match("fr-FR");
        assertEquals("fr_FR", match.getLocale());
    }

    @Test
    public void noMatchReturnsNull() {
        Languages languages = new Languages();
        languages.addLanguage("fr_FR",
            new Translator(MissingTranslationBehavior.ORIGINAL));
        assertNull(languages.match("zz-ZZ"));
    }
}

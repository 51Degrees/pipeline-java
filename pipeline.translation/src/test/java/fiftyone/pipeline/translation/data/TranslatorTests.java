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

import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WeightedValue;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TranslatorTests {

    private static Map<String, String> map() {
        Map<String, String> m = new HashMap<>();
        m.put("France", "La France");
        m.put("Germany", "Allemagne");
        return m;
    }

    @Test
    public void translatesString() {
        Translator translator =
            new Translator(map(), MissingTranslationBehavior.ORIGINAL);
        assertEquals("La France",
            translator.translate("France", new ArrayList<>()));
    }

    @Test
    public void lookupIsCaseInsensitive() {
        Translator translator =
            new Translator(map(), MissingTranslationBehavior.ORIGINAL);
        assertEquals("La France",
            translator.translate("france", new ArrayList<>()));
    }

    @Test
    public void missingValueOriginal() {
        Translator translator =
            new Translator(map(), MissingTranslationBehavior.ORIGINAL);
        assertEquals("Spain",
            translator.translate("Spain", new ArrayList<>()));
    }

    @Test
    public void missingValueEmptyString() {
        Translator translator =
            new Translator(map(), MissingTranslationBehavior.EMPTY_STRING);
        assertEquals("",
            translator.translate("Spain", new ArrayList<>()));
    }

    @Test
    public void missingValueFlowError() {
        Translator translator =
            new Translator(map(), MissingTranslationBehavior.FLOW_ERROR);
        List<Exception> errors = new ArrayList<>();
        Object result = translator.translate("Spain", errors);
        assertNull(result);
        assertEquals(1, errors.size());
    }

    @Test
    public void translatesPlainStringList() {
        Translator translator =
            new Translator(map(), MissingTranslationBehavior.ORIGINAL);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) translator.translate(
            Arrays.asList("France", "Germany", "Spain"), new ArrayList<>());
        assertEquals(Arrays.asList("La France", "Allemagne", "Spain"), result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void translatesWeightedListPreservingWeights() {
        Translator translator =
            new Translator(map(), MissingTranslationBehavior.ORIGINAL);

        List<IWeightedValue<String>> input = new ArrayList<>();
        input.add(new WeightedValue<>(30000, "France"));
        input.add(new WeightedValue<>(35535, "Germany"));
        AspectPropertyValue<List<IWeightedValue<String>>> wrapped =
            new AspectPropertyValueDefault<>(input);

        AspectPropertyValue<List<IWeightedValue<String>>> result =
            (AspectPropertyValue<List<IWeightedValue<String>>>)
                translator.translate(wrapped, new ArrayList<>());

        assertTrue(result.hasValue());
        List<IWeightedValue<String>> values = result.getValue();
        assertEquals("La France", values.get(0).getValue());
        assertEquals(30000, values.get(0).getRawWeighting());
        assertEquals("Allemagne", values.get(1).getValue());
        assertEquals(35535, values.get(1).getRawWeighting());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void copiesNoValueMessage() {
        Translator translator =
            new Translator(map(), MissingTranslationBehavior.ORIGINAL);
        AspectPropertyValueDefault<List<IWeightedValue<String>>> empty =
            new AspectPropertyValueDefault<>();
        empty.setNoValueMessage("nothing here");

        AspectPropertyValue<List<IWeightedValue<String>>> result =
            (AspectPropertyValue<List<IWeightedValue<String>>>)
                translator.translate(empty, new ArrayList<>());

        assertEquals(false, result.hasValue());
        assertEquals("nothing here", result.getNoValueMessage());
    }
}

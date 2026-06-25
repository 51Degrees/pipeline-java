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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

/**
 * Translator used to translate values based on a set of translations. The
 * translator supports translating various string based types. The result is
 * the same type as the source e.g. a string will be translated to a string, a
 * list of strings will be translated to a list of strings, a weighted list of
 * strings to a weighted list of strings (preserving each item's weight), etc.
 * <p>
 * For {@link AspectPropertyValue} types, if the value has no value then the no
 * value message is copied to the result.
 */
public class Translator {

    /**
     * Internal translation lookup, keyed case-insensitively.
     */
    private final Map<String, String> translations;

    /**
     * The behaviour to use when a translation is missing.
     */
    private final MissingTranslationBehavior behavior;

    /**
     * Construct a translator with no translations configured. Every value will
     * use the missing-translation behaviour.
     * @param behavior the behaviour to use when a translation is missing
     */
    public Translator(MissingTranslationBehavior behavior) {
        this(null, behavior);
    }

    /**
     * Construct a translator.
     * @param translations the translations to use. The key is the source value
     *                     and the value is the translated value. May be null
     *                     for an empty translator.
     * @param behavior the behaviour to use when a translation is missing
     */
    public Translator(
        Map<String, String> translations,
        MissingTranslationBehavior behavior) {
        this.translations = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (translations != null) {
            this.translations.putAll(translations);
        }
        this.behavior = behavior;
    }

    /**
     * Translate the value to the language this translator is configured for.
     * Supports a {@link String}, a {@link List} of strings, a {@link List} of
     * {@link IWeightedValue} of strings, and {@link AspectPropertyValue}
     * wrappers around any of those. The result has the same shape as the
     * input.
     * @param value the value to translate
     * @param errors list to which errors encountered during translation are
     *               added
     * @return the translated value, with the same type as the source
     */
    public Object translate(Object value, List<Exception> errors) {
        if (value instanceof String) {
            return translateString((String) value, errors);
        }
        if (value instanceof AspectPropertyValue) {
            return translateAspect((AspectPropertyValue<?>) value, errors);
        }
        if (value instanceof List) {
            return translateList((List<?>) value, errors);
        }
        throw new UnsupportedOperationException("The value type '" +
            (value == null ? "null" : value.getClass().getName()) +
            "' is not supported for translation.");
    }

    /**
     * Translate a single string value, applying the missing-translation
     * behaviour when no (non-blank) translation exists.
     */
    private String translateString(String value, List<Exception> errors) {
        if (value != null) {
            String result = translations.get(value);
            if (result != null && result.trim().isEmpty() == false) {
                return result;
            }
        }
        switch (behavior) {
            case EMPTY_STRING:
                return "";
            case FLOW_ERROR:
                errors.add(new NoSuchElementException(
                    "There was no translation found for the value '" +
                    value + "'."));
                return null;
            case ORIGINAL:
            default:
                return value;
        }
    }

    /**
     * Translate a list of either plain strings or weighted strings. The
     * element type is determined from the first non-null element. Weighted
     * items keep their raw weighting; only the value is translated.
     */
    private Object translateList(List<?> values, List<Exception> errors) {
        Object sample = null;
        for (Object item : values) {
            if (item != null) {
                sample = item;
                break;
            }
        }
        if (sample instanceof IWeightedValue) {
            List<IWeightedValue<String>> result =
                new ArrayList<>(values.size());
            for (Object item : values) {
                @SuppressWarnings("unchecked")
                IWeightedValue<String> weighted = (IWeightedValue<String>) item;
                result.add(new WeightedValue<>(
                    weighted.getRawWeighting(),
                    translateString(weighted.getValue(), errors)));
            }
            return result;
        }
        List<String> result = new ArrayList<>(values.size());
        for (Object item : values) {
            result.add(translateString((String) item, errors));
        }
        return result;
    }

    /**
     * Translate the value wrapped by an {@link AspectPropertyValue}, copying
     * the no-value message when the source has no value.
     */
    private AspectPropertyValue<Object> translateAspect(
        AspectPropertyValue<?> value,
        List<Exception> errors) {
        if (value.hasValue() == false) {
            AspectPropertyValueDefault<Object> result =
                new AspectPropertyValueDefault<>();
            result.setNoValueMessage(value.getNoValueMessage());
            return result;
        }
        Object inner = value.getValue();
        Object translated;
        if (inner instanceof List) {
            translated = translateList((List<?>) inner, errors);
        } else if (inner instanceof String) {
            translated = translateString((String) inner, errors);
        } else {
            throw new UnsupportedOperationException("The value type '" +
                (inner == null ? "null" : inner.getClass().getName()) +
                "' is not supported for translation.");
        }
        return new AspectPropertyValueDefault<>(translated);
    }
}

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

package fiftyone.pipeline.translation.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.Evidence;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.translation.data.ITranslationData;
import fiftyone.pipeline.translation.data.Languages;
import fiftyone.pipeline.translation.data.MissingTranslationBehavior;
import fiftyone.pipeline.translation.data.TranslationProperty;
import fiftyone.pipeline.translation.data.Translator;
import fiftyone.pipeline.translation.util.YamlTranslations;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flow element that translates values from a single source element and stores
 * translated values under its own element data key.
 * <p>
 * Translations are provided as YAML format files, where the file name defines
 * the language contained in the file (e.g. {@code countries.fr_FR.yml}).
 * <p>
 * The language to translate to is determined by looking through the evidence
 * for a key containing a locale code. The keys checked are, in order of
 * precedence, {@code query.translation}, {@code query.accept-language} and
 * {@code header.accept-language}. If a fixed language is supplied to the
 * constructor, it is used instead, regardless of the evidence.
 * <p>
 * Only string based types are supported for translation (string, list of
 * strings, weighted list of strings and the {@code AspectPropertyValue}
 * wrappers); the output type matches the input.
 *
 * @param <T> the type of element data the engine writes
 */
public class TranslationEngineBase<T extends ITranslationData>
    extends FlowElementBase<T, ElementPropertyMetaData> {

    /**
     * The keys, in order of precedence, used to get the locale code for the
     * language to translate to.
     */
    private static final List<String> EVIDENCE_KEYS = Arrays.asList(
        "query.translation",
        "query.accept-language",
        "header.accept-language");

    /**
     * Pattern used to identify locale codes (e.g. "en_GB") in file names and
     * the fixed language.
     */
    private static final Pattern LOCALE_PATTERN =
        Pattern.compile("[a-z]{2}_[A-Z]{2}");

    /**
     * Translator with no translations configured. Used to populate the output
     * properties (with pass-through or no-value content) when there is no
     * target language or translator available.
     */
    private final Translator emptyTranslator;

    private final String fixedLanguage;
    private final EvidenceKeyFilter evidenceKeyFilter;
    private final String sourceElementDataKey;
    private final MissingTranslationBehavior behavior;
    private final List<TranslationProperty> translationProperties;
    private final Languages languages;

    /**
     * The concrete element data type, used to build the typed data key. The
     * generic {@link Class} cannot be recovered by reflection because
     * {@link TranslationEngineBase} passes a type variable up to
     * {@link FlowElementBase}, so it is supplied explicitly.
     */
    private final Class<T> dataType;

    private List<ElementPropertyMetaData> properties;

    /**
     * Create a new translation engine.
     * @param sourceElementDataKey element data key of the source flow element
     * @param translations the translations to execute (source -> destination
     *                     property name pairs)
     * @param sources the YAML translation sources, keyed on file name
     * @param fixedLanguage fixed language to translate to. If set, the engine
     *                     always translates to this language; otherwise the
     *                     language is taken from the evidence
     * @param behavior the behaviour when a translation is missing for a value
     * @param dataType the concrete element data type, used for the typed data
     *                key
     * @param logger logger instance
     * @param elementDataFactory factory used to create the element data
     */
    public TranslationEngineBase(
        String sourceElementDataKey,
        List<TranslationProperty> translations,
        Map<String, String> sources,
        String fixedLanguage,
        MissingTranslationBehavior behavior,
        Class<T> dataType,
        Logger logger,
        ElementDataFactory<T> elementDataFactory) {
        super(logger, elementDataFactory);
        this.dataType = dataType;
        if (translations == null || translations.isEmpty()) {
            throw new IllegalArgumentException(
                "At least one property translation must be configured.");
        }
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException(
                "At least one source file must be configured.");
        }
        if (sourceElementDataKey == null ||
            sourceElementDataKey.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "The source element key must be configured.");
        }

        this.sourceElementDataKey = sourceElementDataKey.trim();
        this.behavior = behavior;
        this.emptyTranslator = new Translator(behavior);
        this.fixedLanguage =
            fixedLanguage != null ? validateLocale(fixedLanguage) : null;
        this.languages = parseSources(sources, behavior);
        this.evidenceKeyFilter = new EvidenceKeyFilterWhitelist(
            EVIDENCE_KEYS, String.CASE_INSENSITIVE_ORDER);
        this.translationProperties = new ArrayList<>(translations);
    }

    /**
     * The translation sources used by this engine, keyed by locale.
     * @return the {@link Languages} instance
     */
    protected Languages getLanguages() {
        return languages;
    }

    @Override
    public String getElementDataKey() {
        return "translation";
    }

    @Override
    public TypedKey<T> getTypedDataKey() {
        if (typedKey == null) {
            typedKey = new TypedKeyDefault<>(getElementDataKey(), dataType);
        }
        return typedKey;
    }

    /**
     * Element data key of the source flow element.
     * @return source element data key
     */
    public String getSourceElementDataKey() {
        return sourceElementDataKey;
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceKeyFilter;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        if (properties == null) {
            List<ElementPropertyMetaData> result = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (TranslationProperty translation : translationProperties) {
                String name = translation.getDestinationProperty();
                if (seen.add(name.toLowerCase())) {
                    result.add(new ElementPropertyMetaDataDefault(
                        name, this, "", Object.class, true));
                }
            }
            properties = result;
        }
        return properties;
    }

    @Override
    protected void processInternal(FlowData data) {
        T translationData = data.getOrAdd(getTypedDataKey(), getDataFactory());

        ElementData sourceData = data.get(sourceElementDataKey);
        if (sourceData == null) {
            data.addError(new NoSuchElementException("The source data '" +
                sourceElementDataKey +
                "' could not be found in the FlowData."), this);
            return;
        }

        String language = getTargetLanguage(data);
        if (language == null) {
            if (behavior == MissingTranslationBehavior.FLOW_ERROR) {
                data.addError(new NoSuchElementException("The evidence did " +
                    "not contain a language to translate to."), this);
            } else {
                populate(sourceData, emptyTranslator, translationData, data);
            }
            return;
        }

        Translator translator = languages.getTranslator(language);
        if (translator == null) {
            if (behavior == MissingTranslationBehavior.FLOW_ERROR) {
                data.addError(new NoSuchElementException("There was no " +
                    "translator configured for the language '" +
                    language + "'."), this);
            } else {
                populate(sourceData, emptyTranslator, translationData, data);
            }
            return;
        }

        populate(sourceData, translator, translationData, data);
    }

    /**
     * Populate the translation data with every configured translation using
     * the provided translator.
     */
    private void populate(
        ElementData sourceData,
        Translator translator,
        T translationData,
        FlowData data) {
        for (TranslationProperty property : translationProperties) {
            Object sourceValue = sourceData.get(property.getSourceProperty());
            if (sourceValue == null) {
                // The source type is unknown here, so use an
                // AspectPropertyValue carrying a no-value message.
                AspectPropertyValueDefault<String> value =
                    new AspectPropertyValueDefault<>();
                value.setNoValueMessage("The source property '" +
                    property.getSourceProperty() +
                    "' could not be found in the source data.");
                translationData.put(property.getDestinationProperty(), value);
            } else {
                List<Exception> errors = new ArrayList<>();
                translationData.put(
                    property.getDestinationProperty(),
                    translator.translate(sourceValue, errors));
                for (Exception error : errors) {
                    data.addError(error, this);
                }
            }
        }
    }

    /**
     * Get the highest precedence locale code from the evidence, or the fixed
     * language if one was configured.
     */
    private String getTargetLanguage(FlowData data) {
        if (fixedLanguage != null) {
            return fixedLanguage;
        }
        if (data == null) {
            return null;
        }
        Evidence evidence = data.getEvidence();
        for (String key : EVIDENCE_KEYS) {
            Object value = evidence.get(key);
            if (value instanceof String &&
                ((String) value).trim().isEmpty() == false) {
                return (String) value;
            }
        }
        return null;
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }

    /**
     * Parse the source files into a {@link Languages} instance containing a
     * {@link Translator} for each file.
     */
    private static Languages parseSources(
        Map<String, String> sources,
        MissingTranslationBehavior behavior) {
        Languages languages = new Languages();
        for (Map.Entry<String, String> source : sources.entrySet()) {
            String language = getLanguageName(source.getKey());
            Map<String, String> translations =
                YamlTranslations.parse(source.getValue());
            languages.addLanguage(
                language, new Translator(translations, behavior));
        }
        return languages;
    }

    /**
     * Get the locale code from a source file name following the
     * {@code name.locale.yml} convention (e.g. "countries.fr_FR.yml").
     */
    private static String getLanguageName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Source name cannot be null or whitespace.");
        }
        String[] parts = name.split("\\.");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Source name '" + name +
                "' does not have the correct format. It should be " +
                "'somename.locale.yml' e.g. 'countries.en_GB.yml'.");
        }
        String locale = validateLocale(parts[parts.length - 2]);
        if (locale == null) {
            throw new IllegalArgumentException("Source name '" + name +
                "' does not contain a valid locale code.");
        }
        return locale;
    }

    /**
     * Return the validated locale code found within the supplied value, or
     * null if none is present.
     */
    private static String validateLocale(String locale) {
        if (locale == null) {
            return null;
        }
        Matcher matcher = LOCALE_PATTERN.matcher(locale);
        return matcher.find() ? matcher.group() : null;
    }
}

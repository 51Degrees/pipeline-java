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

import fiftyone.pipeline.translation.data.MissingTranslationBehavior;
import fiftyone.pipeline.translation.data.TranslationData;
import fiftyone.pipeline.translation.data.TranslationProperty;
import org.slf4j.ILoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for {@link TranslationEngine}.
 */
public class TranslationEngineBuilder {

    private final ILoggerFactory loggerFactory;
    private final List<TranslationProperty> translationProperties =
        new ArrayList<>();
    private final Map<String, String> sources = new LinkedHashMap<>();
    private String sourceElementDataKey;
    private String fixedLanguage = null;
    private MissingTranslationBehavior behavior =
        MissingTranslationBehavior.ORIGINAL;

    /**
     * Construct a new builder.
     * @param loggerFactory the logger factory to use for the engine and the
     *                     element data it creates
     */
    public TranslationEngineBuilder(ILoggerFactory loggerFactory) {
        if (loggerFactory == null) {
            throw new IllegalArgumentException("loggerFactory");
        }
        this.loggerFactory = loggerFactory;
    }

    /**
     * Set the element data key of the source element the engine reads from.
     * @param sourceElementDataKey the source element data key
     * @return this builder
     */
    public TranslationEngineBuilder setSourceElementDataKey(
        String sourceElementDataKey) {
        this.sourceElementDataKey = sourceElementDataKey;
        return this;
    }

    /**
     * Set a fixed language for the engine to translate to. If set, the engine
     * always translates to this language instead of resolving it from the
     * evidence.
     * @param language the language to translate to
     * @return this builder
     */
    public TranslationEngineBuilder setFixedLanguage(String language) {
        this.fixedLanguage = language;
        return this;
    }

    /**
     * Set the behaviour when a translation is missing for a value.
     * @param behavior the missing-translation behaviour
     * @return this builder
     */
    public TranslationEngineBuilder setMissingTranslationBehavior(
        MissingTranslationBehavior behavior) {
        this.behavior = behavior;
        return this;
    }

    /**
     * Add a translation from a source property to a destination property.
     * @param source the source property key to translate
     * @param destination the destination property key to store the translated
     *                   value under on the engine data
     * @return this builder
     */
    public TranslationEngineBuilder addTranslation(
        String source,
        String destination) {
        if (source == null || destination == null) {
            throw new IllegalArgumentException(
                "Source and destination must not be null.");
        }
        translationProperties.add(
            new TranslationProperty(source, destination));
        return this;
    }

    /**
     * Add a translation source by name and content. The name follows the
     * {@code abc.en_GB.yml} convention, where the locale code determines the
     * language contained in the file.
     * @param name the file name
     * @param source the file contents (YAML)
     * @return this builder
     */
    public TranslationEngineBuilder addSource(String name, String source) {
        sources.put(name, source);
        return this;
    }

    /**
     * Add a translation source by reading the supplied file as UTF-8. The file
     * name follows the {@code abc.en_GB.yml} convention.
     * @param file the source file
     * @return this builder
     * @throws IOException if the file cannot be read
     */
    public TranslationEngineBuilder addSource(File file) throws IOException {
        byte[] content = Files.readAllBytes(file.toPath());
        return addSource(
            file.getName(), new String(content, StandardCharsets.UTF_8));
    }

    /**
     * Build a translation engine.
     * @return a new {@link TranslationEngine}
     */
    public TranslationEngine build() {
        return new TranslationEngine(
            sourceElementDataKey,
            translationProperties,
            sources,
            fixedLanguage,
            behavior,
            loggerFactory.getLogger(TranslationEngine.class.getName()),
            (flowData, flowElement) -> new TranslationData(
                loggerFactory.getLogger(TranslationData.class.getName()),
                flowData));
    }
}

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

import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.translation.data.ITranslationData;
import fiftyone.pipeline.translation.data.MissingTranslationBehavior;
import fiftyone.pipeline.translation.data.TranslationProperty;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Concrete translation engine. See {@link TranslationEngineBase} for
 * implementation details.
 */
public class TranslationEngine
    extends TranslationEngineBase<ITranslationData>
    implements ITranslationEngine {

    /**
     * Create a new translation engine.
     * @param sourceElementDataKey element data key of the source flow element
     * @param translations the source -> destination property name pairs
     * @param sources the YAML translation sources, keyed on file name
     * @param fixedLanguage fixed language to translate to, or null to resolve
     *                     from the evidence
     * @param behavior the behaviour when a translation is missing for a value
     * @param logger logger instance
     * @param elementDataFactory factory used to create the element data
     */
    public TranslationEngine(
        String sourceElementDataKey,
        List<TranslationProperty> translations,
        Map<String, String> sources,
        String fixedLanguage,
        MissingTranslationBehavior behavior,
        Logger logger,
        ElementDataFactory<ITranslationData> elementDataFactory) {
        super(
            sourceElementDataKey,
            translations,
            sources,
            fixedLanguage,
            behavior,
            ITranslationData.class,
            logger,
            elementDataFactory);
    }
}

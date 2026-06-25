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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Set of translators for one or more languages, with static utility methods
 * for parsing Accept-Language header values and resolving language tags
 * against the available locales.
 */
public class Languages {

    /**
     * The result of matching a language tag against the available locales: the
     * translator to use and the locale key that was matched.
     */
    public static class Match {

        private final Translator translator;
        private final String locale;

        Match(Translator translator, String locale) {
            this.translator = translator;
            this.locale = locale;
        }

        /**
         * @return the translator for the matched locale
         */
        public Translator getTranslator() {
            return translator;
        }

        /**
         * @return the locale key that was matched (e.g. "fr_FR")
         */
        public String getLocale() {
            return locale;
        }
    }

    /**
     * Internal dictionary of translators keyed case-insensitively by locale.
     */
    private final Map<String, Translator> translators =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Add a language and its translator to the set of languages.
     * @param language locale code for the language e.g. "en_GB", "fr_FR"
     * @param translator translator for the language
     */
    public void addLanguage(String language, Translator translator) {
        if (language == null || translator == null) {
            throw new IllegalArgumentException(
                "Language and translator must not be null.");
        }
        translators.put(language, translator);
    }

    /**
     * Match the supplied language tag (a locale code or a full Accept-Language
     * header value) against the available locales.
     * @param language a locale code (e.g. "fr_FR") or an Accept-Language header
     *                value (e.g. "es,de-DE;q=0.8,en;q=0.5")
     * @return the matched translator and locale, or null if no match was found
     */
    public Match match(String language) {
        String locale = resolveLocale(language, translators.keySet(), "en");
        if (locale == null) {
            return null;
        }
        Translator translator = translators.get(locale);
        if (translator == null) {
            return null;
        }
        return new Match(translator, locale);
    }

    /**
     * Get the translator for the supplied language tag, or null if none of the
     * available locales match.
     * @param language a locale code or an Accept-Language header value
     * @return the matched translator, or null
     */
    public Translator getTranslator(String language) {
        Match m = match(language);
        return m == null ? null : m.getTranslator();
    }

    /**
     * Parse an Accept-Language header value (e.g.
     * "es,de-DE;q=0.8,en;q=0.5") into an ordered list of normalised language
     * tags. Tags are ordered by quality (descending, stable so equal-quality
     * tags keep their original order), with dashes replaced by underscores
     * (e.g. "en-GB" becomes "en_GB").
     * @param acceptLanguage the raw Accept-Language header value
     * @return ordered list of normalised language tags, highest preference
     *         first
     */
    public static List<String> parseAcceptLanguage(String acceptLanguage) {
        List<String> result = new ArrayList<>();
        if (acceptLanguage == null || acceptLanguage.trim().isEmpty()) {
            return result;
        }

        List<Ranking> rankings = new ArrayList<>();
        for (String part : acceptLanguage.split(",")) {
            String[] tokens = part.split(";");
            String value = tokens[0].trim().replace('-', '_');
            if (value.isEmpty()) {
                continue;
            }
            double quality = 1.0;
            for (int i = 1; i < tokens.length; i++) {
                String token = tokens[i].trim();
                if (token.startsWith("q=")) {
                    try {
                        quality = Double.parseDouble(token.substring(2).trim());
                    } catch (NumberFormatException e) {
                        quality = 1.0;
                    }
                }
            }
            rankings.add(new Ranking(value, quality));
        }

        // Stable sort by quality descending preserves the original order for
        // equal-quality tags.
        rankings.sort(new Comparator<Ranking>() {
            @Override
            public int compare(Ranking a, Ranking b) {
                return Double.compare(b.quality, a.quality);
            }
        });

        for (Ranking ranking : rankings) {
            result.add(ranking.value);
        }
        return result;
    }

    /**
     * Resolve an Accept-Language header value against a set of available locale
     * keys, returning the best matching locale. Handles both exact locale
     * matches (e.g. "fr_FR") and 2-character language code fallbacks (e.g.
     * "fr" matching "fr_FR").
     * <p>
     * If the highest-priority candidate's language matches
     * {@code baseLanguage} (e.g. "en"), resolution stops immediately and
     * returns null, since the source values are already in the base language
     * and no translation is needed. This prevents falling through to a
     * lower-priority language.
     * @param acceptLanguage the raw Accept-Language header value
     * @param availableLocales the set of available locale keys
     * @param baseLanguage the 2-char code of the base language the source
     *                     values are already in (e.g. "en"); may be null
     * @return the matched locale key, or null if no match was found
     */
    public static String resolveLocale(
        String acceptLanguage,
        Collection<String> availableLocales,
        String baseLanguage) {
        for (String candidate : parseAcceptLanguage(acceptLanguage)) {
            // Exact match first.
            for (String locale : availableLocales) {
                if (locale.equalsIgnoreCase(candidate)) {
                    return locale;
                }
            }

            // No exact match. If this candidate's language is the base
            // language, the source values are already in that language: stop
            // and return null rather than falling through to a lower-priority
            // language.
            if (baseLanguage != null &&
                startsWithIgnoreCase(candidate, baseLanguage)) {
                return null;
            }

            // 2-char language code fallback (e.g. "fr" matches "fr_FR").
            if (candidate.length() == 2) {
                for (String locale : availableLocales) {
                    if (startsWithIgnoreCase(locale, candidate)) {
                        return locale;
                    }
                }
            }
        }
        return null;
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.length() >= prefix.length() &&
            value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * A parsed Accept-Language entry: the normalised tag and its quality.
     */
    private static class Ranking {

        private final String value;
        private final double quality;

        Ranking(String value, double quality) {
            this.value = value;
            this.quality = quality;
        }
    }
}
